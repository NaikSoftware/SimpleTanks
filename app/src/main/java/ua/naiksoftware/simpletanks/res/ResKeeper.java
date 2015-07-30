package ua.naiksoftware.simpletanks.res;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.utils.BitmapUtil;

/**
 * Класс для централизованого получения изображений, их кэширования.
 *
 * Created by Naik on 08.07.15.
 */
public class ResKeeper {

    private static final HashMap<ImageID, Bitmap> cacheImages = new HashMap<ImageID, Bitmap>();

    /**
     * Получить изображение из кэша или если его там нету, то загрузить, добавить в кэш и вернуть
     * @param imageID
     * @param resources
     * @return запрашиваемый ресурс
     */
    public static Bitmap getImage(ImageID imageID, Resources resources) {
        Bitmap bitmap = cacheImages.get(imageID);
        if (bitmap != null) {
            return bitmap;
        }
        switch (imageID) {
            case BRICK: bitmap = BitmapFactory.decodeResource(resources, R.drawable.brick); break;
            case TANK_1_UP: bitmap = BitmapFactory.decodeResource(resources, R.drawable.tank1); break;
            case TANK_1_DOWN: bitmap = BitmapUtil.reflect(getImage(ImageID.TANK_1_UP, resources), BitmapUtil.ReflectType.VERTICAL); break;
            case TANK_1_LEFT: bitmap = BitmapUtil.rotate(getImage(ImageID.TANK_1_UP, resources), -90, false); break;
            case TANK_1_RIGHT: bitmap = BitmapUtil.rotate(getImage(ImageID.TANK_1_UP, resources), 90, false); break;
            case BULLET_VERTICAL: bitmap = BitmapFactory.decodeResource(resources, R.drawable.bullet); break;
            case BULLET_HORIZONTAL: bitmap = BitmapUtil.rotate(getImage(ImageID.BULLET_VERTICAL, resources), 90, false); break;
        }
        if (bitmap != null) {
            cacheImages.put(imageID, bitmap);
        }
        return bitmap;
    }

    /**
     * You must call this method after using images
     */
    public static void clearImageCache() {
        for (Map.Entry<ImageID, Bitmap> entry : cacheImages.entrySet()) {
            entry.getValue().recycle();
        }
        cacheImages.clear();
    }
}
