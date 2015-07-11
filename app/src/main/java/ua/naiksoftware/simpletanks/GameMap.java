package ua.naiksoftware.simpletanks;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    }

    public void draw(Canvas canvas) {
        int x, y;
        Tile tile;
        for (int i = 0; i < mapW; i++) {
            x = mapX + i * TILE_SIZE;
            for (int j = 0; j < mapH; j++) {
                y = mapY + j * TILE_SIZE;
                tile = tiles[i][j];
                if (tile != null) {
                    canvas.drawBitmap(tile.bitmap, x, y, tilePaint);
                }
            }
        }
    }

    public void setPosition(int x, int y) {
        mapX = x;
        mapY = y;
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
