package ua.naiksoftware.simpletanks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import ua.naiksoftware.utils.InetUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class GameClient extends GameConnection implements ServiceListener {

	private static final String TAG = GameClient.class.getSimpleName();

	private JmDNS jmdns;
	private Activity activity;
	private Socket socket;
	private ArrayList<Server> serversList;
	private ServersListAdapter serversListAdapter;

	public GameClient(Activity activity) {
		super(activity);
		this.activity = activity;
	}

	public void start() {
		final View view = LayoutInflater.from(activity).inflate(R.layout.client_dialog_layout, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String clientName = ((EditText) view.findViewById(R.id.clientName)).getText().toString().trim();
					if (clientName.isEmpty()) {
						showToast(R.string.client_name_empty_notice);
					} else { // Все нормально
						createNetwork();
					}
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create();
		dialog.show();
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
			if (serversList != null) {
				for (Server server : serversList) {
					server.stop();
				}
				serversList.clear();
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.getMessage(), ex);
		}
	}

	@Override
	protected void onWifiConnected(final WifiManager wifi) {
		final View view = LayoutInflater.from(activity).inflate(R.layout.client_discover_servers_dialog, null);
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
	serversList = new ArrayList<>();
	serversListAdapter = new ServersListAdapter(activity, 0, serversList);
	ListView listView = (ListView) view.findViewById(R.id.serversList);
	listView.setAdapter(serversListAdapter);
	listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			showToast("Selected server " + serversList.get(position).name);
			dialog.dismiss();
		}
	});
	dialog.show();
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
		showToast("Connected as " + InetUtils.getIpAddr(wifi));
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		Log.i(TAG, "Service added: " + event.getType() + "\n Info: " + event.getInfo());
		// Для вызова serviceResolved(...)
        jmdns.requestServiceInfo(event.getType(), event.getName(), 1 /* timeout 1ms*/);
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		Log.i(TAG, "Service removed: " + event.getType() + "\n Info: " + event.getInfo());
		serversList.remove(new Server(event.getInfo().getHostAddresses()[0]));
		serversListAdapter.notifyDataSetChanged();
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		Log.i(TAG, "Service resolved: " + event.getType() + "\n Info: " + event.getInfo());
		String name = event.getInfo().getPropertyString(GameServer.KEY_SERVER_NAME);
		String descr = event.getInfo().getServer();
		String ip = event.getInfo().getInetAddresses()[0].getHostAddress();
		serversList.add(new Server(name, descr, ip));
		inUI(new Runnable() {
			
			@Override
			public void run() {
				serversListAdapter.notifyDataSetChanged();
			}
		});
	}
	
	static class Server {
		
		String name, descr;
		String ip;
		Socket socket;
		
		/* Конструктор для временных обьектов для удаления других
		 * с таким же IP из коллекций */
		Server(String ip) {
			this.ip = ip;
		}
		
		Server(String name, String descr, String ip /*Socket socket*/) {
			this.name = name;
			this.descr = descr;
			this.ip = ip; //socket.getInetAddress().getHostAddress();
			//this.socket = socket;
		}
		
		void connect() throws IOException {
			
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
}
