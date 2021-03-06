package ua.naiksoftware.simpletanks.network.starter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import ua.naiksoftware.simpletanks.GameView;
import ua.naiksoftware.simpletanks.MainActivity;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.drawable.GameMap;
import ua.naiksoftware.simpletanks.drawable.User;
import ua.naiksoftware.simpletanks.holders.ServerGameHolder;
import ua.naiksoftware.simpletanks.network.ClientsListAdapter;

/**
 * Запускает сервер, подключает клиентов и стартует игру.
 */
public class GameServer extends SinglePlayer {

    private static final String TAG = GameServer.class.getSimpleName();

    private static final byte END_WAITING = 66;
    public static final byte CONNECT_REQUEST = 1;

    public static final int START_PLAY = 3;
    public static final int CONN_CANCELLED = 4;
    public static final int ADD_USER = 5;
    public static final int REMOVE_USER = 6;
    public static final int PING_CLIENT = 7;
    public static final int SEND_USER = 8;
    public static final int SEND_PLAY_EVENT = 9;

    public static final int CODE_ERROR = 222;
    public static final int CODE_OK = 200;

    public static final String KEY_SERVER_NAME = "key_name";

    private JmDNS jmdns;
    private MainActivity activity;
    private Server server;
    private ClientsListAdapter clientsListAdapter;
    private ArrayList<Client> clientsList; // Клиенты сервера
    private final Object lock = new Object();
    private User myUser; // Игрок, запускающий сервер
    private GameMap gameMap;
    private String pathToMap;
    private GameView gameView;

    public GameServer(MainActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    protected void paramsReady() {
        gameMap = getGameMap();
        pathToMap = gameMap.getPath();
        myUser = getMyUser();
        inBG(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    createNetwork(false);
                }
            }
        });
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
                        if (clientsList.size() < 1) {
                            toast(R.string.have_not_clients);
                            stop();
                            return;
                        }
                        startPlay();
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
                                        try {
                                            jmdns = JmDNS.create(addr, HOSTNAME);
                                        } catch (IOException e) {
                                            Log.d(TAG, "JmDNS create error", e);
                                            toast(R.string.notify_net_without_mdns);
                                        }
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
                                        toast("Register server error: " + e.toString());
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
                    setGameRunning(false);
                    if (gameView != null) {
                        inUI(new Runnable() {
                            @Override
                            public void run() {
                                activity.showMainMenu();
                            }
                        });
                        gameView = null;
                    }
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
                                    DataOutputStream out = cli.out;
                                    try {
                                        out.writeInt(REMOVE_USER);
                                        out.writeLong(client.getID());
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
                    clientSock.setSoTimeout(10000);
                    clientSock.setSoLinger(true, 0);
                    int code = clientSock.getInputStream().read();
                    if (code == END_WAITING // Принимаем сообщение конца ожидания только от себя, на всякий случай
                            && clientSock.getInetAddress().getHostAddress().equals(myIp)) {
                        Log.d(TAG, "End waiting clients code detected");
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
                toast("Waiting error");
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
            DataOutputStream out = newClient.out;
            out.writeInt(ADD_USER);
            out.writeUTF(myUser.getName());
            out.writeLong(myUser.getID());
            out.writeUTF(myUser.getIp());
            /* Теперь обновляем список юзеров сервера у всех клиентов */
            clientsList.add(newClient);
            for (Client client : clientsList) {
                if (client == newClient) {
                    continue;
                }
                out = client.out;
                out.writeInt(ADD_USER);
                out.writeUTF(newClient.getName());
                out.writeLong(newClient.getID());
                out.writeUTF(newClient.getIp());
                /* А новому клиенту шлем остальных */
                out = newClient.out;
                out.writeInt(ADD_USER);
                out.writeUTF(client.getName());
                out.writeLong(client.getID());
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
                        stopMulticastReceiving();
                        stopMDNS();
                        pingClientThread.interrupt();
                        pingClientThread = null;
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

    public class Client extends User {

        final Socket socket;
        public final DataInputStream in;
        public final DataOutputStream out;

        Client(Socket socket) throws IOException {
            super(null, -1, null); // Установим параметры позже
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setName(in.readUTF());
            changeID(in.readLong());
            setIp(socket.getInetAddress().getHostAddress());
            out.writeUTF(pathToMap); // Отсылаем карту сервера
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

    private void startPlay() {
        // Ожидаем готовности клиентов и готовим ресурсы
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(new ProgressBar(activity))
                .setCancelable(false)
                .show();
        inBG(new Runnable() {
            @Override
            public void run() {
                server.acceptClients();
                for (int i = 0; i < clientsList.size(); i++) {
                    Client client = clientsList.get(i);
                    try {
                        int code = client.in.readInt();
                        client.out.writeInt(gameMap.TILE_SIZE);
                        if (code != CODE_OK) {
                            throw new IOException("Client " + client.getName() + " sends fail code");
                        }
                        client.loadResources(activity.getResources());
                    } catch (IOException e) {
                        e.printStackTrace();
                        toast("Client " + client.getName() + " disconnected");
                        clientsList.remove(client);
                        i--;
                    }
                }
                myUser.loadResources(activity.getResources());
                // Клиенты ответили
                inUI(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        setGameRunning(true);
                        ServerGameHolder gameHolder = new ServerGameHolder(GameServer.this, activity);
                        gameView = new GameView(gameHolder);
                        View v = LayoutInflater.from(activity).inflate(R.layout.play_screen, null);
                        ((ViewGroup) v.findViewById(R.id.game_map_layout)).addView(gameView);
                        activity.setContentView(v);
                    }
                });
            }
        });
    }
}
