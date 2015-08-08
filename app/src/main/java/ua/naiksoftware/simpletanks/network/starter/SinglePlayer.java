package ua.naiksoftware.simpletanks.network.starter;

import ua.naiksoftware.simpletanks.MainActivity;
import ua.naiksoftware.simpletanks.R;
import ua.naiksoftware.simpletanks.drawable.GameMap;
import ua.naiksoftware.simpletanks.drawable.Robot;
import ua.naiksoftware.simpletanks.drawable.User;
import ua.naiksoftware.simpletanks.network.Game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * Start game in single-player mode (network not needed).
 */
public class SinglePlayer extends Game {

	private static final String TAG = SinglePlayer.class.getSimpleName();

	private final Activity activity;
	private User myUser;
	private GameMap gameMap;
	private ArrayList<Robot> bots;

	public SinglePlayer(MainActivity activity) {
		super(activity);
		this.activity = activity;
	}

	@Override
    public void start() {
        final View view = LayoutInflater.from(activity).inflate(R.layout.server_dialog_layout, null);
        final Spinner mapsSpinner = (Spinner)view.findViewById(R.id.map_spinner);
        final Map<String, String> maps;
        try {
            maps =  GameMap.readMapsList(activity.getResources());
            mapsSpinner.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1,
			maps.keySet().toArray(new String[maps.keySet().size()])));
        } catch (IOException e) {
            e.printStackTrace();
            toast(R.string.error_reading_map_list);
            return;
        }
        new AlertDialog.Builder(activity)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = ((EditText) view.findViewById(R.id.serverName)).getText().toString().trim();
					if (name.isEmpty()) {
						toast(R.string.serv_name_empty_notice);
					} else { // Все нормально
						myUser = new User(name, User.GEN_NEW_ID, activity.getString(R.string.owner_server));
						InputStream inputStream = null;
						try {
							String pathToMap = maps.get(mapsSpinner.getSelectedItem());
							gameMap = new GameMap(pathToMap, activity.getResources());
						} catch (IOException e) {
							Log.e(TAG, "Error loading game map", e);
							toast(R.string.error_loading_map);
							return;
						} finally {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						showSelectBotsDialog();
					}
				}
			}).setNegativeButton(android.R.string.cancel, null)
			.show();
    }

	@Override
	public void botsSelected(ArrayList<Robot> bots) {
		this.bots = bots;
		paramsReady();
	}

	protected void paramsReady() {
		toast("paramsReady");
	}

	@Override
	public void stop() {
		// TODO: Implement this method
	}

	@Override
	public ArrayList<? extends User> getUsers() {
		// This is a single-user game mode
		return null;
	}

	@Override
	public User getMyUser() {
		return myUser;
	}

	@Override
	public GameMap getGameMap() {
		return gameMap;
	}

	@Override
	public ArrayList<Robot> getRobots() {
		return bots;
	}

	@Override
	protected void onConnected() {
		// Network not used in singleplayer game mode
	}

}
