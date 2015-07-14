package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;

import java.util.ArrayList;

import ua.naiksoftware.simpletanks.User;

/**
 * Created by Naik on 10.07.15.
 */
public interface GameHolder {

    int NO_CLICK = -1;

    void startGame();
    Activity getActivity();
    void onClick(int click);
    GameConnection getGameConnection();
    void drawObjects(Canvas canvas, int deltaTime);
	void stopGame();

}
