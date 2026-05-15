package org.opencpn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MigrateController {
    private final static int OCPN_SAF_DIALOG_MIGRATE_REQUEST_CODE = 7000;
    private final Activity activity;
    private final AppController app;
    public String g_migrateResult = "";
    public boolean g_migrateCancel = false;
    public boolean g_migrateActive = false;
    public boolean m_SAFmigrateChooserActive = false;
    public String m_SAFmigrateChooserString = "";
    public String g_migrateMessage = "";
    public int g_folderCount = 0;
    public Uri g_migrateSourceFolderURI = null;

    public MigrateController(Activity activity, AppController ac) {
        this.activity = activity;
        this.app = ac;
    }

    /// --------------------------------------------------------------------
    // Activity result entry point
    // --------------------------------------------------------------------

    public boolean onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (requestCode == OCPN_SAF_DIALOG_MIGRATE_REQUEST_CODE) {
            Uri treeUriDL = null;

            if (resultCode == Activity.RESULT_OK) {

                treeUriDL = data.getData();

                Log.i("OpenCPN", "onqtActivityResult OCPN_SAF_DIALOG_MIGRATE_DL_REQUEST_CODE...URI is: " + treeUriDL.toString());
/*
                File tf = new File(treeUriDL.getPath());
                final String[] split = tf.getPath().split(":");//split the path.

                if(split[0].contains("primary")) {
                    m_SAFmigrateChooserString = "file:internal Storage/" + split[split.length - 1];
                }
                else {
                    m_SAFmigrateChooserString = "file:SdCard/" + split[split.length - 1];
                }
*/

                DocumentFile source = DocumentFile.fromTreeUri(activity.getApplicationContext(), treeUriDL);
                File ff = getFile(activity.getApplicationContext(), source);
                if (ff == null){
                    m_SAFmigrateChooserActive = false;
                    m_SAFmigrateChooserString = "cancel:";
                } else {
                    m_SAFmigrateChooserString = "file:" + ff.getPath();
                    g_migrateSourceFolderURI = treeUriDL;
                    m_SAFmigrateChooserActive = false;
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                m_SAFmigrateChooserActive = false;
                m_SAFmigrateChooserString = "cancel:";
            }

            return true;
        }

        return false;
    }


    public String migrateSetup() {
        // Todo
        //app.getHostManager().DisableRotation();
        m_SAFmigrateChooserActive = true;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                intent.putExtra("android.content.extra.INITIAL_URI", "content://com.android.externalstorage.documents/root/primary");
                activity.startActivityForResult(intent, OCPN_SAF_DIALOG_MIGRATE_REQUEST_CODE);
            }
        });
