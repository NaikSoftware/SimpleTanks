package ua.naiksoftware.simpletanks.res;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.HashMap;

/**
 * Created by Naik on 25.07.15.
 */
public class Music {

    private static Context context;

    /* Music */
    private static final HashMap<Object, SparseArray<MediaPlayer>> musicsMap = new HashMap<Object, SparseArray<MediaPlayer>>();
    private static Handler handler;
    private static final int EXIT = 1;
    private static final Thread playThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Looper.myLooper().quit();
                }
            };
            Looper.loop();
        }
    });

    /* Sound */
    private static SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    private static final HashMap<Object, SparseIntArray> soundsMap = new HashMap<Object, SparseIntArray>();

    public static void init(Context context) {
        Music.context = context;
        playThread.start();
    }

    private static MediaPlayer getMusic(Object key, int rawID) {
        SparseArray<MediaPlayer> musics = musicsMap.get(key);
        if (musics == null) {
            musics = new SparseArray<MediaPlayer>();
            musicsMap.put(key, musics);
        }
        MediaPlayer music = musics.get(rawID);
        if (music == null) {
            music = MediaPlayer.create(context, rawID);
            musics.put(rawID, music);
        }
        return music;
    }

    public static void playMusic(final Object key, final int rawID, final boolean loop) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                MediaPlayer music = getMusic(key, rawID);
                if (loop) {
                    if (!music.isPlaying()) {
                        music.setLooping(true);
                        music.start();
                    }
                } else {
                    if (music.isPlaying()) {
                        music.seekTo(0);
                    }
                    music.start();
                }
            }
        });
    }

    public static void stopMusic(Object key, int rawID) {
        MediaPlayer sound = getMusic(key, rawID);
        sound.stop();
    }

    public static void playSound(Object key, int rawID, float volume, boolean loop) {
        SparseIntArray sounds = soundsMap.get(key);
        if (sounds == null) {
            sounds = new SparseIntArray();
            soundsMap.put(key, sounds);
        }
        int soundID = sounds.get(rawID);
        if (soundID == 0) {
            soundID = soundPool.load(context, rawID, 1);
            sounds.put(rawID, soundID);
        }
        if (!loop) {
            soundPool.play(soundID, volume, volume, 0, 0, 1);
        }
    }

    public static void stopSound(Object key, int rawID) {
        SparseIntArray sounds = soundsMap.get(key);
        Integer soundID = sounds.get(rawID);
        soundPool.stop(soundID);
    }

    public static void stopAll() {
        for (SparseArray<MediaPlayer> musics : musicsMap.values()) {
            for (int i = 0, size = musics.size(); i < size; i++) {
                MediaPlayer mediaPlayer = musics.get(i);
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
            }
            musics.clear();
        }
        musicsMap.clear();
        handler.sendEmptyMessage(EXIT);
        for (SparseIntArray sounds : soundsMap.values()) {
            for (int i = 0, size = sounds.size(); i < size; i++) {
                int soundID = sounds.get(i);
                if (soundID != 0) {
                    soundPool.stop(soundID);
                    soundPool.unload(soundID);
                }
            }
            sounds.clear();
        }
        soundsMap.clear();
        soundPool.release();
    }
}
