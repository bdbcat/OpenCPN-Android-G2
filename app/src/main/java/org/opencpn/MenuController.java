package org.opencpn;

import static org.opencpn.FileController.OCPN_FILEMANAGER_REQUEST_CODE;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.widget.ListPopupWindow;

import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.bindings.QtActivity;

import java.io.File;

import bms.ocpnfilemanager.activity.FileManagerActivity;

public class MenuController {
    private final static int OCPN_ACTION_FOLLOW = 0x1000;
    private final static int OCPN_ACTION_ROUTE = 0x1001;
    private final static int OCPN_ACTION_RMD = 0x1002;
    private final static int OCPN_ACTION_SETTINGS_BASIC = 0x1003;
    private final static int OCPN_ACTION_SETTINGS_EXPERT = 0x1004;
    private final static int OCPN_ACTION_TRACK_TOGGLE = 0x1005;
    private final static int OCPN_ACTION_MOB = 0x1006;
    private final static int OCPN_ACTION_TIDES_TOGGLE = 0x1007;
    private final static int OCPN_ACTION_CURRENTS_TOGGLE = 0x1008;
    private final static int OCPN_ACTION_ENCTEXT_TOGGLE = 0x1009;
    private final static int OCPN_ACTION_TRACK_ON = 0x100a;
    private final static int OCPN_ACTION_TRACK_OFF = 0x100b;
    private final static int OCPN_ACTION_ENCSOUNDINGS_TOGGLE = 0x100c;
    private final static int OCPN_ACTION_ENCLIGHTS_TOGGLE = 0x100d;

    private final Activity activity;
    private final OCPNNativeLib nativeLib;
    private final AppController app;
    private MenuItem itemRouteAnnunciator;
    private MenuItem itemRouteMenuItem;
    private MenuItem itemFollowActive;
    private MenuItem itemFollowActiveBlue;
    private MenuItem itemFollowActiveGreen;
    private MenuItem itemFollowInActive;

    private MenuItem itemRouteCreate;
    private MenuItem itemRouteCreateActive;
    private MenuItem itemTrackActive;
    private MenuItem itemTrackInActive;
    private int m_nFollowState = 1;

    private ImageButton followButton;
    private ImageButton routeButton;
    private ImageButton trackButton;
    private ImageButton rmdButton;
    private ImageButton overflowButton;
    private View toolbarOverlay;
    private boolean optionsMenuEnabled;
    private boolean m_optionsItemActionEnable;
    private Menu m_menu;

    class MenuState {
        boolean trackActive;
        int followState;
        boolean routeActiveState;
        boolean routeAnnunciatorVisible;
    }
    private MenuState menuState;


    public MenuController(Activity activity, OCPNNativeLib nativeLib, AppController app) {
        this.activity = activity;
        this.nativeLib = nativeLib;
        this.app = app;
    
        menuState = new MenuState();
        menuState.trackActive = false;
        menuState.followState = 0;
        menuState.routeActiveState = false;
        menuState.routeAnnunciatorVisible = false;
    }

    public String enableOptionsMenu(int bEnable) {
        Log.i("OpenCPN", "enableOptionsMenu " + bEnable);
        if (bEnable == 1)
            optionsMenuEnabled = true;
        else
            optionsMenuEnabled = false;

        // Can only mess with the UI view if this method is executed from the main UI thread.
        //  Otherwise, provokes a "CalledFromWrongThreadException"
        //  But, for some reason, this code don't work...
/*
        if(Looper.getMainLooper().getThread() == Thread.currentThread())
            invalidateOptionsMenu();
*/
        // So, do this:
        activity.runOnUiThread(new Runnable() {
            public void run() {
                activity.invalidateOptionsMenu();
            }
        });
        return "OK";
    }

    public String enableOptionItemAction(final int enable) {
        Log.i("OpenCPN", "enableOptionItemAction" + enable);
        m_optionsItemActionEnable = (enable != 0);

        return "OK";
    }

    public void applyOverflowLayout(int nIxons){

    }

    public void attachToolbar(View toolbarOverlay) {
        this.toolbarOverlay = toolbarOverlay;

        followButton =
                toolbarOverlay.findViewById(R.id.btn_follow);

        rmdButton =
                toolbarOverlay.findViewById(R.id.btn_rmd);

        routeButton =
                toolbarOverlay.findViewById(R.id.btn_route);

        trackButton =
                toolbarOverlay.findViewById(R.id.btn_track);

        //overflowButton =
        //        toolbarOverlay.findViewById(R.id.btn_overflow);

        ImageButton overflow = toolbarOverlay.findViewById(R.id.btn_overflow);
        overflow.setOnClickListener(v -> showOverflow(v));

        ImageButton follow = toolbarOverlay.findViewById(R.id.btn_follow);
        follow.setOnClickListener(v -> onFollowPressed());

        ImageButton track = toolbarOverlay.findViewById(R.id.btn_track);
        track.setOnClickListener(v -> onTrackPressed());

        ImageButton route =  toolbarOverlay.findViewById(R.id.btn_route);
        route.setOnClickListener(v -> onRoutePressed());

        ImageButton rmd =  toolbarOverlay.findViewById(R.id.btn_rmd);
        rmd.setOnClickListener(v -> onRMDPressed());

        toolbarOverlay.post(this::recomputeToolbarLayout);
    }

