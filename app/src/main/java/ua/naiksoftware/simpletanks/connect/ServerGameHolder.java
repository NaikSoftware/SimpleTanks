package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import ua.naiksoftware.simpletanks.GameMap;
import ua.naiksoftware.simpletanks.Log;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.Tile;
import ua.naiksoftware.simpletanks.User;
import ua.naiksoftware.simpletanks.res.ImageID;

/**
 * Created by Naik on 10.07.15.
 */
public class ServerGameHolder extends GameHolder {

    private static final Random RND = new Random();
    private static final String TAG = ServerGameHolder.class.getSimpleName();
    private static final int SYNC_COORDS_INTERVAL = 100;//ms
    public static final float FLAG_SYNC_NOT_NEEDED = -1000f;

    private final GameServer gameServer;
    private final Activity activity;
    private final ArrayList<GameServer.Client> clients;
    private final User myUser;
    private final GameMap gameMap;
	private ConnectionThread connectionThread;
    private long lastSyncCoords;
    
    public ServerGameHolder(GameServer gameServer, Activity activity) {
        super(gameServer, activity);
        this.gameServer = gameServer;
        this.activity = activity;
        clients = gameServer.getUsers();
        myUser = gameServer.getMyUser();
        this.gameMap = gameServer.getGameMap();
        initWorld();
		connectionThread = new ConnectionThread();
    }

    @Override
    public void startGame() {
		connectionThread.start();
    }

	/* Поток для отсылки и приема данных */
	private class ConnectionThread extends Thread {

		private boolean running;

		@Override
		public void run() {
			running = true;
            int size = clients.size();
            DataOutputStream out;
            GameServer.Client client;
            boolean syncCoords;
			while (running) {
                // Получаем изменения от клиентов (нажатия на кнопки) и сразу обновляем мир
                for (int i = 0; i < size; i++) {
                    client = clients.get(i);
                    try {
                        int userClick = client.in.readInt();
                        client.setMove(userClick);
                    } catch (IOException e) {
                        e.printStackTrace();
                        removeUser(client);
                        size--;
                    }
                }
                myUser.setMove(myClick());
                // Рассылаем изменения клиентам (координаты юзеров)
                syncCoords = System.currentTimeMillis() - lastSyncCoords > SYNC_COORDS_INTERVAL;
                for (int i = 0; i < size; i++) {
                    out = clients.get(i).out;
                    try {
                        for (int j = 0; j < size; j++) {
                            sendUser(out, clients.get(j), syncCoords);
                        }
                        sendUser(out, myUser, syncCoords);
                    } catch (IOException e) {
                        e.printStackTrace();
                        removeUser(clients.get(i));
                        size--;
                    }
                }
                if (syncCoords) lastSyncCoords = System.currentTimeMillis();
                // Отсылаем статус что итерация выполнена и deltaTime

                for (int i = 0; i < size; i++) {
                    out = clients.get(i).out;
                    try {
                        out.writeInt(GameServer.CODE_OK);
                    } catch (IOException e) {
                        e.printStackTrace();
                        gameServer.toast("OK request to " + clients.get(i).getName() + " failed: " + e.getMessage());
                    }
                }
			}
		}

        private void sendUser(DataOutputStream out, User user, boolean syncCoords) throws IOException {
            out.writeInt(GameServer.SEND_USER);
            out.writeLong(user.getId());
            if (syncCoords) {
                out.writeFloat(user.getX());
                out.writeFloat(user.getY());
            } else {
                out.writeFloat(FLAG_SYNC_NOT_NEEDED); // Без остановки не синхронизировать, будет дергаться на клиентах
            }
            out.writeInt(user.getMove());
        }

		public void stopRunning() {
			running = false;
			interrupt();
		}
	}

    private void removeUser(User user) {
        String msg = activity.getString(R.string.user) + " " + user.getName() + " " + activity.getString(R.string.disconnected);
        Log.d(TAG, msg);
        gameServer.toast(msg);
        clients.remove(user);
        int size = clients.size();
        // Уведомляем остальных клиентов об отсоединении юзера
        for (int i = 0; i < size; i++) {
            DataOutputStream out = clients.get(i).out;
            try {
                out.writeInt(GameServer.REMOVE_USER);
                out.writeLong(user.getId());
            } catch (IOException e) {
                e.printStackTrace();
                gameServer.toast("Remove user request error " + e.getMessage());
            }
        }
    }

    private void initWorld() {
        User user;
        for (int i = 0, size = clients.size(); i < size; i++) {
            user = clients.get(i);
            placeUser(user);
            user.setSpeed(gameMap.TILE_SIZE / 200f);
        }
        placeUser(myUser);
        myUser.setSpeed(gameMap.TILE_SIZE / 200f);
    }

    private void placeUser(User user) {
        Tile[][] tiles = gameMap.tiles;
        int mapW = gameMap.mapW;
        int mapH = gameMap.mapH;
        int tileSize = gameMap.TILE_SIZE;
        int n = 0;
        while (true) {
            n++;
            if (n > 100) {
                throw new RuntimeException("Illegal game map, have not place for users");
            }
            int x = RND.nextInt(mapW);
            int y = RND.nextInt(mapH);
            if (tiles[x][y] != null && tiles[x][y].id == ImageID.BRICK) {
                Log.d(TAG, "Find place for user failed: x=" + x + " y=" + y + " tile=" + tiles[x][y]);
                continue;
            }
            user.setX(x * tileSize);
            user.setY(y * tileSize);
            Log.d(TAG, "User " + user.getName() + " placed on " + x + ", " + y);
            break;
        }
    }

	@Override
	public void stopGame() {
		connectionThread.stopRunning();
	}
}
