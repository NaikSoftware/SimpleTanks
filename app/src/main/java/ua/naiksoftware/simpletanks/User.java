package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Random;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.ResKeeper;

/**
 * Created by Naik on 07.07.15.
 */
public class User {

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int DOWN = 3;
    public static final int UP = 4;

    private String name;
    private String ip;
    private long id;
    private boolean moved;
    private int direction;
    private int x;
    private int y;
    private Bitmap bitmap;
    private static final Paint paint = new Paint();
    private final int type;

    public User(long id, int type) {
        this.id = id;
        this.type = type;
    }

    public User(String name, String ip, int type) {
        this.name = name;
        id = System.currentTimeMillis() + new Random().nextInt(100);
        this.ip = ip;
        this.type = type;
    }

    public User(String name, long id, String ip, int type) {
        this.name = name;
        this.id = id;
        this.ip = ip;
        this.type = type;
    }

    public void loadResources(Resources res) {
        switch (type) {
            case 1: bitmap = ResKeeper.getImage(ImageID.TANK_1, res);
        }
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

    public void setMove(int click) {

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof User) {
            return ((User) o).getId() == id;
        } else {
            return false;
        }
    }
}
