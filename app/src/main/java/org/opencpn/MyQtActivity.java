package org.opencpn;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import org.qtproject.qt5.android.bindings.QtActivity;
import org.opencpn.OpenCPNBridge;

public class MyQtActivity extends QtActivity {

    private OpenCPNBridge bridge;
    private Toast nack_button_toast;
    private long lastBackPressTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bridge = OpenCPNBridge.getInstance(this);
        OpenCPNBridge.init(this);

        bridge.onCreate(getIntent());

        OpenCPNBridge.onCreate();

        //  Handle various other onCreate tasks.
        //  Some may move to bridge.onCreate()
        //=============================================================

        // Setup custom back-button handler
        // This callback is only called when MyFragment is at least started
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event
                if (handleOCPNBackButton()) {       // true return signals "Exit App".
                    setEnabled(false);
                    m_inExit = true;
                    preClose();
                    finish();       // all done
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);




    }



    //  Various Utiilty functions
    //  Some may move to bridge

    boolean handleOCPNBackButton() {
        if (lastBackPressTime < System.currentTimeMillis() - 3000) {
            nack_button_toast = Toast.makeText(this, "Press back again to close OpenCPN", 3000);
            nack_button_toast.show();
            lastBackPressTime = System.currentTimeMillis();
            return false;
        } else {
            if (nack_button_toast != null)
                nack_button_toast.cancel();
            if (System.currentTimeMillis() < lastBackPressTime + 3000)
                return true;        // Requesting App exit.
        }
        return false;
    }


    void preClose(){
        //TODO
        //nativeLib.ScheduleCleanExit();
    }


}

