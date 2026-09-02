package org.opencpn;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

public class NativeBridgeManager {

    private static NativeBridgeManager ourInstance = new NativeBridgeManager();

    public static NativeBridgeManager getInstance() {
        return ourInstance;
    }

    enum LifecycleState {
        CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED,
        DESTROYED
    }
    private static OCPNNativeLib nativeLib;

    private static boolean qtReady = false;

    private static final List<Runnable> pendingCalls = new ArrayList<>();

    private LifecycleState lastState = LifecycleState.CREATED;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NativeBridgeManager() {
        nativeLib = OCPNNativeLib.getInstance();
    }


    public static void postJsonEvent(String payload) {
        runOrQueue(() -> nativeLib.postJsonEvent(payload));
    }




    // ----------------------------------------------------------------------------------
    // --- Public lifecycle API ---

    public void onStart() {
        lastState = LifecycleState.STARTED;
        postJsonEvent(JSONHelpers.makeEvent(0,"onStart"));
    }

    public void onResume() {
        lastState = LifecycleState.RESUMED;
        postJsonEvent(JSONHelpers.makeEvent(0,"onResume"));
    }

    public void onPause() {
        lastState = LifecycleState.PAUSED;
        postJsonEvent(JSONHelpers.makeEvent(0,"onPause"));
    }

    public void onStop() {
        lastState = LifecycleState.STOPPED;
        postJsonEvent(JSONHelpers.makeEvent(0, "onStop"));
    }

    public void onDestroy() {
        lastState = LifecycleState.DESTROYED;
        postJsonEvent(JSONHelpers.makeEvent(0,"onDestroy"));
    }

    // --- Core logic ---

    private static void runOrQueue(Runnable r) {
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
                postJsonEvent(JSONHelpers.makeEvent(0,"onResume"));
                break;

            case STARTED:
                postJsonEvent(JSONHelpers.makeEvent(0,"onStart"));
                break;

            case PAUSED:
                postJsonEvent(JSONHelpers.makeEvent(0,"onPause"));
                break;

            case STOPPED:
                postJsonEvent(JSONHelpers.makeEvent(0, "onStop"));
                break;

            case DESTROYED:
                postJsonEvent(JSONHelpers.makeEvent(0,"onDestroy"));
                break;

            default:
                break;
        }
    }
}