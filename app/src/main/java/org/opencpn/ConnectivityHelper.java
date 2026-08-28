package org.opencpn;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.net.InetAddress;
import java.net.Inet4Address;

public class ConnectivityHelper {

    private final Context context;
    private WifiManager wifi;
    private ConnectivityManager connectivityManager;

    public ConnectivityHelper(Context ctx) {
        this.context = ctx;
    }

    public void init() {
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public String getIpAddress() {
        return getDeviceIpAddress(connectivityManager);
    }

    public static String getDeviceIpAddress(ConnectivityManager connectivityManager) {
        LinkProperties linkProperties = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork());
        InetAddress inetAddress;
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            inetAddress = linkAddress.getAddress();
            if (inetAddress instanceof Inet4Address
                    && !inetAddress.isLoopbackAddress()
                    && inetAddress.isSiteLocalAddress()) {
                return inetAddress.getHostAddress();
            }
        }
        return "0";
    }

    public String hasBluetooth(final int parm) {
        String ret = "Yes";
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            ret = "No";
        }

        return ret;
    }

}