/*
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                intent.putExtra("android.content.extra.INITIAL_URI", "content://com.android.externalstorage.documents/root/primary");
                activity.startActivityForResult(intent, OCPN_SAF_DIALOG_MIGRATE_REQUEST_CODE);
            }
        });
*/
        return "OK";
    }

    public String migrateFolder(String sourceFolder, String destFolder) {
        doMigrateAsync(sourceFolder, destFolder);
        return "OK";
    }

    public String cancelMigration() {
        //EnableRotation();
        g_migrateCancel = true;
        return "OK";
    }


    class MigrateFolderTask extends AsyncTask<String, String, String> {

        private List<DocumentFile> walkTreeForDirs(DocumentFile dir) {
            List<DocumentFile> fileTree = new ArrayList<DocumentFile>();
            DocumentFile[] array = dir.listFiles();


            for (int i = 0; i < array.length; i++) {
                g_migrateMessage = "Counting folders: " + Integer.toString(g_folderCount);
                g_folderCount++;

                if (array[i].isDirectory()) {
                    if (!containsDirectory(array[i]))
                        fileTree.add(array[i]);
                    else if (containsFile(array[i])) {
                        fileTree.add(array[i]);
                        fileTree.addAll(walkTreeForDirs(array[i]));
                    } else
                        fileTree.addAll(walkTreeForDirs(array[i]));
                }

                if (g_migrateCancel) {
                    fileTree.clear();
                    break;
                }
            }

            if (g_migrateCancel) {
                fileTree.clear();
            }


            return fileTree;
        }

        private boolean containsDirectory(DocumentFile dir) {
            DocumentFile[] array = dir.listFiles();

            for (int i = 0; i < array.length; i++) {
                if (array[i].isDirectory()) {
                    return true;
                }
            }
            return false;

        }

        private boolean containsFile(DocumentFile dir) {
            DocumentFile[] array = dir.listFiles();

            for (int i = 0; i < array.length; i++) {
                if (array[i].isFile()) {
                    return true;
                }
            }
            return false;

        }

        @Override
        protected String doInBackground(String... arg0) {

            //DisableRotation();

            g_migrateActive = true;
            String rv = "";

            String srcPath = arg0[0]; //"/storage/emulated/0/Charts/RNC/US_REGION02";
            DocumentFile source = DocumentFile.fromTreeUri(activity.getApplicationContext(), g_migrateSourceFolderURI);

            // Get a list of all unique folders
            List<DocumentFile> dirList = walkTreeForDirs(source);

            // If the source has no subdirs, or has files, we just want to copy itself
            if (dirList.isEmpty()) {
                dirList.add(source);
            }

            if (containsFile(source)) {
                dirList.add(source);
            }


            int iFile = 0;
            for (DocumentFile file : dirList) {
                if (g_migrateCancel) {
                    rv = "canceled";
                    break;
                }
                String exceptionError;

                if (file.isDirectory()) {

                    Log.i("OpenCPN", "Migrating: " + file.getName());

                    //Make the target directory
                    String path = file.getUri().getPath();

                    File tfile = new File(file.getUri().getPath());//create path from uri
                    final String[] split = tfile.getPath().split(":");//split the path.

                    String sdestDir = arg0[1] + "/MigratedCharts/" + split[2];
                    File destDir = new File(sdestDir);

                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }

                    // Copy all the files

                    DocumentFile[] array = file.listFiles();
                    for (int i = 0; i < array.length; i++) {

                        if (!array[i].isFile())
                            continue;

                        if (g_migrateCancel) {
                            rv = "canceled";
                            break;
                        }

                        InputStream inStream;
                        BufferedInputStream binStream;
                        try {
                            inStream = activity.getContentResolver().openInputStream(array[i].getUri());
                            binStream = new BufferedInputStream(inStream);

                            String fileName = sdestDir + "/" + array[i].getName();
                            File ffileName = new File(fileName);
                            if (ffileName.exists())
                                ffileName.delete();


                            g_migrateMessage = "Migrating " + iFile + "/" + dirList.size() + ";" + array[i].getName();
                            Log.i("OpenCPN", g_migrateMessage);

                            OutputStream outStream = new FileOutputStream(fileName);

                            byte[] buffer = new byte[32 * 1024];
                            int read;
                            while ((read = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, read);
                            }
                            inStream.close();
                            // write the output file (You have now copied the file)
                            outStream.flush();
                            outStream.close();

                        } catch (FileNotFoundException fnfe1) {
                            exceptionError = fnfe1.getMessage();
                        } catch (Exception e) {
                            exceptionError = e.getMessage();
                        }
                    }
                } else {
                    InputStream inStream;
                    BufferedInputStream binStream;
                    try {
                        inStream = activity.getContentResolver().openInputStream(file.getUri());
                        binStream = new BufferedInputStream(inStream);

                        File tfile = new File(file.getUri().getPath());//create path from uri
                        final String[] split = tfile.getPath().split(":");//split the path.

                        String sdestDir = arg0[1] + "/MigratedCharts/" + split[2];

                        String fileName = sdestDir + "/" + file.getName();
                        File ffileName = new File(fileName);
                        if (ffileName.exists())
                            ffileName.delete();

                        g_migrateMessage = "Migrating " + iFile + "/" + dirList.size() + ";" + file.getName();
                        Log.i("OpenCPN", g_migrateMessage);

                        OutputStream outStream = new FileOutputStream(fileName);

                        byte[] buffer = new byte[32 * 1024];
                        int read;
                        while ((read = inStream.read(buffer)) != -1) {
                            outStream.write(buffer, 0, read);
                        }
                        inStream.close();
                        // write the output file (You have now copied the file)
                        outStream.flush();
                        outStream.close();

                    } catch (FileNotFoundException fnfe1) {
                        exceptionError = fnfe1.getMessage();
                    } catch (Exception e) {
                        exceptionError = e.getMessage();
                    }
                }
                iFile++;
            }

            // Return the actual new folder
            return rv;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            g_migrateResult = result;
            Log.i("OpenCPN", g_migrateResult);

            g_migrateActive = false;
            g_migrateMessage = "Migration complete";
            //EnableRotation();

        }
    }


    public String doMigrateAsync(String sourceFolder, String destinationFolder) {

        // Clear the data
        g_migrateActive = false;
        g_migrateResult = "";
        g_migrateMessage = "Migration started";
        g_migrateCancel = false;


        new MigrateFolderTask().execute(sourceFolder, destinationFolder);

        return "OK";
    }

    public String isSAFChooserFinished() {
        if (!m_SAFmigrateChooserActive) {
            Log.i("OpenCPN", "m_SAFChooserActive:  returning " + m_SAFmigrateChooserString);
            return m_SAFmigrateChooserString;
        } else {
            return "no";
        }
    }


    public String getMigrateStatus() {
        return g_migrateMessage;
    }

    public String restartOCPNAfterMigrate() {
        g_migrateMessage = "Restart pending.";

        PackageManager packageManager = activity.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(activity.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);

        activity.finish();
        activity.startActivity(mainIntent);


        Log.i("OpenCPN", "System.exit");
        System.exit(0);


        return "OK";
    }

    public static File getFile(final Context context, final DocumentFile document) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }

        try {
            final List<StorageVolume> volumeList = context
                    .getSystemService(StorageManager.class)
                    .getStorageVolumes();

            if ((volumeList == null) || volumeList.isEmpty()) {
                return null;
            }

            // There must be a better way to get the document segment
            final String documentId = DocumentsContract.getDocumentId(document.getUri());
            final String documentSegment = documentId.substring(documentId.lastIndexOf(':') + 1);

            for (final StorageVolume volume : volumeList) {
                String volumePath = null;

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    final Class<?> class_StorageVolume = Class.forName("android.os.storage.StorageVolume");

                    @SuppressWarnings("JavaReflectionMemberAccess")
                    @SuppressLint("DiscouragedPrivateApi") final Method method_getPath = class_StorageVolume.getDeclaredMethod("getPath");

                    volumePath = (String) method_getPath.invoke(volume);
                } else {
                    // API 30
                    if (Build.VERSION.SDK_INT >= 30) {
                        volumePath = volume.getDirectory().getPath();
                    }
                }

                final File storageFile = new File(volumePath + File.separator + documentSegment);

                // Should improve with other checks, because there is the
                // remote possibility that a copy could exist in a different
                // volume (SD-card) under a matching path structure and even
                // same file name, (maybe a user's backup in the SD-card).
                // Checking for the volume Uuid could be an option but
                // as per the documentation the Uuid can be empty.

                final boolean isTarget = (storageFile.exists())
                        && (storageFile.lastModified() == document.lastModified())
                        && (storageFile.length() == document.length());

                if (isTarget) {
                    return storageFile;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}







