package org.opencpn;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import android.os.Build;

import org.opencpn.opencpn.R;

public class AppController {

    private final Activity activity;

    // Core subsystems
    private NativeBridgeManager nativeBridge;
    private PreferencesManager preferences;
    private ConnectivityHelper connectivity;
    private OCPNConnectivityManager connectivityManager;
    private UsbController usb;
    public HostManager hostManager;
    private MenuController menuController;
    private ToolbarController toolbarController;
    private FileController fileController;
    public DownloadManager downloadManager;
    private OCPNNativeLib nativeLib;

    private boolean inExit = false;
    private boolean m_inExit;


    public AppController(Activity activity) {
        this.activity = activity;
    }

    // ---------------------------------------------------
    // INIT
    // ---------------------------------------------------

    public void init(Bundle savedInstanceState) {

        hostManager = new HostManager(activity);

        nativeBridge = new NativeBridgeManager();

        preferences = new PreferencesManager(activity);
        connectivity = new ConnectivityHelper(activity);
        connectivityManager = new OCPNConnectivityManager(activity);

        usb = new UsbController();

        downloadManager = new DownloadManager();

        fileController = new FileController(
                activity,
                OpenCPNBridge.getInstance(activity),
                nativeBridge
        );

        nativeLib = OCPNNativeLib.getInstance();

        menuController = new MenuController(activity, nativeLib);
        toolbarController = new ToolbarController(activity, menuController);

        // Initialize bridge once
        AndroidBridge.initialize(
                hostManager,
                menuController,
                connectivity,
                fileController,
                connectivityManager,
                downloadManager,
                activity
        );

        OpenCPNBridge.init(activity);
        OpenCPNBridge.getInstance(activity).onCreate(activity.getIntent());
        OpenCPNBridge.getInstance(activity).initFilesystem();

        // --- Android Location services ---
        connectivityManager.init(activity);
        connectivity.init();

        downloadManager.init(activity, nativeLib);

        usb.initIfAvailable(activity);

        nativeLib = OCPNNativeLib.getInstance();

        // Register back button callback
        if (Build.VERSION.SDK_INT >= 33) {

            Log.i("BACK", "registering predictive back");

            activity.getOnBackInvokedDispatcher()
                    .registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                            this::handleBackPressedCommon);
        }

/*
        /// /////////////////////////////////////////////////

        downloadManager.init(this, nativeLib);

        // Initialize the JNI bridge
        AndroidBridge.initialize(hostManager, menuController, connectivity,
                fileController, connectivityManager, downloadManager, this);

        // --- OpenCPNBridge Init ---
        OpenCPNBridge.init(this);
        OpenCPNBridge.getInstance(this).onCreate(getIntent());

        OpenCPNBridge.getInstance(this).initFilesystem();

        // TODO registerNetworkCallback() (API >= 24)
        // TODO register locale change receiver

        PackageManager packMan = getPackageManager();
        m_hasGPS = packMan.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

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


        invalidateOptionsMenu();

        /// //////////////////////////////////////////////////

 */
        hostManager.relocateOCPNPlugins();

    }

    // ---------------------------------------------------
    // LIFECYCLE FORWARDING
    // ---------------------------------------------------

    public void onStart() {
        nativeBridge.onStart();
    }

    public void onResume() {
        inExit = false;
        nativeBridge.onResume();
    }

    public void onPause() {
        nativeBridge.onPause();
    }

    public void onStop() {
        usb.stop(activity);
        nativeBridge.onStop();
    }

    public void onDestroy() {
        connectivity.cleanup();
        nativeBridge.onDestroy();
    }

    // ---------------------------------------------------
    // UI / LAYOUT
    // ---------------------------------------------------


    public void setupHybridLayout(ViewGroup root) {
        try {

            View qtView = root.getChildAt(0);

            if (qtView == null) {
                Log.e("OpenCPN", "Qt view not found");
                return;
            }

            root.removeAllViews();

            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);

            container.setLayoutParams(
                    new FrameLayout.LayoutParams(
                            MATCH_PARENT,
                            MATCH_PARENT));

            View toolbarOverlay =
                    LayoutInflater.from(activity)
                            .inflate(
                                    R.layout.toolbar_overlay,
                                    container,
                                    false);

            container.addView(toolbarOverlay);
            container.addView(qtView);

            root.addView(container);

            // Delegate ALL toolbar behavior
            menuController.attachToolbar(toolbarOverlay);

            // Height reporting
/*
            toolbarOverlay.post(() -> {
                int h = toolbarOverlay.getHeight();
                Log.i("OpenCPN",
                        "Toolbar height=" + h);
                notifyToolbarHeight(h);

            });
*/
        } catch (Exception e) {
            Log.e("OpenCPN",
                    "Hybrid layout failed",
                    e);
        }
    }

    // ---------------------------------------------------
    // BACK / EXIT
    // ---------------------------------------------------

    public boolean handleBack() {

        if (inExit) return true;

/*
        if (toolbarController.handleBack()) {
            inExit = true;
            nativeLib.ScheduleCleanExit();
            return true;
        }

 */
        return false;
    }

    public void shutdown() {
        inExit = true;
        nativeLib.ScheduleCleanExit();
    }

    // ---------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        connectivityManager.handlePermissionResults(
                requestCode, permissions, grantResults);
    }

    // ---------------------------------------------------
    // ACTIVITY RESULT
    // ---------------------------------------------------

    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 Intent data) {

        if ((requestCode >= 5000) && (requestCode < 6000)) {
            fileController.safManager.onActivityResult(
                    requestCode, resultCode, data);
        }
    }

    // ---------------------------------------------------
    // ACCESSORS (optional)
    // ---------------------------------------------------

    public MenuController getMenuController() {
        return menuController;
    }

    public ToolbarController getToolbarController() {
        return toolbarController;
    }

    //---------------------------------------------------------------------------------------
    // Private back-button handler

    private static final long BACK_PRESS_INTERVAL_MS = 3000;
    private long lastBackPressTime = 0;
    private Toast toast;


    private void handleBackPressedCommon() {
        if (m_inExit)
            return;

        if (handleOCPNBackButton()) {
            m_inExit = true;
            //preClose();
            activity.finish();
        }
    }

    boolean handleOCPNBackButton() {

        long now = System.currentTimeMillis();

        if ((now - lastBackPressTime) > BACK_PRESS_INTERVAL_MS) {

            if (toast != null) {
                toast.cancel();
            }

            toast = Toast.makeText(
                    activity,
                    "Press back again to close OpenCPN",
                    Toast.LENGTH_LONG);

            toast.show();

            lastBackPressTime = now;

            return false;
        }

        if (toast != null) {
            toast.cancel();
        }

        return true;
    }
    void preClose(){
        nativeLib.ScheduleCleanExit();
    }


}