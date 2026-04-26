package org.opencpn;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.qtproject.qt5.android.bindings.QtActivity;

public class MyQtActivity extends QtActivity {

    private NativeBridgeManager nativeBridge;
    private PreferencesManager preferences;
    private ConnectivityHelper connectivity;
    private UsbController usb;
    private HostManager hostManager;
    private String m_nativeLibraryDir;
    private boolean m_trackContinuous;
    private boolean m_inExit = false;
    private static NativeBridgeManager sNativeBridge;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Core Modules ---
        hostManager = new HostManager(this);
        nativeBridge = new NativeBridgeManager();
        sNativeBridge = nativeBridge;
        preferences = new PreferencesManager(this);
        connectivity = new ConnectivityHelper(this);
        usb = new UsbController();

        // --- Bridge Init ---
        OpenCPNBridge.init(this);
        OpenCPNBridge.getInstance(this).onCreate(getIntent());

        // TODO Some legacy onCreate tasks may move fully into OpenCPNBridge

        // --- Native library dir ---
        initNativeLibraryDir();

        // --- Android services ---
        connectivity.init();

        // TODO registerNetworkCallback() (API >= 24)
        // TODO register locale change receiver

        // --- Preferences ---
        m_trackContinuous = preferences.applyWindowFlags(this);

        // --- Permissions ---
        checkPermissions();

        // TODO setupEdgeToEdge()
        // TODO setup ActionBar (icon, logo, etc.)
        // TODO lockActivityOrientation(this)

        // TODO Device ID / UUID logic (Android < 29 vs >= 29)
        // TODO m_systemName generation

        // --- USB ---
        usb.initIfAvailable(this);

        // TODO Establish GUI layout if needed (setContentView etc.)

        // TODO ENVIRONMENT_VARIABLES setup (QT_ANDROID_THEME, DPI, etc.)
        // TODO exportMap initialization (if still required)
    }

    private void initNativeLibraryDir() {
        try {
            ApplicationInfo ainfo =
                    getPackageManager().getApplicationInfo(
                            "org.opencpn.opencpn",
                            PackageManager.GET_SHARED_LIBRARY_FILES
                    );

            m_nativeLibraryDir = ainfo.nativeLibraryDir;
            Log.i("OpenCPN", "native library dir " + m_nativeLibraryDir);

        } catch (Exception e) {
            Log.i("OpenCPN", "Exception on getApplicationInfo");
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.i("OpenCPN", "Checking STORAGE permissions");

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1002
                );
            }
        }

        // TODO Bluetooth CONNECT permission (SDK 31+)
        // TODO Fine location permission
    }

    @Override
    protected void onStart() {
        Log.i("OpenCPN", "onStart");

        // TODO o-charts server test (createProcSync5stdout)

        // TODO cancel foreground/background notification

        nativeBridge.onStart();
        super.onStart();

        // TODO QtApplication.invokeDelegate() — verify if ever needed
    }

    @Override
    protected void onResume() {
        Log.i("OpenCPN", "onResume");

        m_inExit = false;

        nativeBridge.onResume();

        // TODO re-init USB serial + scan ports
        // TODO register SailTimer / BLE receiver

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i("OpenCPN", "onPause");

        nativeBridge.onPause();

        // TODO stop toast timer if running
        // TODO unregister SailTimer / BLE receiver

        // TODO background tracking notification handling

        super.onPause();

        // TODO QtApplication.invokeDelegate() — likely removable

        // TODO ANR watchdog for forced exit handling
    }

    @Override
    protected void onStop() {
        Log.i("OpenCPN", "onStop");

        // TODO stop toast timer

        // TODO GPS server OFF if not tracking

        usb.stop(this);
        nativeBridge.onStop();

        // Preserve original delay logic
        if (!m_inExit) {
            Log.i("OpenCPN", "onStop Spin1...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        super.onStop();

        Log.i("OpenCPN", "onStop Done");
    }

    @Override
    protected void onDestroy() {
        Log.i("OpenCPN", "onDestroy");

        nativeBridge.onDestroy();
        connectivity.cleanup();

        // TODO unregister locale receiver

        super.onDestroy();

        Log.i("OpenCPN", "onDestroy Done");

        // Required for Qt cleanup (legacy but still likely needed)
        System.exit(0);
    }


    // JNI methods, redirecting to appropriate controllers

    public static void notifyQtReady() {
        if (sNativeBridge != null) {
            sNativeBridge.setQtReady();
        }
    }

    public String getDisplayMetrics() {
        return hostManager.getDisplayMetrics();
    }

    public String getDeviceInfo() {
        return hostManager.getDeviceInfo();
    }

    public String getSystemDirsAndroid() {
        return hostManager.getSystemDirs();
    }

    public String getAndroidLocaleString() {
        return hostManager.getAndroidLocaleString();
    }

    public String getAndroidVersionCode() {
        return hostManager.getAndroidVersionCode();
    }

    //TODO implement in "files" module
    private String RemoveSystemFile(String filename) {
        //File f = new File(filename);
        //f.delete();
        return "OK";
    }

    public String getScreenOrientation() {
        return hostManager.getScreenOrientation();
    }

    public String disclaimerDialog(final String title, final String message) {
        return hostManager.disclaimerDialog(title, message);

    }

    public String getIpAddress() {
        return connectivity.getIpAddress();
    }

    public String hasBluetooth(int parm) {
        return connectivity.hasBluetooth(parm);
    }

    //TODO Implement this
    public String configureNavSpinnerTS(int flag, int sel) {
        return "OK";
    }

    public String DisableRotation() {
        hostManager.lockActivityOrientation(this);
        return "OK";
    }

    public String EnableRotation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        return "OK";
    }


}