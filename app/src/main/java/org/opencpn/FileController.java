package org.opencpn;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import ar.com.daidalos.afiledialog.*;

public class FileController {


    public static final String PREF_CHOOSER_WILD_CARD = "PREF_CHOOSER_WILD_CARD";
    private static final String PREFS_NAME = "opencpn_storage";
    private static final String PREF_SLC_REMEMBER_CHOICE = "remember_choice";
    private static final String PREF_REMEMBER = "remember_storage_location";

    private static final String PREF_LOCATION = "storage_location";
    public static final int OCPN_FILEMANAGER_REQUEST_CODE = 5564;

    private final Activity activity;
    //private final AndroidBridge bridge;
    public final SafManager safManager;
    private final NativeBridgeManager nativeBridge;
    private static final int BUFFER_SIZE = 1024 * 1024;
    private boolean m_rememberChoice;
    private String m_exportFile;
    public FileController(Activity activity, OpenCPNBridge bridge, NativeBridgeManager nB) {
        this.activity = activity;
        nativeBridge = nB;
        safManager = new SafManager(activity, nativeBridge);
    }

    // -----------------------------------------------------------------------------
    // Helpers
    static private void copyFile(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        int count;
        while ((count = inputStream.read(buffer)) > 0)
            outputStream.write(buffer, 0, count);
    }


