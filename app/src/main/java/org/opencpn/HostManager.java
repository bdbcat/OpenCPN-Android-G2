package org.opencpn;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.app.AlertDialog;
import android.view.Surface;
import android.content.pm.ActivityInfo;

import androidx.appcompat.app.AppCompatActivity;

import org.opencpn.opencpn.BuildConfig;
import org.qtproject.qt5.android.QtActivityDelegate;
import org.qtproject.qt5.android.QtNative;

import java.io.File;
import java.util.Locale;

public class HostManager {

    private final Activity activity;
    private final String m_filesDir;

    // state injected from Activity
    private boolean m_fullScreen = false;
    public android.graphics.Insets m_insets = android.graphics.Insets.NONE;

    private int insetTop = 0;
    private int insetBottom = 0;
    private int insetLeft = 0;
    private int insetRight = 0;

    public HostManager(Activity activity) {
        this.activity = activity;
        m_filesDir = resolveFilesDir();
    }


    public void setFullscreen(boolean fs) {
        m_fullScreen = fs;
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
        s += "\n" + "XXX"; //getPackageName();
        s += "\n UUID:" + "XXX"; //getUUID(this);
        s += "\n systemName:" + "XXX"; //m_systemName;
        s += "\n OCPNRUID:" + "XXX"; //getOCPNuniqueID();
        s += "\n OCPNWVID:" + "XXX"; //getOCPNWVID();
        s += "\n SELID:" + "XXX"; //m_selID;
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
}