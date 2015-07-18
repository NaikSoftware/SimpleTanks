package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.app.*;
import android.content.*;
import android.view.*;

import ua.naiksoftware.simpletanks.connect.GameClient;
import ua.naiksoftware.simpletanks.connect.GameConnection;
import ua.naiksoftware.simpletanks.connect.GameMode;
import ua.naiksoftware.simpletanks.connect.GameServer;
import ua.naiksoftware.simpletanks.res.ResKeeper;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GameMode gameMode;
    private GameConnection gameConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.d(TAG, "___STARTED___");
        applySettings();
        showMainMenu();
    }
    
    public void showMainMenu() {
        setContentView(R.layout.main);
        findViewById(R.id.btnPlay).setOnClickListener(btnListener);
        findViewById(R.id.btnExit).setOnClickListener(btnListener);
        findViewById(R.id.btnSettings).setOnClickListener(btnListener);
        findViewById(R.id.btnInfo).setOnClickListener(btnListener);
    }

    View.OnClickListener btnListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnPlay:
                    new AlertDialog.Builder(MainActivity.this)
                            .setItems(R.array.online_modes, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface di, int pos) {
                                    if (pos == 0) { // Start server
                                        gameMode = GameMode.SERVER;
                                        gameConn = new GameServer(MainActivity.this);
                                    } else if (pos == 1) { // Connect to server
                                        gameMode = GameMode.CLIENT;
                                        gameConn = new GameClient(MainActivity.this);
                                    }
                                    gameConn.start();
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
        if (gameConn != null && gameConn.isGameRunning()) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.exit_notify)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface di, int pos) {
                       gameConn.stop();
                    }
                })
                .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        if (gameConn != null) {
            gameConn.stop();
        }
        ResKeeper.clearImageCache();
        Log.d(TAG, "___DESTROYED___");
        super.onDestroy();
    }
}
