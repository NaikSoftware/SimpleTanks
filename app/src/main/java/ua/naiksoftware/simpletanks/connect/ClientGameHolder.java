package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ua.naiksoftware.simpletanks.Log;
import ua.naiksoftware.simpletanks.User;

/**
 * Created by Naik on 10.07.15.
 */
public class ClientGameHolder implements GameHolder {

    private static final String TAG = ClientGameHolder.class.getSimpleName();
    private final GameClient gameClient;
    private final Activity activity;
    private final ArrayList<? extends User> users;
    private final HashMap<Long, User> usersMap = new HashMap<Long, User>();
    private final User myUser;
    private final DataOutputStream output;
    private final DataInputStream input;
    private final float scale;

    public ClientGameHolder(GameClient gameClient, Activity activity, int serverTileSize) {
        this.gameClient = gameClient;
        this.activity = activity;
        users = gameClient.getUsers();
        myUser = gameClient.getMyUser();
        output = gameClient.getServer().out;
        input = gameClient.getServer().in;
        for (User user : users) {
            usersMap.put(user.getId(), user);
        }
        usersMap.put(myUser.getId(), myUser);
        scale = gameClient.getGameMap().TILE_SIZE / (float)serverTileSize;
    }

    @Override
    public int processActions(int click) {
        User user;
        for (int i = 0, size = users.size() + 1; i < size; i++) {
            try {
                user = usersMap.get(input.readLong());
                user.setX(input.readInt() * scale);
                user.setY(input.readInt() * scale);
            } catch (IOException e) {
                e.printStackTrace();
                gameClient.stop();
                break;
            }
        }
        return 0;
    }

    @Override
    public void drawObjects(Canvas canvas, int deltaTime) {
        for (int i = 0, size = users.size(); i < size; i++) {
            users.get(i).draw(canvas);
        }
        myUser.draw(canvas);
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public GameConnection getGameConnection() {
        return gameClient;
    }
}
