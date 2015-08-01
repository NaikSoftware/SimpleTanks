package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import ua.naiksoftware.simpletanks.Bonus;
import ua.naiksoftware.simpletanks.Bullet;
import ua.naiksoftware.simpletanks.GameMap;
import ua.naiksoftware.simpletanks.Log;
import ua.naiksoftware.simpletanks.PlayEvent;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.User;
import ua.naiksoftware.simpletanks.res.Music;
import ua.naiksoftware.utils.Pool;

/**
 * Управляет логикой игры сервера. Рассылает игровые события клиентам.
 *
 * Created by Naik on 10.07.15.
 */
public class ServerGameHolder extends GameHolder {

    private static final String TAG = ServerGameHolder.class.getSimpleName();

    private static final Random RND = new Random();

    private static final int SYNC_COORDS_INTERVAL = 100;//ms
    public static final float FLAG_SYNC_NOT_NEEDED = -1000f;

    private final GameServer gameServer;
    private final ArrayList<GameServer.Client> clients;
    private User myUser;
    private final GameMap gameMap;
	private ConnectionThread connectionThread;
    private long lastSyncCoords;
    private final ArrayList<Bullet> bullets = getBullets();
    private final ArrayList<PlayEvent> playEvents = new ArrayList<PlayEvent>();
    private boolean fire;
    private Resources resources;
    
    public ServerGameHolder(GameServer gameServer, Activity activity) {
        super(gameServer, activity);
        this.gameServer = gameServer;
        clients = gameServer.getUsers();
        myUser = gameServer.getMyUser();
        this.gameMap = gameServer.getGameMap();
        resources = activity.getResources();
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
            DataOutputStream out;
            GameServer.Client client;
            boolean syncCoords;
            int processedEvents;
            runDelayed(bonusRunnable, this, 5000 + RND.nextInt(10000));
			while (running) {
                // Получаем изменения от клиентов (нажатия на кнопки) и сразу обновляем мир
                for (int i = 0; i < clients.size(); i++) {
                    client = clients.get(i);
                    try {
                        updateUser(client, client.in.readInt());
                    } catch (IOException e) {
                        gameServer.toast(tr(R.string.user) + " " + client.getName() + " " + tr(R.string.disconnected));
                        removeUser(client, true);
                    }
                }
                if (myUser != null) {
                    if (fire) {
                        updateUser(myUser, User.FIRE);
                        fire = false;
                    } else {
                        updateUser(myUser, myClick());
                    }
                }
                // Рассылаем изменения клиентам (координаты юзеров)
                processedEvents = playEvents.size();
                syncCoords = System.currentTimeMillis() - lastSyncCoords > SYNC_COORDS_INTERVAL;
                for (int i = 0; i < clients.size(); i++) {
                    client = clients.get(i);
                    out = client.out;
                    try {
                        for (int j = 0; j < clients.size(); j++) {
                            sendUser(out, clients.get(j), syncCoords);
                        }
                        if (myUser != null) sendUser(out, myUser, syncCoords);
                        // Отсылаем действия в игре (выстрелы, попадания и т.п.)
                        for (int j = 0; j < processedEvents; j++) {
                            sendEvent(out, playEvents.get(j));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (!client.isDestroyed()) { // Убитого юнита и победителя удалим далее, после обработки состояния игры
                            gameServer.toast(tr(R.string.user) + " " + client.getName() + " " + tr(R.string.disconnected));
                            removeUser(client, true);
                        }
                    }
                }
                // Remove only processed events
                for (int i = 0; i < processedEvents; i++) {
                    playEvents.get(0).release();
                    playEvents.remove(0);
                }
                if (syncCoords) lastSyncCoords = System.currentTimeMillis();
                // Отсылаем статус что итерация выполнена и deltaTime
                for (int i = 0; i < clients.size(); i++) {
                    client = clients.get(i);
                    out = client.out;
                    try {
                        out.writeInt(GameServer.CODE_OK);
                    } catch (IOException e) {
                        e.printStackTrace();
                        gameServer.toast("OK request to " + client.getName() + " failed: " + e.getMessage());
                    }
                    if (client.isDestroyed()) {
                        removeUser(client, false);
                    }
                }
                if (myUser != null && myUser.isDestroyed()) {
                    removeUser(myUser, false);
                }
			}
		}

		public void stopRunning() {
			running = false;
			interrupt();
		}
	}

    private void updateUser(User user, int click) {
        if (click == User.FIRE) {
            fire(user);
        } else {
            user.setMove(click);
        }
    }

