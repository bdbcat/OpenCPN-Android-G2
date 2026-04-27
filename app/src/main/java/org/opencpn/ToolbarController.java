package org.opencpn;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.util.TypedValue;

import androidx.appcompat.widget.Toolbar;

import org.opencpn.opencpn.R;

public class ToolbarController {

    private final Activity activity;
    private Toolbar toolbar;
    private final MenuController menuController;
    private boolean initialized = false;

    public ToolbarController(Activity activity, MenuController menuController) {
        this.activity = activity;
        this.menuController = menuController;
    }

    public void init() {
        if (initialized) return;
        initialized = true;

        FrameLayout root = (FrameLayout) activity.getWindow().getDecorView();
        int statusBarHeight = getStatusBarHeight();

        toolbar = new Toolbar(activity);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48),
                Gravity.TOP
        );

        //params.topMargin = statusBarHeight;

        toolbar.setLayoutParams(params);
        toolbar.setBackgroundColor(0xAA222222);   // semi-transparent
        toolbar.setBackgroundColor(0xFFFF0000);

        toolbar.setTitle("OpenCPN"); // optional

        root.addView(toolbar);
        toolbar.bringToFront();
        toolbar.setZ(1000f);
        toolbar.setElevation(20f);

        toolbar.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset;

            if (android.os.Build.VERSION.SDK_INT >= 30) {
                topInset = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
            } else {
                topInset = insets.getSystemWindowInsetTop();
            }

            FrameLayout.LayoutParams flparams = (FrameLayout.LayoutParams) v.getLayoutParams();
            flparams.topMargin = topInset;
            v.setLayoutParams(flparams);

            return insets;
        });

        setupMenu();

        Log.i("TOOLBAR", "Toolbar height: " + toolbar.getHeight());

        toolbar.post(() -> {
            Log.i("TOOLBAR", "Height after layout: " + toolbar.getHeight());

            //ViewGroup root = (ViewGroup) getWindow().getDecorView();

            android.widget.TextView probe = new android.widget.TextView(activity);
            probe.setText("Z-PROBE");
            probe.setTextSize(20f);
            probe.setBackgroundColor(0xCCFF0000); // red
            probe.setTextColor(0xFFFFFFFF);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(80),
                    android.view.Gravity.TOP
            );

            probe.setLayoutParams(lp);
            //root.addView(probe);

// Try everything that should bring it on top
            probe.bringToFront();
            probe.setZ(1000f);
            probe.setElevation(50f);
        });
    }

    private int getStatusBarHeight() {
        int resourceId = activity.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0) {
            return activity.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    private void setupMenu() {
        toolbar.inflateMenu(R.menu.main_menu);
        int s =  toolbar.getMenu().size();

        Log.i("MENU", "Menu size: " + toolbar.getMenu().size());

        toolbar.setOnMenuItemClickListener(item ->
                menuController.handle(item)
        );
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                activity.getResources().getDisplayMetrics()
        );
    }
}
