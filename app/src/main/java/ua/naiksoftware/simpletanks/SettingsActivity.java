package ua.naiksoftware.simpletanks;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

/**
 * Created by naik on 18.07.15.
 */
public class SettingsActivity extends Activity {

    public static final String PREF_LANDSCAPE = "pref_landscape";

    public static final int BACK_FROM_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();

        if (prefs.getBoolean(PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        ToggleButton toggleLandscape = (ToggleButton) findViewById(R.id.toggle_landscape_mode);
        toggleLandscape.setChecked(prefs.getBoolean(PREF_LANDSCAPE, false));
        toggleLandscape.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(PREF_LANDSCAPE, isChecked);
                editor.apply();
            }
        });
        setResult(BACK_FROM_SETTINGS);
    }
}
