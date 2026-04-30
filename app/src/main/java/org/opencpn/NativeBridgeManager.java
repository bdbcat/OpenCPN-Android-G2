package org.opencpn;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

public class NativeBridgeManager {

    enum LifecycleState {
        CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED,
        DESTROYED
    }
    private final OCPNNativeLib nativeLib;

    private boolean qtReady = false;

    private final List<Runnable> pendingCalls = new ArrayList<>();

    private LifecycleState lastState = LifecycleState.CREATED;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NativeBridgeManager() {
        nativeLib = OCPNNativeLib.getInstance();
    }


    //  Helper functions
    public static String makeEvent(String event) {

        try {
            JSONObject obj = new JSONObject();
            obj.put("event", event);
            return obj.toString();
        } catch(Exception e) {
            return "{}";
        }
    }



    public void postEvent(Runnable r) {
        runOrQueue(r);
    }

    public void postJsonEvent(String payload) {
        runOrQueue(() -> nativeLib.postJsonEvent(payload));
    }

    public static String makeEvent(
            String event,
            String key1,
            Object value1)
    {
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", event);
            obj.put(key1, value1);
            return obj.toString();
        } catch(Exception e) {
            return "{}";
        }
    }

    public static String makeEvent(
            String event,
            String key1,
            Object value1,
            String key2,
            Object value2)
        {
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", event);
            obj.put(key1, value1);
            obj.put(key2, value2);
            return obj.toString();
        } catch(Exception e) {
            return "{}";
        }
    }


    // --- Public lifecycle API ---

    public void onStart() {
        lastState = LifecycleState.STARTED;
        postJsonEvent(makeEvent("onStart"));
    }

    public void onResume() {
        lastState = LifecycleState.RESUMED;
        postJsonEvent(makeEvent("onResume"));
    }

    public void onPause() {
        lastState = LifecycleState.PAUSED;
        postJsonEvent(makeEvent("onPause"));
    }

    public void onStop() {
        lastState = LifecycleState.STOPPED;
        postJsonEvent(makeEvent("onStop"));
    }

    public void onDestroy() {
        lastState = LifecycleState.DESTROYED;
        postJsonEvent(makeEvent("onDestroy"));
    }

    // --- Core logic ---

    private void runOrQueue(Runnable r) {
        if (qtReady) {
            r.run();
        } else {
            pendingCalls.add(r);
        }
    }


    public void setQtReady() {
        mainHandler.post(() -> {
            qtReady = true;

            for (Runnable r : pendingCalls) {
                r.run();
            }
            pendingCalls.clear();

            //syncToLatestState();
        });
    }

    private void syncToLatestState() {
        switch (lastState) {
            case RESUMED:
                //nativeLib.onStart();
                nativeLib.onResume();
                break;

            case STARTED:
                nativeLib.onStart();
                break;

            case PAUSED:
                nativeLib.onPause();
                break;

            case STOPPED:
                nativeLib.onStop();
                break;

            case DESTROYED:
                nativeLib.onDestroy();
                break;

            default:
                break;
        }
    }
}