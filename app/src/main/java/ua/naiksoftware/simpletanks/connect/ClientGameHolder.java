package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ua.naiksoftware.simpletanks.Bullet;
import ua.naiksoftware.simpletanks.Log;
import ua.naiksoftware.simpletanks.PlayEvent;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.User;
import ua.naiksoftware.simpletanks.res.Music;

/**
 * Created by Naik on 10.07.15.
 */
public class ClientGameHolder extends GameHolder {

    private static final String TAG = ClientGameHolder.class.getSimpleName();
    private final GameClient gameClient;
    private final ArrayList<? extends User> users;
    private final HashMap<Long, User> usersMap = new HashMap<Long, User>();
    private final DataOutputStream output;
    private final DataInputStream input;
    private final float scale;
    private ConnectionThread connectionThread;
    private ArrayList<Bullet> bullets = getBullets();
    private HashMap<Long, Bullet> bulletsMap = new HashMap<Long, Bullet>();
    private boolean fire;
    private User myUser;
    private boolean finishGame;
    
    public ClientGameHolder(GameClient gameClient, Activity activity, int serverTileSize) {
        super(gameClient, activity);
        this.gameClient = gameClient;
        users = gameClient.getUsers();
        myUser = gameClient.getMyUser();
        output = gameClient.getServer().out;
        input = gameClient.getServer().in;
        for (User user : users) {
            usersMap.put(user.getID(), user);
        }
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
                    if (finishGame) {
                        gameOver();
                        break;
                    }
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
                                gameClient.toast(tr(R.string.user) + " " + user.getName() + " " + tr(R.string.disconnected));
                                removeUser(user);
                                break;
                            case GameServer.CODE_OK:
                                read = false;
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception detected, stop client", e);
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
                Log.e(TAG, "Receive fire " + user);
                bullet = bulletsPool.obtain();
                bullet.setup(user, bulletSpeed * scale);
                bullet.changeID(bulletId);
                bullets.add(bullet);
                bulletsMap.put(bulletId, bullet);
                if (user == myUser) Music.playSound(user, R.raw.shot_tank, 1, false);
                else Music.playSound(user, R.raw.shot_tank, 0.5f, false);
                break;
            case PlayEvent.BULLET_ON_WALL:
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                bullet.release();
                break;
            case PlayEvent.BULLETS_BABAH:
                // remove 1
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                bullet.release();
                // remove 2
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                bullets.remove(bullet);
                bulletsMap.remove(bulletId);
                bullet.release();
                break;
            case PlayEvent.USER_BOMBOM:
                // remove user
                userId = input.readLong();
                user = usersMap.get(userId);
                bulletId = input.readLong();
                bullet = bulletsMap.get(bulletId);
                Log.e(TAG, "Receive user bombom " + user + " by bullet " + bullet);
                if (bullet != null && user != null) { // Проверка, вдруг пришло что-то не то
                    user.shot(bullet);
                    if (user == myUser) updateScreenInfo();
                    if (user.getLifes() < 1) {
                        if (user == myUser || bullet.getOwner() == myUser) {
                            Music.playSound(user, R.raw.explosion, 1, false);
                        } else Music.playSound(user, R.raw.explosion, 0.5f, false);
                        removeUser(user);
                    } else {
                        if (user == myUser || bullet.getOwner() == myUser) {
                            Music.playSound(user, R.raw.hit_tank, 1, false);
                        } else Music.playSound(user, R.raw.hit_tank, 0.5f, false);
                    }
                    // remove bullet
                    bullets.remove(bullet);
                    bulletsMap.remove(bulletId);
                    bullet.release();
                }
                break;
        }
    }
    
    private void removeUser(User user) {
        if (user == myUser) {
            killMyUser();
            myUser = null;
            Log.e(TAG, "Killed my user");
        } else {
            users.remove(user);
            Log.e(TAG, "Remove user " + user);
        }
        if (myUser == null || getWinner() != null) { // Вы или победили или проиграли
            finishGame = true; // завершаем игру.
        } else { // Игра не завершена, покажем просто уведомление.
            gameClient.toast(tr(R.string.user) + " " + user.getName() + " " + tr(R.string.looser));
        }
    }

    @Override
	public void stopGame() {
        connectionThread.stopRunning();
	}
}
