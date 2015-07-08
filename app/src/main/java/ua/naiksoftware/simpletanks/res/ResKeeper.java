package ua.naiksoftware.simpletanks.res;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

import ua.naiksoftware.simpletanks.R;

/**
 * Created by Naik on 08.07.15.
 */
public class ResKeeper {

    private static final HashMap<ImageID, Bitmap> cacheImages = new HashMap<>();

    public static Bitmap getImage(ImageID imageID, Resources resources) {
        Bitmap bitmap = cacheImages.get(imageID);
        if (bitmap != null) {
            return bitmap;
        }
        switch (imageID) {
            case BRICK: bitmap = BitmapFactory.decodeResource(resources, R.drawable.brick);
        }
        if (bitmap != null) {
            cacheImages.put(imageID, bitmap);
        }
        return bitmap;
    }

    public static void clearImageCache() {
        for (Map.Entry<ImageID, Bitmap> entry : cacheImages.entrySet()) {
            entry.getValue().recycle();
        }
        cacheImages.clear();
    }
}
