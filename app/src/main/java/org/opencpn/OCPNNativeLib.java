package org.opencpn;

public class OCPNNativeLib {

    private static OCPNNativeLib ourInstance = new OCPNNativeLib();

    public static OCPNNativeLib getInstance() {
        return ourInstance;
    }

    private OCPNNativeLib() {
    }

  public native int ScheduleCleanExit();
  public native void postJsonEvent(String payload);
  public native String SyncQuery(String payload);

}
