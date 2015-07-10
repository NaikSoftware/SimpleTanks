package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;

import java.util.ArrayList;

import ua.naiksoftware.simpletanks.User;

/**
 * Created by Naik on 10.07.15.
 */
public interface GameHolder {

    int NO_CLICK = 0;
    int CLICK_UP = 1;
    int CLICK_RIGHT = 2;
    int CLICK_DOWN = 3;
    int CLICK_LEFT = 4;
    int CLICK_FIRE = 5;
    int CLICK_MINE = 6;

    int processActions(int click);
    Activity getActivity();
    GameConnection getGameConnection();
    void drawObjects(Canvas canvas, int deltaTime);

}
