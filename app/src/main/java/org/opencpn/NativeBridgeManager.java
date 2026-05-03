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


    public void postJsonEvent(String payload) {
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