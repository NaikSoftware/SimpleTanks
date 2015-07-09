package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.ResKeeper;

/**
 * Created by Naik on 08.07.15.
 */
public class GameMap {

    public final String name;
    private int tileSize;
    private Tile[][] tiles;
    private int mapW, mapH;
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
        tileSize = ResKeeper.getImage(ImageID.BRICK, res).getWidth();
    }

    public void draw(Canvas canvas) {
        int x, y;
        Tile tile;
        for (int i = 0; i < mapW; i++) {
            y = mapY + i * tileSize;
            for (int j = 0; j < mapH; j++) {
                x = mapX + j * tileSize;
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

    public static String assetsPathFromID(int id) {
        switch (id) {
            case 1: return "simple.map";
        }
        throw new IllegalArgumentException("GameMap with ID " + id + " not exists");
    }
}
