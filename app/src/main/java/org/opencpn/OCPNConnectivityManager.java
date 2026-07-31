package org.opencpn;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import java.io.BufferedReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;


public class OCPNConnectivityManager {

    private final static int MY_PERMISSIONS_REQUEST_BT_CONNECT = 3;
    private final OCPNNativeLib nativeLib;
    private Context context;
    private Activity activity;
    private GPSServer m_GPSServer;
    public ServiceConnection GPSconnection;

    private boolean m_GPSServiceStarted;
    private boolean mGPSBound;
    private boolean m_gpsServerActionPending;
    private int m_gpsServerPendingParm = 0;
    private boolean m_gpsOn;
    private boolean m_GPSPermissionRequested;
    public boolean m_hasGPS = true;

    public UsbSerialHelper uSerialHelper;

    private WifiManager wifi = null;
    private WifiManager.MulticastLock m_multicastlock = null;

    private BTScanHelper scanHelper;
    private Boolean m_ScanHelperStarted = false;
    private BluetoothSPP m_BTSPP;
    private Boolean m_BTServiceCreated = false;
    private String m_BTStat;
    private String m_scannedSerial;
    private boolean g_postActive;
    private String g_postResult;


    public  OCPNConnectivityManager(Activity avtivity, OCPNNativeLib nl) {
        this.activity = activity;
        nativeLib = nl;
    }

