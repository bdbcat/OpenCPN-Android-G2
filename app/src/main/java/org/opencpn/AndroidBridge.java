package org.opencpn;

import static org.opencpn.MyQtActivity.sNativeBridge;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

public class AndroidBridge {
    private static HostManager hostManager;
    private static ToolbarController toolbarController;
    private static ConnectivityHelper connectivityHelper;
    private static FileController fileController;
    private static OCPNConnectivityManager connectivityManager;

    private static Activity activity;
    private static DownloadManager downloadManager;

    public static void initialize(
            HostManager hm,
            ToolbarController tc,
            ConnectivityHelper ch,
            FileController fc,
            OCPNConnectivityManager cm,
            DownloadManager dm,
            Activity act)
    {
        hostManager = hm;
        toolbarController = tc;
        connectivityHelper = ch;
        fileController = fc;
        connectivityManager = cm;
        downloadManager = dm;
        activity = act;
    }

    public static String dispatchCommand(String payload) {

        Log.i("OpenCPN", "JNI RX: " + payload);

        try {

            JSONObject obj = new JSONObject(payload);

            String cmd = obj.getString("cmd");

            switch(cmd) {

                case "PING": {

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("reply", "PONG");

                    return result.toString();
                }

                case "GET_DEVICE_INFO": {

                    String aresult = hostManager.getDeviceInfo();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "GET_SYSTEM_DIRS": {
                    String aresult = hostManager.getSystemDirs();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "NOTIFY_QT_READY": {
                    if (sNativeBridge != null) {
                        sNativeBridge.setQtReady();
                    }

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", "OK");

                    return result.toString();
                }

                case "GET_SCREEN_ORIENTATION": {
                    String aresult = hostManager.getScreenOrientation();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "GET_DISPLAY_METRICS": {
                    String aresult = hostManager.getDisplayMetrics();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "GET_LOCALE_STRING": {
                    String aresult = hostManager.getAndroidLocaleString();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "GET_VERSION_CODE": {
                    String aresult = hostManager.getAndroidVersionCode();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "GET_IPV4_ADDRESS": {
                    String aresult = connectivityHelper.getIpAddress();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "DEVICE_HAS_BLUETOOTH": {
                    String aresult = connectivityHelper.hasBluetooth(0);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);

                    return result.toString();
                }

                case "DISABLE_ROTATION": {
                    hostManager.lockActivityOrientation(activity);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", "OK");

                    return result.toString();
                }

                case "ENABLE_ROTATION": {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", "OK");

                    return result.toString();
                }

                case "SHOW_TOAST": {

                    String message = obj.optString("message", "");

                    Toast.makeText(
                            hostManager.getActivity(),
                            message,
                            Toast.LENGTH_SHORT
                    ).show();

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");

                    return result.toString();
                }

                //----------------------------------------------------------------
                // File management

                case "SECURE_COPY_FILE": {
                    String in_file = obj.optString("in_file", "");
                    String out_file = obj.optString("out_file", "");

                    int seq = obj.optInt("seq", -1);
                    if (seq < 0) {
                        JSONObject result = new JSONObject();
                        result.put("status", "ERROR");
                        result.put("message", "INVALID_SEQ");
                    }

                    String aresult = fileController.SecureFileCopy(in_file, out_file);;

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }



                case "ENSURE_TREE_ACCESS": {
                    int seq = obj.optInt("seq", -1);
                    String treeId = obj.optString("treeId", "");

                    String aresult = fileController.ensureTreeAccess(seq, treeId);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }

                case "FILE_CHOOSER": {
                    String initialDir = obj.optString("initialDir", "");
                    String title = obj.optString("title", "");
                    String suggestion = obj.optString("suggestion", "");
                    String wildcard = obj.optString("wildcard", "");


                    int seq = obj.optInt("seq", -1);
                    if (seq < 0) {
                        JSONObject result = new JSONObject();
                        result.put("status", "ERROR");
                        result.put("message", "INVALID_SEQ");
                    }

                    String aresult = fileController.fileChooserDialog(seq, initialDir, title, suggestion, wildcard);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }

                case "QUERY_GPS_SERVICE": {
                    int seq = obj.optInt("seq", -1);
                    int server_cmd = obj.optInt("query_code", -1);

                    String aresult = connectivityManager.handleGPSServerCommand(server_cmd);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }

                case "DOWNLOAD_FILE": {
                    int seq = obj.optInt("seq", -1);
                    String url = obj.optString("in_url", "");
                    String destination = obj.optString("out_file", "");

                    String aresult = downloadManager.downloadFile(url, destination);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }



                default: {

                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    return result.toString();
                }
            }
        }
        catch(Exception e) {

            Log.e("OpenCPN", "dispatchCommand exception", e);

            try {

                JSONObject result = new JSONObject();
                result.put("status", "EXCEPTION");
                result.put("message", e.toString());

                return result.toString();

            }
            catch(Exception ignored) {

                return "{\"status\":\"EXCEPTION\"}";
            }
        }
    }

    public static String dispatchCommandLegacy(String command) {
        Log.i("OpenCPN", "JNI CMD: " + command);

        try {
            switch(command) {

                case "PING":
                    return "PONG";

                case "GET_DEVICE_INFO":
                    return hostManager.getDeviceInfo();

                //case "SHOW_MENU":
                //    return ToolbarController.showMenu();

                default:
                    return "UNKNOWN_COMMAND";
            }
        }
        catch(Exception e) {
            Log.e("OpenCPN", "dispatchCommand exception", e);
            return "EXCEPTION";
        }

    }
}