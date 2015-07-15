package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;

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
public class ServerGameHolder implements GameHolder {

    private static final Random RND = new Random();
    private static final String TAG = ServerGameHolder.class.getSimpleName();

    private final GameServer gameServer;
    private final Activity activity;
    private final ArrayList<GameServer.Client> clients;
    private final User myUser;
    private final GameMap gameMap;
    private int click = NO_CLICK;
	private ConnectionThread connectionThread;
	private final Object lock = new Object();
    
    public ServerGameHolder(GameServer gameServer, Activity activity) {
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

    @Override
    public void onClick(int click) {
        this.click = click;
    }

    @Override
    public void drawObjects(Canvas canvas, int deltaTime) {
        User user;
        for (int i = 0; i < clients.size(); i++) {
            user = clients.get(i);
            processUser(user, deltaTime);
            user.draw(canvas);
        }
        processUser(myUser, deltaTime);
        myUser.draw(canvas);
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
            User user;
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
                myUser.setMove(click);
                // Рассылаем изменения клиентам (координаты юзеров, события)
                for (int i = 0; i < size; i++) {
                    out = clients.get(i).out;
                    try {
                        out.writeInt(GameServer.SEND_DATA);
                        for (int j = 0; j < size; j++) {
                            user = clients.get(j);
                            out.writeLong(user.getId());
                            out.writeInt(user.getX());
                            out.writeInt(user.getY());
                            out.writeInt(user.getMove());
                        }
                        user = myUser;
                        out.writeLong(user.getId());
                        out.writeInt(user.getX());
                        out.writeInt(user.getY());
                        out.writeInt(user.getMove());
                    } catch (IOException e) {
                        e.printStackTrace();
                        removeUser(clients.get(i));
                        size--;
                    }
                }
                // Отсылаем статус что итерация выполнена и deltaTime

                for (int i = 0; i < size; i++) {
                    out = clients.get(i).out;
                    try {
                        int codeOk = GameServer.CODE_OK;
                        out.writeInt(codeOk);
                    } catch (IOException e) {
                        e.printStackTrace();
                        gameServer.toast("OK request to " + clients.get(i).getName() + " failed: " + e.getMessage());
                    }
                }
			}
		}

		public void stopRunning() {
			running = false;
			interrupt();
		}
	}

    private void processUser(User user, int deltaTime) {
        if (user.getMove() != NO_CLICK) {
            user.move(deltaTime);
            gameMap.intersectWithUser(user);
            User user2;
            for (int i = 0; i < clients.size(); i++) {
                user2 = clients.get(i);
                if (user != user2) user.intersectWith(user2);
            }
            if (user != myUser) user.intersectWith(myUser);
        }
    }

    private void removeUser(User user) {
        String msg = activity.getString(R.string.user) + " " + user.getName() + " " + activity.getString(R.string.disconnected);
        Log.e(TAG, msg);
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

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public GameConnection getGameConnection() {
        return gameServer;
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
