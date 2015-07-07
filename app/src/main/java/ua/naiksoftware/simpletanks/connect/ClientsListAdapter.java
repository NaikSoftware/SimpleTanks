package ua.naiksoftware.simpletanks.connect;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ua.naiksoftware.simpletanks.R;

public class ClientsListAdapter extends ArrayAdapter<GameServer.Client> {

	public ClientsListAdapter(Context context, int resource,
			List<GameServer.Client> list) {
		super(context, resource, list);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.clients_row, null);
			viewHolder = new ViewHolder();
			viewHolder.clientName = (TextView) convertView.findViewById(R.id.clients_row_name);
			viewHolder.clientIp = (TextView) convertView.findViewById(R.id.clients_row_ip);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		GameServer.Client client = getItem(position);
		viewHolder.clientName.setText(client.getName());
		viewHolder.clientIp.setText(client.getIp().toString());
		return convertView;
	}
	
	private static class ViewHolder {
		TextView clientName;
		TextView clientIp;
	}

}
