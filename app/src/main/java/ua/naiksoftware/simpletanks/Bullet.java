package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

import ua.naiksoftware.utils.Pool;

/**
 * Created by Naik on 19.07.15.
 */
public class Bullet  implements Pool.Entry{

    private boolean released;
    private float speed;
    private float x, y;
    private final Rect boundsRect = new Rect(), movedRect = new Rect();
    private User owner;
    public static Bitmap bitmapVert, bitmapHoriz;
    private Bitmap bitmap;
    private static final Paint paint = new Paint();
    private int direction;
    private int width, height;
    private long id;
    private static final Random RND = new Random();

    public Bullet() {
    }

    public Rect draw(Canvas canvas, int deltaTime) {
        float diff = speed * deltaTime;
        switch (direction) {
            case User.UP:
                y -= diff;
                movedRect.set((int)x, (int)y, (int)x + width, (int)(y + diff) + height);
                break;
            case User.DOWN:
                y += diff;
                movedRect.set((int)x, (int)(y - diff), (int)x + width, (int)y + height);
                break;
            case User.LEFT:
                x -= diff;
                movedRect.set((int)x, (int)y, (int)(x + diff) + width, (int)y + height);
                break;
            case User.RIGHT:
                x += diff;
                movedRect.set((int)(x - diff), (int)y, (int)x + width, (int)y + height);
                break;
        }
        canvas.drawBitmap(bitmap, x, y, paint);
        return movedRect;
    }

    public void setup(User owner, float speed) {
        this.speed = speed;
        this.owner = owner;
        this.direction = owner.getDirection();
        Rect rect = owner.getBoundsRect();
        switch (direction) {
            case User.UP:
            case User.DOWN:
                bitmap = bitmapVert;
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                x = rect.left + rect.width() / 2 - width / 2f;
                y = (direction == User.UP ? rect.top : rect.bottom);
                break;
            case User.LEFT:
            case User.RIGHT:
                bitmap = bitmapHoriz;
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                y = rect.top + rect.height() / 2 - height / 2;
                x = (direction == User.LEFT ? rect.left : rect.right);
        }
        id = System.currentTimeMillis() + RND.nextInt(100);
        released = false;
    }

    public void changeID(long id) {
        this.id = id;
    }

    public long getID() {
        return id;
    }

    public Rect getBoundsRect() {
        boundsRect.set((int)x, (int)y, (int)x + width, (int)y + height);
        return boundsRect;
    }

    public User getOwner() {
        return owner;
    }

    public float getSpeed() {
        return speed;
    }

    @Override
    public boolean released() {
        return released;
    }

    @Override
    public void release() {
        id = 0;
        bitmap = null;
        released = true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bullet && ((Bullet)o).id == id;
    }
}
