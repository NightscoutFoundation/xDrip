package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.eveningoutpost.dexdrip.R;

// jamorham

// ask confirmation, run a runnable

public class GenericConfirmDialog {

    public static void show(final Activity activity, String title, CharSequence message, Runnable runnable) {
        show(activity, title, message, runnable, true);
    }

    public static void inform(final Activity activity, String title, String message, Runnable runnable) {
        // Simply inform without offering a decline option.
        show(activity, title, message, runnable, false);
    }

    public static void show(final Activity activity, String title, CharSequence message, Runnable runnable, boolean negativeButton) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message);

        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            if (runnable != null) {
                runnable.run();
            }
        });

        if (negativeButton) {
            builder.setNegativeButton(R.string.no, (dialog, which) ->
            {
                dialog.cancel();
            });
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

