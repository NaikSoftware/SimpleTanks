package ua.naiksoftware.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class InetUtils {
	
	public static String getIpAddr(Context context) {
        return getIpAddr((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
    }
	
	public static String getIpAddr(WifiManager wifiManager) {
		int ip = wifiManager.getConnectionInfo().getIpAddress();
		return String.format(
			"%d.%d.%d.%d",
			(ip & 0xff),
			(ip >> 8 & 0xff),
			(ip >> 16 & 0xff),
			(ip >> 24 & 0xff));
	}
	
	public static boolean isConnected(ConnectivityManager connManager) {
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED) {
			return false;
		}
		return true;
	}
}