    private void sendUser(DataOutputStream out, User user, boolean syncCoords) throws IOException {
        out.writeInt(GameServer.SEND_USER);
        out.writeLong(user.getID());
        if (syncCoords) {
            out.writeFloat(user.getX());
            out.writeFloat(user.getY());
        } else {
            out.writeFloat(FLAG_SYNC_NOT_NEEDED); // Без остановки не синхронизировать, будет дергаться на клиентах
        }
        out.writeInt(user.getMove());
    }

    private void sendEvent(DataOutputStream out, PlayEvent event) throws IOException {
        out.writeInt(GameServer.SEND_PLAY_EVENT);
        out.writeInt(event.getType());
        Bullet bullet;
        Bonus bonus;
        User user;
        switch (event.getType()) {
            case PlayEvent.USER_FIRE:
                bullet = (Bullet)event.getParams()[0];
                out.writeLong(bullet.getID());
                out.writeLong(bullet.getOwner().getID());
                out.writeFloat(bullet.getSpeed());
                break;
            case PlayEvent.BULLET_ON_WALL:
                bullet = (Bullet)event.getParams()[0];
                out.writeLong(bullet.getID());
                break;
            case PlayEvent.BULLETS_BABAH:
                bullet = (Bullet)event.getParams()[0];
                out.writeLong(bullet.getID());
                bullet = (Bullet)event.getParams()[1];
                out.writeLong(bullet.getID());
                break;
            case PlayEvent.USER_BOMBOM:
                user = (User)event.getParams()[0];
                out.writeLong(user.getID());
                bullet = (Bullet)event.getParams()[1];
                out.writeLong(bullet.getID());
                break;
            case PlayEvent.CREATE_BONUS:
                bonus = (Bonus)event.getParams()[0];
                out.writeLong(bonus.getID());
                out.writeInt(bonus.getType());
                out.writeInt(bonus.getDuration());
                out.writeInt(bonus.getX());
                out.writeInt(bonus.getY());
                break;
            case PlayEvent.CATCH_BONUS:
                user = (User)event.getParams()[0];
                out.writeLong(user.getID());
                bonus = (Bonus)event.getParams()[1];
                out.writeLong(bonus.getID());
                break;
        }
    }

    @Override
    public void onClick(int click) {
        super.onClick(click);
        if (click == User.FIRE) {
            fire = true;
        }
    }

    private void fire(User user) {
        if (System.currentTimeMillis() - user.getLastFire() > User.FIRE_INTERVAL) {
            Bullet bullet = bulletsPool.obtain();
            bullet.setup(user, bulletsDefaultSpeed);
            PlayEvent event = eventsPool.obtain();
            event.setup(PlayEvent.USER_FIRE, bullet);
            playEvents.add(event);
            bullets.add(bullet);
            user.updateLastFire();
            if (user == myUser) {
                Music.playSound(user, R.raw.shot_tank, 1, false);
            } else Music.playSound(user, R.raw.shot_tank, 0.5f, false);
        }
    }

    @Override
    protected void bulletsBabah(Bullet bullet, Bullet bullet2) {
        PlayEvent event = eventsPool.obtain();
        event.setup(PlayEvent.BULLETS_BABAH, bullet, bullet2);
        bullets.remove(bullet);
        bullets.remove(bullet2);
        playEvents.add(event);
    }

    @Override
    protected void userBombom(User user, Bullet bullet) {
        user.shot(bullet);
        PlayEvent event = eventsPool.obtain();
        event.setup(PlayEvent.USER_BOMBOM, user, bullet);
        bullets.remove(bullet);
        playEvents.add(event);
        if (user == myUser) updateScreenInfo();
        if (user.getLifes() < 1) {
            if (user == myUser || bullet.getOwner() == myUser) {
                Music.playSound(user, R.raw.explosion, 1, false);
            } else Music.playSound(user, R.raw.explosion, 0.5f, false);
            user.destroy();
        } else {
            Music.playSound(user, R.raw.hit_tank, 1, false);
        }
    }

    @Override
    protected void bulletOnWall(Bullet bullet) {
        PlayEvent event = eventsPool.obtain();
        event.setup(PlayEvent.BULLET_ON_WALL, bullet);
        bullets.remove(bullet);
        playEvents.add(event);
    }