    private void recomputeToolbarLayout() {

        int totalWidth =
                toolbarOverlay.getWidth();

        ListPopupWindow appIcon;

        ImageButton overflowButton =  toolbarOverlay.findViewById(R.id.btn_overflow);
        TextView appTitle =  toolbarOverlay.findViewById(R.id.app_title);

        int overflowWidth =
                overflowButton.getWidth();
        int leftWidth = overflowButton.getWidth()
                + appTitle.getWidth();

        int iconWidth = overflowWidth;
        int available =
                totalWidth
                        - leftWidth
                        - overflowWidth;
        int nIcons =
                available / iconWidth;
        Log.i("MENU",
                "Available icons = " + nIcons);
        applyOverflowLayout(nIcons);
    }

    void onFollowPressed() {
        nativeLib.invokeMenuItem(OCPN_ACTION_FOLLOW);
    }

    void onRMDPressed() {
        nativeLib.invokeMenuItem(OCPN_ACTION_RMD);
    }

    void onRoutePressed() {
        nativeLib.invokeMenuItem(OCPN_ACTION_ROUTE);
    }

    void onTrackPressed() {
        nativeLib.invokeMenuItem(OCPN_ACTION_TRACK_TOGGLE);
    }

    private void showOverflow(View anchor) {

        //TODO verify this
        if (!m_optionsItemActionEnable)          // option items may be disabled by core
            return;

        PopupMenu popup =
                new PopupMenu(activity, anchor);

        popup.getMenuInflater().inflate(
                R.menu.popup_menu,
                popup.getMenu());

        m_menu = popup.getMenu();
        applyMenuState(m_menu);

        popup.setOnMenuItemClickListener(
                item -> handle(item));

        popup.show();
    }

    public boolean handle(MenuItem item) {

        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.ocpn_action_createroute:
                nativeLib.invokeMenuItem(OCPN_ACTION_ROUTE);
                return true;

            case R.id.ocpn_route_create_active:             // exiting Route Create mode
                nativeLib.invokeMenuItem(OCPN_ACTION_ROUTE);
                return true;

            case R.id.ocpn_action_encText:
                nativeLib.invokeMenuItem(OCPN_ACTION_ENCTEXT_TOGGLE);
                return true;

            case R.id.ocpn_action_encSoundings:
                nativeLib.invokeMenuItem(OCPN_ACTION_ENCSOUNDINGS_TOGGLE);
                return true;

            case R.id.ocpn_action_encLights:
                nativeLib.invokeMenuItem(OCPN_ACTION_ENCLIGHTS_TOGGLE);
                return true;

            case R.id.ocpn_action_tides:
                nativeLib.invokeMenuItem(OCPN_ACTION_TIDES_TOGGLE);
                return true;

            case R.id.ocpn_action_currents:
                nativeLib.invokeMenuItem(OCPN_ACTION_CURRENTS_TOGGLE);
                return true;

            case R.id.ocpn_action_mob:
                nativeLib.invokeMenuItem(OCPN_ACTION_MOB);
                return true;

            case R.id.ocpn_action_googlemaps:
                app.getHostEnvironment().invokeGoogleMaps();
                return true;

            case R.id.ocpn_action_filemanager:
                File dir = activity.getExternalFilesDir(null);
                Uri startUri = Uri.fromFile(dir);
                Intent intent = new Intent("OPEN", startUri, activity, FileManagerActivity.class);
                activity.startActivityForResult(intent, OCPN_FILEMANAGER_REQUEST_CODE);
                return true;

            case R.id.ocpn_action_toggle_fullscreen:
                app.getHostEnvironment().toggleFullscreen();
                return true;

            case R.id.ocpn_action_settings_basic:
                nativeLib.invokeMenuItem(OCPN_ACTION_SETTINGS_BASIC);
                return true;

            default:
                return false;
        }
    }
    
    public String setFollowIconState(int state) {
        menuState.followState = state;
        applyButtonState();
        return "OK";
    }

    public String setTrackingIconState(int state) {
        menuState.trackActive = (state == 1);
        applyButtonState();
        return "OK";
    }

    public String setRouteActive(boolean active) {
        menuState.routeActiveState = active;
        applyMenuState(m_menu);
        return "OK";
    }

    void applyButtonState() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (menuState.followState) {
                    case 0:
                        followButton.setImageResource(R.drawable.follow);
                        break;
                    case 1:
                        followButton.setImageResource(R.drawable.follow_active_green);
                        break;
                }

                if (menuState.trackActive) { trackButton.setImageResource(R.drawable.trackon); }
                else { trackButton.setImageResource(R.drawable.trackoff); }
                // Route Button not visible
                setToolbarButtonVisible(R.id.btn_route, false);

            }
        });
    }

    public void applyMenuState(Menu menu) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Route item
                itemRouteCreate = menu.findItem(R.id.ocpn_action_createroute);
                if (null != itemRouteCreate) {
                    itemRouteCreate.setVisible(!menuState.routeActiveState);
                }
                itemRouteCreateActive = menu.findItem(R.id.ocpn_route_create_active);
                if (null != itemRouteCreateActive) {
                    itemRouteCreateActive.setVisible(menuState.routeActiveState);
                }

                // Track icon
                itemTrackActive = menu.findItem(R.id.ocpn_action_track_toggle_ison);
                if (null != itemTrackActive) {
                    itemTrackActive.setVisible(menuState.trackActive);
                }
                itemTrackInActive = menu.findItem(R.id.ocpn_action_track_toggle_isoff);
                if (null != itemTrackInActive) {
                    itemTrackInActive.setVisible(!menuState.trackActive);
                }

                MenuItem itemSendEmailActive = menu.findItem(R.id.ocpn_action_email);
                itemSendEmailActive.setVisible(false);
            }
        });

    }

    private void setToolbarButtonVisible(int id, boolean visible)
    {

        View v =  app.getToolbarOverlay().findViewById(id);

        if (v != null) {
            v.setVisibility(
                    visible ? View.VISIBLE : View.GONE);
        }
    }

}