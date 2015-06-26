package ua.naiksoftware.simpletanks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import ua.naiksoftware.utils.InetUtils;
import android.app.Activity;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public class GameClient extends GameConnection implements ServiceListener {

	private static final String TAG = GameClient.class.getSimpleName();

	private JmDNS jmdns;
	private Activity activity;
	private Socket socket;

	public GameClient(Activity activity) {
		super(activity);
		this.activity = activity;
	}

	public void start() {
		createNetwork();
	}

	public void stop() {
		try {
			if (jmdns != null) {
				Log.i(TAG, "Stopping GameClient discovery...");
				jmdns.unregisterAllServices();
				jmdns.close();
				jmdns = null;
			}
			stopMulticastDNS();
		} catch (Exception ex) {
			Log.e(TAG, ex.getMessage(), ex);
		}
	}

	@Override
	protected void onWifiConnected(final WifiManager wifi) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					InetAddress deviceIpAddress = InetAddress.getByName(InetUtils.getIpAddr(wifi));
					jmdns = JmDNS.create(deviceIpAddress, HOSTNAME);
					jmdns.addServiceListener(SERVICE_TYPE, GameClient.this);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		Log.i(TAG, "Started GameClient discovery...");
		Toast.makeText(activity, "Connected as " + InetUtils.getIpAddr(wifi), Toast.LENGTH_LONG).show();
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		Log.i(TAG, "Service added: " + event.getType() + "\n Info: " + event.getInfo());
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		Log.i(TAG, "Service removed: " + event.getType() + "\n Info: " + event.getInfo());
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		Log.i(TAG, "Service resolved: " + event.getType() + "\n Info: " + event.getInfo());
	}
}
