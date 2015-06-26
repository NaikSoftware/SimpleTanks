package ua.naiksoftware.simpletanks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import ua.naiksoftware.utils.InetUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class GameServer extends GameConnection {

	private static final String TAG = GameServer.class.getSimpleName();
	
	private static final byte END_WAITING = 66;
	private static final byte CONNECT_REQUEST = 1;
	
	public static final String KEY_SERVER_NAME = "key_name";
	
	private JmDNS jmdns;
	private Activity activity;
	private Server server;
	private ClientsListAdapter clientsListAdapter;
	private ArrayList<Client> clientsList;
	private Handler background;
	private String servName;
	
	public GameServer(Activity activity) {
		super(activity);
		this.activity = activity;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				/* Looper нужен чтобы метод run не завершался сразу,
				 * он нужен для работы Handler */
				Looper.prepare();
				background = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						Log.d(TAG, "Handle stop background Thread");
						Looper.myLooper().quit();
					}
				};
				Looper.loop();
			}
		}).start();
	}
	
	public void start() {
		final View view = LayoutInflater.from(activity).inflate(R.layout.server_dialog_layout, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String servName = ((EditText) view.findViewById(R.id.serverName)).getText().toString().trim();
					if (servName.isEmpty()) {
						showToast(R.string.serv_name_empty_notice);
					} else { // Все нормально
						GameServer.this.servName = servName;
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
	            Log.i(TAG, "Stopping GameServer...");
	            jmdns.unregisterAllServices();
	            jmdns.close();
	            jmdns = null;
	        }
	        if (server != null) {
	        	server.stop();
	        }
	        stopMulticastDNS();
	        background.sendEmptyMessage(0); // остановить фоновый поток
	    } catch (Exception ex) {
	        Log.e(TAG, ex.getMessage(), ex);
	    }
	}
	
	@Override
	protected void onWifiConnected(WifiManager wifi) {
		final View view = LayoutInflater.from(activity).inflate(R.layout.server_wait_clients_dialog, null);
		clientsList = new ArrayList<>();
		clientsListAdapter = new ClientsListAdapter(activity, 0, clientsList);
		((ListView) view.findViewById(R.id.clientsList)).setAdapter(clientsListAdapter);
		AlertDialog dialog = new AlertDialog.Builder(activity)
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
		dialog.show();
		background.post(new Runnable() {
	        	
				@Override
				public void run() {
					try {
						server = new Server();
						server.start();
						jmdns = JmDNS.create();
			            ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, HOSTNAME,
			            		PORT, "SimpleTanks server on " + android.os.Build.DEVICE);
			            serviceInfo.setText(new Hashtable<String, String>() {{
			            	put(KEY_SERVER_NAME, servName);
			            }});
			            jmdns.registerService(serviceInfo);
			            Log.i(TAG, "Started GameServer...");
					} catch (IOException e) {
						e.printStackTrace();
						showToast(e.getMessage());
					}
				}
			});
	}
	
	/* Поток для приема клиентов сервером */
	private class Server implements Runnable {
		
		Thread thread;
		ServerSocket serverSocket;
		String myIp = InetUtils.getIpAddr(activity);
		
		Server() throws IOException {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(myIp, PORT));
		}
		
		void start() {
			thread = new Thread(this);
			thread.start();
		}
		
		@Override
		public void run() {
			try {
				clientsList.clear();
				while (true) {
					Socket clientSock = serverSocket.accept();
					int code = clientSock.getInputStream().read();
					showToast("Client code received " + code);
					if (code  == END_WAITING // Принимаем сообщение конца ожидания только от самого себя, на всякий случай
							&& clientSock.getInetAddress().getHostAddress().equals(myIp)) {
						Log.d(TAG, "End waiting clients code detected");
						showToast("End waiting detected");
						stopMulticastDNS();
						break;
					} else if (code == CONNECT_REQUEST) { // Кто-то хочет присоединиться к игре
						clientsList.add(new Client(clientSock));
						inUI(new Runnable() {
							
							@Override
							public void run() {
								clientsListAdapter.notifyDataSetChanged();
							}
						});
						showToast("Connected new client");
						Log.i(TAG, "Connected new client");
					}
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
		
		void acceptClients() {
			background.post(new Runnable() {
				
				@Override
				public void run() {
					try {
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
			try {
				serverSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Server socket close error", e);
			}
		}
	}
	
	static class Client {
		
		String name;
		Socket socket;
		DataInputStream in;
		DataOutputStream out;
		
		Client(Socket socket) throws IOException {
			this.socket = socket;
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			name = in.readUTF();
		}
	}
	
}
