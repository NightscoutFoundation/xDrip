package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;

// jamorham

// double check an alarm was intended to be cancelled

public class DidYouCancelAlarm {

    public static synchronized void dialog(final Activity activity, Runnable runnable) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cancel_alarm);
        builder.setMessage(R.string.please_confirm_to_cancel_the_alert);

        builder.setPositiveButton(R.string.yes_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runnable.run();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        // apparently possible dialog is already showing, probably due to hash code
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            //
        }
        try {
            dialog.show();
        } catch (Exception e) {
            UserError.Log.wtf(DidYouCancelAlarm.class.getSimpleName(), "Unable to show dialog: " + e);
        }
    }
}
