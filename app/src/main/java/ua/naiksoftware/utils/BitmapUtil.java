package ua.naiksoftware.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by Naik on 11.07.15.
 */
public class BitmapUtil {

    private static final Matrix matrix = new Matrix();

    public static Bitmap rotate(Bitmap bitmap, int deg, boolean crop) {
        matrix.reset();
        matrix.preRotate(deg);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (crop) {
            int diffX = (resizedBitmap.getWidth() - bitmap.getWidth()) / 2;
            int diffY = (resizedBitmap.getHeight() - bitmap.getHeight()) / 2;
            if (diffX < 0) diffX = 0; // Если после поворота размер не увеличился, а уменьшился,
            if (diffY < 0) diffY = 0; // то игнорируем его.
            Bitmap resultBitmap = Bitmap.createBitmap(resizedBitmap, diffX, diffY, bitmap.getWidth(), bitmap.getHeight());
            resizedBitmap.recycle();
            return resultBitmap;
        } else {
            return resizedBitmap;
        }
    }

    public static Bitmap reflect(Bitmap bitmap, ReflectType type) {
        float w = 1, h = 1;
        switch (type) {
            case HORIZONTAL:
                w = -1;
                break;
            case VERTICAL:
                h = -1;
                break;
            case COMBINE:
                w = h = -1;
                break;
        }
        matrix.reset();
        matrix.preScale(w, h);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public enum ReflectType {
        HORIZONTAL, VERTICAL, COMBINE
    }

}
