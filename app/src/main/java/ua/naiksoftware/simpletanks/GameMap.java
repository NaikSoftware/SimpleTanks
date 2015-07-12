package ua.naiksoftware.simpletanks;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.Log;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.ResKeeper;

/**
 * Created by Naik on 08.07.15.
 */
public class GameMap {

    public final String name;
    public final int TILE_SIZE;
    public final Tile[][] tiles;
    public final int mapW, mapH;
    private int mapX, mapY;
    private final int mapWpix, mapHpix;
    private final Paint tilePaint = new Paint();

    public GameMap(InputStream input, Resources res) throws IOException {
        DataInputStream dis = new DataInputStream(input);
        name = dis.readUTF();
        mapW = dis.readInt();
        mapH = dis.readInt();
        tiles = new Tile[mapW][mapH];
        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                switch (dis.readByte()) {
                    case 1: tiles[i][j] = new Tile(ImageID.BRICK, res);
                }
            }
        }
        TILE_SIZE = ResKeeper.getImage(ImageID.BRICK, res).getWidth();
        mapWpix = mapW * TILE_SIZE;
        mapHpix = mapH * TILE_SIZE;
        tilePaint.setColor(Color.DKGRAY);
        tilePaint.setStyle(Paint.Style.STROKE);
    }

    public void draw(Canvas canvas) {
        int fromX = mapX / TILE_SIZE;
        int fromY = mapY / TILE_SIZE;
        int toX = (mapX + canvas.getWidth()) / TILE_SIZE + 1;
        int toY = (mapY + canvas.getHeight()) / TILE_SIZE + 1;
        if (toX > mapW) toX = mapW;
        if (toY > mapY) toY = mapH;
        int fromDrawX = mapX - fromX * TILE_SIZE;
        int fromDrawY = mapY - fromY * TILE_SIZE;
        int x, y;
        Tile tile;
        for (int i = fromX; i < toX; i++) {
            x = fromDrawX + i * TILE_SIZE;
            for (int j = fromY; j < toY; j++) {
                y = fromDrawY + j * TILE_SIZE;
                tile = tiles[i][j];
                if (tile != null) {
                    canvas.drawBitmap(tile.bitmap, x, y, tilePaint);
                }
            }
        }
        canvas.drawRect(mapX, mapY, mapWpix, mapHpix, tilePaint);
    }

    public void setPosition(int x, int y) {
        mapX = x;
        mapY = y;
    }

    // TODO: в релизе слить след. 2 метода в 1 (сделать 1 if)
    public void intersectWithUser(User user) {
        Rect userRect = user.getBoundsRect();

        if (userRect.top < 0) user.setY(0);
        else if (userRect.bottom > mapHpix) user.setY(mapHpix - userRect.height());
        if (userRect.left < 0) user.setX(0);
        else if (userRect.right > mapWpix) user.setX(mapWpix - userRect.width());

        if (tiles[userRect.right / TILE_SIZE][userRect.top / TILE_SIZE] != null){
            backUser(user, userRect); return;
        }
        if (tiles[userRect.right / TILE_SIZE][userRect.bottom / TILE_SIZE] != null) {
            backUser(user, userRect); return;
        }
        if (tiles[userRect.left / TILE_SIZE][userRect.top / TILE_SIZE] != null) {
             backUser(user, userRect); return;
        }
        if (tiles[userRect.left / TILE_SIZE][userRect.bottom / TILE_SIZE] != null) {
            backUser(user, userRect);
        }
    }

    private void backUser(User user, Rect userRect) {
        switch (user.getDirection()) {
            case User.RIGHT:
                user.setX(userRect.left - (userRect.right % TILE_SIZE)); break;
            case User.LEFT:
                user.setX(userRect.left + TILE_SIZE - (userRect.left % TILE_SIZE)); break;
            case User.UP:
                user.setY(userRect.top + TILE_SIZE - (userRect.top % TILE_SIZE)); break;
            case User.DOWN:
                user.setY(userRect.top - (userRect.bottom % TILE_SIZE)); break;
        }
    }

    public static Map<String, String> readMapsList(Resources res) throws IOException {
        Map<String,String> maps = new HashMap<String, String>();
        AssetManager assets = res.getAssets();
        for (String path : assets.list("")) {
            try {
                maps.put(new DataInputStream(assets.open(path)).readUTF(), path);
            } catch (IOException e) {
                // ignore for skip built-in folders (images, sounds, webkit) ...
            }
        }
        return maps;
    }
}
