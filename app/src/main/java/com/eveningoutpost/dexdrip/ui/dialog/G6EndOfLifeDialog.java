package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;

import com.eveningoutpost.dexdrip.R;

// Navid200

public class G6EndOfLifeDialog {

    private static final int MAX_START_DAYS = 99;

    // Inform the user that Transmitter Days is approaching maximum, or that it has passed.
    public static void show(final Activity activity, final Runnable runnable, final boolean endOfLife, final boolean modified, final int currentDays) {
        String title = activity.getString(R.string.reminder);
        String message;
        if (endOfLife) { // Cannot start sensors
            title = activity.getString(R.string.notification);
            message = activity.getString(R.string.TX_EOL_notification);
        } else { // Approaching end of life
            if (modified) { // modified transmitter
                message = activity.getString(R.string.TX_EOL_reminder_mod, currentDays);
            } else {
                message = activity.getString(R.string.TX_EOL_reminder, MAX_START_DAYS, currentDays);
            }
        }
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message);

        builder.setPositiveButton(R.string.proceed, (dialog, which) -> runnable.run());

        if (endOfLife) {
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        }

        final AlertDialog dialog = builder.create();
        // apparently possible dialog is already showing, probably due to hash code
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            //
        }
        dialog.show();
    }
}