    public void init(Activity activity) {
        this.activity = activity;
        wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        m_GPSServer = new GPSServer();

         GPSconnection = new ServiceConnection() {


            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                GPSServer.LocalBinder binder = (GPSServer.LocalBinder) service;
                m_GPSServer = binder.getService();
                mGPSBound = true;
                m_GPSServiceStarted = true;

                if (m_gpsServerActionPending) {
                    m_GPSServer.doService(m_gpsServerPendingParm);
                    m_gpsServerActionPending = false;
                }
            }

            public void onServiceDisconnected(ComponentName arg0) {
                mGPSBound = false;
                m_GPSServiceStarted = false;
            }
        };
    }

    public void handlePermissionResults(int requestCode, String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!m_GPSServiceStarted) {
                Log.i("OpenCPN", "Starting GPS Server in onRequestPermissionsResult");
                Intent intent = new Intent(activity, GPSServer.class);
                activity.startService(intent);
                m_gpsServerActionPending = true;
                m_gpsServerPendingParm = GPSServer.GPS_ON;
                activity.getApplicationContext().bindService(intent, GPSconnection, Context.BIND_AUTO_CREATE);
            }
        } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            Log.i("OpenCPN", "GPS permission denied in onRequestPermissionsResult");
        }
    }

    void enableMulticast() {
        try {
            if (wifi != null) {
                Log.e("OpenCPN", "Create multicastLock");

                m_multicastlock = wifi.createMulticastLock("mylock");
                m_multicastlock.setReferenceCounted(true);
                if (!m_multicastlock.isHeld()) m_multicastlock.acquire();
            }
        }
        catch(UnsupportedOperationException e ){  // maybe "Exceeded maximum number of wifi locks"
            return;
        }

        return;
    }

    public void quickStopGpsService() {

        // A short method to allow quick stopping of GPS service
        // Unbind from the GPS service
        if (mGPSBound) {
            activity.getApplicationContext().unbindService(GPSconnection);
            mGPSBound = false;
        }

        if (m_GPSServiceStarted)
            m_GPSServer.doService(GPSServer.GPS_STOPSERVICE);

    }

    public String handleGPSServerCommand(final int server_cmd) {
        Log.i("OpenCPN", "queryGPSServer");

        if (GPSServer.GPS_PROVIDER_AVAILABLE == server_cmd) {
            String ret_string = "NO";
            if (m_hasGPS)
                ret_string = "YES";

            Log.i("OpenCPN", "Available: " + ret_string);

            return ret_string;
        }

        if (server_cmd == GPSServer.GPS_ON) {
            if (Build.VERSION.SDK_INT >= 23) {
                Log.i("OpenCPN", "Checking GPS permissions");

                if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    // Permission is not already granted
                    // No explanation needed, we can request the permission.
                    if (!m_GPSPermissionRequested) {
                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MyQtActivity.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        m_GPSPermissionRequested = true;
                        Log.i("OpenCPN", "Requesting GPS permissions");

                    }

                    return "PENDING";

                    // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.

                } else {
                    // Permission has already been granted
                    Log.i("OpenCPN", "GPS permissions already granted");
                }
            }

            if (!m_GPSServiceStarted) {
                Log.i("OpenCPN", "Starting GPS Server");
                Intent intent = new Intent(activity, GPSServer.class);
                activity.startService(intent);
                m_gpsServerActionPending = true;
                m_gpsServerPendingParm = server_cmd;
                activity.getApplicationContext().bindService(intent, GPSconnection, Context.BIND_AUTO_CREATE);
            }

            m_gpsOn = true;

        }
        if (server_cmd == GPSServer.GPS_OFF) {
            m_gpsOn = false;
        }

        if (m_GPSServiceStarted) {
            Log.i("OpenCPN", "Invoking GPS Server.doService");
            return m_GPSServer.doService(server_cmd);
        } else {
            Log.i("OpenCPN", "GPS Server.doService pending...");
            return "PENDING";
        }
    }

    //--------------------------------------------------------------------------------
    //  Bluetooth support
    //--------------------------------------------------------------------------------

    public String startBTService(final String address) {
        Log.i("DEBUGGER_TAG", "startBTService");
        Log.i("DEBUGGER_TAG", address);
        m_BTStat = "Unknown";


        if (!isBluetoothPermissionOK()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN},
                        MY_PERMISSIONS_REQUEST_BT_CONNECT);
            }
            return "NOK.Pending";
        }

        if (m_BTServiceCreated && m_BTSPP.isServiceAvailable() && (m_BTSPP.getServiceState() == BluetoothState.STATE_CONNECTED)) {
            return "OK";
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

///
                if (!m_BTServiceCreated) {
//                    Log.i("DEBUGGER_TAG", "Bluetooth createBTService");
                    m_BTSPP = new BluetoothSPP(activity.getApplicationContext());

                    if (!m_BTSPP.isBluetoothAvailable() || !m_BTSPP.isBluetoothEnabled()) {
                        //                           Toast.makeText(getApplicationContext()
                        //                                           , "Bluetooth is not available"
                        //                                           , Toast.LENGTH_SHORT).show();
                    } else {
                        m_BTSPP.setupService();
                        m_BTServiceCreated = true;
                    }
                }


                m_BTSPP.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
                    public void onDataReceived(byte[] data, String message) {
//                        Log.i("DEBUGGER_TAG", message);
                        // Do something when data incoming
                        nativeLib.processBTNMEA(message);

                    }
                });

                if (m_BTSPP.isServiceAvailable()) {
//                    Log.i("DEBUGGER_TAG", "Bluetooth startService");
                    m_BTSPP.startService(BluetoothState.DEVICE_OTHER);

//                    Log.i("DEBUGGER_TAG", "Bluetooth connectA");
//                    m_BTSPP.connect(address);
                    m_BTSPP.resetAutoConnect();
                    m_BTSPP.autoConnectAddress(address);

                }

                if (!m_BTSPP.isBluetoothEnabled())
                    m_BTStat = "NOK.BTNotEnabled";
                else if (!m_BTSPP.isServiceAvailable())
                    m_BTStat = "NOK.ServiceNotAvailable";
                else
                    m_BTStat = "OK";


            }
        });


        Log.i("DEBUGGER_TAG", "startBTService return: " + m_BTStat);
        return m_BTStat;
    }


    public String stopBTService(final int parm) {
        Log.i("DEBUGGER_TAG", "stopBTService");
        String ret_str = "";

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (m_BTServiceCreated) {
                    Log.i("DEBUGGER_TAG", "Bluetooth stopService");
                    m_BTSPP.stopService();
                }


            }
        });

        ret_str = "OK";
        return ret_str;
    }

    public String sendBTMessage(final String message) {
        Log.i("DEBUGGER_TAG", "sendBTMessage: " + message);


        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (m_BTServiceCreated) {
                    m_BTSPP.send(message, false);

                    if (!m_BTSPP.isBluetoothEnabled())
                        m_BTStat = "NOK.BTNotEnabled";
                    else if (!m_BTSPP.isServiceAvailable())
                        m_BTStat = "NOK.ServiceNotAvailable";
                    else
                        m_BTStat = "OK";
                }
            }
        });


        return m_BTStat;
    }
    public boolean isBluetoothPermissionOK() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) ||
                    (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED)) {
                return false;
            }
        }
        return true;
    }

    public String startBlueToothScan(final int parm) {
        Log.i("DEBUGGER_TAG", "startBlueToothScan");

        if (!isBluetoothPermissionOK()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN},
                        MY_PERMISSIONS_REQUEST_BT_CONNECT);


            }
            return "OK";
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!m_ScanHelperStarted) {
                    scanHelper = new BTScanHelper(activity);
                    m_ScanHelperStarted = true;
                }

                scanHelper.doDiscovery();

            }
        });


        return ("OK");

    }

    public String stopBlueToothScan(final int parm) {
//        Log.i("DEBUGGER_TAG", "stopBlueToothScan");
        if (!isBluetoothPermissionOK())
            return "NOK";

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (m_ScanHelperStarted) {
                    scanHelper.doDiscovery();
                    scanHelper.stopDiscovery();
                }


            }
        });


        return ("OK");

    }

    public String getBlueToothScanResults(final int parm) {
        String ret_str = "";

        if (!isBluetoothPermissionOK())
            return ret_str;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!m_ScanHelperStarted) {
                    scanHelper = new BTScanHelper(activity);
                    m_ScanHelperStarted = true;
                }


            }
        });

        if (m_ScanHelperStarted)
            ret_str = scanHelper.getDiscoveredDevices();
        ;

