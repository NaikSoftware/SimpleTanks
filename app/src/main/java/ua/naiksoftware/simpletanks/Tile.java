package ua.naiksoftware.simpletanks;

import android.content.res.Resources;
import android.graphics.Bitmap;

import ua.naiksoftware.simpletanks.res.ImageID;
import ua.naiksoftware.simpletanks.res.ResKeeper;

/**
 * Created by Naik on 08.07.15.
 */
public class Tile {

    public final Bitmap bitmap;
    public final ImageID id;

    public Tile(ImageID imageID, Resources resources) {
        bitmap = ResKeeper.getImage(imageID, resources);
        this.id = imageID;
    }



}
