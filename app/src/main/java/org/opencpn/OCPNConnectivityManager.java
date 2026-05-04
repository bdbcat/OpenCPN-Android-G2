package org.opencpn;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;


public class OCPNConnectivityManager {

    private Context context;
    private MyQtActivity activity;
    private GPSServer m_GPSServer;
    public ServiceConnection GPSconnection;

    private boolean m_GPSServiceStarted;
    private boolean mGPSBound;
    private boolean m_gpsServerActionPending;
    private int m_gpsServerPendingParm = 0;
    private boolean m_gpsOn;
    private boolean m_GPSPermissionRequested;


    public  OCPNConnectivityManager(Activity avtivity) {
        this.activity = activity;
    }

    public void init(MyQtActivity activity) {
        this.activity = activity;

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

    public String handleGPSServerCommand(final int server_cmd) {
        Log.i("OpenCPN", "queryGPSServer");

        if (GPSServer.GPS_PROVIDER_AVAILABLE == server_cmd) {
            String ret_string = "NO";
            if (activity.m_hasGPS)
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

}
