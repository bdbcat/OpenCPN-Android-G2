package org.opencpn;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.app.IntentService;
import android.os.ResultReceiver;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.IOException;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.opencpn.MySSLSocketFactory;
import org.opencpn.opencpn.R;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class DownloadService extends IntentService {
    public static final int UPDATE_PROGRESS = 8344;
    public static final int DOWNLOAD_DONE = 8345;
    private static volatile DownloadService sInstance;
    private volatile boolean mCancelRequested = false;
    private URLConnection mConnection;

    public DownloadService() {
        super("DownloadService");
        Log.i("OpenCPN", "DownloadService ctor ");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        super.onDestroy();
    }


    public static void cancelCurrentDownload() {
        DownloadService service = sInstance;
        if (service != null)
            service.requestCancel();
    }

    public static void requestCancel() {
        DownloadService service = sInstance;
        if (service != null) {
            service.cancelCurrentDownload();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String urlToDownload = intent.getStringExtra("url");
        String fileDestination = intent.getStringExtra("file");
        boolean bUseDocFile = intent.getBooleanExtra("useDocFile", false);
        mCancelRequested = false;

        Uri fURI = Uri.parse(fileDestination);
        String file = "";
        try {
            file = fURI.getPath();
        } catch (Exception e) {
        }

        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        Log.i("OpenCPN", "onHandleIntent " + fileDestination + " " + file);

        OutputStream outputStream = null;

        if (bUseDocFile) {
            Log.i("OpenCPN", "onHandleIntent : using DocumentFile interface for: " + file);

            File destFile = new File(file);
            DocumentFile dfile = getDocumentFile(destFile, false, true);

            if (null == dfile)
                Log.i("OpenCPN", "onHandleIntent : dfile is NULL");


            try {
                outputStream = getContentResolver().openOutputStream(dfile.getUri());
            } catch (Exception e) {
                Log.i("OpenCPN", "onHandleIntent exceptionA: " + file);
            }


        } else {
            try {
                outputStream = new FileOutputStream(file);
            } catch (Exception e) {
                Log.i("OpenCPN", "onHandleIntent exceptionB: " + file);
            }
        }


        long total = 0;
        int fileLength = -1;
        InputStream input = null;

        if (null != outputStream) {
            try {
                URL url = new URL(urlToDownload);

                Bundle resultData0 = new Bundle();

                int progress = (int) (total * 100 / fileLength);
                resultData0.putInt("progress", 0);
                resultData0.putInt("sofar", 0);
                resultData0.putInt("filelength", 100);


                receiver.send(UPDATE_PROGRESS, resultData0);


                mConnection = url.openConnection();
                mConnection.connect();

                int code = ((HttpURLConnection) mConnection).getResponseCode();
                Log.i("OpenCPN", "response code: " + Integer.toString(code));

                if (code / 100 == 3) {
                    String loc = mConnection.getHeaderField("Location");
                    Log.i("OpenCPN", "Redirect: " + loc);

                    URL urlRedirect = new URL(loc);

                    mConnection = urlRedirect.openConnection();
                    mConnection.connect();

                    int codeRedirect = ((HttpURLConnection) mConnection).getResponseCode();
                    Log.i("OpenCPN", "response code on redirect: " + Integer.toString(codeRedirect));

                }


                // this will be useful so that you can show a typical 0-100% progress bar
                fileLength = mConnection.getContentLength();

                // download the file from stream
                input = new BufferedInputStream(mConnection.getInputStream());


                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data, 0, 1024)) != -1) {

                    if (mCancelRequested)
                        throw new InterruptedIOException("Download cancelled");

                    total += count;
                    // publishing the progress....
                    Bundle resultData = new Bundle();

                    progress = (int) (total * 100 / fileLength);
                    resultData.putInt("progress", progress);
                    resultData.putInt("sofar", (int) total);
                    resultData.putInt("filelength", fileLength);

//                Log.i("OpenCPN", "RECEIVER SEND " + total + " " + fileLength + " " + progress);
                    if (mCancelRequested)
                        throw new InterruptedIOException("Download cancelled");

                    receiver.send(UPDATE_PROGRESS, resultData);
                    outputStream.write(data, 0, count);
                }

                outputStream.flush();
                outputStream.close();
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("OpenCPN", "Download Service Exception A ", e);
            }


            Bundle resultData = new Bundle();
            resultData.putInt("progress", 100);
            resultData.putInt("sofar", (int) total);
            resultData.putInt("filelength", fileLength);
            receiver.send(DOWNLOAD_DONE, resultData);
        }
    }

    /**
        * Get a list of external SD card paths. (Kitkat or higher.)
        *
        * @return A list of external SD card paths.
        */
       private String[] getExtSdCardPaths() {
           List<String> paths = new ArrayList<String>();



               for (File file : this.getExternalFilesDirs("external")) {
                   if (file != null && !file.equals(this.getExternalFilesDir("external"))) {
                       int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                       if (index < 0) {
                           //Log.w(Application.TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                       } else {
                           String path = file.getAbsolutePath().substring(0, index);
                           try {
                               path = new File(path).getCanonicalPath();
                           } catch (IOException e) {
                               // Keep non-canonical path.
                           }
                           paths.add(path);
                       }
                   }
               }

           return paths.toArray(new String[paths.size()]);
       }


    public String getExtSdCardFolder(final File file) {
        String[] extSdPaths = getExtSdCardPaths();
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                   return extSdPaths[i];
                }
           }
        }
        catch (IOException e) {
           return null;
        }

        return null;
   }

    public DocumentFile getDocumentFile(final File file, final boolean isDirectory, boolean bCreate) {
        String baseFolder = getExtSdCardFolder(file);

        if (baseFolder == null) {
            return null;
        }

        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if(fullPath.length() > baseFolder.length())
                relativePath = fullPath.substring(baseFolder.length() + 1);
            else
            relativePath = "";
            }
        catch (IOException e) {
            return null;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sUri = preferences.getString("SDURI", "");
        if(sUri.isEmpty())
            return null;

        Uri ltreeUri = Uri.parse(sUri);

        if (ltreeUri == null) {
            return null;
        }


        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(this, ltreeUri);

        if(relativePath.isEmpty())
            return document;

        try {
            String[] parts = relativePath.split("\\/");
            for (int i = 0; i < parts.length; i++) {
                DocumentFile nextDocument = document.findFile(parts[i]);

                if (nextDocument == null) {
                    if(!bCreate)
                        return null;
                    if ((i < parts.length - 1) || isDirectory) {
                        nextDocument = document.createDirectory(parts[i]);
                    } else {
                        nextDocument = document.createFile("image", parts[i]);
                    }
                }
                document = nextDocument;
                }
            }catch(Exception e){
                int yyp = 0;
            }

        return document;
        }



}































