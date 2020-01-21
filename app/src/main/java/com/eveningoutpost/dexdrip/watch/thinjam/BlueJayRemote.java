package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import android.content.Intent;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.watch.thinjam.io.ThinJamApiReceiver;

import lombok.val;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.getAppContext;

public class BlueJayRemote {

    private static final String TAG = "BlueJayRemote";

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
            UserError.Log.e(TAG, "Broadcasting: " + finalMessage);

            val intent = new Intent(Intents.BLUEJAY_THINJAM_API);

            intent.putExtra(ThinJamApiReceiver.API_COMMAND, BlueJayAPI.TEXT_FIT);
            intent.putExtra(ThinJamApiReceiver.API_PARAM, finalMessage);

            // TODO set destination package
            intent.setPackage("com.eveningoutpost.dexdrip");

            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            getAppContext().sendBroadcast(intent);

        }
    }

}
