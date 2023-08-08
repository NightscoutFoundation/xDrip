package com.eveningoutpost.dexdrip.plugin;

import static com.eveningoutpost.dexdrip.plugin.Consent.setGiven;

import android.app.Activity;
import android.app.AlertDialog;

import com.eveningoutpost.dexdrip.R;

/**
 * JamOrHam
 *
 * Request plug-in use consent from user via a dialog.
 */

public class Dialog {

    private static final String TAG = "PluginDialog";

    public static void ask(final Activity activity, final PluginDef pluginDef, final String text, final Runnable success, final Runnable failure) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(String.format("Download %s plugin?", pluginDef.name))     // TODO I18n
                .setMessage(String.format("Download and use the %s plugin published by %s?\n\n%s", pluginDef.name, pluginDef.author, text));

        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            setGiven(pluginDef);
            success.run();
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> failure.run());

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

    public static boolean txIdMatch(final String txId) {
       return txId != null && txId.length() > 0 && (txId.length() == 4 || (Character.isLetter(txId.charAt(0)) && !txId.equals("ABCDEF")));
    }

    public static boolean askIfNeeded(final Activity activity, final String txId) {
        /*
        if (txIdMatch(txId)) {
          val plugin = Registry.get("keks");
            if (!Consent.isGiven(plugin)) {
                runOnUiThread(() -> ask(activity, plugin, "To use Dex ONE transmitters a plugin is needed.\n\nPlease select YES to download and use the plugin which is published by xDrip+ project lead JamOrHam",
                        () -> Loader.getInstance(plugin, ""),
                        () -> UserError.Log.wtf(TAG, "User declined to download plugin")));
                return true;
            } else {
                Loader.getInstance(plugin, "");
            }
        }
        */
        return false;
    }

}
