package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Intent;
import android.os.Bundle;

import static com.eveningoutpost.dexdrip.xdrip.getAppContext;

/**
 * jamorham
 *
 * Locally broadcast an xDrip intent for other apps, caller should check enabled() first
 * handles different and legacy configuration options for package/permission destination
 */

public class SendXdripBroadcast {

    public static void send(final Intent intent, final Bundle bundle) {
        if (bundle != null) intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        final String destination = Pref.getString("local_broadcast_specific_package_destination", "").trim();

        if (destination.length() > 3) {
            for (final String this_dest : destination.split(" ")) {
                if (this_dest != null && this_dest.length() > 3) {
                    // send to each package in space delimited list
                    intent.setPackage(this_dest);
                    sendWithOrWithoutPermission(intent, bundle);
                }
            }
        } else {
            // no package specified
            sendWithOrWithoutPermission(intent, bundle);
        }
    }

    private static void sendWithOrWithoutPermission(final Intent intent, final Bundle bundle) {

        if (Pref.getBooleanDefaultFalse("broadcast_data_through_intents_without_permission")) {
            getAppContext().sendBroadcast(intent);
        } else {
            getAppContext().sendBroadcast(intent, Intents.RECEIVER_PERMISSION);
        }
    }

    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("broadcast_data_through_intents");
    }

}
