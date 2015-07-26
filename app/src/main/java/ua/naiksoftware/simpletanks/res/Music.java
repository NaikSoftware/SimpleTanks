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

    /* Sound */
    private static SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    private static final HashMap<Object, SparseIntArray> soundsMap = new HashMap<Object, SparseIntArray>();
    private static final SparseIntArray playingSounds = new SparseIntArray();

    public static void init(Context context) {
        Music.context = context;
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

    public static void stopMusic(Object key, int rawID) {
        MediaPlayer sound = getMusic(key, rawID);
        sound.stop();
    }

    private static SparseIntArray sounds;
    public static synchronized void playSound(final Object key, final int rawID, final float volume, final boolean loop) {
        sounds = soundsMap.get(key);
        if (sounds == null) {
            sounds = new SparseIntArray();
            soundsMap.put(key, sounds);
        }
        int soundID = sounds.get(rawID);
        if (soundID == 0) {
            final long startLoad = System.currentTimeMillis();
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    sounds.put(rawID, sampleId);
                    long loadTime = System.currentTimeMillis() - startLoad;
                    if (loadTime < 500) {
                        // Проиграть звук только если прошло не более 500 мс после начала загрузки
                        playSound(key, rawID, volume, loop);
                    }
                }
            });
            soundPool.load(context, rawID, 1);
            return;
        }
        int loops = loop ? Integer.MAX_VALUE : 0;
        if (playingSounds.get(soundID) == 0) { // Если этот звук еще не играет
            playingSounds.put(soundID, soundPool.play(soundID, volume, volume, 0, loops, 1));
        }
    }

    public static synchronized void stopSound(Object key, int rawID) {
        SparseIntArray sounds = soundsMap.get(key);
        int soundID = sounds.get(rawID);
        int playID = playingSounds.get(soundID);
        soundPool.stop(playID);
        playingSounds.delete(soundID);
    }

    public static synchronized void stopAll() {
        for (SparseArray<MediaPlayer> musics : musicsMap.values()) {
            for (int i = 0, size = musics.size(); i < size; i++) {
                MediaPlayer mediaPlayer = musics.valueAt(i);
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            musics.clear();
        }
        musicsMap.clear();
        for (SparseIntArray sounds : soundsMap.values()) {
            for (int i = 0, size = sounds.size(); i < size; i++) {
                int soundID = sounds.get(i);
                int playID = playingSounds.get(soundID);
                if (playID != 0) soundPool.stop(playID);
                if (soundID != 0) soundPool.unload(soundID);
            }
            sounds.clear();
        }
        playingSounds.clear();
        soundsMap.clear();
        soundPool.release();
    }
}
