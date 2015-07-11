package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

    public ServerGameHolder(GameServer gameServer, Activity activity) {
        this.gameServer = gameServer;
        this.activity = activity;
        clients = gameServer.getUsers();
        myUser = gameServer.getMyUser();
        this.gameMap = gameServer.getGameMap();
        initWorld();
    }

    @Override
    public void onViewCreated() {
        final View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        switch (v.getId()) {
                            case R.id.btnUp: click = User.UP; break;
                            case R.id.btnDown: click = User.DOWN; break;
                            case R.id.btnLeft: click = User.LEFT; break;
                            case R.id.btnRight: click = User.RIGHT; break;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                            click = NO_CLICK;
                }
                return false;
            }
        };
        (activity.findViewById(R.id.btnUp)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnDown)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnLeft)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnRight)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnFire)).setOnTouchListener(listener);
    }

    @Override
    public void processActions(int deltaTime) {
        int size = clients.size();
        // Получаем изменения от клиентов (нажатия на кнопки)
        // Обновляем мир
        for (int i = 0; i < size; i++) {
            //processUser(clients.get(i), userClick);
        }
        processUser(myUser, click, deltaTime);
        // Рассылаем изменения клиентам (координаты юзеров, события)
        DataOutputStream out;
        User user;
        for (int i = 0; i < size; i++) {
            out = clients.get(i).out;
            try {
                out.writeInt(GameServer.SEND_DATA);
                for (int j = 0; j < size; j++) {
                    user = clients.get(j);
                    out.writeLong(user.getId());
                    out.writeInt(user.getX());
                    out.writeInt(user.getY());
                    out.writeInt(user.getDirection());
                }
                user = myUser;
                out.writeLong(user.getId());
                out.writeInt(user.getX());
                out.writeInt(user.getY());
                out.writeInt(user.getDirection());
            } catch (IOException e) {
                e.printStackTrace();
                user = clients.get(i);
                String msg = activity.getString(R.string.user) + " " + user.getName() + " " + activity.getString(R.string.disconnected);
                Log.e(TAG, msg);
                gameServer.toast(msg);
                clients.remove(user);
                size--;
                // Уведомляем остальных клиентов об отсоединении юзера
                for (int j = 0; j < size; j++) {
                    out = clients.get(j).out;
                    try {
                        out.writeInt(GameServer.REMOVE_USER);
                        out.writeLong(user.getId());
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        gameServer.toast("Remove user request error " + e.getMessage());
                    }
                }
            }
        }
        // Отсылаем статус что итерация выполнена и deltaTime

        for (int i = 0; i < size; i++) {
            out = clients.get(i).out;
            try {
                out.writeInt(GameServer.CODE_OK);
                out.writeInt(deltaTime);
            } catch (IOException e) {
                e.printStackTrace();
                gameServer.toast("OK request to " + clients.get(i).getName() + " failed: " + e.getMessage());
            }
        }
    }

    private void processUser(User user, int click, int deltaTime) {
        switch (click) {
            case User.FIRE:
                break;
            case User.MINE:
                break;
            case User.UP:
            case User.DOWN:
            case User.LEFT:
            case User.RIGHT:
                user.move(click, deltaTime);
        }
    }

    @Override
    public void drawObjects(Canvas canvas) {
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
}
