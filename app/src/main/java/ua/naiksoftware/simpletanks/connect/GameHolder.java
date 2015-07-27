package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;

import ua.naiksoftware.simpletanks.Bullet;
import ua.naiksoftware.simpletanks.User;
import ua.naiksoftware.simpletanks.GameMap;
import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.Music;
import ua.naiksoftware.simpletanks.res.ResKeeper;
import ua.naiksoftware.utils.Pool;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import ua.naiksoftware.simpletanks.R;

/**
 * Created by Naik on 10.07.15.
 */
public abstract class GameHolder {

    public static final int NO_CLICK = -1;

    private static final int COLOR_BG = Color.DKGRAY;
    private static final int  COLOR_MAP_BG = 0xFF5F9EA0;

    private static final Paint PAINT_MAP_BG = new Paint();

    static {
        PAINT_MAP_BG.setColor(COLOR_MAP_BG);
    }

    private final Activity activity;
    private final GameConnection gameConnection;
    private int click = NO_CLICK;
    private GameMap gameMap;
    private ArrayList<? extends User> users;
    private User myUser;
    private int scrW, scrH;
    private int mapWidth, mapHeight;
    private final int tileSize;
    protected final float bulletsDefaultSpeed;
    private final ArrayList<Bullet> bullets = new ArrayList<Bullet>();
    protected final Pool<Bullet> bulletsPool;
    private ProgressBar lifeProgressBar;
    private TextView lifesTextView, minesTextView;
    private boolean myUserKilled;

    public GameHolder(GameConnection gameConnection, final Activity activity) {
        this.gameConnection = gameConnection;
        this.activity = activity;
        users = gameConnection.getUsers();
        myUser = gameConnection.getMyUser();
        gameMap = gameConnection.getGameMap();
        mapWidth = gameMap.mapWpix;
        mapHeight = gameMap.mapHpix;
        tileSize = gameMap.TILE_SIZE;
        bulletsDefaultSpeed = tileSize / 100f;
        bulletsPool = new Pool<Bullet>(10, new Pool.ObjectFactory<Bullet>() {
            {
                Bullet.bitmapVert = ResKeeper.getImage(ImageID.BULLET_VERTICAL, activity.getResources());
                Bullet.bitmapHoriz = ResKeeper.getImage(ImageID.BULLET_HORIZONTAL, activity.getResources());
            }
            @Override
            public Bullet create() {
                return new Bullet();
            }
        });
        myUser.setMoveVolume(1.0f); // Движение своего юнита делаем на всю громкость
    }

    protected String tr(int stringID) {
        return activity.getString(stringID);
    }
    
    public abstract void startGame();
    
    public Activity getActivity() {
        return activity;
    }
    
    public void onClick(int click) {
        this.click = click;
    }
    
    protected int myClick() {
        return click;
    }

    public void setupScreen(int w, int h) {
        scrW = w;
        scrH = h;
        lifesTextView = (TextView)activity.findViewById(R.id.lifes);
        minesTextView = (TextView)activity.findViewById(R.id.mines);
        lifeProgressBar = (ProgressBar) activity.findViewById(R.id.lifesProgressBar);
        lifeProgressBar.setMax(100); // %
        updateScreenInfo();
    }

