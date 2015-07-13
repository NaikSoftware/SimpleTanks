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
        scale = gameClient.getGameMap().TILE_SIZE / (float) serverTileSize;
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
        User user;
        try {
            output.writeInt(click);
            while (true) {
                switch (input.readInt()) {
                    case GameServer.SEND_DATA:
                        for (int i = 0, size = users.size() + 1; i < size; i++) {
                            user = usersMap.get(input.readLong());
                            user.setX(input.readInt() * scale);
                            user.setY(input.readInt() * scale);
                            user.setDirection(input.readInt());
                        }
                        break;
                    case GameServer.REMOVE_USER:
                        user = usersMap.get(input.readLong());
                        users.remove(user);
                        String msg = activity.getString(R.string.user) + " " + user.getName() + " " + activity.getString(R.string.disconnected);
                        gameClient.toast(msg);
                        break;
                    case GameServer.CODE_OK:
                        int serverDeltaTime = input.readInt();
                        return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            gameClient.stop();
        }
    }

    @Override
    public void drawObjects(Canvas canvas) {
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
