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

    public GameHolder(GameConnection gameConnection, Activity activity) {
        this.gameConnection = gameConnection;
        this.activity = activity;
        users = gameConnection.getUsers();
        myUser = gameConnection.getMyUser();
        gameMap = gameConnection.getGameMap();
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
    
    public GameConnection getGameConnection() {
        return gameConnection;
    }
    
    public void drawObjects(Canvas canvas, int deltaTime) {
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
