package ua.naiksoftware.simpletanks;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by Naik on 10.07.15.
 */
public class Log {

    private static final String token = " : ";
    private static final long MAX_LEN = 51200;//50 Kb

    public static void d(String tag, String message) {
        try {
            boolean noClear;
            File file = new File(Environment.getExternalStorageDirectory(), "log_simpletanks.txt");
            if (file.length() > MAX_LEN) {
                noClear = false;
            } else {
                noClear = true;
            }
            FileWriter fw = new FileWriter(file, noClear);
            String msg = "\n" + new Date().toString() + token + tag + token + message;
            fw.write(msg);
            fw.flush();
            fw.close();
            //Log.d("L", msg);
        } catch (IOException e) {
            android.util.Log.e("L", "Err in logging", e);
        }
    }

    public static void e(String tag, String message) {
        d(tag, message);
    }

    public static void e(String tag, String message, Exception e) {
        d(tag, e.toString() + "\n         " + e.getMessage() + " :\n         " + message);
    }

}
