package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ua.naiksoftware.simpletanks.Bullet;
import ua.naiksoftware.simpletanks.PlayEvent;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.User;

/**
 * Created by Naik on 10.07.15.
 */
public class ClientGameHolder extends GameHolder {

    private static final String TAG = ClientGameHolder.class.getSimpleName();
    private final GameClient gameClient;
    private final Activity activity;
    private final ArrayList<? extends User> users;
    private final HashMap<Long, User> usersMap = new HashMap<Long, User>();
    private final DataOutputStream output;
    private final DataInputStream input;
    private final float scale;
    private ConnectionThread connectionThread;
    private ArrayList<Bullet> bullets = getBullets();
    private HashMap<Long, Bullet> bulletsMap = new HashMap<Long, Bullet>();
    private boolean fire;
    
    public ClientGameHolder(GameClient gameClient, Activity activity, int serverTileSize) {
        super(gameClient, activity);
        this.gameClient = gameClient;
        this.activity = activity;
        users = gameClient.getUsers();
        output = gameClient.getServer().out;
        input = gameClient.getServer().in;
        for (User user : users) {
            usersMap.put(user.getID(), user);
        }
        User myUser = gameClient.getMyUser();
        usersMap.put(myUser.getID(), myUser);
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
        super.onClick(click);
        if (click == User.FIRE) {
            fire = true;
        }
    }

    @Override
    protected void bulletsBabah(Bullet bullet, Bullet bullet2) {

    }

    @Override
    protected void userBombom(User user, Bullet bullet) {

    }

    @Override
    protected void bulletOnWall(Bullet bullet) {

    }

    /* Поток для отсылки и приема данных */
    private class ConnectionThread extends Thread {

        private boolean running;

        @Override
        public void run() {
            try {
                running = true;
                User user;
                float tmp;
                boolean read;
                while (running) {
                    if (fire) {
                        // Если была нажата кнопка выстрела ранее, то отсылаем выстрел, чтобы не
                        // допустить пропусков выстрелов.
                        output.writeInt(User.FIRE);
                        fire = false;
                    } else {
                        output.writeInt(myClick());
                    }
                    read = true;
                    while (read) {
                        switch (input.readInt()) {
                            case GameServer.SEND_USER:
                                user = usersMap.get(input.readLong());
                                tmp = input.readFloat();
                                if (Math.abs(tmp - ServerGameHolder.FLAG_SYNC_NOT_NEEDED) > 0.1) {
                                    user.setX(tmp * scale); // Синхронизируем координаты с сервером
                                    user.setY(input.readFloat() * scale);
                                }
                                user.setMove(input.readInt());
                                break;
                            case GameServer.SEND_PLAY_EVENT:
                                processGameEvent(input);
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

    private void processGameEvent(DataInputStream input) throws IOException {
        Bullet bullet;
        User user;
        long bulletId, userId;
        switch (input.readInt()) {
            case PlayEvent.USER_FIRE:
                bulletId = input.readLong();
                userId = input.readLong();
                float bulletSpeed = input.readFloat();
                user = usersMap.get(userId);
                bullet = bulletsPool.obtain();
                bullet.setup(user, bulletSpeed * scale);
                bullet.changeID(bulletId);
                bullets.add(bullet);
                bulletsMap.put(bulletId, bullet);
                break;
            case PlayEvent.BULLET_ON_WALL:
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                break;
            case PlayEvent.BULLETS_BABAH:
                // remove 1
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                // remove 2
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                break;
            case PlayEvent.USER_BOMBOM:
                // remove user
                userId = input.readLong();
                // remove bullet
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                break;
        }
    }

	@Override
	public void stopGame() {
        connectionThread.stopRunning();
	}
}