    public void drawGame(Canvas canvas, int deltaTime) {
        canvas.drawColor(COLOR_BG); // Clear canvas

        int x = (int)myUser.getX() - scrW / 2 + tileSize / 2;
        int y = (int)myUser.getY() - scrH / 2 + tileSize / 2;

        if (x < 0 || mapWidth < scrW) x = 0;
        else if (x > mapWidth - scrW) x = mapWidth - scrW;

        if (y < 0 || mapHeight < scrH) y = 0;
        else if (y > mapHeight - scrH) y = mapHeight - scrH;

        if (mapWidth < scrW) canvas.translate(scrW / 2 - mapWidth / 2, 0);
        if (mapHeight < scrH) canvas.translate(0, scrH / 2 - mapHeight / 2);

        canvas.drawRect(0, 0, mapWidth, mapHeight, PAINT_MAP_BG);
        gameMap.setPosition(x, y);
        gameMap.draw(canvas);
        canvas.translate(-x, -y);
        Bullet bullet, bullet2;
        Rect rect;
        User user;
        bulletsIteration: for (int i = 0; i < bullets.size(); i++) {
            bullet = bullets.get(i);
            rect = bullet.draw(canvas, deltaTime);
            if (gameMap.intersectsWith(rect)) {
                bulletOnWall(bullet);
                continue;
            }
            for (int j = 0; j < bullets.size(); j++) {
                bullet2 = bullets.get(j);
                if (bullet == bullet2) continue;
                if (Rect.intersects(rect, bullet2.getBoundsRect())) {
                    bulletsBabah(bullet, bullet2);
                    continue bulletsIteration; // Только 2 пули могут столкнуться одновременно
                }
            }
            for (int j = 0; j < users.size(); j++) {
                user = users.get(j);
                if (user == bullet.getOwner()) continue;
                if (Rect.intersects(rect, user.getBoundsRect())) {
                    userBombom(user, bullet);
                    continue bulletsIteration; // Одна пуля может поразить только одного юнита
                }
            }
            if (!myUserKilled && myUser != bullet.getOwner() && Rect.intersects(rect, myUser.getBoundsRect())) {
                userBombom(myUser, bullet);
            }
        }
        for (int i = 0; i < users.size(); i++) {
            user = users.get(i);
            processUser(user, deltaTime);
            user.draw(canvas);
        }
        if (!myUserKilled) {
            processUser(myUser, deltaTime);
            myUser.draw(canvas);
        }
    }

    private void processUser(User user, int deltaTime) {
        if (user.getMove() != NO_CLICK) {
            user.move(deltaTime);
            gameMap.intersectWith(user);
            User user2;
            for (int i = 0; i < users.size(); i++) {
                user2 = users.get(i);
                if (user != user2) user.intersectWith(user2);
            }
            if (!myUserKilled && user != myUser) user.intersectWith(myUser);
        }
    }
    
    protected void killMyUser() {
        myUserKilled = true;
    }
    
    protected void updateScreenInfo() {
        gameConnection.inUI(new Runnable(){
                @Override
                public void run() {
                    lifesTextView.setText(String.valueOf(myUser.getLifes()));
                    lifeProgressBar.setProgress(myUser.getLifeProgress());
                    minesTextView.setText(String.valueOf(myUser.getMinesCount()));
                }
            });
    }
    
    protected void gameOver() {
        final User winner = getWinner();
        final View v = LayoutInflater.from(activity).inflate(R.layout.game_over, null);
        TextView msgView = (TextView)v.findViewById(R.id.game_over_message);
        TextView subMsgView = (TextView)v.findViewById(R.id.sub_game_over_message);
        if (winner == myUser) {
            msgView.setText(R.string.you_winner);
            Music.playMusic(this, R.raw.music_game_won, false);
        } else {
            msgView.setText(R.string.you_looser);
            Music.playMusic(this, R.raw.music_game_lost, false);
            if (users.size() == 1) {
                subMsgView.setText(tr(R.string.user) + " " + winner.getName() + " " + tr(R.string.winner));
            }
            users.remove(winner);
        }
        gameConnection.inUI(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity)
                        .setView(v)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (winner == myUser) Music.stopMusic(GameHolder.this, R.raw.music_game_won);
                                else Music.stopMusic(GameHolder.this, R.raw.music_game_lost);
                                gameConnection.stop();
                            }
                        })
                        .show();
            }
        });
    }

    protected User getWinner() {
        User winner = null;
        if (myUserKilled && users.size() == 1 || !myUserKilled && users.size() == 0) {
            winner = !myUserKilled ? myUser : users.get(0);
        }
        return winner;
    }

    protected ArrayList<Bullet> getBullets() {
        return bullets;
    }

    protected abstract void bulletsBabah(Bullet bullet, Bullet bullet2);
    protected abstract void userBombom(User user, Bullet bullet);
    protected abstract void bulletOnWall(Bullet bullet);
    
	public abstract void stopGame();

}
