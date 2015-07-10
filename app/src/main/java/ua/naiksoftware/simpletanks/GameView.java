package ua.naiksoftware.simpletanks;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import ua.naiksoftware.simpletanks.connect.GameHolder;

/**
 * Created by Naik on 08.07.15.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread gameThread;

    public GameView(GameHolder gameHolder) {
        super(gameHolder.getActivity());
        gameThread = new GameThread(getHolder(), gameHolder);
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
        destroy();
    }

    public void destroy() {
        if (gameThread == null) {
            return;
        }
        gameThread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try {
                if (gameThread.isAlive() && Thread.currentThread() != gameThread) {
                    gameThread.join();
                }
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                // если не получилось, то будем пытаться еще и еще
            }
        }
        gameThread = null;
    }
}
