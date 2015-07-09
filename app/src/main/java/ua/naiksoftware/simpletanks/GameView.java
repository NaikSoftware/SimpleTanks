package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import ua.naiksoftware.simpletanks.connect.GameConnection;

/**
 * Created by Naik on 08.07.15.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread gameThread;

    public GameView(Activity activity, GameConnection gameConnection) {
        super(activity);
        gameThread = new GameThread(getHolder(), activity, gameConnection);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        gameThread.setRunning(false);
        gameThread.interrupt();
        boolean retry = true;
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // если не получилось, то будем пытаться еще и еще
            }
        }
    }
}
