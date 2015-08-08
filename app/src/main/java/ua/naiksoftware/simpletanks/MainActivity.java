package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import ua.naiksoftware.simpletanks.network.Game;
import ua.naiksoftware.simpletanks.network.starter.GameClient;
import ua.naiksoftware.simpletanks.network.starter.GameServer;
import ua.naiksoftware.simpletanks.network.starter.SinglePlayer;
import ua.naiksoftware.simpletanks.res.Music;
import ua.naiksoftware.simpletanks.res.ResKeeper;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        applySettings();
        Music.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showMainMenu();
    }

    public void showMainMenu() {
        game = null;
        setContentView(R.layout.main);
        findViewById(R.id.btnPlay).setOnClickListener(btnListener);
        findViewById(R.id.btnExit).setOnClickListener(btnListener);
        findViewById(R.id.btnSettings).setOnClickListener(btnListener);
        findViewById(R.id.btnInfo).setOnClickListener(btnListener);
        Music.playMusic(this, R.raw.atmo_music, false);
    }

    View.OnClickListener btnListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Music.playSound(MainActivity.this, R.raw.sfx_button, 0.5f, false);
            switch (v.getId()) {
                case R.id.btnPlay:
                    new AlertDialog.Builder(MainActivity.this)
                            .setItems(R.array.game_modes, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface di, int pos) {
                                    handleStartGame(pos);
                                }
                            }).show();
                    break;
                case R.id.btnSettings:
                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 0);
                    break;
                case R.id.btnInfo:
                    new AlertDialog.Builder(MainActivity.this)
                            .setView(LayoutInflater.from(MainActivity.this).inflate(R.layout.info, null))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    break;
                case R.id.btnExit:
                    finish();
                    break;
            }
        }
    };

    private void handleStartGame(int mode) {
        switch (mode) {
            case 0: // Singleplayer
                Music.stopMusic(MainActivity.this, R.raw.atmo_music);
                game = new SinglePlayer(this);
                game.start();
                break;
            case 1: // LAN Multiplayer
                new AlertDialog.Builder(MainActivity.this)
                        .setItems(R.array.lan_modes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface di, int pos) {
                                Music.stopMusic(MainActivity.this, R.raw.atmo_music);
                                if (pos == 0) { // Start server
                                    game = new GameServer(MainActivity.this);
                                } else if (pos == 1) { // Find servers via mDNS
                                    game = new GameClient(MainActivity.this, false);
                                } else if (pos == 2) { // Connect to server directly
                                    game = new GameClient(MainActivity.this, true);
                                }
                                game.start();
                            }
                        }).show();
                break;
            case 2: // Internet Multiplayer
                Toast.makeText(MainActivity.this, "Not implemented", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void applySettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(SettingsActivity.PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == SettingsActivity.BACK_FROM_SETTINGS) {
            applySettings();
        }
    }

    @Override
    public void onBackPressed() {
        Music.playSound(this, R.raw.sfx_button, 0.5f, false);
        if (game != null && game.isGameRunning()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_notify)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface di, int pos) {
                            if (game != null) game.stop();
                        }
                    })
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        if (game != null) {
            game.stop();
        }
        ResKeeper.clearImageCache();
        Music.dispose();
        super.onDestroy();
    }
}
