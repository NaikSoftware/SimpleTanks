package ua.naiksoftware.simpletanks;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ClientsListAdapter extends ArrayAdapter<GameServer.Client> {

	public ClientsListAdapter(Context context, int resource,
			List<GameServer.Client> list) {
		super(context, resource, list);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.clients_row, null);
			viewHolder = new ViewHolder();
			viewHolder.clientName = (TextView) convertView.findViewById(R.id.clients_row_name);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.clientName.setText(getItem(position).name);
		return convertView;
	}
	
	private static class ViewHolder {
		TextView clientName;
	}

}
