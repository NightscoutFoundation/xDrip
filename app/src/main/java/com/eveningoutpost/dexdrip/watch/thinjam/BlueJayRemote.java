package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import android.content.Intent;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.watch.thinjam.io.ThinJamApiReceiver;

import lombok.val;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.getAppContext;

public class BlueJayRemote {

    private static final String TAG = "BlueJayRemote";

    public static void sendTextFit(final String text) {
        sendAPIbroadcast(BlueJayAPI.TEXT_FIT, text);
    }

    public static void sendColourPng(final byte[] pngBytes, final String parameters) {
        sendAPIbroadcast(BlueJayAPI.COLOUR_PNG, parameters, pngBytes);
    }

    public static void sendMonoPng(final byte[] pngBytes, final String parameters) {
        sendAPIbroadcast(BlueJayAPI.MONO_PNG, parameters, pngBytes);
    }

    public static void sendLatestBG() {
        if (BlueJayEntry.isRemoteEnabled()) {
            final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
            if (dg == null) return;

            val sb = new StringBuilder();

            if (dg.isReallyStale()) {
                sb.append("Stale data: ").append(dg.minutesAgo(true));
            } else {
                sb.append(dg.unitized)
                        .append(" ")
                        .append(dg.delta_name)
                        .append(" ")
                        .append(dg.unitized_delta);

                if (msSince(dg.timestamp) > MINUTE_IN_MS) {
                    sb.append(" ").append(dg.minutesAgo(true));
                }
            }

            val finalMessage = sb.toString();
            UserError.Log.d(TAG, "Broadcasting: " + finalMessage);
            sendTextFit(finalMessage);
        }
    }

    private static void sendAPIbroadcast(final String command, final String param) {
        sendAPIbroadcast(command, param, null);
    }

    private static void sendAPIbroadcast(final String command, final String param, final byte[] bytes) {
        val intent = getAPIintent();

        intent.putExtra(ThinJamApiReceiver.API_COMMAND, command);
        intent.putExtra(ThinJamApiReceiver.API_PARAM, param);
        if (bytes != null) {
            intent.putExtra(ThinJamApiReceiver.API_BYTES, bytes);
        }

        // TODO set destination package
        intent.setPackage("com.eveningoutpost.dexdrip");

        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        getAppContext().sendBroadcast(intent);
    }

    private static Intent getAPIintent() {
        return new Intent(Intents.BLUEJAY_THINJAM_API);
    }

}
