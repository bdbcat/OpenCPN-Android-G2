package org.opencpn;

import static org.opencpn.FileController.OCPN_FILEMANAGER_REQUEST_CODE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.widget.ListPopupWindow;

import org.opencpn.opencpn.R;

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

    private float mScale = 1.0f;

    private static final float ICON_SIZE = 32;
    private static final float ICON_MARGIN_START = 4;
    private static final float ICON_MARGIN_END = 8;

    private static final float TITLE_SIZE = 18;
    private static final float TITLE_MARGIN_END = 12;

    private static final float BUTTON_SIZE = 52;
    private static final float BUTTON_PADDING = 4;
    private static final float BUTTON_MARGIN = 2;
    private int nIcons;
    private NativeBridgeManager nativeBridge;
    private JSONHelpers JsonHelpers;

    class MenuState {
        boolean trackActive;
        int followState;
        boolean routeActiveState;
        boolean routeAnnunciatorVisible;
    }
    private MenuState menuState;


    public MenuController(Activity activity, AppController app, NativeBridgeManager _nb) {
        this.activity = activity;
        this.app = app;
        this.nativeBridge = _nb;
    
        menuState = new MenuState();
        menuState.trackActive = false;
        menuState.followState = 0;
        menuState.routeActiveState = false;
        menuState.routeAnnunciatorVisible = false;
    }

    double getScale() { return mScale; }

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

    public void applyOverflowLayout(){
        if(nIcons < 3) {
            trackButton.setVisibility(View.GONE);
         } else {
            trackButton.setVisibility(View.VISIBLE);
         }
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

        overflowButton =
                toolbarOverlay.findViewById(R.id.btn_overflow);

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
        setScale(mScale);;
    }

    private void recomputeToolbarLayout() {

        int totalWidth =
                toolbarOverlay.getWidth();

        ListPopupWindow appIcon;

        ImageButton overflowButton =  toolbarOverlay.findViewById(R.id.btn_overflow);
        ImageButton toolButton =  toolbarOverlay.findViewById(R.id.btn_follow);
        TextView appTitle =  toolbarOverlay.findViewById(R.id.app_title);

        int overflowWidth =
                overflowButton.getWidth();
        int leftWidth = overflowButton.getWidth()
                + appTitle.getWidth();

        int iconWidth = toolButton.getWidth();
        int available =
                totalWidth
                        - leftWidth
                        - overflowWidth;
        nIcons =  available / iconWidth;
        Log.i("MENU","Available icons = " + nIcons);
        applyOverflowLayout();
    }

    void onFollowPressed() {
        nativeBridge.postJsonEvent(
                JsonHelpers.makeEvent(0, "menuAction", "action", "action_follow"));
    }

    void onRMDPressed() {
        nativeBridge.postJsonEvent(
                JsonHelpers.makeEvent(0, "menuAction", "action", "action_rmd"));
    }

    void onRoutePressed() {
        nativeBridge.postJsonEvent(
                JsonHelpers.makeEvent(0, "menuAction", "action", "route_create"));
    }

    void onTrackPressed() {
        //nativeLib.invokeMenuItem(OCPN_ACTION_TRACK_TOGGLE);
        nativeBridge.postJsonEvent(
                JsonHelpers.makeEvent(0, "menuAction", "action", "track_toggle"));
    }

    private void showOverflow(View anchor) {

        //TODO verify this
        if (!m_optionsItemActionEnable)          // option items may be disabled by core
            return;

         ContextThemeWrapper popupContext =
                new ContextThemeWrapper(activity, R.style.OpenCPNPopupTheme);

        PopupMenu popup =
                new PopupMenu(popupContext, anchor);

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
            case R.id.ocpn_action_track_toggle:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "track_toggle"));
                return true;

            case R.id.ocpn_action_createroute:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "route_create"));
                return true;

            case R.id.ocpn_route_create_active:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "route_finish"));
                return true;

            case R.id.ocpn_action_encText:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "enc_text_toggle"));
                return true;

            case R.id.ocpn_action_encSoundings:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "enc_soundings_toggle"));
                return true;

            case R.id.ocpn_action_encLights:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "enc_lights_toggle"));
                return true;

            case R.id.ocpn_action_tides:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "tides_toggle"));
                return true;

            case R.id.ocpn_action_currents:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "currents_toggle"));
                return true;

            case R.id.ocpn_action_mob:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "MOB_activate"));
                return true;

            case R.id.ocpn_action_settings_basic:
                nativeBridge.postJsonEvent(
                        JsonHelpers.makeEvent(0, "menuAction", "action", "Settings_basic"));
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

                // Set visibility of "track toggle" based on number of tool icons shown
                MenuItem itemTrackToggle = menu.findItem(R.id.ocpn_action_track_toggle);
                if (null != itemTrackToggle)
                    itemTrackToggle.setVisible(nIcons == 2);
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

    //  Menu scaling support
    public void setScale(float scale) {
        if (scale < 1) scale = 1;   // Clamp, no downsize allowed

        mScale = scale;
        scaleButton(followButton);
        scaleButton(rmdButton);
        scaleButton(trackButton);
        scaleButton(overflowButton);

        toolbarOverlay.post(this::recomputeToolbarLayout);
    }

    private int scaledDp(float value) {
        return Math.round(value * mScale *
                activity.getResources().getDisplayMetrics().density);
    }

    private void scaleButton(ImageButton button) {
        ViewGroup.LayoutParams raw = button.getLayoutParams();

        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) raw;

            lp.width = scaledDp(BUTTON_SIZE);
            lp.height = scaledDp(BUTTON_SIZE);
            lp.leftMargin = scaledDp(BUTTON_MARGIN);
            lp.rightMargin = scaledDp(BUTTON_MARGIN
            );
            button.setLayoutParams(lp);

            button.setPadding(
                    scaledDp(BUTTON_PADDING),
                    scaledDp(BUTTON_PADDING),
                    scaledDp(BUTTON_PADDING),
                    scaledDp(BUTTON_PADDING));

        }
    }
}