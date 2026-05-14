package org.opencpn;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

public class AndroidBridge {
    private HostManager hostManager;
    private ToolbarController toolbarController;
    private ConnectivityHelper connectivityHelper;
    private FileController fileController;
    private OCPNConnectivityManager connectivityManager;

    private Activity activity;
    private DownloadManager downloadManager;
    private MenuController menuController;
    private AppController appController;
    private NativeBridgeManager nativeBridge;

    public AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    public void initialize(
            HostManager hm,
            MenuController mc,
            ConnectivityHelper ch,
            FileController fc,
            OCPNConnectivityManager cm,
            DownloadManager dm,
            AppController ac,
            NativeBridgeManager nb )
    {
        hostManager = hm;
        menuController = mc;
        connectivityHelper = ch;
        fileController = fc;
        connectivityManager = cm;
        downloadManager = dm;
        appController = ac;
        nativeBridge = nb;
    }

    public String dispatchCommand(String payload) {

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
                    if (nativeBridge != null) {
                        nativeBridge.setQtReady();
                    }

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", "OK");

                    return result.toString();
                }

                case "FINISH_APP": {
                    appController.finishApp();

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

                case "GET_VERSION_NAME": {
                    String aresult = hostManager.getAndroidVersionName();

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

 //                public String privateDirChooserDialog(final int seq, final String initialDir, final String Title, final int addFile) {

                case "PRIVATE_DIR_CHOOSER_ASYNC": {
                    String initialDir = obj.optString("initialDir", "");
                    String title = obj.optString("title", "");
                    String addFile = obj.optString("addFile", "");

                    int seq = obj.optInt("seq", -1);
                    if (seq < 0) {
                        JSONObject result = new JSONObject();
                        result.put("status", "ERROR");
                        result.put("message", "INVALID_SEQ");
                    }

                    String aresult = fileController.privateDirChooserDialogAsync(seq, initialDir, title, addFile);

                    String payload_t = aresult;
                    if (aresult.startsWith("cancel"))
                        payload_t = "";
                    if (aresult.startsWith("exception"))
                        payload_t = "";


                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", payload_t);
                    return result.toString();
                }

                case "PRIVATE_FILE_CHOOSER_ASYNC": {
                    String initialDir = obj.optString("initialDir", "");
                    String title = obj.optString("title", "");
                    String addFile = obj.optString("addFile", "");

                    int seq = obj.optInt("seq", -1);
                    if (seq < 0) {
                        JSONObject result = new JSONObject();
                        result.put("status", "ERROR");
                        result.put("message", "INVALID_SEQ");
                    }

                    String aresult = fileController.privateFileChooserDialogAsync(seq, initialDir, title, addFile);

                    String payload_t = aresult;
                    if (aresult.startsWith("cancel"))
                        payload_t = "";
                    if (aresult.startsWith("exception"))
                        payload_t = "";


                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", payload_t);
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

                case "SET_FOLLOW_ICON_STATE": {
                    int seq = obj.optInt("seq", -1);
                    int icon_state = obj.optInt("icon_state", 0);

                    String aresult = menuController.setFollowIconState(icon_state);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }

                case "SET_TRACKING_ICON_STATE": {
                    int seq = obj.optInt("seq", -1);
                    int icon_state = obj.optInt("icon_state", 0);

                    String aresult = menuController.setTrackingIconState(icon_state);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }


                case "PLAY_SOUND": {
                    int seq = obj.optInt("seq", -1);
                    int icon_state = obj.optInt("icon_state", 0);
                    String fileName = obj.optString("filename", "");
                    String sound = obj.optString("sound", "");

                    String jResult =  hostManager.playSound(fileName, sound);
                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }


                case "GET_FULL_SCREEN": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "IS_HELP_AVAIL": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "LAUNCH_HELPBOOK": {
                    //fileController.exportFile("myfile");

                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "CANCEL_TIMED_TOAST": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "GET_TZ_OFFSET": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

               case "IS_NET_AVAIL": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "GET_MAPI_LICENSE": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "CANCEL_MIGRATION": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "MIGRATE_SETUP": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "GET_MIGRATE_STATUS": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "RESTART_AFTER_MIGRATE": {
                    JSONObject result = new JSONObject();
                    result.put("status", "ERROR");
                    result.put("message", "UNKNOWN_COMMAND");

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }


                    case "VALIDATE_WRITE_LOCATION": {
                    String destination = obj.optString("destination", "");

                    String aresult = downloadManager.validateWriteLocation(destination, "", 0,0);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", aresult);
                    return result.toString();
                }


                case "SET_TRACK_CONTINUOUS": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = hostManager.setTrackContinuous(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }


                case "SET_FULL_SCREEN": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = hostManager.setFullscreen(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }

                // TODO move this to menu controller
                case "SET_ROUTE_ANNUNCIATOR": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = hostManager.setRouteAnnunciator(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }


                case "ENABLE_BACK_BUTTON": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = appController.setBackButtonState(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }


                case "LAST_CALL_INIT": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = hostManager.lastCallOnInit(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }

                case "START_BT_SCAN": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = "";//startBlueToothScan(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "STOP_BT_SCAN": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = "";//stopBlueToothScan(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }


                case "GET_BT_SCAN_RESULT": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = "";//getBlueToothScanResults(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "stopBTService": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = "";//stopBTService(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }


                case "SCAN_SERIAL_PORTS": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = "";//scanSerialPorts(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }


                case "GET_DOWNLOAD_STATUS": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = downloadManager.getDownloadStatus(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }


                case "CANCEL_DOWNLOAD": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = downloadManager.cancelDownload(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }


                case "ENABLE_OPTIONS_MENU": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = menuController.enableOptionsMenu(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }


                case "ENABLE_ACTIONBAR": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = menuController.enableOptionItemAction(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    return result.toString();
                }


                case "DO_COLOR_PICKER": {
                    int val = obj.optInt("parameter", 0);
                    String jResult = ""; //doColorPickerDialog(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);

                    Log.i("OpenCPN","^^^^^^UNIMPLEMENTED: " + cmd);
                    return result.toString();
                }

                case "LAUNCH_WEB_VIEW": {
                    String val = obj.optString("sparameter", "");
                    String jResult = hostManager.launchWebView(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }

                case "SHOW_TOAST": {
                    String val = obj.optString("sparameter", "");
                    String jResult = hostManager.showToast(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }

                case "START_BT_SERVICE": {
                    String val = obj.optString("sparameter", "");
                    String jResult = connectivityManager.startBTService(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }

                case "SEND_BT_MESSAGE": {
                    String val = obj.optString("sparameter", "");
                    String jResult = connectivityManager.sendBTMessage(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }

                case "STOP_SERIAL_PORT": {
                    String val = obj.optString("sparameter", "");
                    String jResult = connectivityManager.stopSerialPort(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }

                case "GET_UNZIP_STATUS": {
                    String val = obj.optString("sparameter", "");
                    String jResult = downloadManager.getUnzipStatus(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
                    return result.toString();
                }

                case "REMOVE_SYSTEM_FILE": {
                    String val = obj.optString("sparameter", "");
                    String jResult = fileController.RemoveSystemFile(val);

                    JSONObject result = new JSONObject();
                    result.put("status", "OK");
                    result.put("payload", jResult);
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

    public String dispatchCommandLegacy(String command) {
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