package com.eveningoutpost.dexdrip.ui.helpers;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Handle requesting Do Not Disturb access if needed/we don't have it
 */

public class DoNotDisturb {

    private static final String TAG = DoNotDisturb.class.getSimpleName();

    public static void checkAndAskForDoNotDisturbAccess(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mNotificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            try {
                if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                    JoH.show_ok_dialog(activity, activity.getString(R.string.please_allow_permission), activity.getString(R.string.allow_do_not_disturb_text), () -> {
                        val intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    });

                } else {
                    UserError.Log.d(TAG, "We have notification policy access");
                }
            } catch (Exception e) {
                JoH.static_toast_long("Unable to open system settings");
                UserError.Log.wtf(TAG, "Failed to start settings intent: " + e);
            }
        }
    }

}
