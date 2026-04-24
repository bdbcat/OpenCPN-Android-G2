package org.opencpn;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.bindings.QtActivity;
import org.opencpn.OpenCPNBridge;
import org.opencpn.OCPNResultReceiver;
import org.opencpn.OCPNResultReceiver.Receiver;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.google.android.gms.location.sample.locationupdatesforegroundservice.LocationUpdatesService;


public class MyQtActivity extends QtActivity implements Receiver {

    private OpenCPNBridge bridge;
    private Toast nack_button_toast;
    private long lastBackPressTime = 0;
    private boolean inExit = false;
    public OCPNResultReceiver mReceiver;

    public LocationServiceReceiver myLocationReceiver;

    private UsbSerialHelper uSerialHelper;
    private boolean m_trackContinuous;
    private Object mNotificationBuilder;
    private NotificationManager m_notificationManager;
    private String CHANNEL_ID = "CHANNEL_ID";

    private final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 2;
    private final static int MY_PERMISSIONS_REQUEST_BT_CONNECT = 3;
    private String m_nativeLibraryDir;

    private static String m_UUID = null;
    private static String m_OCPNUUID = null;
    private static String m_OCPNWVID = null;
    private static String m_selID = null;
    private ConnectivityManager connectivityManager = null;
    private WifiManager wifi = null;
    WifiManager.MulticastLock m_multicastlock = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bridge = OpenCPNBridge.getInstance(this);
        OpenCPNBridge.init(this);

        bridge.onCreate(getIntent());

        OpenCPNBridge.onCreate();

        //  Handle various other onCreate tasks.
        //  Some may move to bridge.onCreate()
        //=============================================================

        // Get the location of embedded shared libraries, for general use later
        try {
            ApplicationInfo ainfo = this.getApplicationContext().getPackageManager().getApplicationInfo("org.opencpn.opencpn", PackageManager.GET_SHARED_LIBRARY_FILES);
            Log.i("OpenCPN", "native library dir " + ainfo.nativeLibraryDir);
            m_nativeLibraryDir = ainfo.nativeLibraryDir;
        } catch (Exception e) {
            Log.i("OpenCPN", "Exception on getApplicationInfo");
        }



/*
        // Setup custom back-button handler
        // This callback is only called when MyFragment is at least started
        //TODO  This needs androidx.appcompat.app.AppCompatActivity;
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event
                if (handleOCPNBackButton()) {       // true return signals "Exit App".
                    setEnabled(false);
                    inExit = true;
                    preClose();
                    finish();       // all done
                }
            }
        });
*/

        // Setup Download service receiver
        mReceiver = new OCPNResultReceiver(new Handler());
        mReceiver.setReceiver(this);

        // Setup LocationUpdateService
        myLocationReceiver = new LocationServiceReceiver();

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // On devices supporting (>= API24), establish extended network state callbacks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            //TODO
            // registerNetworkCallback();
        }

        // Register my desire to get locale change notifications
        IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        //TODO
        //registerReceiver(mLocaleChangeReceiver, filter);


        // USB Serial Port setup
        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            uSerialHelper = new UsbSerialHelper();
            Log.i("OpenCPN", "onCreate initUSBSerial");
        }


        // Apply any Android specific preference items
        applyAndroidPreferences();

        // Validate app permissions
        // Here, thisActivity is the current activity
        if (Build.VERSION.SDK_INT >= 23) {
            Log.i("OpenCPN", "Checking STORAGE permissions");

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // No explanation needed; request the permission
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.

            } else {
                // Permission has already been granted
            }
        }


    }


    //  Various Utiilty functions
    //  Some may move to bridge

    boolean handleOCPNBackButton() {
        if (lastBackPressTime < System.currentTimeMillis() - 3000) {
            nack_button_toast = Toast.makeText(this, "Press back again to close OpenCPN", 3000);
            nack_button_toast.show();
            lastBackPressTime = System.currentTimeMillis();
            return false;
        } else {
            if (nack_button_toast != null)
                nack_button_toast.cancel();
            if (System.currentTimeMillis() < lastBackPressTime + 3000)
                return true;        // Requesting App exit.
        }
        return false;
    }


    void preClose() {
        //TODO
        //nativeLib.ScheduleCleanExit();
    }

    public void applyAndroidPreferences() {
        //  Apply any Android private settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Boolean bnoSleep = preferences.getBoolean("prefb_noSleep", false);
        if (bnoSleep)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        m_trackContinuous = preferences.getBoolean("prefb_trackOnPause", false);
    }

    private void buildNotificationBuilder() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {   //6.0

            mNotificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.opencpn_mobile_notification)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            m_notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        } else {

            mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.opencpn_mobile_notification)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            m_notificationManager = getSystemService(NotificationManager.class);
            createNotificationChannel();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "OCPN_DEFAULT_CHANNEL";
            String description = "Normal notifications from OpenCPN";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            m_notificationManager = getSystemService(NotificationManager.class);
            m_notificationManager.createNotificationChannel(channel);
        }
    }

    public boolean foregrounded() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }


    //  Download Service Receiver results handler
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
/*
        if (resultCode == DownloadService.UPDATE_PROGRESS) {

            int progress = resultData.getInt("progress");

            m_downloadTotal = resultData.getInt("sofar");
            m_downloadFilelength = resultData.getInt("filelength");
            m_downloadStatus = 2;           // STATUS_RUNNING

//            Log.i("OpenCPN", "UPDATE_PROGRESS " + m_downloadTotal + " " + m_downloadFilelength + " " + progress);

            //mProgressDialog.setProgress(progress);
            //if (progress == 100) {
            //    mProgressDialog.dismiss();

            nativeLib.setDownloadStatus(m_downloadStatus, m_downloadURL);

        }
        if (resultCode == DownloadService.DOWNLOAD_DONE) {
            m_downloadTotal = resultData.getInt("sofar");
            m_downloadFilelength = resultData.getInt("filelength");
            m_downloadStatus = 8;           // STATUS_SUCCESSFUL
            if (0 == m_downloadTotal)
                m_downloadStatus = 16;      // STATUS_ERROR

            nativeLib.setDownloadStatus(m_downloadStatus, m_downloadURL);
            Log.i("OpenCPN", "DOWNLOAD_DONE " + m_downloadStatus + " " + m_downloadTotal + " " + m_downloadTotal + " " + m_downloadFilelength);
        }

        if (resultCode == UnzipService.UNZIP_DONE) {
            Log.i("OpenCPN", "UNZIP_DONE ");
            m_unzipDone = true;
        }

        if (resultCode == UnzipService.UNZIP_FILE) {
            Log.i("OpenCPN", "UNZIP_FILE ");
            String lastFile = resultData.getString("lastFile");
            m_unzipLastFile = lastFile;
            Log.i("OpenCPN", "UNZIP_FILE: " + m_unzipLastFile);
        }

        if (resultCode == UnzipService.TOPDIR) {
            unzipTopDir = resultData.getString("topDir");
        }
*/
    }

    //  Broadcast Receiver for LocationUpdateService support
    private class LocationServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Verbose debug information about enabling and disabling
            /*
            if (foregrounded())
                Log.i("OpenCPN", "FOREGROUND");
            else
                Log.i("OpenCPN", "BACKGROUND");
            */

            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                String msg = GPSServer.createRMC(location);
                //Log.i("OpenCPN", String.valueOf(ncb) + " " + msg);

                //TODO
                //if (null != nativeLib) {
                //    nativeLib.processNMEA(msg);
                //}

                //Toast.makeText(QtActivity.this, getLocationText(location), Toast.LENGTH_SHORT).show();
            }
        }
    }

}

