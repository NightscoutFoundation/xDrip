package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.insulin.InsulinManager;
import com.eveningoutpost.dexdrip.insulin.opennov.data.Pens;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.val;

// JamOrHam

public class ChooseInsulinPenDialog {

    public static void show(final Activity activity, final String serial) {
        if (serial == null || serial.equals("")) {
            JoH.static_toast_long(xdrip.gs(R.string.invalid_serial));
            return;
        }

        val insulinProfile1 = InsulinManager.getProfile(0);
        val insulinProfile2 = InsulinManager.getProfile(1);
        val insulinProfile3 = InsulinManager.getProfile(2);

        val builder = new AlertDialog.Builder(activity)
                .setTitle(xdrip.gs(R.string.choose_type))
                .setMessage(xdrip.gs(R.string.pen_contains_which_insulin_type, serial));

        if (insulinProfile1 != null) {
            builder.setPositiveButton(insulinProfile1.getDisplayName(), (dialog, which) -> {
                set(serial, insulinProfile1.getName());
                dialog.cancel();
            });
        }
        if (insulinProfile2 != null) {
            builder.setNegativeButton(insulinProfile2.getDisplayName(), (dialog, which) -> {
                set(serial, insulinProfile2.getName());
                dialog.cancel();
            });
        }
        if (insulinProfile3 != null) {
            builder.setNeutralButton(insulinProfile3.getDisplayName(), (dialog, which) -> {
                set(serial, insulinProfile3.getName());
                dialog.cancel();
            });
        } else {
            builder.setNeutralButton(R.string.cancel, (dialog, which) -> {
                dialog.cancel();
            });
        }
        val dialog = builder.create();
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

    private static void set(final String serial, final String type) {
        Pens.load().updatePenBySerial(serial, type).save();
        JoH.static_toast_long(xdrip.gs(R.string.set_pen_to_format, serial, type));
    }

}


