package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.SurfaceHolder;

import ua.naiksoftware.simpletanks.connect.GameConnection;
import ua.naiksoftware.simpletanks.connect.GameHolder;

/**
 * Created by Naik on 08.07.15.
 */
public class GameThread extends Thread {

    private static final int COLOR_BG = 0xFF5F9EA0;
    private static long FPS, FPS_Count, FPS_Start;
    private static final Paint FPS_Paint = new Paint();
    private boolean running;
    private SurfaceHolder surfaceHolder;
    private Activity activity;
    private GameHolder gameHolder;
    private GameMap gameMap;

    public GameThread(SurfaceHolder surfaceHolder, GameHolder gameHolder) {
        super("GameThread");
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
        long start;
        int deltaTime = 0;
        while (running) {
            start = System.currentTimeMillis();
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    continue;
                }
                synchronized (surfaceHolder) {
                    gameHolder.processActions(deltaTime);
                    draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            deltaTime = (int) (System.currentTimeMillis() - start);
        }
    }

    private void draw(Canvas canvas) {
        canvas.drawColor(COLOR_BG);
        gameMap.draw(canvas);
        gameHolder.drawObjects(canvas);
        canvas.drawText(getFPS(), 10, 10, FPS_Paint);
    }

    private static String getFPS() {
        FPS_Count++;
        if (FPS_Start == 0) {
            FPS_Start = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - FPS_Start >= 1000) {
            FPS = FPS_Count;
            FPS_Count = 0;
            FPS_Start = System.currentTimeMillis();
        }
        return Long.toString(FPS);
    }
}