    private void removeUser(User user, boolean notifyOthers) {
        if (user == myUser) {
            killMyUser();
            myUser = null;
        } else {
            clients.remove(user);
        }
        if (getWinner() != null) { // Определен победитель, завершаем игру.
            gameOver();
        } else if (user == myUser) {
            // Если еще нет победителя, но вы уже проиграли, то просто покажем уведомление
            // не завершая игры.
            gameServer.toast(R.string.you_looser);
        } else { // Если проиграл кто-то другой, то тоже покажем уведомление.
            gameServer.toast(tr(R.string.user) + " " + user.getName() + " " + tr(R.string.looser));
        }
        if (!notifyOthers) return;
        int size = clients.size();
        // Уведомляем остальных клиентов об отсоединении юзера
        for (int i = 0; i < size; i++) {
            DataOutputStream out = clients.get(i).out;
            try {
                out.writeInt(GameServer.REMOVE_USER);
                out.writeLong(user.getID());
            } catch (IOException e) {
                e.printStackTrace();
                gameServer.toast("Remove user request error " + e.getMessage());
            }
        }
    }

    private final Runnable bonusRunnable = new Runnable() {
        @Override
        public void run() {
            int bonusType;
            int rnd  = RND.nextInt(100);
            if (rnd < 50) bonusType = Bonus.TYPE_LIFE;
            else bonusType = Bonus.TYPE_TRANSPARENT;

            int duration = 5000 + RND.nextInt(4000);
            Bonus bonus = new Bonus(Bonus.GEN_NEW_ID, bonusType, duration, resources);
            Rect bonusRect = bonus.getBoundsRect();
            moveToFreePlace(bonusRect, clients);
            bonus.setPosition(bonusRect.left, bonusRect.top);
            addBonus(bonus);
            Music.playSound(bonus, R.raw.show_bonus, 1, false);

            PlayEvent event = eventsPool.obtain();
            event.setup(PlayEvent.CREATE_BONUS, bonus);
            playEvents.add(event);

            runDelayed(this, bonus, 7000 + RND.nextInt(10000));
        }
    };

    @Override
    protected void catchBonus(User user, Bonus bonus) {
        applyBonus(user, bonus);
        removeBonus(bonus);
        PlayEvent event = eventsPool.obtain();
        event.setup(PlayEvent.CATCH_BONUS, user, bonus);
        playEvents.add(event);
    }

    private void initWorld() {
        User user;
        ArrayList<User> placedUsers = new ArrayList<User>();
        for (int i = 0, size = clients.size(); i < size; i++) {
            user = clients.get(i);
            placeUser(user, placedUsers);
            user.setSpeed(gameMap.TILE_SIZE / User.DEFAULT_SPEED_FACTOR);
            placedUsers.add(user);
        }
        placeUser(myUser, placedUsers);
        myUser.setSpeed(gameMap.TILE_SIZE / User.DEFAULT_SPEED_FACTOR);
        myUser.changeUnitColor(User.MY_UNIT_COLOR);
    }

    private void placeUser(User user, ArrayList<User> placedUsers) {
        Rect place = user.getBoundsRect();
        moveToFreePlace(place, placedUsers);
        user.setX(place.left);
        user.setY(place.top);
    }

    /* Передвигает rect на свободное место на карте */
    private void moveToFreePlace(Rect rect, ArrayList<? extends User> placedUsers) {
        int mapW = gameMap.mapW;
        int mapH = gameMap.mapH;
        int tileSize = gameMap.TILE_SIZE;
        int n = 0;
        genNewPlace: while (true) {
            n++;
            if (n > 300) { // 300 попыток найти место
                return;
            }
            rect.offsetTo(RND.nextInt(mapW) * tileSize, RND.nextInt(mapH) * tileSize);
            if (gameMap.intersectsWith(rect)) {
                continue;
            }
            for (int i = 0; i < placedUsers.size(); i++) {
                if (Rect.intersects(rect, placedUsers.get(i).getBoundsRect())) {
                    continue genNewPlace;
                }
            }
            ArrayList<Bonus> bonuses = getBonusList();
            for (int i = 0; i < bonuses.size(); i++) {
                if (Rect.intersects(rect, bonuses.get(i).getBoundsRect())) {
                    continue genNewPlace;
                }
            }
            break; // complete
        }
    }

	@Override
	public void stopGame() {
        stopDelayedCallbacks();
		connectionThread.stopRunning();
	}

    private final Pool<PlayEvent> eventsPool = new Pool<PlayEvent>(30, new Pool.ObjectFactory<PlayEvent>() {
        @Override
        public PlayEvent create() {
            return new PlayEvent();
        }
    });

}
