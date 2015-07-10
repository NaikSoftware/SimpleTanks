package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import ua.naiksoftware.simpletanks.GameMap;
import ua.naiksoftware.simpletanks.Log;
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

    public ServerGameHolder(GameServer gameServer, Activity activity) {
        this.gameServer = gameServer;
        this.activity = activity;
        clients = gameServer.getUsers();
        myUser = gameServer.getMyUser();
        this.gameMap = gameServer.getGameMap();
        initWorld();
    }

    @Override
    public int processActions(int click) {
        double start = System.currentTimeMillis();
        // Получаем изменения от клиентов (нажатия на кнопки)
        // Обновляем мир
        int deltaTime = (int) (System.currentTimeMillis() - start);
        // Рассылаем изменения клиентам (координаты юзеров, события) и deltaTime
        DataOutputStream out;
        User user;
        for (int i = 0, size = clients.size(); i < size; i++) {
            out = clients.get(i).out;
            try {
                for (int j = 0; j < size; j++) {
                    user = clients.get(j);
                    out.writeLong(user.getId());
                    out.writeInt(user.getX());
                    out.writeInt(user.getY());
                }
                user = myUser;
                out.writeLong(user.getId());
                out.writeInt(user.getX());
                out.writeInt(user.getY());
            } catch (IOException e) {
                e.printStackTrace();
                user = clients.get(i);
                Log.e(TAG, "User " + user.getName() + " disconnected");
                clients.remove(user);
                // TODO: disconnect client and show notify
            }
        }

        // Теперь можно отрисовать мир и у себя
        return deltaTime;
    }

    @Override
    public void drawObjects(Canvas canvas, int deltaTime) {
        for (int i = 0, size = clients.size(); i < size; i++) {
            clients.get(i).draw(canvas);
        }
        myUser.draw(canvas);
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
        }
        placeUser(myUser);
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
}
