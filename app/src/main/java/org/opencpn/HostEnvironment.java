package org.opencpn;


import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaDrm;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.app.AlertDialog;
import android.view.Surface;
import android.content.pm.ActivityInfo;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencpn.opencpn.BuildConfig;
import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.QtActivityDelegate;
import org.qtproject.qt5.android.QtNative;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostEnvironment {

    private static final int OCPN_GRIB_REQUEST_CODE = 6000;
    private final static int OCPN_SETTINGS_REQUEST_CODE = 6002;
    private final static int OCPN_GOOGLEMAPS_REQUEST_CODE = 6004;
    private final static int ID_CMD_POST_JSON_TO_PLUGINS = 304;
    private final static int ID_CMD_APPLY_SETTINGS = 300;
    private final static int ID_CMD_NULL_REFRESH = 301;
    private final static int ID_CMD_SETVP = 303;

    private final Activity activity;
    private final String m_filesDir;
    private final OCPNNativeLib nativeLib;
    private final OCPNConnectivityManager connectivityManager;
    private final NativeBridgeManager nb;

    // state injected from Activity
    private boolean m_fullScreen = false;
    public android.graphics.Insets m_insets = android.graphics.Insets.NONE;

    private int insetTop = 0;
    private int insetBottom = 0;
    private int insetLeft = 0;
    private int insetRight = 0;
    private String m_GRIBReturn;
    private MediaPlayer mediaPlayer;
    private boolean m_trackContinuous;
    private boolean m_showRouteAnnunciator;

    private Toast mTimedToast;
    private CountDownTimer mtoastCountDown;
    private boolean ToastTimerRunning = false;
    private String m_scannedSerial;
    private String m_settingsReturn;
    private double m_gminitialZoom;
    private double m_gminitialLat = 0.0;
    private double m_gminitialLon = 0.0;
    private Semaphore mutexC;
    private ProgressDialog ringProgressDialog = null;

    private String m_ColorDialogString;
    private Boolean m_ColorDialogDone = false;;
    private static String m_UUID = null;
    private static String m_OCPNUUID = null;
    private static String m_OCPNWVID = null;
    private static String m_selID = null;
    private String m_systemName;
    private static String OCPNuniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private BroadcastReceiver mLocaleChangeReceiver;

    public HostEnvironment(Activity activity, OCPNNativeLib nativeLib, OCPNConnectivityManager cm, NativeBridgeManager nb) {
        this.activity = activity;
        m_filesDir = resolveFilesDir();
        this.nativeLib = nativeLib;
        this.connectivityManager = cm;
        this.nb = nb;
    }


    public void Init() {

    //  Initialize Device ID / UUID logic
        m_UUID = getUUID(activity);
        m_OCPNUUID = getOCPNuniqueID();
        m_OCPNWVID = getOCPNWVID();

        // Decide which "id" value to use
        if ((m_UUID != null) && (m_UUID.length() > 0) && (Build.VERSION.SDK_INT < 29))   // Strictly less than Android 10
            m_selID = m_UUID;
        else if (m_OCPNWVID != null)
            m_selID = m_OCPNWVID;
        else
            m_selID = m_OCPNUUID;

        m_systemName = getSystemName(m_selID);
        Log.i("OpenCPN", "msn " + m_systemName);


        // Locale change notification
        // BroadcastReceiver which receives broadcast Intent for local changes
        mLocaleChangeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("OpenCPN", "mLocaleChangeReceiver");

                final String action = intent.getAction();
                Log.i("OpenCPN", "onLocaleChange: " + action);
                String language = getCurrentAndroidLocale().getLanguage();
                Log.i("OpenCPN", "  new language is: " + language);

                setAppLocale(getCurrentAndroidLocale());
            }
        };

        // Register my desire to get locale change notifications
        IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        activity.registerReceiver(mLocaleChangeReceiver, filter);

/*
        /// /////////////////////////////////////////////////`

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

        // TODO m_systemName generation

        // --- USB ---
        usb.initIfAvailable(this);

        // TODO Establish GUI layout if needed (setContentView etc.)

        // TODO ENVIRONMENT_VARIABLES setup (QT_ANDROID_THEME, DPI, etc.)
        // TODO exportMap initialization (if still required)


        invalidateOptionsMenu();

        /// //////////////////////////////////////////////////

 */
    }


    private String getUUID(Context context) {
        final String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId.isEmpty())
            return "";

        final UUID uuid;
        try {
            uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return uuid.toString();
    }

    private String getSystemName(String sID) {
        String name = android.os.Build.DEVICE;
        if (name.length() > 11)
            name = name.substring(0, 10);

        // Get a hash of the supplied string
        int uuidHash = -(sID.hashCode());
        // And take the mod(10000) result
        int val = Math.abs(uuidHash) % 10000;
        name += String.valueOf(val);

        Pattern replace = Pattern.compile("[-_/()~#%&*{}:;?|<>!$^+=@ ]");
        Matcher matcher2 = replace.matcher(name);
        name = matcher2.replaceAll("0");

        name = name.toLowerCase(Locale.ROOT);
        return name;

    }

    public String getOCPNuniqueID() {
        if (OCPNuniqueID == null) {
            SharedPreferences sharedPrefs = activity.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            OCPNuniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (OCPNuniqueID == null) {
                OCPNuniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, OCPNuniqueID);
                editor.commit();
            }
        }
        return OCPNuniqueID;
    }


    public String getOCPNWVID() {
        String rv = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            MediaDrm mediaDrm;

            final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
            try {
                mediaDrm = new MediaDrm(WIDEVINE_UUID);
            } catch (Exception e) {
                return rv;
            }

            byte[] deviceUniqueIdArray = mediaDrm.getPropertyByteArray("deviceUniqueId");
            StringBuilder sb = new StringBuilder();
            for (byte b : deviceUniqueIdArray) {
                sb.append(String.format("%02X", b));
            }
            //System.out.println(sb.toString());
            //String encodedWidevineId = Base64.encodeToString(deviceUniqueIdArray, DEFAULT).trim();
            return sb.toString();
        } else
            return rv;
    }

    //  locale functions
    public Locale getCurrentAndroidLocale() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
