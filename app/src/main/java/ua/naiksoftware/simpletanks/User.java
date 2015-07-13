package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.ResKeeper;

/**
 * Created by Naik on 07.07.15.
 */
public class User {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int UP = 3;
    public static final int FIRE = 5;
    public static final int MINE = 6;

    private String name;
    private String ip;
    private long id;
    private boolean moved;
    private int direction;
    private int x;
    private int y;
    private final Bitmap[] bitmapArray = new Bitmap[4];
    private Bitmap bitmap;
    private static final Paint paint = new Paint();
    private final int type;
    private float speed;
    private final Rect boundsRect = new Rect();
    private int transparentWidth;
    private int spriteSize;

    public User(long id, int type) {
        this.id = id;
        this.type = type;
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
        transparentWidth = spriteSize / 6;
    }

    public long getId() {
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
        moved = false;
        canvas.drawBitmap(bitmap, x, y, paint);
    }

    public void setMove(int click) {
        moved = true;
        setDirection(click);
    }

    public void move(int click, int deltaTime) {
        setMove(click);
        int diff = (int)(speed * deltaTime);
        switch (click) {
            case UP: y -= diff; break;
            case DOWN: y += diff; break;
            case LEFT: x -= diff; break;
            case RIGHT: x += diff; break;
        }
    }

    public void setDirection(int direction) {
        if (direction != this.direction) {
            this.direction = direction;
            bitmap = bitmapArray[direction];
            switch (direction) {
                case User.LEFT:
                    if (direction != User.RIGHT) {
                        x -= transparentWidth;
                    }
                    break;
                case User.RIGHT:
                    if (direction != User.LEFT) {
                        x += transparentWidth;
                    }
                    break;
                case User.DOWN:
                    if (direction != User.UP) {
                        y += transparentWidth;
                    }
                    break;
                case User.UP:
                    if (direction != User.DOWN) {
                        y -= transparentWidth;
                    }
                    break;
            }
        }
    }

    public int getDirection() {
        return direction;
    }

    public Rect getBoundsRect() {
        if (direction == UP || direction == DOWN) {
            boundsRect.set(x + transparentWidth, y, x + spriteSize - transparentWidth, y + spriteSize);
        } else {
            boundsRect.set(x, y + transparentWidth, x + spriteSize, y + spriteSize - transparentWidth);
        }
        return boundsRect;
    }

    public int getX() {
        return x;
    }

    public void setX(float x) {
        this.x = (int)x;
    }

    public int getY() {
        return y;
    }

    public void setY(float y) {
        this.y = (int)y;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof User) {
            return ((User) o).getId() == id;
        } else {
            return false;
        }
    }
}
