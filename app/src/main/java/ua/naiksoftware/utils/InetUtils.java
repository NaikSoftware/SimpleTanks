package ua.naiksoftware.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import java.net.*;
import java.util.*;

public class InetUtils {
	
	public static String getWifiAddr(Context context) {
        return getWifiAddr((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
    }
	
	public static String getWifiAddr(WifiManager wifiManager) {
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
	
	public static Inet4Address[] getLocalAddresses() throws SocketException {
        ArrayList<Inet4Address> results = new ArrayList<Inet4Address>(1);
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback() || !iface.supportsMulticast()) {
                continue;
            }
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address) {
                    results.add((Inet4Address) addr);
                }
            }
        }
        return results.toArray(new Inet4Address[results.size()]);
    }
}
