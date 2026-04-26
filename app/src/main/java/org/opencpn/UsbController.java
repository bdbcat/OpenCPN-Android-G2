package org.opencpn;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class UsbController {

    private UsbSerialHelper helper;

    public void initIfAvailable(Context ctx) {
        PackageManager pm = ctx.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            helper = new UsbSerialHelper();
            Log.i("OpenCPN", "USB serial initialized");

            // TODO initial scan of serial ports
        }
    }

    public void stop(Context ctx) {
        if (helper != null) {
            helper.deinitUSBSerial(ctx);

            // TODO ensure clean detach of devices
        }
    }
}