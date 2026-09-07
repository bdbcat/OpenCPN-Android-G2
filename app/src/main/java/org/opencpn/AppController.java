package org.opencpn;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import android.os.Build;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencpn.opencpn.R;

public class AppController {

    private final Activity activity;

    // Core subsystems
    private NativeBridgeManager nativeBridge;
    private PreferencesManager preferences;
    private ConnectivityHelper connectivityHelper;
    private OCPNConnectivityManager connectivityManager;
    private UsbController usb;
    public HostEnvironment hostEnvironmaent;
    private MenuController menuController;
    private ToolbarController toolbarController;
    private FileController fileController;
    public DownloadManager downloadManager;
    private OCPNNativeLib nativeLib;

    private boolean inExit = false;
    private boolean m_inExit;
    private boolean m_backButtonEnable = true;
    private AndroidBridge androidBridge;
    public View m_toolbarOverlay;
    public View m_qtView;
    public View m_statusBarFiller;

    private MigrateController migrateController;
    private ProcessController processController;

    public AppController(Activity activity) {
        this.activity = activity;
    }
    public LinearLayout m_container;


    // ---------------------------------------------------
    // INIT
    // ---------------------------------------------------

    public void init(Bundle savedInstanceState) {
        nativeBridge = NativeBridgeManager.getInstance();
        nativeLib = OCPNNativeLib.getInstance();

        preferences = new PreferencesManager(activity);
        connectivityHelper = new ConnectivityHelper(activity);
        connectivityManager = new OCPNConnectivityManager(activity, nativeLib);

        usb = new UsbController();

        downloadManager = new DownloadManager();

        fileController = new FileController(
                activity,
                OpenCPNBridge.getInstance(activity),
                nativeBridge
        );


        menuController = new MenuController(activity, this, nativeBridge);
        toolbarController = new ToolbarController(activity, menuController);

        androidBridge = new AndroidBridge(activity);

        hostEnvironmaent = new HostEnvironment(activity, nativeLib,  connectivityManager,
                nativeBridge, this, menuController, this.nativeBridge);

        // Initialize bridge once
        androidBridge.initialize(
                hostEnvironmaent,
                menuController,
                connectivityHelper,
                fileController,
                connectivityManager,
                downloadManager,
                this,
                nativeBridge
                );

        OpenCPNBridge.init(activity);
        OpenCPNBridge.getInstance(activity).onCreate(activity.getIntent());
        OpenCPNBridge.getInstance(activity).initFilesystem();

        // --- Android Location services ---
        connectivityManager.init(activity, nativeBridge);
        connectivityHelper.init();

        downloadManager.init(activity, nativeBridge);

        usb.initIfAvailable(activity);

        nativeLib = OCPNNativeLib.getInstance();

        processController = new ProcessController(activity);

        // Register back button callback
        if (Build.VERSION.SDK_INT >= 33) {

            Log.i("BACK", "registering predictive back");

            activity.getOnBackInvokedDispatcher()
                    .registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                            this::handleBackPressedCommon);
        }

