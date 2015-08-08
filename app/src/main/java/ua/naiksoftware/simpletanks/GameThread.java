package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import ua.naiksoftware.simpletanks.drawable.User;
import ua.naiksoftware.simpletanks.holders.GameHolder;

/**
 * Created by Naik on 08.07.15.
 */
public class GameThread extends Thread {

    private static long FPS, FPS_Count, FPS_Start;
    private static final Paint FPS_Paint = new Paint();
    private boolean running;
    private SurfaceHolder surfaceHolder;
    private Activity activity;
    private GameHolder gameHolder;

    static {
        FPS_Paint.setTextSize(23);
    }

    public GameThread(SurfaceHolder surfaceHolder, final GameHolder gameHolder) {
        super("GameThread");
        this.surfaceHolder = surfaceHolder;
        this.gameHolder = gameHolder;
        activity = gameHolder.getActivity();
        final View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        switch (v.getId()) {
                            case R.id.btnUp: gameHolder.onClick(User.UP); break;
                            case R.id.btnDown: gameHolder.onClick(User.DOWN); break;
                            case R.id.btnLeft: gameHolder.onClick(User.LEFT); break;
                            case R.id.btnRight: gameHolder.onClick(User.RIGHT); break;
                            case R.id.btnFire: gameHolder.onClick(User.FIRE); break;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        gameHolder.onClick(GameHolder.NO_CLICK);
                }
                return false;
            }
        };
        (activity.findViewById(R.id.btnUp)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnDown)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnLeft)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnRight)).setOnTouchListener(listener);
        (activity.findViewById(R.id.btnFire)).setOnTouchListener(listener);
        gameHolder.startGame();
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
                    draw(canvas, deltaTime);
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            deltaTime = (int) (System.currentTimeMillis() - start);
        }
		gameHolder.stopGame();
    }

    private void draw(Canvas canvas, int deltaTime) {
        canvas.save();
        gameHolder.drawGame(canvas, deltaTime);
        canvas.restore();
        canvas.drawText(getFPS(), 10, 25, FPS_Paint);
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
