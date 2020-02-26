package com.eveningoutpost.dexdrip.watch.thinjam.io;

// jamorham

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayAPI;

import lombok.val;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.UtilityModels.Intents.BLUEJAY_THINJAM_API;

public class ThinJamApiReceiver extends BroadcastReceiver {

    public static final String API_COMMAND = "API_COMMAND";
    public static final String API_PARAM = "API_PARAM";
    public static final String API_BYTES = "API_BYTES";

    private static final String TAG = "ThinJamAPI";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        final PowerManager.WakeLock wl = JoH.getWakeLock("thinjam-receiver", 60000);
        try {
            UserError.Log.d(TAG, "onReceiver: " + intent.getAction());
            if (intent.getAction().equals(BLUEJAY_THINJAM_API)) {
                val command = intent.getStringExtra(API_COMMAND);
                if (!emptyString(command)) {
                    val parameter = intent.getStringExtra(API_PARAM);
                    val bytes = intent.getByteArrayExtra(API_BYTES);
                    BlueJayAPI.processAPI(command, parameter, bytes);
                } else {
                    UserError.Log.e(TAG, "Empty command received in api");
                }
            } else {
                UserError.Log.wtf(TAG, "Invalid action received: " + intent.getAction());
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception processing api receive: " + e);

        } finally {
            JoH.releaseWakeLock(wl);
        }
    }
}