    // ------------------------------------------------------------------------------
    // General utility functions
    public String SecureFileCopy(String inFile, String outFile) {
        Log.i("OpenCPN", "SecureFileCopy: " + inFile + " to: " + outFile);

        // If the inFile seems to be a uri...
        if (inFile.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(inFile);
                InputStream inStream = activity.getContentResolver().openInputStream(uri);

                FileOutputStream outStream = new FileOutputStream(outFile);
                copyFile(inStream, outStream);
                inStream.close();
                outStream.flush();
                outStream.close();
                return "OK";
            } catch (Exception e) {
                Log.i("OpenCPN", "SecureFileCopy Exception");
                return "Exception";
            }
        }
        else if (outFile.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(outFile);
                OutputStream outStream = activity.getContentResolver().openOutputStream(uri);
                FileInputStream inStream = new FileInputStream(inFile);

                copyFile(inStream, outStream);
                inStream.close();
                outStream.flush();
                outStream.close();
                return "OK";
            } catch (Exception e) {
                Log.i("OpenCPN", "SecureFileCopy Exception");
                return "Exception";
            }
        }


        //if (outFile.length() == 0)
        //    outFile = m_fileChooserFinalString;

        // Process "Export" procedure as speial case
        //String exportURI = exportMap.get(outFile);
        //if (exportURI != null) {
        //    try {
                // target file Uri is know, so direct copy is reasonable.
        //        Uri target = Uri.parse(exportURI);
        //        OutputStream outStream = getContentResolver().openOutputStream(target);
        //        FileInputStream inStream = new FileInputStream(inFile);
        //        copyFile(inStream, outStream);
        //        exportMap.remove(outFile);      // success
        //        return "OK";
        //    }
        //    catch (Exception e) {
        //        Log.i("OpenCPN", "SecureFileCopy ExceptionE");
        //        return "Exception";
        //    }
        //}

        try {
            FileOutputStream outStream = null;
            FileInputStream inStream;
/*
            //  Is destination on an SDCard?
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                File outFileObj = new File(outFile);
                String sdRoot = activity.getExtSdCardFolder(outFileObj);
                if (null != sdRoot) {
                    Log.i("OpenCPN", "SecureFileCopy destination on SDCard");

                    if (Build.VERSION.SDK_INT >= 30) {      // Android 11 does not need permission to write on private dir
                        File xfiles[] = getExternalFilesDirs(null);
                        if (xfiles.length > 1) {
                            String sdHome = "";
                            try {
                                sdHome = xfiles[1].getCanonicalPath();
                            } catch (Exception e) {
                            }

                            if (outFile.startsWith(sdHome))
                                outStream = new FileOutputStream(outFile);
                        }
                    } else {
                        DocumentFile targetDF = getDocumentFile(outFileObj, false, true);

                        try {
                            OutputStream os = getContentResolver().openOutputStream(targetDF.getUri());
                            outStream = (FileOutputStream) os;
                        } catch (Exception e) {
                            Log.i("OpenCPN", "SecureFileCopy ExceptionB");
                        }
                    }

                } else

                 {

                    try {
                        outStream = new FileOutputStream(outFile);
                    } catch (Exception e) {
                        // The target directory need SAF access
                        // First copy the file to local cache directory.
                        File file = new File(outFile);
                        String fileName = file.getName();
                        String cacheFileName = getExternalCacheDir().getAbsolutePath();
                        cacheFileName += "/" + fileName;
                        FileOutputStream outStreamCache = new FileOutputStream(cacheFileName);
                        FileInputStream inStreamCache = new FileInputStream(inFile);
                        try {
                            copyFile(inStreamCache, outStreamCache);
                        } catch (Exception ec) {
                            Log.i("OpenCPN", "SecureFileCopy ExceptionF");
                            return "Exception";
                        }

                        File ofile = new File(outFile);
                        String ofileName = ofile.getName();

                        StartSAFDirSelector("Documents", ofileName);
                        return "OK";

                    }
                }
            } else
*/
            {
                Log.i("OpenCPN", "SecureFileCopy destination on internal storage");
                try {
                    outStream = new FileOutputStream(outFile);
                } catch (Exception e) {
                    Log.i("OpenCPN", "SecureFileCopy ExceptionC");
                    return "Exception";
                }
            }

            inStream = new FileInputStream(inFile);

            try {
                copyFile(inStream, outStream);
            } catch (Exception e) {
                Log.i("OpenCPN", "SecureFileCopy ExceptionA");
            }


            return "OK";

        } catch (Exception e) {
            Log.i("OpenCPN", "SecureFileCopy Exception");
            return "Exception";
        }
    }

    public String fileDomainSelector(int seq, boolean isImport) {
        activity.runOnUiThread(() -> {
            android.content.SharedPreferences prefs = activity.getSharedPreferences(
                    PREFS_NAME, Activity.MODE_PRIVATE);
            Integer fdc_remembered_ordinal =  prefs.getInt(PREF_SLC_REMEMBER_CHOICE, -1);
            if (fdc_remembered_ordinal == -1)
                fdc_remembered_ordinal = 0;

            StorageLocationDialog.show(
                    activity,
                    isImport,      // false = Export
                    fdc_remembered_ordinal,
                    new StorageLocationDialog.Callback() {

                        @Override
                        public void onLocationSelected(
                                StorageLocationDialog.StorageLocation location,
                                boolean rememberChoice) {
                            m_rememberChoice = rememberChoice;
                            if (m_rememberChoice) {
                                android.content.SharedPreferences prefs = activity.getSharedPreferences(
                                        PREFS_NAME, Activity.MODE_PRIVATE);

                                prefs.edit()
                                        .putInt(PREF_SLC_REMEMBER_CHOICE, location.ordinal())
                                        .apply();
                            }

                            switch (location) {

                                case APP_PRIVATE:
                                    Log.i("OpenCPN","^^^^^^PRIVATE");
                                    nativeBridge.postJsonEvent(
                                            JSONHelpers.makeEvent(seq,
                                                    "fileDomainSelected",
                                                    "domain", "Private",
                                                    "action", "select"));
                                    break;

                                case SHARED_DOCUMENTS:
                                    Log.i("OpenCPN","^^^^^^SYSTEM");
                                    nativeBridge.postJsonEvent(
                                            JSONHelpers.makeEvent(seq,
                                                    "fileDomainSelected",
                                                    "domain", "System",
                                                    "action", "select"));
                                    break;

                                case CANCELLED:
                                    nativeBridge.postJsonEvent(
                                            JSONHelpers.makeEvent(seq,
                                                    "fileDomainSelected",
                                                    "domain", "Cancel",
                                                    "action", "select"));
                                    break;
                            }
                        }
                    });
        });

        return "OK";

    }

    //--------------------------------------------------------------------------------------
    //  Directory chooser dialog for local sandbox directories
    //  Async version
    //--------------------------------------------------------------------------------------
    public String privateDirChooserDialogAsync(final int seq, final String initialDir, final String Title, final String addFile) {

        String dirStart = initialDir;
        if (dirStart.isEmpty())
            dirStart = activity.getExternalFilesDir(null).getPath();

        final String dirThreadStart = dirStart;

        Log.i("OpenCPN", "localDirChooserDialog create and show " + dirThreadStart);

       activity.runOnUiThread(() -> {

            final FileChooserDialog dialog =
                    new FileChooserDialog(
                            activity,
                            dirThreadStart);

                        dialog.setShowFullPath(true);
                        dialog.setFolderMode(true);
                        boolean baddfile = false;
                        if (addFile.startsWith("true")) baddfile = true;
                        dialog.setFolderMode(true);
                        dialog.setCanCreateFiles(baddfile);
                        dialog.setTitle(Title);

                        dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
                            public void onFileSelected(Dialog source, File file) {
                                source.hide();
                                String fileName = file.getAbsolutePath();
                                nativeBridge.postJsonEvent(
                                        JSONHelpers.makeEvent(seq,
                                                "privateDirSelected",
                                                "dirName", fileName,
                                               "action", "select"));

                            }

                            public void onFileSelected(Dialog source, File folder, String name) {
                                //Log.i("OpenCPN", "Activity onFileSelectedB: " + folder.getPath() +  File.separator + name);

                                File newFolder = new File(folder.getPath() + File.separator + name);
                                boolean success = true;
                                if (!newFolder.exists())
                                    success = newFolder.mkdirs();

                                if (!success) {
                                    Toast toast = Toast.makeText(source.getContext(), "Could not create file in: " + folder.getPath(), Toast.LENGTH_LONG);
                                    toast.show();
                                }

                                dialog.loadFolder(folder.getPath());
                            }

                        });

                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                Log.i("OpenCPN", "DirChooserDialog Cancel");
                                nativeBridge.postJsonEvent(
                                        JSONHelpers.makeEvent(seq,
                                                "privateDirSelected",
                                                "dirName", "",
                                                "action", "cancel"));
                            }
                        });

                        dialog.show();
                });

        return "OK";
    }

    //--------------------------------------------------------------------------------------
    //  File chooser dialog for local sandbox directories
    //  Async version
    //--------------------------------------------------------------------------------------
    public String privateFileChooserDialogAsync(final int seq, final String initialDir, final String Title, final String addFile) {

        String dirStart = initialDir;
        if (dirStart.isEmpty())
            dirStart = activity.getExternalFilesDir(null).getPath();

        final String dirThreadStart = dirStart;

        Log.i("OpenCPN", "localFileChooserDialog create and show " + dirThreadStart);

        activity.runOnUiThread(() -> {

            final FileChooserDialog dialog =
                    new FileChooserDialog(
                            activity,
                            dirThreadStart);

            dialog.setShowFullPath(true);
            dialog.setFolderMode(false);
            boolean baddfile = false;
            if (addFile.startsWith("true")) baddfile = true;
            dialog.setCanCreateFiles(baddfile);
            dialog.setTitle(Title);

            dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
                public void onFileSelected(Dialog source, File file) {
                    source.hide();
                    String fileName = file.getAbsolutePath();
                    nativeBridge.postJsonEvent(
                            JSONHelpers.makeEvent(seq,
                                    "privateFileSelected",
                                    "fileName", fileName,
                                    "action", "select"));

                }

                public void onFileSelected(Dialog source, File folder, String name) {
                    //Log.i("OpenCPN", "Activity onFileSelectedB: " + folder.getPath() +  File.separator + name);

                    File newFolder = new File(folder.getPath() + File.separator + name);
                    boolean success = true;
                    if (!newFolder.exists())
                        success = newFolder.mkdirs();

                    if (!success) {
                        Toast toast = Toast.makeText(source.getContext(), "Could not create file in: " + folder.getPath(), Toast.LENGTH_LONG);
                        toast.show();
                    }

                    dialog.loadFolder(folder.getPath());
                }

            });

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Log.i("OpenCPN", "DirChooserDialog Cancel");
                    nativeBridge.postJsonEvent(
                            JSONHelpers.makeEvent(seq,
                                    "privateDirSelected",
                                    "dirName", "",
                                    "action", "cancel"));
                }
            });

            dialog.show();
        });

        return "OK";
    }

    //--------------------------------------------------------------------------------------
    //  Directory chooser dialog for local sandbox directories
    //  Sync version
    //--------------------------------------------------------------------------------------
    String filechooserString = "";
    public String privateDirChooserDialogSync(final int seq, final String initialDir, final String Title, final String addFile) {

        String dirStart = initialDir;
        if (dirStart.isEmpty())
            dirStart = activity.getExternalFilesDir(null).getPath();

        final String dirThreadStart = dirStart;

        Log.i("OpenCPN", "localDirChooserDialog create and show " + dirThreadStart);

        final CountDownLatch latch = new CountDownLatch(1);
        ///
       activity.runOnUiThread(() -> {

            final FileChooserDialog dialog =
                    new FileChooserDialog(
                            activity,
                            dirThreadStart);

            dialog.setShowFullPath(true);
            dialog.setFolderMode(true);
            boolean baddfile = false;
            if (addFile.startsWith("true")) baddfile = true;
            dialog.setFolderMode(true);
            dialog.setCanCreateFiles(baddfile);
            dialog.setTitle(Title);

            dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
                public void onFileSelected(Dialog source, File file) {
                    source.hide();
                    String fileName = file.getAbsolutePath();
                    filechooserString = fileName;
                    latch.countDown();


                    //Toast toast = Toast.makeText(source.getContext(), "File selected: " + file.getName(), Toast.LENGTH_LONG);
                    //toast.show();

                    //Log.i("OpenCPN", "Activity onFileSelectedA: " + file.getPath());
                    //m_filechooserString = "file:" + file.getPath();
                    //m_FileChooserDone = true;

                }

                public void onFileSelected(Dialog source, File folder, String name) {
                    //Log.i("OpenCPN", "Activity onFileSelectedB: " + folder.getPath() +  File.separator + name);

                    File newFolder = new File(folder.getPath() + File.separator + name);
                    boolean success = true;
                    if (!newFolder.exists())
                        success = newFolder.mkdirs();

                    if (!success) {
                        Toast toast = Toast.makeText(source.getContext(), "Could not create file in: " + folder.getPath(), Toast.LENGTH_LONG);
                        toast.show();
                    }

                    dialog.loadFolder(folder.getPath());
                }

            });

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Log.i("OpenCPN", "DirChooserDialog Cancel");
                    filechooserString = "cancel:";
                    //m_FileChooserDone = true;
                    latch.countDown();
                }
            });

            dialog.show();
        });




        // And wait for it to complete
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            filechooserString = "exception";
        }

        return filechooserString;
    }

    //--------------------------------------------------------------------------------------
    //  File chooser dialog for local sandbox files
    //--------------------------------------------------------------------------------------
