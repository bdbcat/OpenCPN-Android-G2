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
import android.window.OnBackInvokedDispatcher;
import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.bindings.QtActivity;
import org.opencpn.OCPNConnectivityManager;

public class MyQtActivity extends QtActivity {

    // Definitions
    public final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public final static int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 2;
    public final static int MY_PERMISSIONS_REQUEST_BT_CONNECT = 3;

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
    public static NativeBridgeManager sNativeBridge;
    private OCPNNativeLib nativeLib;
    private FileController fileController;
    private OCPNConnectivityManager connectivityManager;
    public boolean m_hasGPS = false;
    private DownloadManager downloadManager;


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

        root.post(() -> setupHybridLayout(root));
    }


    protected void xonPostCreate(Bundle savedInstanceState) {
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
                        MATCH_PARENT,
                        MATCH_PARENT
                ));

                // --- Step 4: Inflate toolbar layout ---

                LayoutInflater inflater = LayoutInflater.from(this);
                //View toolbarLayout = inflater.inflate(R.layout.activity_main, container, false);
                View toolbar_overlay = inflater.inflate(R.layout.toolbar_overlay, container,false);

                // --- Step 6: Assemble layout ---
                container.addView(toolbar_overlay);
                container.addView(qtView);

                // --- Step 7: Attach back to root ---
                root.addView(container);

                Log.i("OpenCPN", "Layout restructuring complete");

                // Add overflow listener
                ImageButton overflow =
                        toolbar_overlay.findViewById(R.id.btn_overflow);

                overflow.setOnClickListener(v -> {

                    PopupMenu popup =
                            new PopupMenu(this, overflow);

                    popup.getMenuInflater().inflate(
                            R.menu.activity_main_actions,
                            popup.getMenu());

                    popup.setOnMenuItemClickListener(item ->
                            menuController.handle(item));

                    popup.show();
                });

                ImageButton follow =
                        toolbar_overlay.findViewById(R.id.btn_follow);

                follow.setOnClickListener(v -> {
                    menuController.onFollowPressed();
                });

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
            //toolbarController.init();
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

        if (hasFocus) {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(0);


        }
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
            nativeLib.onScreenResized(w, h, toolbarHeight);
        }
    }

    // JNI methods, redirecting to appropriate controllers


    //TODO implement in "files" module
    private String RemoveSystemFile(String filename) {
        //File f = new File(filename);
        //f.delete();
        return "OK";

    }


    public String disclaimerDialog(final String title, final String message) {
        return hostManager.disclaimerDialog(title, message);

    }

  //TODO Implement this
    public String configureNavSpinnerTS(int flag, int sel) {
        return "OK";
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
            preClose();
            finish();
        }
    }

    boolean handleOCPNBackButton() {

        long now = System.currentTimeMillis();

        if ((now - lastBackPressTime) > BACK_PRESS_INTERVAL_MS) {

            if (toast != null) {
                toast.cancel();
            }

            toast = Toast.makeText(
                    this,
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

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 33) {  // handle by OnBackInvokedDispatcher
            return;
        }
        handleBackPressedCommon();
   }

    void preClose(){
        nativeLib.ScheduleCleanExit();
    }

    public boolean xonCreateOptionsMenu(Menu menu)
    {
        //getMenuInflater().inflate(R.menu.test_menu, menu);
        MenuItem item = menu.add(
                Menu.NONE,
                1001,
                Menu.NONE,
                "Test2");

        item.setIcon(android.R.drawable.ic_menu_compass);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        Log.i("MENU",
                "Menu size=" + menu.size());

        for (int i = 0; i < menu.size(); i++) {
            MenuItem itemx = menu.getItem(i);

            Log.i("MENU",
                    "item=" + itemx.getTitle());
        }
        return true;
    }

    public boolean xonPrepareOptionsMenu(Menu menu) {
        Log.i("MENU", "onPrepareOptionsMenu");
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Dispatch cased on numeric range of requestCode
        if ((requestCode >= 5000) && (resultCode < 6000)) {
            fileController.safManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("OpenCPN", "onRequestPermissionsResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                connectivityManager.handlePermissionResults(requestCode, permissions, grantResults);
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // task you need to do.
                    Log.i("OpenCPN", "MY_PERMISSIONS_REQUEST_WRITE_STORAGE permission granted");

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i("OpenCPN", "MY_PERMISSIONS_REQUEST_WRITE_STORAGE permission denied");
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_BT_CONNECT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // task you need to do.
                    Log.i("OpenCPN", "MY_PERMISSIONS_REQUEST_BT_CONNECT permission granted");

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i("OpenCPN", "MY_PERMISSIONS_REQUEST_BT_CONNECT permission denied");
                }
                return;
            }
        }
    }

    private void setupHybridLayout(ViewGroup root) {

        try {

            View qtView = root.getChildAt(0);

            if (qtView == null) {
                Log.e("OpenCPN", "Qt view not found");
                return;
            }

            root.removeAllViews();

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);

            container.setLayoutParams(
                    new FrameLayout.LayoutParams(
                            MATCH_PARENT,
                            MATCH_PARENT));

            View toolbarOverlay =
                    LayoutInflater.from(this)
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
            toolbarOverlay.post(() -> {
                int h = toolbarOverlay.getHeight();
                Log.i("OpenCPN",
                        "Toolbar height=" + h);
                notifyToolbarHeight(h);

            });

        } catch (Exception e) {
            Log.e("OpenCPN",
                    "Hybrid layout failed",
                    e);
        }
    }

    private void notifyToolbarHeight(int h) {

        Log.i("OpenCPN",
                "Toolbar height=" + h);

        // JNI callback later if needed
    }


}
