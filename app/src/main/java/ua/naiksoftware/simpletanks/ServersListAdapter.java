package ua.naiksoftware.simpletanks;

import java.util.List;

import ua.naiksoftware.simpletanks.GameClient.Server;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ServersListAdapter extends ArrayAdapter<GameClient.Server> {

	public ServersListAdapter(Context context, int resource,
			List<GameClient.Server> list) {
		super(context, resource, list);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.servers_row, null);
			viewHolder = new ViewHolder();
			viewHolder.serverName = (TextView) convertView.findViewById(R.id.servers_row_name);
			viewHolder.serverDescr = (TextView) convertView.findViewById(R.id.servers_row_descr);
			viewHolder.serverIP = (TextView) convertView.findViewById(R.id.servers_row_ip);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		Server server = getItem(position);
		viewHolder.serverName.setText(server.name);
		viewHolder.serverDescr.setText(server.descr);
		viewHolder.serverIP.setText(server.ip);
		return convertView;
	}
	
	private static class ViewHolder {
		TextView serverName;
		TextView serverDescr;
		TextView serverIP;
	}

}