/*
    public String FileChooserDialog(final String initialDir, final String Title, final String Suggestion, final String wildcard) {
        m_FileChooserDone = false;
        String safeDir = getExternalFilesDir(null).getPath();
        boolean bSafe = initialDir.startsWith(safeDir);


        // Check to see if "scoped storage model is active (Anroid 10 and above)
        if (!bSafe && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)) {
        } else {    //  Use the old-fashioned legacy file browser.
            Log.i("DEBUGGER_TAG", "FileChooserDialog create and show " + initialDir);
            final CountDownLatch latch = new CountDownLatch(1);
            Thread thread = new Thread() {
                @Override
                public void run() {

                    // Block this thread for 20 msec.
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                    }

                    // After sleep finishes blocking, create a Runnable to run on the UI Thread.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String startDir = initialDir;

                            File target = new File(initialDir);
                            if (!target.canWrite())
                                startDir = getExternalFilesDir(null).getPath();

                            FileChooserDialog dialog = new FileChooserDialog(m_activity, startDir);

                            dialog.setShowFullPath(true);
                            dialog.setTitle(Title);

                            //  Creating a file?
                            if (!Suggestion.isEmpty()) {
                                dialog.setCanCreateFiles(true);
                                dialog.setsuggestedOnCreate(Suggestion);
                            }

                            dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
                                public void onFileSelected(Dialog source, File file) {
                                    source.hide();
                                    //Toast toast = Toast.makeText(source.getContext(), "File selected: " + file.getName(), Toast.LENGTH_LONG);
                                    //toast.show();
                                    Log.i("OpenCPN", "FileChooserDialog selected: " + file.getName());

                                    m_filechooserString = "file:" + file.getPath();
                                    m_FileChooserDone = true;
                                    latch.countDown();
                                }

                                public void onFileSelected(Dialog source, File folder, String name) {
                                    source.hide();
                                    //Toast toast = Toast.makeText(source.getContext(), "File created: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
                                    //toast.show();
                                    Log.i("OpenCPN", "Listener: FileChooserDialog created: " + folder.getName() + "/" + name);

                                    m_filechooserString = "file:" + folder.getPath() + "/" + name;

                                    final File newFile = new File(folder.getPath() + File.separator + name);
                                    if (!folder.canWrite()) {
                                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                                            Log.i("OpenCPN", "FileChooserDialog listener needs SAF dialog");

                                            //  Is this on an SDCard?
                                            String sdRoot = getExtSdCardFolder(folder);
                                            if (null != sdRoot) {
                                                Log.i("OpenCPN", "FileChooserDialog listener found sdCard");

                                                // This will NOT create the file
                                                DocumentFile f = getDocumentFile(folder, true, false);
                                                if (null == f) {
                                                    startSAFDialog(OCPN_SAF_DIALOG_A_REQUEST_CODE);
                                                    return;
                                                }
                                            }
                                        }
                                    } else {
                                        try {
                                            newFile.createNewFile();
                                        } catch (IOException e) {
                                            m_filechooserString = "cancel:";
                                        }
                                    }

                                    m_FileChooserDone = true;
                                    latch.countDown();
                                }
                            });

                            dialog.setOnCancelListener(new OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    Log.i("OpenCPN", "FileChooserDialog Cancel");
                                    m_filechooserString = "cancel:";
                                    m_FileChooserDone = true;
                                    latch.countDown();
                                }
                            });

                            dialog.show();
                        }
                    });
                }
            };

            // Don't forget to start the thread.
            thread.start();

            // And wait for it to complete
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                m_filechooserString = "cancel:";
            }

            return m_filechooserString;
        }
    }
*/

    public String RemoveSystemFile(String filename) {
        File f = new File(filename);
        f.delete();
        return "OK";
    }


    // SAF interface
    public String ensureTreeAccess(int seq, String treeId) {
        return safManager.ensureTreeAccess(seq, treeId);
    }

    public String fileChooserDialogSAF(final int seq, final String initialDir, final String Title, final String Suggestion, final String wildcard) {
        String safeDir = activity.getExternalFilesDir(null).getPath();
        boolean bSafe = initialDir.startsWith(safeDir);

        // Check to see if "scoped storage model is active (Anroid 10 and above)
        if (!bSafe && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)) {
            // A bit of a hack...
            // Get the specified directory by taking all after /storage/emoulated/0/
            int index = initialDir.indexOf('0');
            String init_last_seg = "";
            if (index + 2 < initialDir.length())
                init_last_seg = initialDir.substring(index + 2);

            // Some initial directory requests are wrong (e.g. vdr_pi). Fix it here
            if (init_last_seg.startsWith("Android")) {
                int indexA = init_last_seg.indexOf('/');
                if (indexA + 1 < init_last_seg.length()) {
                    init_last_seg = init_last_seg.substring(indexA + 1);
                }
            }
            // Persist the wildcard for later access
            SharedPreferences sharedPrefs = activity.getSharedPreferences(PREF_CHOOSER_WILD_CARD, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_CHOOSER_WILD_CARD, wildcard);
            editor.apply();


            if (Suggestion.isEmpty()) {         // Choosing a file to read
                //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //intent.setType("*/*");       // Need this for ACTION_OPEN_DOCUMENT
                //activity.startActivityForResult(intent, OCPN_SAF_DIALOG_FILE_CHOOSER_REQUEST_CODE);

                safManager.openDocument(seq, wildcard);
            } else {            // choosing a directory to save a specified file
                // Check to see it resisted permission for the target directory already exist
                List<UriPermission> list = activity.getContentResolver().getPersistedUriPermissions();
                boolean bfound = false;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).isWritePermission()) {
                        Uri p = list.get(i).getUri();
                        String last = p.getLastPathSegment();
                        if (last != null) {
                            if (last.equals("primary:" + init_last_seg)) {
                                bfound = true;
                                break;
                            }
                        }
                    }
                }

                if (bfound) {       // directory permission already available
                    // Prepare the required new file using SAF
                    //Get the tree Uri from persistant storage

                    // verify the persisted access to the target dir
                    //  If not available, then re-run the SAF directory picker.
                    SharedPreferences preferences = activity.getSharedPreferences("OCPN_PERSISTED_PERMISSION", Context.MODE_PRIVATE);
                    String sUri = preferences.getString("filestorageuri", "");
                    if (sUri.isEmpty()) {
                        //StartSAFDirSelector(Suggestion, "Documents");
                        return "OK";
                    }

                    // Check to be sure the target Document trr exists, and is accessible
                    // If not, then cancel the entire operation
                    Uri selectedTreeUri = Uri.parse(sUri);
                    DocumentFile documentFile = DocumentFile.fromTreeUri(activity, selectedTreeUri);

                    safManager.openTreePicker();

                    //File ff = null;

                    //File ff = getFile(this, documentFile);

                    //String path = null;
                    //if (ff != null) {
                    //    path = ff.getAbsolutePath();
                    //} else {
                        //   m_filechooserString = "cancel:";
                        //   m_FileChooserDone = true;
                    //    return "OK";
                   // }

                    // All is well, fire off the equivalent of a "Save-as" dialog.
                    //Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    //intent.setType("*/*");       // Need this for ACTION_OPEN_DOCUMENT
                    //intent.addCategory(Intent.CATEGORY_OPENABLE);
                    //intent.setType("application/text");
                    //intent.putExtra(Intent.EXTRA_TITLE, Suggestion);
                    //m_filechooserCacheString = Suggestion;
                    //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, selectedTreeUri);
                    //startActivityForResult(intent, OCPN_SAVE_AS_FILE_CHOOSER_REQUEST_CODE);
                } else {      // Not persistend yet, so do it now.
                    // This would be the path for very first time access to a new directory
                    // StartSAFDirSelector("Documents", Suggestion);
                }
            }
        }
        return "OK";
    }

    public String fileSaveAsDialogSAF(final int seq, final String initialDir, final String Title, final String Suggestion) {
        String safeDir = activity.getExternalFilesDir(null).getPath();
        boolean bSafe = initialDir.startsWith(safeDir);

        // Check to see if "scoped storage model is active (Anroid 10 and above)
        if (!bSafe && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)) {
            // A bit of a hack...
            // Get the specified directory by taking all after /storage/emoulated/0/
            int index = initialDir.indexOf('0');
            String init_last_seg = "";
            if (index + 2 < initialDir.length())
                init_last_seg = initialDir.substring(index + 2);

            // Some initial directory requests are wrong (e.g. vdr_pi). Fix it here
            if (init_last_seg.startsWith("Android")) {
                int indexA = init_last_seg.indexOf('/');
                if (indexA + 1 < init_last_seg.length()) {
                    init_last_seg = init_last_seg.substring(indexA + 1);
                }
            }

            // choosing a directory to save a specified file

                // Check to see it resisted permission for the target directory already exist
                List<UriPermission> list = activity.getContentResolver().getPersistedUriPermissions();
                boolean bfound = false;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).isWritePermission()) {
                        Uri p = list.get(i).getUri();
                        String last = p.getLastPathSegment();
                        if (last != null) {
                            if (last.equals("primary:" + init_last_seg)) {
                                bfound = true;
                                break;
                            }
                        }
                    }
                }

                if (bfound) {       // directory permission already available
                    // Prepare the required new file using SAF
                    //Get the tree Uri from persistant storage

                    // verify the persisted access to the target dir
                    //  If not available, then re-run the SAF directory picker.
                    SharedPreferences preferences = activity.getSharedPreferences("OCPN_PERSISTED_PERMISSION", Context.MODE_PRIVATE);
                    String sUri = preferences.getString("filestorageuri", "");
                    if (sUri.isEmpty()) {
                        //StartSAFDirSelector(Suggestion, "Documents");
                        //return "OK";
                    }

                    // All is well, fire off the equivalent of a "Save-as" dialog.
                    safManager.saveDocumentAs(seq, Suggestion);

                } else {      // Not persistend yet, so do it now.
                    // This would be the path for very first time access to a new directory
                    // StartSAFDirSelector("Documents", Suggestion);
                }

        }
        return "OK";
    }

    public String exportFile(String inFile) {
        m_exportFile = inFile;

        activity.runOnUiThread(() -> {
            StorageLocationDialog.show(
                activity,
               false,      // false = Export
                 0,
                new StorageLocationDialog.Callback() {

                    @Override
                    public void onLocationSelected(
                            StorageLocationDialog.StorageLocation location,
                            boolean rememberChoice) {
                        m_rememberChoice = rememberChoice;

                        switch (location) {

                            case APP_PRIVATE:
                                exportToPrivateStorage(m_exportFile);
                                break;

                            case SHARED_DOCUMENTS:
                                launchSafExport(m_exportFile);
                                break;

                            case CANCELLED:
                                break;
                        }
                    }
                });
        });

        return "OK";
    }

    private String exportToPrivateStorage( final String outFile) {
        persistRememberChoice();
        return "OK";

    }

    private String launchSafExport( final String outFile) {
        persistRememberChoice();
        return "OK";

    }

    private void persistRememberChoice() {
        SharedPreferences prefs =
                activity.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE);
        if( m_rememberChoice) {
        prefs.edit()
                .putBoolean(PREF_REMEMBER, true)
                .apply();
        } else {
            prefs.edit()
                    .putBoolean(PREF_REMEMBER, false)
                    .apply();

        }
    }

    private String getAllFilesResult;
    private String getAllFilesFilespec;

    public void traverse(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    traverse(file);
                } else {
                    if (file.getName().matches(getAllFilesFilespec)) {

                        //Log.i("OpenCPN", "traverse:File: " + file.getAbsolutePath());
                        getAllFilesResult += file.getAbsolutePath() + ";";
                    }
                }
            }
        }
    }

    public String getAllFilesWithFilespec(String dir, String filespec) {
        getAllFilesResult = "";

        int dot_pos = filespec.lastIndexOf(".");
        if ((dot_pos < filespec.length() - 1) && (dot_pos >= 0)) {
            try {
                getAllFilesFilespec = "(.*)[.]" + filespec.substring(dot_pos + 1) + "$";
                Log.i("OpenCPN", "getAllFilesWithFilespec " + getAllFilesFilespec);

                File dirt = new File(dir);
                traverse(dirt);
            } catch (Exception e) {
                Log.i("OpenCPN", "getAllFilesWithFilespec exception");
                e.printStackTrace();
            }
        }

        return getAllFilesResult;
    }



}       // class FileController

