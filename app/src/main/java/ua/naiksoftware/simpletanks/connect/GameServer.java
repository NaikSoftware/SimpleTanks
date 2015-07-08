package ua.naiksoftware.simpletanks.connect;

import android.app.*;
import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.jmdns.*;

import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.User;

public class GameServer extends GameConnection {

    private static final String TAG = GameServer.class.getSimpleName();

    private static final byte END_WAITING = 66;
    public static final byte CONNECT_REQUEST = 1;

    public static final int START_PLAY = 3;
    public static final int CONN_CANCELLED = 4;
    public static final int ADD_USER = 5;
    public static final int REMOVE_USER = 6;
    public static final int PING_CLIENT = 7;

    public static final String KEY_SERVER_NAME = "key_name";

    private JmDNS jmdns;
    private Activity activity;
    private Server server;
    private ClientsListAdapter clientsListAdapter;
    private ArrayList<Client> clientsList; // Клиенты сервера
    private final Object lock = new Object();
    private User myUser; // Игрок, запускающий сервер

    public GameServer(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public void start() {
        final View view = LayoutInflater.from(activity).inflate(R.layout.server_dialog_layout, null);
        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String servName = ((EditText) view.findViewById(R.id.serverName)).getText().toString().trim();
                        if (servName.isEmpty()) {
                            toast(R.string.serv_name_empty_notice);
                        } else { // Все нормально
                            myUser = new User(servName, System.currentTimeMillis(), activity.getString(R.string.owner_server));
                            inBG(new Runnable() {

                                @Override
                                public void run() {
                                    synchronized (lock) {
                                        createNetwork(false);
                                    }
                                }
                            });
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onConnected() {
        final View view = LayoutInflater.from(activity).inflate(R.layout.server_wait_clients_dialog, null);
        clientsList = new ArrayList<Client>();
        clientsListAdapter = new ClientsListAdapter(activity, 0, clientsList);
        ((ListView) view.findViewById(R.id.clientsList)).setAdapter(clientsListAdapter);
        final AlertDialog waitClientsDialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.start_game, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        server.acceptClients();
                    }
                })
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stop();
                    }
                })
                .create();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.select_server_ip)
                .setAdapter(new ArrayAdapter<InetAddress>(activity, android.R.layout.simple_list_item_1, getAvailableAddresses()), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface di, final int which) {
                        waitClientsDialog.show();
                        inBG(new Runnable() {

                            @Override
                            public void run() {
                                synchronized (lock) {
                                    try {
                                        InetAddress addr = getAvailableAddresses()[which];
                                        server = new Server(addr);
                                        server.start();
                                        jmdns = JmDNS.create(addr, HOSTNAME);
                                        ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, HOSTNAME, PORT, "SimpleTanks server on " + android.os.Build.DEVICE);
                                        serviceInfo.setText(new Hashtable<String, String>() {
                                            {
                                                put(KEY_SERVER_NAME, myUser.getName());
                                            }
                                        });
                                        jmdns.registerService(serviceInfo);
                                        Log.i(TAG, "Started GameServer...");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        toast(e.getMessage());
                                    }
                                }
                            }
                        });
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void stop() {
        inBG(new Runnable() {

            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        stopMulticastReceiving();
                        stopMDNS();
                        if (server != null) {
                            server.stop();
                        }
                        for (Client client : clientsList) {
                            client.close();
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                    toast(R.string.server_stopped);
                }
            }
        });
    }

    private void stopMDNS() throws IOException {
        if (jmdns != null) {
            Log.i(TAG, "Stopping GameServer...");
            jmdns.unregisterAllServices();
            jmdns.close();
            jmdns = null;
        }
    }

    /* Поток для приема клиентов сервером */
    private class Server implements Runnable {

        static final long PING_DELAY = 400;//ms

        Thread thread, pingClientThread;
        ServerSocket serverSocket;
        String myIp;

        Server(InetAddress addr) throws IOException {
            myIp = addr.getHostAddress();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(addr, PORT));
        }

        void start() {
            thread = new Thread(this);
            thread.start();
            pingClientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        for (Client client : clientsList) {
                            if (!client.ping()) {
                                clientsList.remove(client);
                                for (Client cli : clientsList) {
                                    DataOutputStream out = cli.getOut();
                                    try {
                                        out.writeInt(REMOVE_USER);
                                        out.writeLong(client.getId());
                                    } catch (IOException e) {
                                        e.printStackTrace();// ignore, handle errors in accept thread
                                    }
                                }
                                updateListView();
                                break;
                            }
                        }
                        try {
                            Thread.sleep(PING_DELAY);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Ping thread interrupted");
                            break;
                        }
                    }
                }
            });
            pingClientThread.start();
        }

        @Override
        public void run() {
            try {
                clientsList.clear();
                while (true) {
                    Socket clientSock = serverSocket.accept();
                    int code = clientSock.getInputStream().read();
                    toast("Client code received " + code);
                    if (code == END_WAITING // Принимаем сообщение конца ожидания только от себя, на всякий случай
                            && clientSock.getInetAddress().getHostAddress().equals(myIp)) {
                        Log.d(TAG, "End waiting clients code detected");
                        toast("End waiting detected");
                        break;
                    } else if (code == CONNECT_REQUEST) { // Кто-то хочет присоединиться к игре
                       acceptNewClient(clientSock);
                    }
                }
                for (Client client : clientsList) {
                    client.startGameRequest();
                }
            } catch (IOException e) {
                Log.e(TAG, "Server exception in accept thread", e);
                if (!serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "Exception in closing server clock", e2);
                    }
                }
            }
        }

        void acceptNewClient(Socket clientSock) throws IOException {
            /* Создаем нового клиента и отсылаем ему данные о владельце сервера */
            Client newClient = new Client(clientSock);
            DataOutputStream out = newClient.getOut();
            out.writeInt(ADD_USER);
            out.writeUTF(myUser.getName());
            out.writeLong(myUser.getId());
            out.writeUTF(myUser.getIp());
            /* Теперь обновляем список юзеров сервера у всех клиентов */
            clientsList.add(newClient);
            for (Client client : clientsList) {
                if (client == newClient) {
                    continue;
                }
                out = client.getOut();
                out.writeInt(ADD_USER);
                out.writeUTF(newClient.getName());
                out.writeLong(newClient.getId());
                out.writeUTF(newClient.getIp());
                /* А новому клиенту шлем остальных */
                out = newClient.getOut();
                out.writeInt(ADD_USER);
                out.writeUTF(client.getName());
                out.writeLong(client.getId());
                out.writeUTF(client.getIp());
            }

            updateListView();
            Log.i(TAG, "Connected new client");
        }



        void updateListView() {
            inUI(new Runnable() {

                @Override
                public void run() {
                    clientsListAdapter.notifyDataSetChanged();
                }
            });
        }

        void acceptClients() {
            inBG(new Runnable() {

                @Override
                public void run() {
                    try {
                        pingClientThread.interrupt();
                        pingClientThread = null;
                        stopMulticastReceiving();
                        stopMDNS();
                        Socket s = new Socket(myIp, PORT);
                        s.getOutputStream().write(END_WAITING);
                        s.getOutputStream().flush();
                        s.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Exception in acceptClients socket", e);
                    }
                }
            });
        }

        void stop() {
            Log.i(TAG, "Stopping Server");
            if (thread != null) {
                thread.interrupt();
            }
            if (pingClientThread != null) {
                pingClientThread.interrupt();
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Server socket close error", e);
            }
        }
    }

    class Client extends User {

        final Socket socket;
        DataInputStream in;
        DataOutputStream out;

        Client(Socket socket) throws IOException {
            super(0);
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setName(in.readUTF());
            setId(in.readLong());
            setIp(socket.getInetAddress().getHostAddress());
            out.writeUTF(myUser.getName() + " default map"); // Отсылаем карту сервера
        }

        public DataOutputStream getOut() {
            return out;
        }

        boolean ping() {
            try {
                out.writeInt(PING_CLIENT);
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        void startGameRequest() throws IOException {
            out.writeInt(START_PLAY);
        }

        void close() throws IOException {
            socket.close();
        }
    }

    @Override
    public ArrayList<Client> getUsers() {
        return clientsList;
    }

    @Override
    public User getMyUser() {
        return myUser;
    }
}
