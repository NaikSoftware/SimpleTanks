package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Created by Naik on 08.07.15.
 */
public class GameThread extends Thread {

    private boolean running;
    private SurfaceHolder surfaceHolder;
    private Activity activity;

    public GameThread(SurfaceHolder surfaceHolder, Activity activity) {
        this.surfaceHolder = surfaceHolder;
        this.activity = activity;
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
                synchronized (surfaceHolder) {
                    draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void draw(Canvas canvas) {

    }
}
