package org.opencpn;

import android.app.Activity;
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
    private MenuItem itemRouteAnnunciator;
    private MenuItem itemRouteMenuItem;
    private MenuItem itemFollowActive;
    private MenuItem itemFollowActiveBlue;
    private MenuItem itemFollowActiveGreen;
    private MenuItem itemFollowInActive;
    private MenuItem itemTrackActive;
    private MenuItem itemTrackInActive;
    private int m_nFollowState = 1;

    private ImageButton followButton;
    private ImageButton routeButton;
    private ImageButton trackButton;
    private ImageButton rmdButton;
    private ImageButton overflowButton;
    private View toolbarOverlay;

    class MenuState {
        boolean trackActive;
        int followState;
        boolean routeAnnunciatorVisible;
    }
    private MenuState menuState;


    public MenuController(QtActivity activity, OCPNNativeLib nativeLib) {
        this.activity = activity;
        this.nativeLib = nativeLib;
    
        menuState = new MenuState();
        menuState.trackActive = false;
        menuState.followState = 0;
        menuState.routeAnnunciatorVisible = false;
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

        PopupMenu popup =
                new PopupMenu(activity, anchor);

        popup.getMenuInflater().inflate(
                R.menu.popup_menu,
                popup.getMenu());

        applyMenuState(popup.getMenu());

        popup.setOnMenuItemClickListener(
                item -> handle(item));

        popup.show();
    }

    public boolean handle(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.ocpn_action_follow:
                //Log.i("DEBUGGER_TAG", "Invoke OCPN_ACTION_FOLLOW while in-active");
                nativeLib.invokeMenuItem(OCPN_ACTION_FOLLOW);
                return true;

            case R.id.ocpn_action_routemanager:
                //onRouteManagerPressed();
                return true;

            case R.id.ocpn_action_mob:
                //onMobPressed();
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


    void applyButtonState() {
        switch (menuState.followState) {
            case 0:
                followButton.setImageResource(R.drawable.follow);
                break;
            case 1:
                followButton.setImageResource(R.drawable.follow_active_green);
        }

        if (menuState.trackActive) { trackButton.setImageResource(R.drawable.trackon); }
        else { trackButton.setImageResource(R.drawable.trackoff); }

    }

    public void applyMenuState(Menu menu) {
        activity.getMenuInflater().inflate(R.menu.popup_menu, menu);

        itemRouteAnnunciator = menu.findItem(R.id.ocpn_route_create_active);
        if (null != itemRouteAnnunciator) {
            itemRouteAnnunciator.setVisible(menuState.routeAnnunciatorVisible);
        }

        itemRouteMenuItem = menu.findItem(R.id.ocpn_action_createroute);
        if (null != itemRouteMenuItem) {
            itemRouteMenuItem.setVisible(!menuState.routeAnnunciatorVisible);
        }
/*
        // No need for duplicate images
        //follow.setImageResource(R.drawable.follow_active_green);

        //ImageButton follow = menu.findItem(R.id.btn_follow);
        ImageButton follow =
                toolbarOverlay.findViewById(R.id.btn_follow);

        follow.setImageResource(
                R.drawable.follow_active_green);

        switch (menuState.followState) {
            case 0:
                follow.setImageResource(R.drawable.follow_active_green);
                itemFollowActive.setVisible(false);
                itemFollowActiveGreen.setVisible(false);
                itemFollowActiveBlue.setVisible(false);
                itemFollowInActive.setVisible(true);
           
        }

        // Auto follow icon
        itemFollowActive = menu.findItem(R.id.ocpn_action_follow_active);
        itemFollowActiveGreen = menu.findItem(R.id.ocpn_action_follow_active_green);
        itemFollowActiveBlue = menu.findItem(R.id.ocpn_action_follow_active_blue);
        itemFollowInActive = menu.findItem(R.id.ocpn_action_follow);

        if (menuState.followState == 0)  {
            itemFollowInActive.setVisible(true);
            itemFollowActive.setVisible(false);
            itemFollowActiveGreen.setVisible(false);
            itemFollowActiveBlue.setVisible(false);
        } else if (menuState.followState == 1) {
            itemFollowInActive.setVisible(false);
            itemFollowActive.setVisible(false);
            itemFollowActiveGreen.setVisible(true);
            itemFollowActiveBlue.setVisible(false);
        } else if (menuState.followState == 2) {
            itemFollowInActive.setVisible(false);
            itemFollowActive.setVisible(false);
            itemFollowActiveGreen.setVisible(false);
            itemFollowActiveBlue.setVisible(true);
        } else {
            itemFollowInActive.setVisible(false);
            itemFollowActive.setVisible(false);
            itemFollowActiveGreen.setVisible(false);
            itemFollowActiveBlue.setVisible(false);
        }
*/
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



}