package org.opencpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.arieslabs.assetbridge.Assetbridge;


public class OpenCPNBridge {

    private static Activity activity;

    private static OpenCPNBridge instance;

    private static String m_filesDir;

    public OpenCPNBridge(Context contest){

    };

    public static synchronized OpenCPNBridge getInstance(Activity activity) {
        if (instance == null) {
            instance = new OpenCPNBridge(activity.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }
    public static void init(Activity a) {
        activity = a;
    }

    public static void onCreate() {
        initFilesystem();
    }

    public void onCreate(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (intent == null) return;

        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {

            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                importFileToPrivateStorage(fileUri);
            }

        } else if (Intent.ACTION_VIEW.equals(action)) {

            Uri fileUri = intent.getData();
            if (fileUri != null) {
                importFileToPrivateStorage(fileUri);
            }
        }
    }

    public static void setBackButtonState(int enabled) {
        // temporary stub (you can route to Activity later)
    }

    public static void shutdown() {
        activity = null;
    }

    //  Handle file import for intent startup
    private void importFileToPrivateStorage(Uri sourceUri) {
        try {
            // Get filename (fallback name if can't resolve)
            String fileName = queryFileName(sourceUri);
            if (fileName == null) fileName = "imported_file_" + System.currentTimeMillis();
            String file_ext = fileName.substring(fileName.lastIndexOf("."));

            // InputStream from content URI
            InputStream inputStream = activity.getContentResolver().openInputStream(sourceUri);

            String destinationDir = activity.getExternalFilesDir(null).getAbsolutePath();

            if (file_ext.length() > 0) {
                if (file_ext.equals(".grb") || file_ext.equals(".grib") || file_ext.equals(".grb2")) {
                    destinationDir += "/GRIBS";
                    File directory = new File(destinationDir);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                } else if (file_ext.equals(".gpx") || file_ext.equals(".GPX")) {
                    destinationDir += "/Import";
                    File directory = new File(destinationDir);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                }

                String destFile = destinationDir + "/" + fileName;
                OutputStream outputStream = new FileOutputStream(destFile);

                // Copy data
                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                //Toast.makeText(this, "File imported: " + destFile, Toast.LENGTH_LONG).show();
                Log.d("OpenCPN", "Imported to: " + destFile);
            }
        } catch (Exception e) {
            Log.e("OpenCPN", "Failed to import file", e);
            //Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to get filename from Uri (best effort)
    private String queryFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.w("OpenCPN", "Couldn't resolve file name", e);
            }
        }

        // Fallback if needed
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    //  Initialize file references, manage installation state.
    public static void initFilesystem() {
        m_filesDir = resolveFilesDir();
        Log.i("OpenCPN", "Application filesDir: " + m_filesDir);

        setupLogDir();

        boolean needsCopy = checkInstallState(m_filesDir);

        if (needsCopy)
        {
            CreateCachedVersionFile();
            unpackAssets(m_filesDir);
        }
    }

    private static String resolveFilesDir() {
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

    private static void setupLogDir() {
        try {
            String dir = activity.getExternalCacheDir().getAbsolutePath();

            final File path = new File(activity.getExternalCacheDir(), "OCPN_logs");
            if (!path.exists()) {
                path.mkdir();
            }
            String spath = path.getAbsolutePath() + File.separator + "OCPN_logcat" + ".txt";

            final File oldFile = new File(spath);
            if (oldFile.exists()) {
                Log.i("OpenCPN", "Delete logfile: " + spath);
                oldFile.delete();
            }

            Runtime.getRuntime().exec("logcat " + "-f " + spath + " -s OpenCPN -s libgorp.so ");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static boolean checkInstallState(String dir) {
        boolean b_needcopy = false;
        if (null == activity.getLastNonConfigurationInstance()) {
            // if splash screen is defined, then show it

            String tmpdir = "";

            tmpdir = activity.getApplicationInfo().dataDir + "/qt-reserved-files/";

            boolean m_bNeworUpdateInstall = !checkCacheVersionValid(tmpdir, "OCPNcache.version");

            if (m_bNeworUpdateInstall) {
                b_needcopy = true;

                //deleteRecursively(new File(tmpdir));
            }


            //  Take a look for a specific file in the data directory, just to confirm...
            File testFile = new File(m_filesDir + "/license.txt");
            Log.i("OpenCPN", "Checking for required UI files...");
            if (!testFile.exists()) {
                Log.i("OpenCPN", "Force copy invoked.");
                b_needcopy = true;
            }

        }
        return b_needcopy;
    }

    private static boolean checkCacheVersionValid(String prefix, String cacheName) {
            Log.i("OpenCPN", "checkCacheVersionValid " + prefix);

            long packageVersion = -1;
            try {
                PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                packageVersion = packageInfo.lastUpdateTime;
            } catch (Exception e) {
                e.printStackTrace();
            }

            ApplicationInfo ai = activity.getApplicationInfo();
            if ((ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE)
                packageVersion++;

            Log.i("OpenCPN", "checkCacheVersionValid: current running packageVersion " + String.valueOf(packageVersion));

            File versionFile = new File(prefix + cacheName);

            Log.i("OpenCPN", "checkCacheVersionValid: version file: " + prefix + cacheName);

            long cacheVersion = 0;
            if (versionFile.exists() && versionFile.canRead()) {
                Log.i("OpenCPN", "version file exists ");
                try {
                    DataInputStream inputStream = new DataInputStream(new FileInputStream(versionFile));
                    cacheVersion = inputStream.readLong();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Log.i("OpenCPN", "checkCacheVersionValid:  cached value is: " + String.valueOf(cacheVersion));

            if (cacheVersion == packageVersion) {
                Log.i("OpenCPN", "checkCacheVersionValid return true");
                return true;
            } else {
                Log.i("OpenCPN", "checkCacheVersionValid return false");
                return false;
            }
    }

    private static void CreateCachedVersionFile() {
        try {
            long packageVersion = -1;
            try {
                PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                packageVersion = packageInfo.lastUpdateTime;
            } catch (Exception e) {
                e.printStackTrace();
            }

            ApplicationInfo ai = activity.getApplicationInfo();
            if ((ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE)
                packageVersion++;

            String tmpdir = activity.getApplicationInfo().dataDir + "/qt-reserved-files/";
            File versionFile = new File(tmpdir + "/OCPNcache.version");

            File parentDirectory = versionFile.getParentFile();
            if (!parentDirectory.exists())
                parentDirectory.mkdirs();

            versionFile.createNewFile();

            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(versionFile));
            outputStream.writeLong(packageVersion);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void unpackAssets(String files_dir) {
        //Toast.makeText(getApplicationContext(), "Please stand by while OpenCPN initializes..." ,Toast.LENGTH_LONG).show();

        //Log.i("OpenCPN", "getSystemDirs(): " + activity.getSystemDirs());

        String dirFiles = files_dir;

        Log.i("OpenCPN", "Checking write access to: " + dirFiles);
        File fc = new File(dirFiles);
        boolean bDirReady = false;
        if (!fc.exists()) {
            Log.i("OpenCPN", "Dir does not exist: " + dirFiles);

            try {
                if (fc.mkdirs())
                    bDirReady = true;
                else {
                    if (fc.isDirectory()) {
                        bDirReady = true;
                    } else
                        Log.i("OpenCPN", "mkdirs fails: " + dirFiles);
                }
            } catch (Exception e) {
                Log.i("OpenCPN", "Exception on mkdirs: " + dirFiles);
            }
        } else
            bDirReady = true;


        boolean bWrite = false;
        try {
            if (fc.canWrite()) {
                bWrite = true;
            }
        } catch (Exception e) {
            bWrite = false;
        }

        Log.i("OpenCPN", "Write access check result: " + String.valueOf(bWrite));

        // Actually try a write....
        File testWriteDir = new File(dirFiles + "/uidata");
        try {
            if (testWriteDir.mkdir()) {
                Log.i("OpenCPN", "Write access verify resultA: OK");
            } else {
                if (testWriteDir.isDirectory()) {
                    Log.i("OpenCPN", "Write access verify resultB: OK");
                } else {
                    Log.i("OpenCPN", "mkdir fails: " + dirFiles + "/uidata");
                    bWrite = false;
                }
            }
        } catch (Exception e) {
            Log.i("OpenCPN", "Write access verify result: FAILS");
            bWrite = false;
        }


        // Absolute bailout....
        //if (!bWrite || !bDirReady) {
        //    m_filesDir = getFilesDir().getAbsolutePath();
        //    dirFiles = m_filesDir;
        //    Log.i("OpenCPN", "Forcing Application filesDir to: " + m_filesDir);
        //}


        Log.i("OpenCPN", "asset bridge start unpack");
        Assetbridge.unpackNoDoc(activity, dirFiles);
        Log.i("OpenCPN", "asset bridge finish unpack");
    }


}