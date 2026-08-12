package org.opencpn;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.bindings.QtActivity;


public class MyQtActivity extends QtActivity {

    // Definitions
    public final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public final static int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 2;
    public final static int MY_PERMISSIONS_REQUEST_BT_CONNECT = 3;

    private NativeBridgeManager nativeBridge;
    private PreferencesManager preferences;
    private ConnectivityHelper connectivity;
    private UsbController usb;

    private MenuController menuController;
    //private ToolbarController toolbarController;
    private String m_nativeLibraryDir;
    private boolean m_trackContinuous;
    private boolean m_inExit = false;
    private OCPNNativeLib nativeLib;
    private FileController fileController;
    private OCPNConnectivityManager connectivityManager;
    public boolean m_hasGPS = false;
    private DownloadManager downloadManager;
    public AppController app;

    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // --- Core Modules ---
        hostManager = new HostManager(this);
        nativeBridge = new NativeBridgeManager();
        sNativeBridge = nativeBridge;
        preferences = new PreferencesManager(this);
        connectivity = new ConnectivityHelper(this);
        connectivityManager = new OCPNConnectivityManager(this);

        usb = new UsbController();
        //toolbarController = new ToolbarController(this, menuController);
        fileController = new FileController(this, OpenCPNBridge.getInstance(this), sNativeBridge);
        downloadManager = new DownloadManager();

        // Initialize the JNI bridge
        AndroidBridge.initialize(hostManager, menuController, connectivity,
                fileController, connectivityManager, downloadManager, this);

         // --- Native library dir ---
        initNativeLibraryDir();
        nativeLib = OCPNNativeLib.getInstance();

        menuController = new MenuController(this, nativeLib);


        // --- Android Location services ---
        connectivityManager.init(this);
        connectivity.init();

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

        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBackPressedCommon);
        }

        invalidateOptionsMenu();

        hostManager.relocateOCPNPlugins();
    }

     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = new AppController(this);
        app.init(savedInstanceState);

        initNativeLibraryDir();
        nativeLib = OCPNNativeLib.getInstance();

        checkPermissions();
    }

    public AppController getAppController() {
        return app;
    }


    @Override
    protected void onStart() {
        super.onStart();
        app.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.onResume();
    }

    @Override
    protected void onPause() {
        Log.i("OpenCPN", "MyQtActivity onPause()");
        app.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i("OpenCPN", "MyQtActivity onStop()");
        app.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i("OpenCPN", " MyQtActivity onDestroy()");

        /*
         * OpenCPN is a native Qt/wx application. Native state persistence
         * is completed before Activity.finish() is requested.
         *
         * QtActivity.onDestroy() does not reliably return on Android/Qt 5.15,
         * preventing normal post-destroy cleanup. Explicit process termination
         * ensures a clean restart and releases all native Qt state.
         */
        System.exit(0);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ViewGroup root = findViewById(android.R.id.content);
        root.post(() -> app.setupHybridLayout(root));
        app.getHostEnvironment().setupEdgeToEdge();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        // Android 12 and earlier:
        // Intercept KEYCODE_BACK before Qt
        // converts it into a native window close.

        // Android 13+ uses OnBackInvokedDispatcher instead.

        // Filter an possibly process back button
        // Back button is always captured here and used for app exit
        // OpenCPN core does not process back button events
        if (Build.VERSION.SDK_INT < 33 && keyCode == KeyEvent.KEYCODE_BACK) {
            app.handleBackPressedCommon();
            return true;    // no further processing required
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        app.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        app.onActivityResult(requestCode, resultCode, data);
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


                        // Remove listener (VERY important)
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);



;
                        // Second defer — allows Qt/Surface to settle
                        root.post(() -> {

                            int finalWidth = root.getWidth();
                            int finalHeight = root.getHeight();

                            Log.i("OpenCPN",
                                    "FINAL layout size: "
                                            + finalWidth + "x" + finalHeight);

                            int toolbarHeight = 0;

                            View toolbar = findViewById(R.id.btn_overflow);

                            if (toolbar != null) {
                                toolbarHeight = toolbar.getHeight();
                            }

                            ViewGroup rootx = findViewById(android.R.id.content);

                            View qtView = rootx.getChildAt(0);
                            qtView.getLayoutParams().width  = MATCH_PARENT;
                            qtView.getLayoutParams().height = MATCH_PARENT;
                            qtView.requestLayout();

                            dispatchRotationToNative(
                                    finalWidth,
                                    finalHeight,
                                    toolbarHeight);
                        });

                    }
                }
        );
    }

    private void dispatchRotationToNative(int w, int h, int toolbarHeight) {

        Log.i("OpenCPN", "Dispatch rotation: " + w + "x" + h +
                " toolbar=" + toolbarHeight);

        if (nativeLib != null) {
            //nativeLib.onScreenResized(w, h, toolbarHeight);
        }
    }

    // JNI methods, redirecting to appropriate controllers




    public String disclaimerDialog(final String title, final String message) {
        return app.hostEnvironmaent.disclaimerDialog(title, message);

    }

  //TODO Implement this
    public String configureNavSpinnerTS(int flag, int sel) {
        return "OK";
    }





    private void notifyToolbarHeight(int h) {

        Log.i("OpenCPN",
                "Toolbar height=" + h);

        // JNI callback later if needed
    }


}
