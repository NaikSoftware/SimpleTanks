package ua.naiksoftware.simpletanks;

import ua.naiksoftware.utils.Pool;

/**
 * Created by Naik on 20.07.15.
 */
public class PlayEvent implements Pool.Entry {

    public static final int USER_FIRE = 1;
    public static final int BULLETS_BABAH = 2;
    public static final int USER_BOMBOM = 3;
    public static final int BULLET_ON_WALL = 4;
    public static final int CREATE_BONUS = 5;
    public static final int CATCH_BONUS = 6;

    private int type;
    private Object[] params;
    private boolean released;

    public PlayEvent() {
    }

    public void setup(int type, Object... params) {
        this.type = type;
        this.params = params;
        released = false;
    }

    public Object[] getParams() {
        return params;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean released() {
        return released;
    }

    @Override
    public void release() {
        switch (type) {
            case PlayEvent.BULLET_ON_WALL:
                ((Bullet)params[0]).release();
                break;
            case PlayEvent.BULLETS_BABAH:
                ((Bullet)params[0]).release();
                ((Bullet)params[1]).release();
                break;
            case PlayEvent.USER_BOMBOM:
                ((Bullet)params[1]).release();
                break;

        }
        released = true;
    }

    @Override
    public String toString() {
        String result = "Type " + type + " released=" + released + " ";
        for (int i = 0; i < params.length; i++) {
            result += params[i] + "; ";
        }
        return result;
    }
}
