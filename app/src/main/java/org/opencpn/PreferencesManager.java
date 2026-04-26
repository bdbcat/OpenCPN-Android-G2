package org.opencpn;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.WindowManager;

public class PreferencesManager {

    private final Context context;

    public PreferencesManager(Context ctx) {
        this.context = ctx;
    }

    public boolean applyWindowFlags(Activity activity) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);

        boolean noSleep = prefs.getBoolean("prefb_noSleep", false);

        if (noSleep)
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return prefs.getBoolean("prefb_trackOnPause", false);
    }
}