package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ua.naiksoftware.simpletanks.Log;
import ua.naiksoftware.simpletanks.R;
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
    private int click = NO_CLICK;
    private ConnectionThread connectionThread;

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
        int tileSize = gameClient.getGameMap().TILE_SIZE;
        scale = tileSize / (float) serverTileSize;
        for (User user : users) {
            user.setSpeed(tileSize / 200f);
        }
        myUser.setSpeed(tileSize / 200f);
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
        for (int i = 0; i < users.size(); i++) {
            user = users.get(i);
            user.move(deltaTime);
            user.draw(canvas);
        }
        myUser.move(deltaTime);
        myUser.draw(canvas);
    }

    private class ConnectionThread extends Thread {

        private boolean running;

        @Override
        public void run() {
            try {
                running = true;
                User user;
                boolean read;
                while (running) {
                    output.writeInt(click);
                    read = true;
                    while (read) {
                        switch (input.readInt()) {
                            case GameServer.SEND_DATA:
                                for (int i = 0, size = users.size() + 1; i < size; i++) {
                                    user = usersMap.get(input.readLong());
                                    user.setX(input.readInt() * scale);
                                    user.setY(input.readInt() * scale);
                                    user.setMove(input.readInt());
                                }
                                break;
                            case GameServer.REMOVE_USER:
                                user = usersMap.get(input.readLong());
                                users.remove(user);
                                String msg = activity.getString(R.string.user) + " " + user.getName() + " " + activity.getString(R.string.disconnected);
                                gameClient.toast(msg);
                                break;
                            case GameServer.CODE_OK:
                                read = false;
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                gameClient.stop();
            }
        }

        public void stopRunning() {
            running = false;
            interrupt();
        }
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public GameConnection getGameConnection() {
        return gameClient;
    }

	@Override
	public void stopGame() {
        connectionThread.stopRunning();
	}
}
