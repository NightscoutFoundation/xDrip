package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import static com.eveningoutpost.dexdrip.models.JoH.isLANConnected;

/**
 * jamorham
 *
 * Manage whether the user is on their Home wifi network
 */

public class HomeWifi {

    private static final String PREF = "home_wifi_preference";

    // check if connected to the specified wifi ssid
    public static boolean isConnected() {
        return isSet() && getSSID().equals(getSetting());
        // TODO do we want to support fuzzy matching?
    }

    // true if anything has been set
    public static boolean isSet() {
        return getSetting().length() > 0;
    }

    // ui methods

    public static void ask(final Activity activity) {

        if (isConnectedToAnything()) {
            final String ssid = getSSID();
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.set_home_network));
            builder.setMessage(activity.getString(R.string.my_home_network_is, ssid));

            builder.setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                   Pref.setString(PREF,ssid);
                   JoH.static_toast_long(activity.getString(R.string.set_home_network_to, ssid));
                }
            });
            builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            builder.create().show();
        } else {
            JoH.static_toast_long(activity.getString(R.string.not_connected_to_wifi));
        }
    }


    // private methods

    // get the setting
    private static String getSetting() {
        return Pref.getStringDefaultBlank(PREF);
    }

    // safely get the connected Wifi SSID
    private static String getSSID() {
        final String ssid = JoH.getWifiSSID();
        return ssid != null ? ssid : "";
    }

    // check if wifi seems to be connected to a SSID
    private static boolean isConnectedToAnything() {
        return isLANConnected() && getSSID().length() > 0;
        // TODO this could report false positive on lan
    }
}