        hostEnvironmaent.Init();
        hostEnvironmaent.relocateOCPNPlugins();

    }

    public AndroidBridge getBridge() {
        return androidBridge;
    }

    public View getToolbarOverlay() {
        return m_toolbarOverlay;
    }

    public HostEnvironment getHostEnvironment() {
        return hostEnvironmaent;
    }

    public FileController getFileController() {
        return fileController;
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public ProcessController getProcessController() {
        return processController;
    }

    // ---------------------------------------------------
    // LIFECYCLE FORWARDING
    // ---------------------------------------------------

    public void onStart() {
        nativeBridge.onStart();
    }

    public void onResume() {
        inExit = false;
        connectivityManager.onResume(); // Sailtimer/USBserial
        nativeBridge.onResume();
    }

    public void onPause() {
        connectivityManager.onPause();  // Sailtimer/USBserial
        nativeBridge.onPause();
    }

    public void onStop() {
        nativeBridge.onStop();
    }

    public void onDestroy() {}

    // ---------------------------------------------------
    // UI / LAYOUT
    // ---------------------------------------------------



    public void rebuildHybridLayout(ViewGroup root) {
        try {
            View qtView = m_qtView;

            if (qtView == null) {
                // Initial setup: Qt view is currently the first child of root.
                qtView = root.getChildAt(0);

                if (qtView == null) {
                    Log.e("OpenCPN", "Qt view not found");
                    return;
                }

                m_qtView = qtView;
            }

            // If the hybrid layout already exists, detach the Qt view
            // before removing the old container.  A View cannot be added
            // to a new parent while it still has the old parent.
            if (m_container != null) {
                m_container.removeView(qtView);
                root.removeView(m_container);

                m_container = null;
                m_toolbarOverlay = null;
                m_statusBarFiller = null;
            } else {
                // Initial setup.  Remove the original Qt view from root.
                root.removeView(qtView);
            }

            // Create the Android-side container.
            m_container = new LinearLayout(activity);
            m_container.setOrientation(LinearLayout.VERTICAL);
            m_container.setLayoutParams(
                    new FrameLayout.LayoutParams(
                            MATCH_PARENT,
                            MATCH_PARENT));

            // Reserve space for the Android status bar.
            // This view is outside the Qt/wx coordinate system.
            m_statusBarFiller = new View(activity);
            m_statusBarFiller.setBackgroundColor(Color.BLACK);

            LinearLayout.LayoutParams statusParams =
                    new LinearLayout.LayoutParams(
                            MATCH_PARENT,
                            getHostEnvironment().m_insets.top);

            m_container.addView(m_statusBarFiller, statusParams);

            // Create a fresh toolbar using the current scale.
            m_toolbarOverlay =
                    LayoutInflater.from(activity)
                            .inflate(
                                    R.layout.toolbar_overlay,
                                    m_container,
                                    false);

            m_container.addView(m_toolbarOverlay);

            // Reinsert the existing Qt view below the Android toolbar.
            m_container.addView(qtView);

            // Install the new hybrid container.
            root.addView(m_container);

            // Reconnect toolbar behavior.
            menuController.attachToolbar(m_toolbarOverlay);
            menuController.applyButtonState();


        } catch (Exception e) {
            Log.e("OpenCPN",
                    "Hybrid layout failed",
                    e);
        }
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
        if ((requestCode >= 6000) && (requestCode < 7000)) {
            hostEnvironmaent.onActivityResult(
                    requestCode, resultCode, data);
        }
        if ((requestCode >= 7000) && (requestCode < 8000)) {
            if(migrateController != null) {
                migrateController.onActivityResult(
                        requestCode, resultCode, data);
            }
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

    public String setBackButtonState(final int isActive) {
        Log.i("OpenCPN", "setBackButtonState" + isActive);
        m_backButtonEnable = (isActive != 0);
        return "OK";
    }

    // Handle Back Button on Android < 16
    // n.b. Android 16+ uses OnBackInvokedDispatcher
    public boolean handleBack() {
        handleBackPressedCommon();
        return true;
    }

    boolean handleOCPNBackButton() {
        if (!m_backButtonEnable)
            return false;

        Log.i("OpenCPN", "TLWCount " + nativeLib.getTLWCount());

        if (nativeLib.getTLWCount() > 1)
            return false;

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

    public boolean handleBackPressedCommon() {
        if (m_inExit)
            return true;

        if (handleOCPNBackButton()) {
            m_inExit = true;
            preClose();
            return true;
        } else {
            return false;
        }
    }

    void preClose(){
        connectivityManager.quickStopGpsService();
        nativeLib.ScheduleCleanExit();
    }

    void finishApp() {
        Log.i("OpenCPN", "finishApp");
        activity.finish();
    }

 public String DisableRotation() {
        //TODO investigate
 //       activity.lockActivityOrientation(this);
        return "OK";
    }

    public String EnableRotation() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        return "OK";
    }


    //---------------------------------------------------------------
    // Chart migrator support
    //-----------------------------------------------------------------
    public String doMigrateSetup() {
        migrateController = new MigrateController(activity, this);
        return migrateController.migrateSetup();

    }

    public String isMigrateSAFChooserFinished() {
        return migrateController.isSAFChooserFinished();
    }

    public String migrateFolder(String source, String destination) {
        return migrateController.migrateFolder(source, destination);
    }

    public String getMigrateStatus() {
        return migrateController.getMigrateStatus();
    }

    public String restartOCPNAfterMigrate() {
        return migrateController.restartOCPNAfterMigrate();
    }

}