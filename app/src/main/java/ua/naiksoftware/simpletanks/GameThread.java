package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

import ua.naiksoftware.simpletanks.connect.GameConnection;
import ua.naiksoftware.simpletanks.connect.GameHolder;

/**
 * Created by Naik on 08.07.15.
 */
public class GameThread extends Thread {

    private static final int COLOR_BG = 0xFF5F9EA0;

    private boolean running;
    private SurfaceHolder surfaceHolder;
    private Activity activity;
    private GameHolder gameHolder;
    private GameMap gameMap;

    public GameThread(SurfaceHolder surfaceHolder, GameHolder gameHolder) {
        this.surfaceHolder = surfaceHolder;
        this.gameHolder = gameHolder;
        activity = gameHolder.getActivity();
        gameMap = gameHolder.getGameConnection().getGameMap();
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        running = true;
        Canvas canvas = null;
        while (running) {
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    continue;
                }
                synchronized (surfaceHolder) {
                    draw(canvas, gameHolder.processActions(0));
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void draw(Canvas canvas, int deltaTime) {
        canvas.drawColor(COLOR_BG);
        gameMap.draw(canvas);
        gameHolder.drawObjects(canvas, deltaTime);
    }
}