//        Log.i("DEBUGGER_TAG", "results");
//        Log.i("DEBUGGER_TAG", ret_str);

        return ret_str;


    }


    //--------------------------------------------------------------------------------
    //  Serial Port support
    //--------------------------------------------------------------------------------
    public String scanSerialPorts(final int parm) {

        String ret_str = "";
        if (null != uSerialHelper) {
            ret_str = uSerialHelper.scanSerialPorts(parm);
        }

        m_scannedSerial = ret_str;
        return ret_str;
    }


    public String startSerialPort(final String name, final String baudRate) {

        String ret_str = "";
        if (null != uSerialHelper) {
            ret_str = uSerialHelper.startUSBSerialPort(name, Integer.parseInt(baudRate));
        }

        return ret_str;
    }

    public String stopSerialPort(final String name) {

        String ret_str = "";
        if (null != uSerialHelper) {
            ret_str = uSerialHelper.stopUSBSerialPort(name);
        }

        return ret_str;
    }

    public String writeSerialPort(final String name, final String message) {
        if (uSerialHelper == null)
            return "NOK";
        return uSerialHelper.writeUSBSerialPort(name, message);

    }

    //-----------------------------------------------------------------------------
    //  HTTP support
    //-----------------------------------------------------------------------------

    public String doHttpPostAsync(final String url, final String encodedData, final String timeoutMsec) {
        //Log.i("OpenCPN", "doHttpPostAsync start ");
        // Clear the data
        g_postActive = false;
        g_postResult = "";

        new PostTask().execute(url, encodedData, "20000");

        return "OK";
    }

    public String checkPostAsync() {
        if (g_postActive)
            return "ACTIVE";
        else
            return g_postResult;
    }

    class PostTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg0) {

            g_postActive = true;
            //Log.i("OpenCPN", "POST url: " + arg0[0]);
            //Log.i("OpenCPN", "POST parms: " + arg0[1]);

            OutputStreamWriter out = null;
            DataOutputStream dataout = null;
            BufferedReader in = null;
            StringBuilder result = new StringBuilder();

            int timeout = 4950;
            try {
                timeout = Integer.parseInt(arg0[2]);
            } catch (NumberFormatException nfe) {
                // Handle parse error.
                nfe.printStackTrace();
                Log.i("OpenCPN", "Timeout value parse exception");
            }

            try {
                URL url = new URL(arg0[0]);

                // On older devices, we need to explicitly enable TLS v1.2 on https connections

                if ((url.getProtocol().equalsIgnoreCase("https")) &&
                        (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22)) {

                    HttpsURLConnection surl = (HttpsURLConnection) url.openConnection();

                    // Force a TLSv1.2 connection
                    MySSLSocketFactory myFact = new MySSLSocketFactory(surl.getSSLSocketFactory());
                    surl.setSSLSocketFactory(myFact);

                    surl.setRequestMethod("POST");
                    surl.setDoOutput(true);
                    surl.setDoInput(true);
                    surl.setUseCaches(false);
                    surl.setAllowUserInteraction(false);
                    //surl.setRequestProperty(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
                    surl.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    surl.setConnectTimeout(timeout);
                    surl.setReadTimeout(timeout);

                    dataout = new DataOutputStream(surl.getOutputStream());

                    // perform POST operation
                    //Log.i("OpenCPN", "doin it...");

                    dataout.writeBytes(arg0[1]);

                    int responseCode = surl.getResponseCode();
                    Log.i("OpenCPN", "AsyncTask HTTPPOST Response code: " + Integer.toString(responseCode));

                    in = new BufferedReader(new InputStreamReader(surl.getInputStream()), 8096);
                    String response;
                    // write html to System.out for debug
                    while ((response = in.readLine()) != null) {
                        //Log.i("OpenCPN", response);
                        result.append(response);
                    }

                    in.close();
                    surl.disconnect();

                } else {
                    HttpURLConnection urlc = null;
                    urlc = (HttpURLConnection) url.openConnection();
                    urlc.setRequestMethod("POST");
                    urlc.setDoOutput(true);
                    urlc.setDoInput(true);
                    urlc.setUseCaches(false);
                    urlc.setAllowUserInteraction(false);
                    //urlc.setRequestProperty(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
                    urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlc.setConnectTimeout(timeout);
                    urlc.setReadTimeout(timeout);

                    dataout = new DataOutputStream(urlc.getOutputStream());

                    // perform POST operation
                    //Log.i("OpenCPN", "doin it...");

                    dataout.writeBytes(arg0[1]);

                    int responseCode = urlc.getResponseCode();
                    Log.i("OpenCPN", "AsyncTask HTTPPOST Response code: " + Integer.toString(responseCode));

                    in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
                    char[] buffer = new char[10000];
                    int nbCharRead = 0;
                    String response = "";
                    try {
                        while ((nbCharRead = in.read(buffer, 0, 10000)) != -1) {
                            String chunk = new String(buffer, 0, nbCharRead);
                            result.append(chunk);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("OpenCPN", "AsyncTask IOException 0", e);
                        result = new StringBuilder("NOK:0:");
                        result.append(e.getMessage());
                    } finally {
                        urlc.disconnect();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.i("OpenCPN", "AsyncTask IOException 1", e);
                result = new StringBuilder("NOK:1:");
                result.append(e.getMessage());
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("OpenCPN", "AsyncTask IOException 2", e);
                        result = new StringBuilder("NOK:2:");
                        result.append(e.getMessage());
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("OpenCPN", "Exception 3", e);
                        result = new StringBuilder("NOK:3:");
                        result.append(e.getMessage());
                    }
                }
            }

            //Log.i("OpenCPN", "async result: " + result);

            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            g_postResult = result;
            g_postActive = false;
        }
    }


}
