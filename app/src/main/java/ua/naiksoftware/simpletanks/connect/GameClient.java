package ua.naiksoftware.simpletanks.connect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import ua.naiksoftware.simpletanks.GameMap;
import ua.naiksoftware.simpletanks.GameView;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.User;
import ua.naiksoftware.simpletanks.MainActivity;

public class GameClient extends GameConnection implements ServiceListener {

    private static final String TAG = GameClient.class.getSimpleName();

    private JmDNS[] jmdns;
    private MainActivity activity;
    private ArrayList<Server> serversList;
    private ServersListAdapter serversListAdapter;
    private final Object lock = new Object();
    private Server server;
    private User myUser;
    private ArrayList<User> users = new ArrayList<User>(2);
    private GameMap gameMap;
    private DataOutputStream output;
    private DataInputStream input;
    private GameView gameView;
    private boolean connectDirectly;

    public GameClient(MainActivity activity, boolean connectDirectly) {
        super(activity);
        this.activity = activity;
        this.connectDirectly = connectDirectly;
    }

    @Override
    public void start() {
        final View view = LayoutInflater.from(activity).inflate(R.layout.client_dialog_layout, null);
        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String clientName = ((EditText) view.findViewById(R.id.clientName)).getText().toString().trim();
                        if (clientName.isEmpty()) {
                            toast(R.string.client_name_empty_notice);
                        } else { // Все нормально
                            myUser = new User(clientName, User.GEN_NEW_ID, activity.getString(R.string.my_device));
                            inBG(new Runnable() {

                                @Override
                                public void run() {
                                    synchronized (lock) {
                                        createNetwork(!connectDirectly);
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
        if (connectDirectly) {
            View view = LayoutInflater.from(activity).inflate(R.layout.client_direct_connect, null);
            final EditText inputAddr = (EditText)view.findViewById(R.id.directServerAddress);
            new AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        server = new Server("Direct", "undefined", inputAddr.getText().toString());
                        server.connect();
                    }
                })
                .setCancelable(false)
                .show();
            return;
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.client_discover_servers_dialog, null);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stop();
                    }
                })
                .setCancelable(false)
                .create();
        serversList = new ArrayList<Server>();
        serversListAdapter = new ServersListAdapter(activity, 0, serversList);
        ListView listView = (ListView) view.findViewById(R.id.serversList);
        listView.setAdapter(serversListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                inBG(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            stopMulticastReceiving();
                        }
                    }
                });
                server = serversList.get(position);
                server.connect();
            }
        });
        dialog.show();
        inBG(new Runnable() {

            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        InetAddress[] addresses = getAvailableAddresses();
                        jmdns = new JmDNS[addresses.length];
                        for (int i = 0; i < addresses.length; i++) {
                            jmdns[i] = JmDNS.create(addresses[i], HOSTNAME);
                            jmdns[i].addServiceListener(SERVICE_TYPE, GameClient.this);
                        }
                        Log.i(TAG, "Started GameClient discovery...");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        Log.i(TAG, "Service added: " + event.getType() + "\n Info: " + event.getInfo());
        // Для вызова serviceResolved(...)
        event.getDNS().requestServiceInfo(event.getType(), event.getName(), 100);
    }

    @Override
    public void serviceResolved(final ServiceEvent event) {
        Log.i(TAG, "Service resolved: " + event.getType() + "\n Info: " + event.getInfo());
        inUI(new Runnable() {

            @Override
            public void run() {
                String name = event.getInfo().getPropertyString(GameServer.KEY_SERVER_NAME);
                String descr = event.getInfo().getName();
                String ip = event.getInfo().getInetAddresses()[0].getHostAddress();
                Server srv = new Server(name, descr, ip);
                if (!serversList.contains(srv)) {
                    serversList.add(srv);
                    serversListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void serviceRemoved(final ServiceEvent event) {
        Log.i(TAG, "Service removed: " + event.getType() + "\n Info: " + event.getInfo());
        final Server srv = new Server(null, null, event.getInfo().getHostAddresses()[0]);
        inUI(new Runnable() {

            @Override
            public void run() {
                serversList.remove(srv);
                serversListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void stop() {
        if (gameView != null) {
            gameView.destroy();
            toast(R.string.server_disconnected);
        }
        inBG(new Runnable() {

            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        stopMulticastReceiving();
                        stopMDNS();
                        if (serversList != null) {
                            for (Server server : serversList) {
                                server.stop();
                            }
                            serversList.clear();
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
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
                    //toast("Client stopped");
                }
            }
        });
    }

    private void stopMDNS() throws IOException {
        if (jmdns != null) {
            for (JmDNS mdns : jmdns) {
                if (mdns == null) {
                    continue;
                }
                Log.i(TAG, "Stopping GameClient discovery " + mdns.getName());
                mdns.removeServiceListener(SERVICE_TYPE, GameClient.this);
                mdns.close();
                mdns = null;
            }
        }
    }

    class Server {

        String name, descr;
        String ip;
        Socket socket;
        DataInputStream in;
        DataOutputStream out;

        Server(String name, String descr, String ip) {
            this.name = name;
            this.descr = descr;
            this.ip = ip;
        }

        /**
         * Connect to selected server, send additional data and show waiting dialog,
         * while server accept or cancel connect to him.
         */
        void connect() {
            final View v = LayoutInflater.from(activity).inflate(R.layout.client_connect_dialog, null);
            final TextView clientsInfo = (TextView)v.findViewById(R.id.client_users_on_server);
                    ((TextView) v.findViewById(R.id.client_conn_to)).setText(activity.getString(R.string.connecting_to) + " " + name);
            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(v)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Отменили подключение к серверу.
                            dialog.dismiss();
                            try {
                                stop();
                            } catch (IOException e) {
                                Log.e(TAG, "Error in stop server while waiting for accept", e);
                            }
                        }
                    })
                    .show();
            inBG(new Runnable() {

                @Override
                public void run() {
                    try {
                        socket = new Socket(ip, PORT);
                        socket.getOutputStream().write(GameServer.CONNECT_REQUEST);
                        out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF(myUser.getName());
                        out.writeLong(myUser.getID());
                        in = new DataInputStream(socket.getInputStream());
                        final String pathToMap = in.readUTF();
                        try {
                            gameMap = new GameMap(activity.getAssets().open(pathToMap), activity.getResources());
                        } catch (IOException e) {
                            toast(activity.getString(R.string.error_loading_map) + " " + pathToMap);
                            stopWaiting(dialog, true);
                            return;
                        }
                        inUI(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) v.findViewById(R.id.client_map_on_server))
                                        .setText(activity.getString(R.string.map) + ": " + gameMap.name);
                            }
                        });
                        int response;
                        while (true) {
                            response = in.readInt();
                            if (response == GameServer.CONN_CANCELLED) {
                                stopWaiting(dialog, true);
                                return;
                            } else if (response == GameServer.ADD_USER || response == GameServer.REMOVE_USER) {
                                switch (response) {
                                    case GameServer.ADD_USER :
                                        users.add(new User(in.readUTF(), in.readLong(), in.readUTF()));
                                        break;
                                    case GameServer.REMOVE_USER:
                                        removeUserByID(in.readLong());
                                }
                                inUI(new Runnable() {
                                    @Override
                                    public void run() {
                                        clientsInfo.setText(getUsersInfo());
                                    }
                                });
                            } else if (response == GameServer.START_PLAY) {
                                stopWaiting(dialog, false);
                                toast("Play!!!");
                                startPlay();
                                return;
                            }
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                stopWaiting(dialog, true);
                                return;
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Connection failed", e);
                        toast(R.string.connect_error);
                        stopWaiting(dialog, true);
                        return;
                    }
                }
            });
        }

        private void stopWaiting(final AlertDialog dialog, boolean stopGameClient) {
            inUI(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                }
            });
            if (stopGameClient) {
                GameClient.this.stop();
            }
        }

        void stop() throws IOException {
            if (socket != null && socket.isConnected()) {
                socket.close();
                socket = null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Server) {
                return ip.equals(((Server) o).ip);
            }
            return false;
        }
    }

    private void startPlay() {
        inBG(new Runnable() {
            @Override
            public void run() {
                // Подготовка ресурсов
                input = server.in;
                output = server.out;
                // Если что-то пойдет не так, отсылаем CODE_ERROR
                for (User user : users) {
                    user.loadResources(activity.getResources());
                }
                myUser.loadResources(activity.getResources());
                // Если все ОК, то отсылаем серверу статус готовности к игре и размер для расчета масштаб
                try {
                    output.writeInt(GameServer.CODE_OK);
                    final int serverTileSize = input.readInt();
                    // Начинаем игру
                    inUI(new Runnable() {
                        @Override
                        public void run() {
                            setGameRunning(true);
                            ClientGameHolder gameHolder = new ClientGameHolder(GameClient.this, activity, serverTileSize);
                            gameView = new GameView(gameHolder);
                            View v = LayoutInflater.from(activity).inflate(R.layout.play_screen, null);
                            ((ViewGroup)v.findViewById(R.id.game_map_layout)).addView(gameView);
                            activity.setContentView(v);
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "Starting game error", e);
                }
            }
        });
    }

    public Server getServer() {
        return server;
    }

    private String getUsersInfo() {
        StringBuilder builder = new StringBuilder();
        for (User user : users) {
            builder.append(user.getName()).append(" - ").append(user.getIp()).append("\n");
        }
        builder.append(myUser.getName()).append(" - ").append(myUser.getIp()).append("\n");
        return builder.toString();
    }

    private void removeUserByID(long id) {
        for (User user : users) {
            if (user.getID() == id) {
                users.remove(user);
                return;
            }
        }
    }

    @Override
    public ArrayList<User> getUsers() {
        return users;
    }

    @Override
    public User getMyUser() {
        return myUser;
    }

    @Override
    public GameMap getGameMap() {
        return gameMap;
    }
}
