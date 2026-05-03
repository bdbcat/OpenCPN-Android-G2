package org.opencpn;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


public class FileController {

    public static final String PREF_CHOOSER_WILD_CARD = "PREF_CHOOSER_WILD_CARD";



    private final MyQtActivity activity;
    //private final AndroidBridge bridge;
    public final SafManager safManager;
    private final NativeBridgeManager nativeBridge;
    private static final int BUFFER_SIZE = 1024 * 1024;

    public FileController(MyQtActivity activity, OpenCPNBridge bridge, NativeBridgeManager nB) {
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




    // SAF interface
    public String ensureTreeAccess(int seq, String treeId) {
        return safManager.ensureTreeAccess(seq, treeId);
    }

    public String fileChooserDialog(final int seq, final String initialDir, final String Title, final String Suggestion, final String wildcard) {
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

                    File ff = null;

                    //File ff = getFile(this, documentFile);

                    String path = null;
                    if (ff != null) {
                        path = ff.getAbsolutePath();
                    } else {
                        //   m_filechooserString = "cancel:";
                        //   m_FileChooserDone = true;
                        return "OK";
                    }

                    // All is well, fire off the equivalent of a "Save-as" dialog.
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.setType("*/*");       // Need this for ACTION_OPEN_DOCUMENT
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/text");
                    intent.putExtra(Intent.EXTRA_TITLE, Suggestion);
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


}       // class FileController

