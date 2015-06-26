package ua.naiksoftware.simpletanks;

import ua.naiksoftware.utils.InetUtils;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public abstract class GameConnection {
	
	private static final String TAG = GameConnection.class.getSimpleName();
	public static final String MSG_KEY = "msg_key";// for handler
	
	protected static final String SERVICE_TYPE = "_simpletanks._tcp.local.";
	protected static final int PORT = 5431;
	protected static final String HOSTNAME = "SimpleTanksHost";
	
	private Activity activity;
	private WifiManager.MulticastLock multicastLock;
	private WifiManager wifi;
	private WifiConnector wifiConnector;
	
	public GameConnection(Activity activity) {
		this.activity = activity;
	}
	
	protected abstract void onWifiConnected(WifiManager wifi);
	public abstract void start();
	public abstract void stop();
	
	protected void createNetwork() {
		wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
		if (wifi == null) {
			Toast.makeText(activity, "Not detected WiFi on device", Toast.LENGTH_LONG).show();
		}
		ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(!wifi.isWifiEnabled() || connectivityManager == null || !InetUtils.isConnected(connectivityManager)) {
			Log.i(TAG, "WiFi not connected");
			wifiConnector = new WifiConnector(activity);
			wifiConnector.execute(new WifiConnector.WifiConnectionHandler(wifi) {

					@Override
					public void onWifiConnected(boolean result) {
						Log.i(TAG, "WiFi was enabled? " + result);
						wifiConnector = null;
						if (result) createNetwork();
						else {
							Toast.makeText(activity, "Not connected to WiFi", Toast.LENGTH_LONG).show();
						}
					}

	        	});
			return;
		} else {
			Log.i(TAG, "WiFi connected, IP: " + InetUtils.getIpAddr(wifi));
		}

		Log.i(TAG, "Starting Mutlicast Lock...");
		multicastLock = wifi.createMulticastLock(getClass().getName());
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();
		
		onWifiConnected(wifi);
	}
	
	public void stopMulticastDNS() {
		if (multicastLock != null) {
			Log.i(TAG, "Releasing Mutlicast Lock...");
			multicastLock.release();
			multicastLock = null;
		}
		if (wifiConnector != null) { // Если включается WiFi, прерываем
			wifiConnector.stop();
		}
	}
	
	Handler ui = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			Toast.makeText(activity, msg.getData().getString(MSG_KEY), Toast.LENGTH_LONG).show();
		}
	};
	
	protected void showToast(String str) {
		Bundle b = new Bundle();
		b.putString(MSG_KEY, str);
		Message msg = new Message();
		msg.setData(b);
		ui.sendMessage(msg);
	}
	
	protected void showToast(int stringId) {
		showToast(activity.getString(stringId));
	}
	
	protected void inUI(Runnable r) {
		ui.post(r);
	}
}
