package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.rarepebble.colorpicker.ColorPickerView;

/**
 * jamorham
 *
 * Show a picker dialog and save the preference and call any required refresher
 */

public class ColorPreferenceDialog {

    public static void pick(final Activity activity, final String pref, final String title, final Runnable runnable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final ColorPickerView picker = new ColorPickerView(activity);
        int color = Color.GRAY;
        try {
            color = Pref.getInt(pref, Color.GRAY);
        } catch (Exception e) {
            //
        }
        picker.setColor(color);
        picker.showAlpha(true);
        picker.showHex(false);
        builder.setTitle(title)
                .setView(picker)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // save result and refresh
                        Pref.setInt(pref, picker.getColor());
                        ColorCache.invalidateCache();
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
}