//            return getResources().getConfiguration().getLocales().get(0);
//        }
//        else
        {
            //noinspection deprecation
            return activity.getResources().getConfiguration().locale;
        }
    }

    public void setAppLocale(Locale newLocale) {
        String lang = newLocale.getLanguage();
        String country = newLocale.getCountry();

        String toSet = lang;
        if (!country.isEmpty())
            toSet += "_" + country;

        Log.i("OpenCPN", "  setAppLocale new language ID is: " + toSet);
        //TODO  Implement this
        // nativeLib.invokeCmdEventCmdString( ID_CMD_SET_LOCALE, toSet);

    }



    // --------------------------------------------------------------------
    // Activity result entry point
    // --------------------------------------------------------------------

    public boolean onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (requestCode == OCPN_GRIB_REQUEST_CODE) {
//            Log.i("DEBUGGER_TAG", "onqtActivityResultGC");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
//                Log.i("DEBUGGER_TAG", "onqtActivityResultGD");
                m_GRIBReturn = data.getStringExtra("GRIB_JSON");
//                Log.i("GRIB", m_GRIBReturn);

                //  Add a message ID string to the JSON for later processing
                String json = "";
                try {
                    JSONObject js = new JSONObject(m_GRIBReturn);
                    js.put("MessageID", "GRIB_APPLY_JSON_CONFIG");
                    json = js.toString();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                nativeLib.invokeCmdEventCmdString(ID_CMD_POST_JSON_TO_PLUGINS, json);

//                nativeLib.sendPluginMessage( "GRIB_APPLY_JSON_CONFIG", m_GRIBReturn);

                // defer hte application of settings until the screen refreshes
                //Handler handler = new Handler();
                //handler.postDelayed(new Runnable() {
                //   public void run() {
                //      nativeLib.invokeCmdEventCmdString( ID_CMD_APPLY_SETTINGS, m_settingsReturn);
                // }
                // }, 100);
            } else if (resultCode == Activity.RESULT_CANCELED) {
//                Log.i("DEBUGGER_TAG", "onqtActivityResultGE");
            }

            return true;
        }
        if (requestCode == OCPN_SETTINGS_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                m_settingsReturn = data.getStringExtra("SettingsString");
                Log.i("OpenCPN", "onActivityResult.SettingsString: " + m_settingsReturn);
                nativeLib.invokeCmdEventCmdString(ID_CMD_NULL_REFRESH, m_settingsReturn);

                // defer hte application of settings until the screen refreshes

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        nativeLib.invokeCmdEventCmdString(ID_CMD_APPLY_SETTINGS, m_settingsReturn);
                    }
                }, 500);

                //  Apply any Android private settings
                applyAndroidPreferences();
            }

            return true;
        }
        if (requestCode == OCPN_GOOGLEMAPS_REQUEST_CODE) {
            // TODO.  Create new module for Google Maps GoogleMapsManager.
            // TODO New manager initialized from App Controller.
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                String finalPosition = data.getStringExtra("finalPosition");
//                Log.i("DEBUGGER_TAG", "finalPositionFromMaps " + finalPosition);

                StringTokenizer tkz = new StringTokenizer(finalPosition, ";");

                String finalLat = finalPosition.valueOf(m_gminitialLat);
                String finalLon = finalPosition.valueOf(m_gminitialLon);
                String finalZoom = finalPosition.valueOf(m_gminitialZoom);
                String zoomFactor = "1.0";

                if (tkz.hasMoreTokens()) {
                    finalLat = tkz.nextToken();
                    finalLon = tkz.nextToken();
                    finalZoom = tkz.nextToken();
                    zoomFactor = tkz.nextToken();
                }

                double zoomF = Double.parseDouble(zoomFactor);
                finalZoom = String.valueOf(m_gminitialZoom * zoomF);


                String vpSet = "";

                vpSet = vpSet.concat(finalLat);
                vpSet = vpSet.concat(";");
                vpSet = vpSet.concat(finalLon);
                vpSet = vpSet.concat(";");
                vpSet = vpSet.concat(finalZoom);
                vpSet = vpSet.concat(";");


                Log.i("DEBUGGER_TAG", "finalPositionString " + vpSet);

                nativeLib.invokeCmdEventCmdString(ID_CMD_SETVP, vpSet);

            } else if (resultCode == Activity.RESULT_CANCELED) {
//                Log.i("DEBUGGER_TAG", "onqtActivityResultE");
            }

            return true;
        }

        return false;
    }

    public void setInsets(android.graphics.Insets insets) {
        m_insets = insets;
    }

    public String getDisplayMetrics() {

        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);

        int statusBarHeight = getStatusBarHeight();
        int navBarHeight = getNavBarHeight();
        int actionBarHeight = getActionBarHeight();

        int width = 600;
        int height = 400;

        Display display = activity.getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 17) {
            width = dm.widthPixels;
            height = dm.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            Point outPoint = new Point();
            display.getRealSize(outPoint);
            width = outPoint.x;
            height = outPoint.y;

        } else {
            width = display.getWidth();
            height = display.getHeight();
        }

        // Fullscreen fix
        if (m_fullScreen) {
            Point outPoint = new Point();
            display.getRealSize(outPoint);
            width = outPoint.x;
            height = outPoint.y;
            height += statusBarHeight;
        }

        float tsize = new Button(activity).getTextSize();

        int tbs = getEffectiveTopInset();

        return String.format("%f;%f;%d;%d;%d;%d;%d;%d;%d;%d;%f;%d;%d;%d;%d;%d",
                dm.xdpi, dm.density, dm.densityDpi,
                width, height - statusBarHeight,
                width, height,
                dm.widthPixels, dm.heightPixels,
                actionBarHeight, tsize, navBarHeight,
                m_insets.left, m_insets.right, m_insets.top, m_insets.bottom);
    }

    private int getStatusBarHeight() {
        int resourceId = activity.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0)
            return activity.getResources().getDimensionPixelSize(resourceId);

        return 0;
    }

    private int getNavBarHeight() {
        int resourceId = activity.getResources()
                .getIdentifier("navigation_bar_height", "dimen", "android");

        if (resourceId > 0)
            return activity.getResources().getDimensionPixelSize(resourceId);

        return 0;
    }


    private int getActionBarHeight() {
        int actionBarHeight = 0;

        if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar =
                    ((androidx.appcompat.app.AppCompatActivity) activity).getSupportActionBar();

            if (actionBar != null && actionBar.isShowing()) {
                actionBarHeight = actionBar.getHeight();
            }
        }
        return actionBarHeight;
    }

    public String getDeviceInfo() {
        String s = "Device Info:";
        s += "\n OS Version: " + System.getProperty("os.version") + "(" + Build.VERSION.INCREMENTAL + ")";
        s += "\n OS API Level: " + Build.VERSION.RELEASE + "{" + Build.VERSION.SDK_INT + "}";
        s += "\n OS SDK_INT:" + Build.VERSION.SDK_INT;
        s += "\n Device: " + Build.DEVICE;
        s += "\n Model (and Product): " + Build.MODEL + " (" + Build.PRODUCT + ")";
        // TODO
        s += "\n" + activity.getPackageName();
        s += "\n UUID:" + getUUID(activity);
        s += "\n systemName:" + m_systemName;
        s += "\n OCPNRUID:" + getOCPNuniqueID();
        s += "\n OCPNWVID:" + getOCPNWVID();
        s += "\n SELID:" + m_selID;
        s += "\n VERSION_CODE:" + String.valueOf(BuildConfig.VERSION_CODE);
        Log.i("OpenCPN", s);

        return s;
    }

    public int getInsetTop() { return insetTop; }
    public int getInsetBottom() { return insetBottom; }
    public int getInsetLeft() { return insetLeft; }
    public int getInsetRight() { return insetRight; }

    private String resolveFilesDir() {
        String filesDir = "";
        File fFiles = activity.getExternalFilesDir(null);
        if (null != fFiles) {
            filesDir = fFiles.getAbsolutePath();
        } else {
            filesDir = activity.getFilesDir().getAbsolutePath();
        }


        //  Maybe the app has been migrated to SDCard...
        ApplicationInfo aif = activity.getApplicationInfo();
        if ((aif.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE) {
            Log.i("OpenCPN", "App might be on EXTERNAL_STORAGE");

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                File[] fx = activity.getExternalFilesDirs(null);
                if (null != fx) {
                    if (fx.length > 1) {
                        Log.i("OpenCPN", "App is really on EXTERNAL_STORAGE");
                        filesDir = fx[1].getAbsolutePath();
                    }
                } else {
                    Log.i("OpenCPN", "App has no ExternalFilesDirs");
                }
            }
        }

        return filesDir;

    }
    public String getSystemDirs() {
        String result = "";


//       String extFiles = getExternalFilesDir(null).getPath();   // Provisional

        // TODO
        /*
        //  Maybe the app has been migrated to SDCard...
        activity.ApplicationInfo ai = activity.getApplicationInfo();
        if ((ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE) {
            //Log.i("OpenCPN", "External");
            result = "EXTAPP;";
//           File[] fx = getExternalFilesDirs(null);
//           if(fx.length > 1){
//               extFiles = fx[1].getPath();
//           }
        } else
        */
        {
            //Log.i("OpenCPN", "Internal");
            result = "INTAPP;";
        }


        String extFiles = m_filesDir;

        // Find the external cache directory
        String cacheDir = "";

        File cache = activity.getExternalCacheDir();
        if (null == cache) {
            File fc = new File(m_filesDir);
            cacheDir = fc.getParent() + "/cache";
        } else {
            cacheDir = cache.getAbsolutePath();
        }

        //  Verify the ExternalStorage Directory
        String sxsd = "";
        File xsd = Environment.getExternalStorageDirectory();
        if (null == xsd) {
            sxsd = activity.getFilesDir().getPath();
        } else {
            sxsd = xsd.getPath();
        }


        result = result.concat(activity.getFilesDir().getPath() + ";");
        result = result.concat(activity.getCacheDir().getPath() + ";");
        result = result.concat(extFiles + ";");
        result = result.concat(cacheDir + ";");
        result = result.concat(sxsd + ";");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] fxd = activity.getApplicationContext().getExternalFilesDirs(null);
            result = result.concat(fxd[0] + ";");
            if (fxd.length > 1)
                result = result.concat(fxd[1] + ";");
            else
                result = result.concat("???" + ";");
        } else {
            result = result.concat("???0" + ";");
            result = result.concat("???1" + ";");
        }


        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        result = result.concat(downloadDir.getAbsolutePath() + ";");

        File docsdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        result = result.concat(docsdDir.getAbsolutePath() + ";");

        Log.i("OpenCPN", "getSystemDirs  result: " + result);

        return result;
    }

    public String getAndroidLocaleString() {
        Locale locale =  activity.getResources().getConfiguration().locale;
        if (null != locale) {
            String lang = locale.getLanguage();
            String country = locale.getCountry();

            String ret = lang;
            if (!country.isEmpty())
                ret += "_" + country;

            Log.i("OpenCPN", "  getAndroidLocaleString language ID is: " + ret);

            return ret;
        } else
            return "en_US";
    }

    public String getAndroidVersionCode() {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            String rv = Integer.toString(pInfo.versionCode);
            return rv;
        } catch (PackageManager.NameNotFoundException e) {
            return "0";
        }
    }

    public String getAndroidVersionName() {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            String rv = pInfo.versionName;;
            return rv;
        } catch (PackageManager.NameNotFoundException e) {
            return "0";
        }
    }

    public String getScreenOrientation() {
        Configuration c = activity.getResources().getConfiguration();
        int orient = c.orientation;
        return String.valueOf(orient);
    }

    public String showDisclaimerDialog(String title, String message) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                        dialog.cancel();
                    }
                })
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        return ("OK");

    }

    public String disclaimerDialog(final String title, final String message) {

        final QtActivityDelegate delegate = QtNative.activityDelegate();

        if (null != delegate) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showDisclaimerDialog(title, message);

                }
            });
        }

        String ret = "OK";
        return ret;
    }

    public void lockActivityOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int height;
        int width;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            height = display.getHeight();
            width = display.getWidth();
        } else {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
            width = size.x;
        }
        switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_180:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
    public void initInsetsListener() {
        View root = activity.getWindow().getDecorView();

        root.setOnApplyWindowInsetsListener((v, insets) -> {

            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets sys =
                        insets.getInsets(android.view.WindowInsets.Type.systemBars());

                insetTop = sys.top;
                insetBottom = sys.bottom;
                insetLeft = sys.left;
                insetRight = sys.right;
            } else {
                insetTop = insets.getSystemWindowInsetTop();
                insetBottom = insets.getSystemWindowInsetBottom();
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
            }

            return insets;
        });

        root.requestApplyInsets();
    }

    
    public int getEffectiveTopInset() {
        int toolbarHeight = 400; //getToolbarHeight(); // from ToolbarController
        return insetTop + toolbarHeight;
    }

    public Activity getActivity() {
        return activity;
    }

    private static String getLocalizedDateNew(LocalDate date, FormatStyle formatStyle, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(formatStyle).withLocale(locale);
            return date.format(formatter);
        }
        else return "";
    }

    public String getLocalizedDateTime( String format, int epoch_seconds) {
        String rv = "";
        //Log.i("OpenCPN", "gldt_in: " + format + "   " + epoch_seconds);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Get a LocalDateTime object for the epoch_seconds time passed in
            // WARNING: int epoch_seconds rolls over in 2038
            Instant instant = Instant.ofEpochSecond(epoch_seconds);
            //Log.i("OpenCPN", "gldt_An: " + instant.toString());

            ZoneId zoneId = ZoneId.systemDefault(); // Use the device's default time zone
            // Or specify a specific timezone: ZoneId zoneId = ZoneId.of("America/New_York");

            LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
            Log.i("OpenCPN", "gldt_B: " + localDateTime.toString());
            LocalDate localDate = localDateTime.toLocalDate();
            LocalTime time = localDateTime.toLocalTime();

            // branch on the requested format string
            if (Objects.equals(format, "$short_date")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault());
                rv = localDate.format(formatter);
            }
            else if (Objects.equals(format, "$weekday_short_date_time")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault());
                String formattedDate = localDate.format(formatter);

                DateTimeFormatter formatter24 = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = time.format(formatter24);

                rv = formattedDate + "  " + formattedTime;
            }
            else if (Objects.equals(format, "$long_date_time")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault());
                String formattedDate = localDate.format(formatter);

                DateTimeFormatter formatter24 = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = time.format(formatter24);

                rv = formattedDate + "  " + formattedTime;
            }
            else if (Objects.equals(format, "$long_date")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault());
                rv = localDate.format(formatter);
            }
            else if (Objects.equals(format, "$weekday_short_date")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault());
                rv = localDate.format(formatter);
            }
            else if (Objects.equals(format, "$short_date_time")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault());
                String formattedDate = localDate.format(formatter);

                DateTimeFormatter formatter24 = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = time.format(formatter24);

                rv = formattedDate + "  " + formattedTime;
            }
            else if (Objects.equals(format, "$24_hour_minutes_seconds")) {
                DateTimeFormatter formatter24 = DateTimeFormatter.ofPattern("HH:mm:ss");
                rv = time.format(formatter24);
            }
            else if (Objects.equals(format, "$hour_minutes_seconds")) {
                DateTimeFormatter formatter24 = DateTimeFormatter.ofPattern("hh:mm:ss");
                rv = time.format(formatter24);
            }
        } else {
            rv = " ";
        }

        //Log.i("OpenCPN", "gldt_out: " + rv);

        return rv;
    }

    public String getAndroidTZOffsetMinutes() {
        TimeZone timeZone = TimeZone.getDefault();
        long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
        int offsetInMillis = timeZone.getOffset(currentTimeMillis);
        int offsetInMinutes = offsetInMillis / 1000 / 60;
        String rv = Integer.toString(offsetInMillis);
        Log.i("OpenCPN", "   offsetInMinutes: " + rv);
        return rv;
    }

    public String doGRIBActivity(String json) {
        Log.i("OpenCPN", "doGRIBActivity");
//        Log.i("DEBUGGER_TAG", json);


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("GRIB_PREFS_JSON", json);
        editor.apply();

        Intent intent = new Intent(activity, org.opencpn.OCPNGRIBActivity.class);
        activity.startActivityForResult(intent, OCPN_GRIB_REQUEST_CODE);

        int pss = 666;
        String ret;
        ret = String.format("%d", pss);
        return ret;

    }

    public void relocateOCPNPlugins() {
        // We need to relocate the PlugIns that have been included as "assets"

        // Reason:  PlugIns can only load from the apps dataDir, which is like:
        //          "/data/data/org.opencpn.opencpn"
        //          This is due to some policy in the system loader....
        //
        //           There is no need to relocate any data files needed by the PlugIns
        //           since they will have been added as assets and moved to the file system
        //           by assetbridge elsewhere.
        //
        //          Since this method runs on every restart, it may be used to condition manually installed
        //          PlugIns as well.  Just somehow install the PlugIn .so file into ".../files/plugins" dir,
        //          and it will be moved to the proper load location on restart.

        Log.i("OpenCPN", "relocateOCPNPlugins");

        //  Detect 64 bits:
        boolean b64 = true;

/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i("OpenCPN", "relocateOCPNPlugins for " + Build.SUPPORTED_ABIS[0]);
            b64 = TextUtils.join(", ", Build.SUPPORTED_ABIS).contains("64") ? TRUE : FALSE;
        } else {
            b64 = FALSE;
        }
