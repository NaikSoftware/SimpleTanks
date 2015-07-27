package ua.naiksoftware.simpletanks.res;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.HashMap;

/**
 * Created by Naik on 25.07.15.
 *
 *      Utility for easy music and sounds thread safe playback.
 *      Usage: in main activity in onCreate method call Music.init(getContext()),
 *      in onDestroy call Music.dispose(). After this you can play music and sounds anywhere:
 *      Music.playMusic(keyObject, R.raw.test)
 */
public class Music {

    private static Context context;

    /** Music mapping: key -> (rawID -> MediaPlayer) */
    private static final HashMap<Object, SparseArray<MediaPlayer>> musicsMap = new HashMap<Object, SparseArray<MediaPlayer>>();

    private static SoundPool soundPool;

    /** Loaded sounds mapping: rawID -> soundID */
    private static final SparseIntArray loadedSounds = new SparseIntArray();

    /** Play sounds mapping: key -> (soundID -> playID) */
    private static final HashMap<Object, SparseIntArray> soundsMap = new HashMap<Object, SparseIntArray>();

    /**
     * You must call this method before using class.
     * @param context
     */
    public static void init(Context context) {
        Music.context = context;
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
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

    /**
     * Run playback long music file, for short see Music.playSound
     * @param key - key for assign player to specified object
     * @param rawID - R.raw.[you music in /res/raw directory]
     * @param loop - if true - play looping, if already playing - doing nothing
     */
    public static synchronized void playMusic(Object key, int rawID, boolean loop) {
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

    /**
     * Stop music by id for key object
     * @param key - key object
     * @param rawID - R.raw.[...]
     */
    public static synchronized void stopMusic(Object key, int rawID) {
        MediaPlayer sound = getMusic(key, rawID);
        sound.stop();
    }

    /**
     * Run playback sort music (sound). Sound will be loaded into memory.
     * @param key - key for assign player to specified object
     * @param rawID - R.raw.[...]
     * @param volume - volume for left and right channels
     * @param loop - if true - play looping, if already playing - doing nothing
     */
    public static synchronized void playSound(final Object key, final int rawID, final float volume, final boolean loop) {
        SparseIntArray playingSounds = soundsMap.get(key);
        if (playingSounds == null) {
            playingSounds = new SparseIntArray();
            soundsMap.put(key, playingSounds);
        }
        int soundID = loadedSounds.get(rawID, -1);
        if (soundID == -1) {
            final long startLoad = System.currentTimeMillis();
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    loadedSounds.put(rawID, sampleId);
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
        int loops = loop ? -1 : 0;
        int playID = playingSounds.get(soundID, -1);
        if (playID == -1) { // Если этот звук еще не играет - играем.
            playingSounds.put(soundID, soundPool.play(soundID, volume, volume, 0, loops, 1));
        } else if (!loop){ // Иначе остановим его и проиграем (если запущено в цикле, ничего не делаем).
            soundPool.stop(playID);
            playingSounds.put(soundID, soundPool.play(soundID, volume, volume, 0, loops, 1));
        }
    }

    /**
     * Stop sound by rawID for specified object key.
     * @param key - object key
     * @param rawID - R.raw.[...]
     */
    public static synchronized void stopSound(Object key, int rawID) {
        int soundID = loadedSounds.get(rawID);
        SparseIntArray playingSounds = soundsMap.get(key);
        int playID = playingSounds.get(soundID);
        soundPool.stop(playID);
        playingSounds.delete(soundID);
    }

    /**
     * You must call this method after using class.
     */
    public static synchronized void dispose() {
        // Stop music.
        for (SparseArray<MediaPlayer> musics : musicsMap.values()) {
            for (int i = 0, size = musics.size(); i < size; i++) {
                MediaPlayer mediaPlayer = musics.valueAt(i);
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            musics.clear();
        }
        musicsMap.clear();
        // Stop sounds.
        for (SparseIntArray playingSounds : soundsMap.values()) {
            for (int i = 0, size = playingSounds.size(); i < size; i++) {
                int playID = playingSounds.valueAt(i);
                soundPool.stop(playID);
            }
            playingSounds.clear();
        }
        soundsMap.clear();
        // Unload sounds.
        for (int i = 0, size = loadedSounds.size(); i < size; i++) {
            int soundID = loadedSounds.valueAt(i);
            soundPool.unload(soundID);
        }
        loadedSounds.clear();
        soundPool.release();
    }
}
