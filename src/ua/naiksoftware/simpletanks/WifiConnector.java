package ua.naiksoftware.simpletanks;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import ua.naiksoftware.utils.InetUtils;

public class WifiConnector extends AsyncTask<WifiConnector.WifiConnectionHandler, Void, Boolean>{
	
	private static final String TAG = WifiConnector.class.getSimpleName();
	private Context context;
	private WifiConnectionHandler wifiConnHandler;
	private ProgressDialog progressDialog;

	public WifiConnector(Context context) {
		this.context = context;
	}
	
	@Override
	protected Boolean doInBackground(WifiConnector.WifiConnectionHandler... params) {
		this.wifiConnHandler = params[0];
		WifiManager wifi = wifiConnHandler.getWifiManager();
		
		long start = System.currentTimeMillis();
		long timeout = wifiConnHandler.timeout();
		
		wifi.setWifiEnabled(true);
		while (!wifi.isWifiEnabled()) {
			if (System.currentTimeMillis() - start > timeout) {
				Log.i(TAG, "Turn On timeout");
				return false;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				return false;
			}
		}
		
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager == null) {
			Log.i(TAG, "ConnectivityManager = null");
			return false;
		}
		
		while (!InetUtils.isConnected(connectivityManager)) {
			if (System.currentTimeMillis() - start > timeout) {
				Log.i(TAG, "Connect timeout");
				return false;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				return false;
			}
		}
		
		while(!wifi.pingSupplicant()) {
			if (System.currentTimeMillis() - start > timeout) {
				Log.i(TAG, "Ping timeout");
				return false;
			}
			try {
				Log.i(TAG, "Ping false");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Log.i(TAG, "Interrupt ping sleep");
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(context, "Please wait", "Connecting to WiFi");
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		if (progressDialog != null) {
			progressDialog.dismiss();
			wifiConnHandler.onWifiConnected(result);
		}
	}
	
	public void stop() {
		cancel(true);
	}
	
	public static abstract class WifiConnectionHandler {
		
		private WifiManager wifiManager;
		
		public WifiConnectionHandler(WifiManager wifiManager) {
			this.wifiManager = wifiManager;
		}
		
		private WifiManager getWifiManager() {
			return wifiManager;
		}
		
		/**
		 * @return timeout in milliseconds
		 */
		public long timeout() {
			return 30000;
		}
		
		public abstract void onWifiConnected(boolean result);
		
	}
}