*/

        if (b64)
            Log.i("OpenCPN", "relocateOCPNPlugins64");
        else
            Log.i("OpenCPN", "relocateOCPNPlugins32");

/*
       // On Moto G
       // This produces "/data/data/org.opencpn.opencpn/files"
       //  Which is where the app files would be with default load
       String iDir = getFilesDir().getPath();
       String ssd = iDir + "/plugins";


       //  Maybe the app has been migrated to SDCard...
       sApplicationInfo ai = getApplicationInfo();
       if((ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) ==  ApplicationInfo.FLAG_EXTERNAL_STORAGE){
           Log.i("OpenCPN", "relocateOCPNPlugins:  OCPN is on EXTERNAL_STORAGE");
           File[] fx = getExternalFilesDirs(null);
           if(fx.length > 1){
               ssd = fx[1].getPath() + File.separator + "plugins";
           }
       }
*/
        File fd = new File(m_filesDir);
        String ssd = fd.getPath() + File.separator + "plugins";

        File sourceDir = new File(ssd);

        // The PlugIn .so files are always relocated to here, which looks like:
        // "/data/data/org.opencpn.opencpn"
        String finalDestination = activity.getApplicationInfo().dataDir;

        File[] dirs = sourceDir.listFiles();
        if (dirs != null) {
            for (int j = 0; j < dirs.length; j++) {
                File sfile = dirs[j];
                Log.i("OpenCPN", "relocateOCPNPlugins considering: " + sfile.getName());

                // As a special case, if present we capture and relocate "libgnustl_shared.so", 32 Bit,  to support some legacy plugins.
                //  We store it in "finalDestination", and plugins can access it by specifying a LD_LIBRARY environment directory
                if (sfile.isFile() && sfile.getAbsolutePath().endsWith("libgnustl_shared.so")) {
                    try {
                        File destinationFile = new File(finalDestination + "/" + "libgnustl_shared.so");
                        if (!destinationFile.exists()) {
                            File parentDirectory = destinationFile.getParentFile();
                            if (!parentDirectory.exists())
                                parentDirectory.mkdirs();

                            InputStream inputStream = new FileInputStream(sfile.getAbsolutePath());
                            OutputStream outputStream = new FileOutputStream(finalDestination + "/" + "libgnustl_shared.so");
                            copyFile(inputStream, outputStream);
                            inputStream.close();
                            outputStream.close();
                            Log.i("OpenCPN", "relocateOCPNPlugins copyFile libgnustl_shared.so OK");
                        } else {
                            Log.i("OpenCPN", "relocateOCPNPlugins libgnustl_shared.so already in place");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("OpenCPN", "relocateOCPNPlugins copyFile libgnustl_shared.so Exception");
                    }
                }

                if (sfile.isFile() && !sfile.getAbsolutePath().endsWith(".so")) {     // Don't relocate legacy plugins on upgrade

                    String source = sfile.getAbsolutePath();
                    String soName = "NOTSET";
                    if (source.endsWith("so64") || source.endsWith("so32")) {
                        if (b64) {
                            if (source.endsWith("so64"))
                                soName = sfile.getName().replace("so64", "so");
                        } else {
                            if (source.endsWith("so32"))
                                soName = sfile.getName().replace("so32", "so");
                        }
                    } else {        // Any binary executables, or any other files not ending with "soxx"?
                        soName = sfile.getName();          // if so, copy directly
                    }

                    if (soName.equals("NOTSET"))
                        continue;                         // skip everything else

                    String dest = finalDestination + "/" + soName;

                    if (soName.contains("celes")) {
                        dest = finalDestination + "/manPlug/" + soName;
                    }
                    try {
                        InputStream inputStream = new FileInputStream(source);
                        OutputStream outputStream = new FileOutputStream(dest);
                        copyFile(inputStream, outputStream);
                        inputStream.close();
                        outputStream.close();
                        Log.i("OpenCPN", "relocateOCPNPlugins copyFile OK: " + source + " to " + dest);

                        if (!dest.endsWith(".so")) {
                            Log.i("OpenCPN", "relocateOCPNPlugins setting executable on: " + dest);
                            File file = new File(dest);
                            file.setExecutable(true);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("OpenCPN", "relocateOCPNPlugins copyFile Exception");
                    }
                }
            }
        }

        // A special case:
        // In OA 5.2.2/61, Squiddio was ported to a managed plugin.
        //  So, on app upgrade, we need to remove the unmanaged (system-type) Squiddio plugin
        //   in order that the managed version can be loaded without CommonName conflist.
        if (BuildConfig.VERSION_CODE >= 61) {
            File squiddio_unmanaged = new File(finalDestination + "/libsquiddio_pi.so");
            if (squiddio_unmanaged.exists())
                squiddio_unmanaged.delete();
        }

    }
    private static final int BUFFER_SIZE =  1048576;
    static void copyFile(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        int count;
        while ((count = inputStream.read(buffer)) > 0)
            outputStream.write(buffer, 0, count);
    }

    public String playSound(final String fileName, final String sound) {
        Log.i("DEBUGGER_TAG", "playSound " + fileName);
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        if (mediaPlayer != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(fileName);
                        mediaPlayer.prepare();
                        mediaPlayer.setOnCompletionListener(
                                new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        BigInteger soundcb = new BigInteger(sound, 16);
                                        long ptr = soundcb.longValue();
                                        nativeLib.onSoundDone(ptr);
                                    }
                                }
                        );
                        //Log.i("DEBUGGER_TAG", "playSoundStart");
                        mediaPlayer.start();
                    } catch (Exception e) {
                        // TODO: Remove this error checking before publishing
                    }
                }
            });

        }
        return "OK";
    }

    public String setTrackContinuous(final int btrack) {
        m_trackContinuous = (btrack != 0);
        return "OK";
    }

    public void doSetFullscreen(final boolean bfull) {

        final QtActivityDelegate delegate = QtNative.activityDelegate();

        if (null != delegate) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    delegate.setFullScreen(bfull);

                    if (bfull) {
                        int flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
                    }

                }
            });
        }
    }

    public void setFullscreen(boolean fs) {
        m_fullScreen = fs;
    }

    public String setFullscreen(final int bfull) {
        doSetFullscreen(bfull != 0);
        m_fullScreen = (bfull != 0);
        return "OK";
    }


    public String getFullscreen() {
        if (m_fullScreen)
            return "YES";
        else
            return "NO";
    }

    public String setRouteAnnunciator(final int viz) {
        m_showRouteAnnunciator = (viz != 0);
//    if( null != itemRouteAnnunciator)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    itemRouteAnnunciator.setVisible(viz != 0);
                    activity.invalidateOptionsMenu();
                }
            });
            return "OK";
        }
    }

    public String lastCallOnInit(final int param) {
        // Do all those things necessary after app initialization, and make ready to run
        connectivityManager.enableMulticast();
        return "OK";
    }

    public String launchWebView(String url) {
        Log.i("OpenCPN", "launchWebView: " + url);

        Intent intent = new Intent(activity, WebViewActivity.class);

        Bundle b = new Bundle();
        b.putString(WebViewActivity.SELECTED_URL, url);
        intent.putExtras(b);

        activity.startActivity(intent);
        return "OK";
    }

    public String showToast(final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Set the toast
                mTimedToast = Toast.makeText(activity.getApplicationContext(), text, Toast.LENGTH_LONG);
                mTimedToast.show();

            }
        });


        return "OK";
    }

    public void toggleFullscreen() {
        m_fullScreen = !m_fullScreen;
        if (m_fullScreen) setFullscreen(1);
        else setFullscreen(0);
        nativeLib.notifyFullscreenChange(m_fullScreen);
    }

    public String doAndroidSettings(String settings) {
        if (null != connectivityManager.uSerialHelper)
            m_scannedSerial = connectivityManager.scanSerialPorts(UsbSerialHelper.NOSCAN);      // No scan, just report latest results.

        m_settingsReturn = new String();

        Intent intent = new Intent(activity, org.opencpn.OCPNSettingsActivity.class);
        intent.putExtra("SETTINGS_STRING", settings);
        intent.putExtra("DETECTEDSERIALPORTS_STRING", m_scannedSerial);
        Log.i("OpenCPN", "doAndroidSettings settings" + settings);
        Log.i("OpenCPN", "doAndroidSettings m_scannedSerial" + m_scannedSerial);

        activity.startActivityForResult(intent, OCPN_SETTINGS_REQUEST_CODE);

        return "55";
    }

    public String checkAndroidSettings() {
        return m_settingsReturn;
    }

    public void applyAndroidPreferences() {
        //  Apply any Android private settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        Boolean bnoSleep = preferences.getBoolean("prefb_noSleep", false);
        if (bnoSleep)
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        m_trackContinuous = preferences.getBoolean("prefb_trackOnPause", false);
    }

    public String invokeGoogleMaps() {
        Log.i("DEBUGGER_TAG", "invokeGoogleMaps");

        Intent intent = new Intent(activity, org.opencpn.OCPNMapsActivity.class);

        String s = nativeLib.getVPCorners();
        if (s.isEmpty()) {
            Log.i("DEBUGGER_TAG", "invokeGoogleMaps..Error, empty VPCorners");
            return "66";
        }

        // Validate the viewport "corners" string
        try {
            StringTokenizer tkz = new StringTokenizer(s, ";");
            String tk = tkz.nextToken();
            double latne = Double.parseDouble(tk);

            tk = tkz.nextToken();
            double lonne = Double.parseDouble(tk);

            tk = tkz.nextToken();
            double latsw = Double.parseDouble(tk);

            tk = tkz.nextToken();
            double lonsw = Double.parseDouble(tk);
        } catch (Exception e) {
            Log.i("DEBUGGER_TAG", "invokeGoogleMaps..Error, Invalid VPCorners");
            return "66";
        }


        String v = nativeLib.getVPS();
        Log.i("DEBUGGER_TAG", "initialPositionString" + v);

        StringTokenizer tkz = new StringTokenizer(v, ";");

        String initialLat = "";
        String initialLon = "";
        String initialZoom = "";

        if (tkz.hasMoreTokens()) {
            initialLat = tkz.nextToken();
            initialLon = tkz.nextToken();
            initialZoom = tkz.nextToken();
        }

        m_gminitialZoom = Double.parseDouble(initialZoom);

        intent.putExtra("VP_CORNERS", s);
        intent.putExtra("VPS", v);

        int height = activity.getWindow().getDecorView().getHeight();
        int width = activity.getWindow().getDecorView().getWidth();
        intent.putExtra("WIDTH", width);
        intent.putExtra("HEIGHT", height);

        activity.startActivityForResult(intent, OCPN_GOOGLEMAPS_REQUEST_CODE);

        return "56";
    }


    /*
    ========================================================================
        // Some general UI support items and dialogs
    =======================================================================
    */

    public String showHTMLAlertDialog(String title, String htmlString) {

        WebView wv = new WebView(activity.getApplicationContext());

        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setMinimumFontSize(50);
        wv.loadData(htmlString, "text/html; charset=UTF-8", null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setView(wv)
                .setCancelable(false)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        //                     alertDialogBuilder.setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
        //                             public void onClick(DialogInterface dialog,int id) {
        // if this button is clicked, close
        // current activity
        //                                     QtActivity.this.finish();
        //                             }
        //                       });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();


        // show it
        alertDialog.show();

        return ("OK");
    }

    public String showBusyCircle() {
        //if(!m_fullScreen)

        {

            mutexC = new Semaphore(0);

            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (null == ringProgressDialog) {
                        Log.i("OpenCPN", "ShowBusyBuild");
                        ringProgressDialog = new ProgressDialog(activity, R.style.MyTheme);
                        ringProgressDialog.setCancelable(false);
                        ringProgressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);

                        Drawable myIcon = activity.getResources().getDrawable(R.drawable.progressbar_custom);
                        ringProgressDialog.setIndeterminateDrawable(myIcon);

                        //  THIS IS IMPORTANT...Keeps the busy spinner from surfacing the hidden navigation buttons.
                        ringProgressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    }

                    ringProgressDialog.show();

                    mutexC.release();

                }
            });
        }

        // One way to wait for the runnable to be done...
        try {
            //mutexC.acquire();            // Cannot get mutex until runnable above exits.
            mutexC.tryAcquire(500, MILLISECONDS);            // Cannot get mutex until runnable above exits.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        String ret = "";
        return ret;
    }

    public String hideBusyCircle() {

        //if (null == ringProgressDialog) {
        //    return "";
        //}

        mutexC = new Semaphore(0);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (null != ringProgressDialog) {
                    //if(ringProgressDialog.isShowing ())
                    {
                        Log.i("OpenCPN", "ShowBusyDismiss");
                        ringProgressDialog.dismiss();
                    }
                }

                mutexC.release();
            }
        });

        // One way to wait for the runnable to be done...
        try {
            //mutexC.acquire();            // Cannot get mutex until runnable above exits.
            mutexC.tryAcquire(500, MILLISECONDS);            // Cannot get mutex until runnable above exits.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String ret = "";
        return ret;
    }

    public String doColorPickerDialog(final int initialColor) {
        m_ColorDialogDone = false;


        Log.i("OpenCPN", "ColorPicker create and show " + initialColor);

        Thread thread = new Thread() {
            @Override
            public void run() {

                // Block this thread for 20 msec.
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }

// After sleep finishes blocking, create a Runnable to run on the UI Thread.

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ColorPickerDialog dialog = new ColorPickerDialog(activity, initialColor) {

                            @Override
                            public void onOK(int selectedColor) {
                                String strI = String.valueOf(selectedColor);
                                Log.i("DEBUGGER_TAG", "Activity on OK button Color: " + strI);
                                m_ColorDialogString = "color:" + strI;
                                m_ColorDialogDone = true;

                                // do something
                            }

                            @Override
                            public void onCancel() {
                                Log.i("DEBUGGER_TAG", "Activity on Cancel Color: ");
                                m_ColorDialogString = "cancel:";
                                m_ColorDialogDone = true;
                                // do something
                            }
                        };
                        dialog.show();
                        Log.i("DEBUGGER_TAG", "ColorPickerDialog Back from show");
                    }
                });
            }
        };

        // Don't forget to start the thread.
        thread.start();

        Log.i("DEBUGGER_TAG", "ColorPickerDialog Returning");

        return "OK";
    }

    public String isColorPickerDialogFinished() {
        Log.i("DEBUGGER_TAG", "poll");
        if (m_ColorDialogDone) {
            Log.i("DEBUGGER_TAG", "done");
            return m_ColorDialogString;
        } else {
            return "no";
        }
    }

    public void showSimpleOKDialog(int seq, final String title, final String message) {
        int timeoutSeconds = 30;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AtomicBoolean completed = new AtomicBoolean(false);

                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(activity);

                alertDialogBuilder
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        nb.postJsonEvent(
                                                JSONHelpers.makeEvent(seq,
                                                        "simpleOKDialogResponse",
                                                        "response", "OK",
                                                        "action", "response"));

                                    }
                                });

                AlertDialog dialog = alertDialogBuilder.create();
                dialog.show();

                if (timeoutSeconds > 0) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        if (completed.compareAndSet(false, true)) {

                            if (dialog.isShowing())
                                dialog.dismiss();
                            nb.postJsonEvent(
                                    JSONHelpers.makeEvent(seq,
                                            "simpleOKDialogResponse",
                                            "response", "OK",
                                            "action", "response"));
                        }

                    }, timeoutSeconds * 1000L);
                }
            }
        });
    }

    public void showSimpleYesNoDialog(int seq, final String title, final String message) {
        int timeoutSeconds = 30;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AtomicBoolean completed = new AtomicBoolean(false);

                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(activity);

                alertDialogBuilder
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        nb.postJsonEvent(
                                                JSONHelpers.makeEvent(seq,
                                                        "simpleYesNoDialogResponse",
                                                        "response", "Yes",
                                                        "action", "response"));

                                    }})
                        .setNegativeButton("No",
                                 new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            nb.postJsonEvent(
                                                    JSONHelpers.makeEvent(seq,
                                                            "simpleYesNoDialogResponse",
                                                            "response", "No",
                                                            "action", "response"));


                                        }});

                AlertDialog dialog = alertDialogBuilder.create();
                dialog.show();

                if (timeoutSeconds > 0) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        if (completed.compareAndSet(false, true)) {

                            if (dialog.isShowing())
                                dialog.dismiss();
                            nb.postJsonEvent(
                                    JSONHelpers.makeEvent(seq,
                                            "simpleOKDialogResponse",
                                            "response", "OK",
                                            "action", "response"));
                        }

                    }, timeoutSeconds * 1000L);
                }
            }
        });
    }


}