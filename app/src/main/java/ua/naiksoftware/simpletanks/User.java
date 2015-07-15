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
	private int move = GameHolder.NO_CLICK;

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
        canvas.drawBitmap(bitmap, x, y, paint);
    }

    public void move(int deltaTime) {
        if (move == GameHolder.NO_CLICK) return;
        int diff = (int)(speed * deltaTime);
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
            switch (click) {
                case User.LEFT:
                    if (this.direction != User.RIGHT) {
                        x -= transparentWidth;
                    }
                    break;
                case User.RIGHT:
                    if (this.direction != User.LEFT) {
                        x += transparentWidth;
                    }
                    break;
                case User.DOWN:
                    if (this.direction != User.UP) {
                        y += transparentWidth;
                    }
                    break;
                case User.UP:
                    if (this.direction != User.DOWN) {
                        y -= transparentWidth;
                    }
                    break;
            }
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
        if (direction == UP || direction == DOWN) {
            boundsRect.set(x + transparentWidth, y, x + spriteSize - transparentWidth, y + spriteSize);
        } else {
            boundsRect.set(x, y + transparentWidth, x + spriteSize, y + spriteSize - transparentWidth);
        }
        return boundsRect;
    }

    public void intersectWith(User user2) {
        Rect user2Bounds = user2.getBoundsRect();
        Rect userBounds = getBoundsRect();
        if (Rect.intersects(userBounds, user2Bounds)) {
            switch (getMove()) {
                case User.UP: y = user2Bounds.bottom; break;
                case User.DOWN: y = user2Bounds.top - userBounds.height(); break;
                case User.LEFT: x = user2Bounds.right; break;
                case User.RIGHT: x = user2Bounds.left - userBounds.width(); break;
            }
        }
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
        return o instanceof User && ((User) o).getId() == id;
    }
}
