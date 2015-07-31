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
 * Бонус на игровом поле. Появляется и исчезает рандомно.
 *
 * Created by Naik on 31.07.15.
 */
public class Bonus {

    /** Дает новую жизнь юниту */
    public static final int TYPE_LIFE = 1;

    /** Делает юнит невидимым на {@code duration} ms */
    public static final int TYPE_TRANSPARENT = 2;

    /** Сгенерировать новый id */
    public static final int GEN_NEW_ID = 0;

    private static final Paint PAINT = new Paint();
    private static final Random RND = new Random();

    private long id;
    private int x, y;
    private Rect boundsRect;
    private Bitmap bitmap;
    private int type;
    private int duration;
    private long startTime;

    /**
     *
     * @param type
     * @param duration время показа бонуса
     * @param res
     */
    public Bonus(long id, int type, int duration, Resources res) {
        if (id == GEN_NEW_ID) {
            this.id = System.currentTimeMillis() + RND.nextInt(100);
        } else {
            this.id = id;
        }
        this.type = type;
        this.duration = duration;
        switch (type) {
            case TYPE_LIFE: bitmap = ResKeeper.getImage(ImageID.BONUS_LIFE, res); break;
            case TYPE_TRANSPARENT: bitmap = ResKeeper.getImage(ImageID.BONUS_TRANSPARENT, res); break;
        }
        boundsRect = new Rect(x, y, bitmap.getWidth(), bitmap.getHeight());
        startTime = System.currentTimeMillis();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     *
     * @return true если бонус "просрочен" и его пора убрать с экрана.
     */
    public boolean timeOver() {
        return (System.currentTimeMillis() - startTime) > duration;
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, x, y, PAINT);
    }

    public Rect getBoundsRect() {
        return boundsRect;
    }

    public long getID() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDuration() {
        return duration;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bonus && ((Bonus)o).id == id;
    }
}
