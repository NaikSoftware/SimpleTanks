package ua.naiksoftware.simpletanks.connect;

import android.app.Activity;
import android.graphics.Canvas;

import java.util.ArrayList;

import ua.naiksoftware.simpletanks.User;
import ua.naiksoftware.simpletanks.GameMap;

/**
 * Created by Naik on 10.07.15.
 */
public abstract class GameHolder {

    public static final int NO_CLICK = -1;
    
    private Activity activity;
    private GameConnection gameConnection;
    private int click = NO_CLICK;
    private GameMap gameMap;
    private ArrayList<? extends User> users;
    private User myUser;
    private int scrW, scrH;
    private int mapWidth, mapHeight;
    private final int tileSize;

    public GameHolder(GameConnection gameConnection, Activity activity) {
        this.gameConnection = gameConnection;
        this.activity = activity;
        users = gameConnection.getUsers();
        myUser = gameConnection.getMyUser();
        gameMap = gameConnection.getGameMap();
        mapWidth = gameMap.mapWpix;
        mapHeight = gameMap.mapHpix;
        tileSize = gameMap.TILE_SIZE;
    }
    
    public abstract void startGame();
    
    public Activity getActivity() {
        return activity;
    }
    
    public void onClick(int click) {
        this.click = click;
    }
    
    protected int myClick() {
        return click;
    }

    public void updateScreen(int w, int h) {
        scrW = w;
        scrH = h;
    }

    public void drawGame(Canvas canvas, int deltaTime) {
        int x = (int)myUser.getX() - scrW / 2 + tileSize / 2;
        int y = (int)myUser.getY() - scrH / 2 + tileSize / 2;
        if (x < 0 || mapWidth < scrW) x = 0;
        else if (x > mapWidth - scrW) x = mapWidth - scrW;
        if (y < 0 || mapHeight < scrH) y = 0;
        else if (y > mapHeight - scrH) y = mapHeight - scrH;
        gameMap.setPosition(x, y);
        gameMap.draw(canvas);
        canvas.translate(-x, -y);
        User user;
        for (int i = 0; i < users.size(); i++) {
            user = users.get(i);
            processUser(user, deltaTime);
            user.draw(canvas);
        }
        processUser(myUser, deltaTime);
        myUser.draw(canvas);
    }

    private void processUser(User user, int deltaTime) {
        if (user.getMove() != NO_CLICK) {
            user.move(deltaTime);
            gameMap.intersectWith(user);
            User user2;
            for (int i = 0; i < users.size(); i++) {
                user2 = users.get(i);
                if (user != user2) user.intersectWith(user2);
            }
            if (user != myUser) user.intersectWith(myUser);
        }
    }
    
	public abstract void stopGame();

}
