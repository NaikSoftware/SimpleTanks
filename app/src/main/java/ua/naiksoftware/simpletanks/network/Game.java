package ua.naiksoftware.simpletanks.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ua.naiksoftware.simpletanks.drawable.GameMap;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.drawable.Robot;
import ua.naiksoftware.simpletanks.drawable.User;
import ua.naiksoftware.utils.InetUtils;

/**
 * Класс - оболочка для запуска игры. Предоставляет общий интерфейс для клиента, сервера
 * и других способов игры.
 */
public abstract class Game {

    private static final String TAG = Game.class.getSimpleName();
    public static final String MSG_KEY = "msg_key";// for handler

    protected static final String SERVICE_TYPE = "_simpletanks._tcp.local.";
    protected static final int PORT = 5431;
    protected static final String HOSTNAME = android.os.Build.DEVICE;

    private Activity activity;
    private WifiManager.MulticastLock multicastLock;
    private WifiConnector wifiConnector;
    private InetAddress[] addresses;
    private ExecutorService background;
    private boolean takeMdnsPackets;
    private boolean gameRunning;
    private View toastView;
    private TextView toastText;

    public Game(Activity activity) {
        this.activity = activity;
        background = Executors.newFixedThreadPool(3);
        toastView = LayoutInflater.from(activity).inflate(R.layout.toast_view, null);
        toastText = (TextView)toastView.findViewById(R.id.toast_text);
    }

    protected abstract void onConnected();

    public abstract void start();

    public abstract void stop();

    public abstract ArrayList<? extends User> getUsers();

    public abstract User getMyUser();

    public abstract GameMap getGameMap();
	
	public abstract void botsSelected(ArrayList<Robot> bots);
	
	public abstract ArrayList<Robot> getRobots();

    protected void createNetwork(boolean takeMdnsPackets) {
        this.takeMdnsPackets = takeMdnsPackets;
        final WifiManager wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        try {
            addresses = InetUtils.getLocalAddresses();
        } catch (SocketException e) {
            Log.e(TAG, "Get local addresses error", e);
        }
        if (addresses == null || addresses.length == 0) {
            Log.i(TAG, "Not connected");
            toast(R.string.network_ip_not_detected);
            if (wifi != null && wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                inUI(new Runnable() {

                    @Override
                    public void run() {
                        turnOnWifi(wifi);
                    }
                });
            }
            return;
        }
        if (takeMdnsPackets && wifi != null && wifi.isWifiEnabled()) {
            Log.i(TAG, "Starting Mutlicast Lock...");
            multicastLock = wifi.createMulticastLock(TAG);
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        }

        inUI(new Runnable() {

            @Override
            public void run() {
                onConnected();
            }
        });
    }

    private void turnOnWifi(final WifiManager wifi) {
        new AlertDialog.Builder(activity).setTitle(R.string.turn_on_wifi)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface di, int pos) {
                        wifiConnector = new WifiConnector(activity);
                        wifiConnector.execute(new WifiConnector.WifiConnectionHandler(wifi) {

                            @Override
                            public void onWifiConnected(boolean result) {
                                Log.i(TAG, "WiFi was enabled? " + result);
                                wifiConnector = null;
                                if (result)
                                    createNetwork(takeMdnsPackets);
                                else {
                                    Toast.makeText(activity, R.string.not_connected, Toast.LENGTH_LONG).show();
                                }
                            }

                        });
                    }
                }).show();
    }

    protected InetAddress[] getAvailableAddresses() {
        return addresses;
    }

    public void stopMulticastReceiving() {
        if (multicastLock != null) {
            Log.i(TAG, "Releasing Mutlicast Lock...");
            multicastLock.release();
            multicastLock = null;
            //toast("Multicast lock stopped");
        }
        if (wifiConnector != null) { // Если включается WiFi, прерываем
            wifiConnector.stop();
        }
    }

    private Handler ui = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Toast toast = new Toast(activity);
            toast.setDuration(Toast.LENGTH_LONG);
            toastText.setText(msg.getData().getString(MSG_KEY));
            toast.setView(toastView);
            toast.show();
        }
    };

    public void toast(String str) {
        Bundle b = new Bundle();
        b.putString(MSG_KEY, str);
        Message msg = new Message();
        msg.setData(b);
        ui.sendMessage(msg);
    }

    public void toast(int stringId) {
        toast(activity.getString(stringId));
    }

    public void inUI(Runnable r) {
        ui.post(r);
    }

    protected void inBG(Runnable r) {
        background.submit(r);
    }
    
    protected void setGameRunning(boolean running) {
        gameRunning = running;
    }
    
    public boolean isGameRunning() {
        return gameRunning;
    }
	
	protected void showSelectBotsDialog() {
		botsSelected(null); // TODO: implement
	}

}
