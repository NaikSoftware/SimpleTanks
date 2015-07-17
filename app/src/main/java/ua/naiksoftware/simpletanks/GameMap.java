package ua.naiksoftware.simpletanks;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        /* Размер нужен больше на "1" для невыхода за границы массива при просмотре следующих клеток
         * Иначе пришлось бы добавить лишних проверок в основной цикл */
        tiles = new Tile[mapW + 1][mapH + 1];
        for (int i = 0; i < mapW + 1; i++) {
            for (int j = 0; j < mapH + 1; j++) {
                if (i == mapW || j == mapH) {
                    tiles[i][j] = new Tile(ImageID.BRICK, res);
                } else {
                    switch (dis.readByte()) {
                        case 1:
                            tiles[i][j] = new Tile(ImageID.BRICK, res);
                    }
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

    public void intersectWith(User user) {
        Rect rect = user.getBoundsRect();

        if (rect.top < 0) {
            user.setY(0);
            return;
        } else if (rect.bottom > mapHpix) {
            user.setY(mapHpix - rect.height());
            return;
        }
        if (rect.left < 0) {
            user.setX(0);
            return;
        } else if (rect.right > mapWpix) {
            user.setX(mapWpix - rect.width());
            return;
        }

        switch (user.getDirection()) {
            case User.RIGHT:
                if (tiles[(rect.right - 1) / TILE_SIZE][rect.top / TILE_SIZE] != null
                        || tiles[(rect.right - 1) / TILE_SIZE][(rect.bottom - 1) / TILE_SIZE] != null) {
                    user.setX(rect.left - (rect.right % TILE_SIZE));
                }
                break;
            case User.LEFT:
                if (tiles[rect.left / TILE_SIZE][rect.top / TILE_SIZE] != null
                        || tiles[rect.left / TILE_SIZE][(rect.bottom - 1) / TILE_SIZE] != null) {
                    user.setX(rect.left + TILE_SIZE - (rect.left % TILE_SIZE));
                }
                break;
            case User.UP:
                if (tiles[rect.left / TILE_SIZE][rect.top / TILE_SIZE] != null
                        || tiles[(rect.right - 1) / TILE_SIZE][rect.top / TILE_SIZE] != null) {
                    user.setY(rect.top + TILE_SIZE - (rect.top % TILE_SIZE));
                }
                break;
            case User.DOWN:
                if (tiles[rect.left / TILE_SIZE][(rect.bottom - 1) / TILE_SIZE] != null
                        || tiles[(rect.right - 1) / TILE_SIZE][(rect.bottom - 1) / TILE_SIZE] != null) {
                    user.setY(rect.top - (rect.bottom % TILE_SIZE));
                }
                break;
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
