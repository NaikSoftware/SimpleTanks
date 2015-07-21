package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.ResKeeper;
import ua.naiksoftware.simpletanks.connect.*;
import android.graphics.Color;
import android.graphics.Typeface;

/**
 * Created by Naik on 07.07.15.
 */
public class User {

    private static final String TAG = User.class.getSimpleName();

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int UP = 3;
    public static final int FIRE = 5;
    public static final int MINE = 6;

    public static final int FIRE_INTERVAL = 400;//ms
    
    private static final int DEFAULT_LIFES = 3;
    private static final int DEFAULT_POWER = 20;
    private static final int DEFAULT_MINES = 0;
    
    private static final int TEXT_SIZE = 13;

    private String name;
    private String ip;
    private long id;
    private int direction;
    private float x, y;
    private final Bitmap[] bitmapArray = new Bitmap[4];
    private Bitmap bitmap;
    private static final Paint paint = new Paint();
    private final int type;
    private float speed;
    private final Rect boundsRect = new Rect();
    private int spriteSize;
	private int move = GameHolder.NO_CLICK;
    private long lastFire = System.currentTimeMillis();
    private int lifes = DEFAULT_LIFES;
    private int lifeProgress = 100; // %
    private int power = DEFAULT_POWER;
    private int minesCount = DEFAULT_MINES;
    private boolean destroyed;

    static {
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
    }
    
    public User(long id, int type) {
        this.id = id;
        this.type = type;
        direction = UP;
    }

    public User(String name, String ip, int type) {
        this.name = name;
        id = System.currentTimeMillis() + new Random().nextInt(100);
        this.ip = ip;
        this.type = type;
        direction = UP;
    }

    public User(String name, long id, String ip, int type) {
        this.name = name;
        this.id = id;
        this.ip = ip;
        this.type = type;
        direction = UP;
    }

    public void loadResources(Resources res) {
        switch (type) {
            case 1:
                bitmapArray[3] = ResKeeper.getImage(ImageID.TANK_1_UP, res);
                bitmapArray[2] = ResKeeper.getImage(ImageID.TANK_1_DOWN, res);
                bitmapArray[1] = ResKeeper.getImage(ImageID.TANK_1_RIGHT, res);
                bitmapArray[0] = ResKeeper.getImage(ImageID.TANK_1_LEFT, res);
        }
        bitmap = bitmapArray[direction];
        spriteSize = bitmap.getWidth();
        boundsRect.set(0, 0, spriteSize, spriteSize);
    }

    public long getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, x, y, paint);
        canvas.drawText(name, x, y - TEXT_SIZE, paint);
    }

    public void move(int deltaTime) {
        if (move == GameHolder.NO_CLICK) return;
        float diff = speed * deltaTime;
        switch (direction) {
            case UP: y -= diff; break;
            case DOWN: y += diff; break;
            case LEFT: x -= diff; break;
            case RIGHT: x += diff; break;
        }
    }

    public void setMove(int click) {
        this.move = click;
        if (click == GameHolder.NO_CLICK) return;
        if (click != direction) {
            setDirection(click);
        }
    }

    public int getMove() {
        return move;
    }

    public void setDirection(int direction) {
        bitmap = bitmapArray[direction];
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    public Rect getBoundsRect() {
        boundsRect.offsetTo((int)x, (int)y);
        return boundsRect;
    }

    public void intersectWith(User user2) {
        Rect user2Bounds = user2.getBoundsRect();
        Rect userBounds = getBoundsRect();
        if (Rect.intersects(userBounds, user2Bounds)) {
            switch (move) {
                case User.UP: y = user2Bounds.bottom; break;
                case User.DOWN: y = user2Bounds.top - userBounds.height(); break;
                case User.LEFT: x = user2Bounds.right; break;
                case User.RIGHT: x = user2Bounds.left - userBounds.width(); break;
            }
        }
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getLastFire() {
        return lastFire;
    }

    public void updateLastFire() {
        lastFire = System.currentTimeMillis();
    }
    
    public int getLifes() {
        return lifes;
    }
    
    public int getLifeProgress() {
        return lifeProgress;
    }
    
    public void shot(Bullet bullet) {
        lifeProgress -= bullet.getOwner().getPower();
        if (lifeProgress <= 0) {
            lifes--;
            if (lifes > 0) lifeProgress = 100;
            else lifeProgress = 0;
        }
    }
    
    public int getPower() {
        return power;
    }
    
    public int getMinesCount() {
        return minesCount;
    }
    
    public void setMinesCount(int n) {
        minesCount = n;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User && ((User) o).getID() == id;
    }
}
