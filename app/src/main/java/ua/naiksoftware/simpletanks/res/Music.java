package ua.naiksoftware.simpletanks.res;

import android.content.Context;
import android.media.MediaPlayer;

import java.util.HashMap;

/**
 * Created by Naik on 25.07.15.
 */
public class Music {

    private static final HashMap<Object, HashMap<Integer, MediaPlayer>> soundsMap = new HashMap<Object, HashMap<Integer, MediaPlayer>>();
    private static Context context;

    public static void init(Context context) {
        Music.context = context;
    }

    private static MediaPlayer getSound(Object key, int rawID) {
        HashMap<Integer, MediaPlayer> sounds = soundsMap.get(key);
        if (sounds == null) {
            sounds = new HashMap<Integer, MediaPlayer>();
            soundsMap.put(key, sounds);
        }
        MediaPlayer sound = sounds.get(rawID);
        if (sound == null) {
            sound = MediaPlayer.create(context, rawID);
            sounds.put(rawID, sound);
        }
        return sound;
    }

    public static void play(Object key, int rawID, boolean loop) {
        MediaPlayer sound = getSound(key, rawID);
        if (loop) {
            if (!sound.isPlaying()) {
                sound.setLooping(true);
                sound.start();
            }
        } else {
            sound.start();
        }
    }

    public static void stop(Object key, int rawID) {
        MediaPlayer sound = getSound(key, rawID);
        sound.stop();
    }

    public static void stopAll() {
        for (HashMap<Integer, MediaPlayer> sounds : soundsMap.values()) {
            for (MediaPlayer mediaPlayer : sounds.values()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            sounds.clear();
        }
        soundsMap.clear();
    }
}
