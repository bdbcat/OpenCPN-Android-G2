package org.opencpn;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toolbar;

//import androidx.appcompat.widget.Toolbar;

import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.bindings.QtActivity;

public class MyQtActivity extends QtActivity {

    private NativeBridgeManager nativeBridge;
    private PreferencesManager preferences;
    private ConnectivityHelper connectivity;
    private UsbController usb;
    private HostManager hostManager;
    private MenuController menuController;
    private ToolbarController toolbarController;
    private String m_nativeLibraryDir;
    private boolean m_trackContinuous;
    private boolean m_inExit = false;
    private static NativeBridgeManager sNativeBridge;
    private OCPNNativeLib nativeLib;

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
        menuController = new MenuController(this);
        toolbarController = new ToolbarController(this, menuController);

        // --- Bridge Init ---
        OpenCPNBridge.init(this);
        OpenCPNBridge.getInstance(this).onCreate(getIntent());

        OpenCPNBridge.getInstance(this).initFilesystem();

        // TODO Some legacy onCreate tasks may move fully into OpenCPNBridge

        // --- Native library dir ---
        initNativeLibraryDir();
        nativeLib = OCPNNativeLib.getInstance();

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


        //setContentView(R.layout.activity_main);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        //setSupportActionBar(toolbar);

        //setActionBar(toolbar);

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final ViewGroup root = findViewById(android.R.id.content);

        if (root == null || root.getChildCount() == 0) {
            Log.e("OpenCPN", "Root view not ready");
            return;
        }

        // Post to ensure Qt has fully attached its view hierarchy
        root.post(() -> {

            try {
                Log.i("OpenCPN", "onPostCreate: restructuring layout");

                // --- Step 1: Capture Qt view ---
                View qtView = root.getChildAt(0);

                if (qtView == null) {
                    Log.e("OpenCPN", "Qt view not found");
                    return;
                }

                Log.i("OpenCPN", "Qt view class: " + qtView.getClass().getName());

                // --- Step 2: Remove Qt view from root ---
                root.removeAllViews();

                // --- Step 3: Create vertical container ---
                LinearLayout container = new LinearLayout(this);
                container.setOrientation(LinearLayout.VERTICAL);
                container.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

                // --- Step 4: Inflate toolbar layout ---
                LayoutInflater inflater = LayoutInflater.from(this);
                View toolbarLayout = inflater.inflate(R.layout.activity_main, container, false);

                Toolbar toolbar = toolbarLayout.findViewById(R.id.my_toolbar);

                if (toolbar != null) {
                    toolbar.setTitle("OpenCPN");
                    Log.i("OpenCPN", "Toolbar initialized");
                } else {
                    Log.e("OpenCPN", "Toolbar not found in layout");
                }

                // Ensure toolbar layout wraps content height
                toolbarLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

                // --- Step 5: Configure Qt view to fill remaining space ---
                LinearLayout.LayoutParams qtParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f   // fill remaining space
                );
                qtView.setLayoutParams(qtParams);

                // --- Step 6: Assemble layout ---
                container.addView(toolbarLayout);
                container.addView(qtView);

                // --- Step 7: Attach back to root ---
                root.addView(container);

                Log.i("OpenCPN", "Layout restructuring complete");

            } catch (Exception e) {
                Log.e("OpenCPN", "Error restructuring layout", e);
            }
        });
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

        if (toolbarController != null) {
            toolbarController.init();
        }

        //hostManager.initInsetsListener();


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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        //if (hasFocus) {
        //    showMenu();
        //}
    }

    public void xshowMenu() {
        View root = findViewById(android.R.id.content);
        final View anchor = getWindow().getDecorView();

        PopupMenu popup = new PopupMenu(this, root);
        popup.getMenuInflater().inflate(R.menu.activity_main_actions, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            return menuController.handle(item);
        });

        popup.show();
    }

    public void showMenu() {
        final ViewGroup root = (ViewGroup)getWindow().getDecorView();

        final View anchor = new View(this);
        anchor.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        root.addView(anchor);

        anchor.post(() -> {
            PopupMenu popup = new PopupMenu(this, anchor);
            popup.getMenuInflater().inflate(R.menu.activity_main_actions, popup.getMenu());

            popup.setOnMenuItemClickListener(item ->
                    menuController.handle(item)
            );

            popup.setOnDismissListener(menu -> root.removeView(anchor));

            popup.show();
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.i("OpenCPN", "onConfigurationChanged");

        // Defer until layout stabilizes
        final View root = findViewById(android.R.id.content);

        root.post(() -> handlePostRotationLayout());
    }

    private void handlePostRotationLayout() {

        final View root = findViewById(android.R.id.content);

        root.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {

                        int width = root.getWidth();
                        int height = root.getHeight();

                        if (width == 0 || height == 0) {
                            return; // still not ready
                        }

                        Log.i("OpenCPN", "FINAL layout size: " + width + "x" + height);

                        // Remove listener (VERY important)
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        int toolbarHeight = 0;
                        View toolbar = findViewById(R.id.my_toolbar);
                        if (toolbar != null) {
                            toolbarHeight = toolbar.getHeight();
                        }

                        dispatchRotationToNative(width, height, toolbarHeight);
                    }
                }
        );
    }

    private void dispatchRotationToNative(int w, int h, int toolbarHeight) {

        Log.i("OpenCPN", "Dispatch rotation: " + w + "x" + h +
                " toolbar=" + toolbarHeight);

        if (nativeLib != null) {
            nativeLib.onScreenResized(w, h, toolbarHeight);
        }
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
        getWindow().getDecorView().setAlpha(0.5f);
        return "OK";
    }

    public String EnableRotation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        return "OK";
    }



}