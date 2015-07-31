package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.Music;
import ua.naiksoftware.simpletanks.res.ResKeeper;
import ua.naiksoftware.simpletanks.connect.*;
import android.graphics.Color;
import android.graphics.Typeface;

/**
 * Created by Naik on 07.07.15.
 */
public class User {

    private static final String TAG = User.class.getSimpleName();

    /** Флаг для генерации нового ID */
    public static final int GEN_NEW_ID = 0;

    /** Движение влево */
    public static final int LEFT = 0;

    /** Движение вправо */
    public static final int RIGHT = 1;

    /** Движение вниз */
    public static final int DOWN = 2;

    /** Движение вверх */
    public static final int UP = 3;

    /** Выстрел */
    public static final int FIRE = 5;

    /** Мина */
    public static final int MINE = 6;

    /** Интервал выстрелов (по умолчанию 400 мс) */
    public static final int FIRE_INTERVAL = 400;//ms

    /* Некоторые значения по умолчанию */
    private static final int DEFAULT_LIFES = 3;
    private static final int DEFAULT_POWER = 20;
    private static final int DEFAULT_MINES = 0;
    
    private static final int TEXT_SIZE = 13;
    private static final float DEFAULT_MOVE_VOLUME = 0.5f;
    private static final int DEFAULT_COLOR = Color.RED;
    public static final int MY_UNIT_COLOR = Color.BLUE;

    /** Для вычисления скорости юнита {@code GameMap.TILE_SIZE / DEFAULT_SPEED_FACTOR} */
    public static final float DEFAULT_SPEED_FACTOR = 250f;

    /* Поля класса */
    private String name;
    private String ip;
    private long id;
    private int direction;
    private float x, y;
    private final Bitmap[] bitmapArray = new Bitmap[4]; // предзагруженные текстуры
    private Bitmap bitmap; // текущая текстура
    private final Paint paint = new Paint();
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
    private float moveVolume = DEFAULT_MOVE_VOLUME;

     { // Initialize block
        paint.setColor(DEFAULT_COLOR);
        paint.setAntiAlias(true);
        paint.setTextSize(TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * Создать нового юзера (на клиенте или сервере)
     * @param name отображаемое имя
     * @param id если передано {@code GEN_NEW_ID} то будет сгенерирован новый ID
     * @param ip IP юзера, или метка что это Вы или владелец сервера
     */
    public User(String name, long id, String ip) {
        this.name = name;
        if (id == GEN_NEW_ID) this.id = System.currentTimeMillis() + new Random().nextInt(100);
        else this.id = id;
        this.ip = ip;
        direction = UP;
    }

    /**
     * Предзагрузка картинок и т.п. Должен быть вызван до начала игры
     * @param res ресурсы
     */
    public void loadResources(Resources res) {
        bitmapArray[3] = ResKeeper.getImage(ImageID.TANK_1_UP, res);
        bitmapArray[2] = ResKeeper.getImage(ImageID.TANK_1_DOWN, res);
        bitmapArray[1] = ResKeeper.getImage(ImageID.TANK_1_RIGHT, res);
        bitmapArray[0] = ResKeeper.getImage(ImageID.TANK_1_LEFT, res);
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

    public void changeID(long id) {
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

    /**
     * Вызывается в игровом цикле после просчета нового состояния
     * @param canvas
     */
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, x, y, paint);
        canvas.drawText(name, x, y - TEXT_SIZE, paint);
        canvas.drawRect(x, y - TEXT_SIZE / 2, x + spriteSize * (((lifes - 1) * 100f + lifeProgress) / (DEFAULT_LIFES * 100f)), y - TEXT_SIZE / 2 + 3, paint);
    }

    /**
     * Просчет новых координат юнита (вызывается в игровом цикле)
     * @param deltaTime время в мс прошедшее после предыдущего вызова метода
     */
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

    /**
     * Установить состояние движения в какую-то сторону или наоборот остановку. Вызывается в цикле
     * обработки нажатий.
     * @param click NO_CLICK, User.LEFT, and other directions
     */
    public void setMove(int click) {
        if (move == GameHolder.NO_CLICK && click != GameHolder.NO_CLICK) {
            Music.playSound(this, R.raw.move_tank, moveVolume, true);
        } else if (move != GameHolder.NO_CLICK && click == GameHolder.NO_CLICK) {
            Music.stopSound(this, R.raw.move_tank);
        }
        this.move = click;
        if (click == GameHolder.NO_CLICK) return;
        if (click != direction) {
            setDirection(click);
        }
    }

    public int getMove() {
        return move;
    }

    /**
     * Установить новую картинку в зависимости отт выбранного направления движения
     * @param direction User.LEFT, and other directions
     */
    public void setDirection(int direction) {
        bitmap = bitmapArray[direction];
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    /**
     * Место (квадрат) занимаемое юнитом
     * @return
     */
    public Rect getBoundsRect() {
        boundsRect.offsetTo((int)x, (int)y);
        return boundsRect;
    }

    /**
     * Если даный юнит пересекается с {@code user2} то отодвинуть его назад
     * @param user2
     */
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

    /**
     *
     * @return метка времени, когда юнит производил прошлый выстрел
     */
    public long getLastFire() {
        return lastFire;
    }

    /**
     * Обновить метку времени последнего выстрела
     */
    public void updateLastFire() {
        lastFire = System.currentTimeMillis();
    }
    
    public int getLifes() {
        return lifes;
    }
    
    public int getLifeProgress() {
        return lifeProgress;
    }

    /**
     * Пуля попала в даного юнита, снимаем жизни
     * @param bullet
     */
    public void shot(Bullet bullet) {
        lifeProgress -= bullet.getOwner().getPower();
        if (lifeProgress <= 0) {
            lifes--;
            if (lifes > 0) lifeProgress = 100;
            else lifeProgress = 0;
        }
    }

    public void incrementLife() {
        if (lifeProgress == 100 && lifes < DEFAULT_LIFES) lifes++;
        else lifeProgress = 100;
    }

    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    /**
     *
     * @return сила выстрела
     */
    public int getPower() {
        return power;
    }
    
    public int getMinesCount() {
        return minesCount;
    }
    
    public void setMinesCount(int n) {
        minesCount = n;
    }

    /**
     * Установить метку что юнит убит, затем мы сможем удалить с игры даного юнита
     * @see User#isDestroyed()
     */
    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Установка громкости звука движения юнита
     * @param moveVolume 0..1
     */
    public void setMoveVolume(float moveVolume) {
        this.moveVolume = moveVolume;
    }

    public void changeUnitColor(int color) {
        paint.setColor(color);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User && ((User) o).getID() == id;
    }

    /**
     * Переопределено
     * @return {@code name + " -> " + ip}
     */
    @Override
    public String toString() {
        return name + " -> " + ip;
    }
}
