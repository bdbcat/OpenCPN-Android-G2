package org.opencpn;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class DownloadManager implements OCPNResultReceiver.Receiver {
    private Activity activity;
    public OCPNResultReceiver mReceiver;
    public OCPNNativeLib nativeLib;

    private int m_downloadTotal;
    private int m_downloadFilelength;
    private int m_downloadStatus;
    private String m_downloadURL;

    public void init( Activity act, OCPNNativeLib nl )  {
      this.activity = act;
      nativeLib = nl;
      mReceiver = new OCPNResultReceiver(new Handler());
      mReceiver.setReceiver(this);
    }

    // -----------------------------------------------------------------------
    // Utility methods


    public void onReceiveResult(int resultCode, Bundle resultData) {
        //super.onReceiveResult(resultCode, resultData);
        if (resultCode == DownloadService.UPDATE_PROGRESS) {

            int progress = resultData.getInt("progress");

            m_downloadTotal = resultData.getInt("sofar");
            m_downloadFilelength = resultData.getInt("filelength");
            m_downloadStatus = 2;           // STATUS_RUNNING

//            Log.i("OpenCPN", "UPDATE_PROGRESS " + m_downloadTotal + " " + m_downloadFilelength + " " + progress);

            nativeLib.setDownloadStatus(m_downloadStatus, m_downloadURL);

        }
        if (resultCode == DownloadService.DOWNLOAD_DONE) {
            m_downloadTotal = resultData.getInt("sofar");
            m_downloadFilelength = resultData.getInt("filelength");
            m_downloadStatus = 8;           // STATUS_SUCCESSFUL
            if (0 == m_downloadTotal)
                m_downloadStatus = 16;      // STATUS_ERROR

            nativeLib.setDownloadStatus(m_downloadStatus, m_downloadURL);
            Log.i("OpenCPN", "DOWNLOAD_DONE " + m_downloadStatus + " " + m_downloadTotal + " " + m_downloadTotal + " " + m_downloadFilelength);
        }
    }



    public String validateWriteLocation(final String destination, final String dummy, final int chainCode, final int dummyInt) {

        Uri fURI = Uri.parse(destination);
        String destPath = "";
        try {
            destPath = fURI.getPath();
        } catch (Exception e) {
        }

        Log.i("OpenCPN", "validateWriteLocation parsed destination: " + destPath);

        // We validate write access to the destination directory
        File dest = new File(destPath);
        File destDir = dest.getParentFile();

        DocumentFile dir = null;

 /*
        //  Is destination on an SDCard?
        String sdRoot = "";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {


            sdRoot = activity.getExtSdCardFolder(dest);
            if (null != sdRoot) {
                if (null != sdRoot) {
                    if (Build.VERSION.SDK_INT >= 30) {      // Android 11 does not need permission to write on private dir
                        File xfiles[] = getExternalFilesDirs(null);
                        if (xfiles.length > 1) {
                            String sdHome = "";
                            try {
                                sdHome = xfiles[1].getCanonicalPath();
                            } catch (Exception e) {
                            }

                            if (destPath.startsWith(sdHome)) {
                                return "OK";
                            }
                        }
                    }

                    dir = getDocumentFile(destDir, true, false);

                    if (null == dir) {
                        Log.i("OpenCPN", "validateWriteLocation Needs to startSAF OCPN_SAF_DIALOG_DL_REQUEST_CODE");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startSAFDialog(OCPN_SAF_DIALOG_DL_REQUEST_CODE);
                            }
                        });

                        return "Pending";

                    }
                }
            }
        }
*/
        return "OK";
    }


    public String downloadFile(final String url, final String destination) {
        Log.i("OpenCPN", "downloadFile " + url + " to " + destination);

        m_downloadURL = url;
        m_downloadStatus = 1;       //STATUS_PENDING
        m_downloadTotal = 0;

        Uri fURI = Uri.parse(destination);
        String destPath = "";
        try {
            destPath = fURI.getPath();
        } catch (Exception e) {
        }

        Log.i("OpenCPN", "downloadFile parsed destination: " + destPath);

        // We validate write access to the destination directory
        File dest = new File("/storage/emulated/0/Android/data/org.opencpn.opencpn/cache/outfile"); //destPath);
        File destDir = dest.getParentFile();

        DocumentFile dir = null;
        boolean buseDocFile = false;

/*
        //  Is destination on an SDCard?
        String sdRoot = "";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {

            sdRoot = getExtSdCardFolder(dest);
            if (null != sdRoot) {
                Log.i("OpenCPN", "downloadFile destination on SDCard");

                if (Build.VERSION.SDK_INT >= 30) {      // Android 11 does not need permission to write on private dir
                    File xfiles[] = getExternalFilesDirs(null);
                    if (xfiles.length > 1) {
                        String sdHome = "";
                        try {
                            sdHome = xfiles[1].getCanonicalPath();
                        } catch (Exception e) {
                        }

                        if (destPath.startsWith(sdHome))
                            buseDocFile = false;
                    }
                } else {
                    buseDocFile = true;
                }
            }
        }
*/
        Log.i("OpenCPN", "downloadFile writeable OK, starting service intent...");

        Intent intent = new Intent(activity, DownloadService.class);
        intent.putExtra("url", url);
        intent.putExtra("file", destination);
        intent.putExtra("receiver", mReceiver);
        intent.putExtra("useDocFile", buseDocFile);

        activity.startService(intent);

        //Log.i("OpenCPN", "RET OK");
        return "OK;77";
    }


    public String getDownloadStatus(final int ID) {

        String ret = "NOSTAT";

        String sstat = String.valueOf(m_downloadStatus);
        ret = sstat + ";" + m_downloadTotal + ";" + m_downloadFilelength;

        Log.i("OpenCPN", "getDownloadStatus " + String.valueOf(ID) + " " + ret);
        return ret;
    }





}
