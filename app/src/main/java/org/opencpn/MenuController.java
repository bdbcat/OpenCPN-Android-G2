package org.opencpn;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

import org.opencpn.opencpn.R;
import org.qtproject.qt5.android.bindings.QtActivity;

public class MenuController {
    private final Activity activity;
    private MenuItem itemRouteAnnunciator;
    private MenuItem itemRouteMenuItem;
    private MenuItem itemFollowActive;
    private MenuItem itemFollowActiveBlue;
    private MenuItem itemFollowActiveGreen;
    private MenuItem itemFollowInActive;
    private MenuItem itemTrackActive;
    private MenuItem itemTrackInActive;
    private int m_nFollowState = 1;

    public MenuController(QtActivity activity) {
        this.activity = activity;
    }

    public void buildMenu(Menu menu) {
        activity.getMenuInflater().inflate(R.menu.activity_main_actions, menu);

        itemRouteAnnunciator = menu.findItem(R.id.ocpn_route_create_active);
        if (null != itemRouteAnnunciator) {
            //itemRouteAnnunciator.setVisible(m_showRouteAnnunciator);
        }

        itemRouteMenuItem = menu.findItem(R.id.ocpn_action_createroute);
        if (null != itemRouteMenuItem) {
            //itemRouteMenuItem.setVisible(!m_showRouteAnnunciator);
        }


        // Auto follow icon
        itemFollowActive = menu.findItem(R.id.ocpn_action_follow_active);
        itemFollowActiveGreen = menu.findItem(R.id.ocpn_action_follow_active_green);
        itemFollowActiveBlue = menu.findItem(R.id.ocpn_action_follow_active_blue);
        itemFollowInActive = menu.findItem(R.id.ocpn_action_follow);

        if (m_nFollowState == 0) {
            itemFollowInActive.setVisible(true);
            itemFollowActive.setVisible(false);
            itemFollowActiveGreen.setVisible(false);
            itemFollowActiveBlue.setVisible(false);
        } else if (m_nFollowState == 1) {
            itemFollowInActive.setVisible(false);
            itemFollowActive.setVisible(false);
            itemFollowActiveGreen.setVisible(true);
            itemFollowActiveBlue.setVisible(false);
        } else if (m_nFollowState == 2) {
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

        // Track icon
        itemTrackActive = menu.findItem(R.id.ocpn_action_track_toggle_ison);
        if (null != itemTrackActive) {
            //itemTrackActive.setVisible(m_isTrackActive);
        }
        itemTrackInActive = menu.findItem(R.id.ocpn_action_track_toggle_isoff);
        if (null != itemTrackInActive) {
            //itemTrackInActive.setVisible(!m_isTrackActive);
        }


        MenuItem itemSendEmailActive = menu.findItem(R.id.ocpn_action_email);
        itemSendEmailActive.setVisible(false);
    }




    public boolean handle(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                //OCPNNativeLib.invokeMenuCommand("menu_settings");
                return true;

            case 2:
                //OCPNNativeLib.invokeMenuCommand("menu_exit");
                return true;
        }
        return false;
    }
}