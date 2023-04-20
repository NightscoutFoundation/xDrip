package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Experience;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import static com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity.AUTO_UPDATE_PREFS_NAME;

// jamorham

// occasionally prompt to opt-in for updates

public class HeyFamUpdateOptInDialog {

    private static final boolean DEBUG = false;

    public static void heyFam(final Activity activity) {
        if (DEBUG || (!Pref.getBooleanDefaultFalse(AUTO_UPDATE_PREFS_NAME)
                && Experience.ageOfThisBuildAtLeast(Constants.DAY_IN_MS * 60)
                && Experience.installedForAtLeast(Constants.DAY_IN_MS * 30)
                && Experience.gotData()
                && JoH.pratelimit("hey-fam-update-reminder", 86400 * 60))) {
            ask1(activity, new Runnable() {
                @Override
                public void run() {
                    ask2(activity, new Runnable() {
                        @Override
                        public void run() {
                            Pref.setBoolean(AUTO_UPDATE_PREFS_NAME, true);
                            JoH.static_toast_long(activity.getString(R.string.update_checking_enabled));
                        }
                    });
                }
            });
        }
    }

    // inform the user about this message
    private static void ask1(Activity activity, Runnable runnable) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.hey_fam);
        builder.setMessage(R.string.you_have_update_checks_off);

        builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runnable.run();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

    }

    // ask the user if they would like to enable updates
    private static void ask2(Activity activity, Runnable runnable) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.enable_update_checks);
        builder.setMessage(R.string.you_can_easily_roll_back_dialog_msg);

        builder.setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runnable.run();
            }
        });

        builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

    }
}

