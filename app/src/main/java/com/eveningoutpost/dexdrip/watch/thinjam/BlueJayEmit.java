package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import android.content.Intent;

import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import lombok.val;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.xdrip.getAppContext;

public class BlueJayEmit {

    private static final String TAG = "BlueJayEmit";

    public static final String EMISSION_TYPE = "API_TYPE";
    public static final String API_PARAM = "API_PARAM";
    public static final String API_BYTES = "API_BYTES";
    public static final String API_TIMESTAMP = "API_TIMESTAMP";

    public static final String EVENT_LONGPRESS = "EVENT_LONGPRESS";
    public static final String EVENT_CHOICE = "EVENT_CHOICE";
    public static final String EVENT_INFO = "EVENT_INFO";


    public static void sendButtonPress(int button) {
        sendAPIbroadcast(EVENT_LONGPRESS, "" + button);
    }

    public static void sendChoice(int choice, final String parameters) {
        sendAPIbroadcast(EVENT_CHOICE, parameters);
    }

    public static void sendInfo(final String parameters) {
        sendAPIbroadcast(EVENT_INFO, parameters);
    }


    private static void sendAPIbroadcast(final String event, final String param) {
        sendAPIbroadcast(event, param, null);
    }

    private static void sendAPIbroadcast(final String event, final String param, final byte[] bytes) {
        if (!BlueJay.remoteApiEnabled()) {
            return;
        }

        if (event != null) {
            val intent = getAPIintent();
            intent.putExtra(EMISSION_TYPE, event);

            if (param != null) {
                intent.putExtra(API_PARAM, param);
            }
            if (bytes != null) {
                intent.putExtra(API_BYTES, bytes);
            }


            intent.putExtra(API_TIMESTAMP, "" + tsl());

            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            final String destination = Pref.getString("local_broadcast_specific_package_destination", "").trim();

            if (destination.length() > 3) {
                for (final String this_dest : destination.split(" ")) {
                    if (this_dest != null && this_dest.length() > 3) {
                        // send to each package in space delimited list
                        intent.setPackage(this_dest);
                        getAppContext().sendBroadcast(intent);
                    }
                }
            } else {
                // no package specified
                getAppContext().sendBroadcast(intent);
            }
        }
    }

    private static Intent getAPIintent() {
        return new Intent(Intents.BLUEJAY_THINJAM_EMIT);
    }

}
