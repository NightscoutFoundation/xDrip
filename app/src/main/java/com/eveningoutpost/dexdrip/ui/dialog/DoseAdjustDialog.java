package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.eveningoutpost.dexdrip.insulin.MultipleInsulins;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.val;

// JamOrHam

public class DoseAdjustDialog {

    public static void show(final Activity activity, final String uuid) {
        if (uuid.equals("")) return;
        final boolean trackPens = MultipleInsulins.isEnabled();

        val t = Treatments.byuuid(uuid);
        val builder = new AlertDialog.Builder(activity)
                .setTitle(xdrip.gs(R.string.adjust_dose) + "? " + t.insulin + "U")
                .setMessage(t.getBestShortText() + "\n" + JoH.dateTimeText(t.timestamp));

        builder.setPositiveButton(R.string.add_note, (dialog, which) -> {
            createNoteFromTreatmentUUID(uuid);
            dialog.cancel();
        });

        if (trackPens) {
            builder.setNeutralButton(R.string.choose_type, (dialog, which) -> {
                dialog.cancel();
                ChooseInsulinPenDialog.show(activity, t.getPenSerial());
            });
        } else {
            builder.setNeutralButton(R.string.cancel, (dialog, which) -> {
                dialog.cancel();
            });
        }

        builder.setNegativeButton(R.string.zero_value, (dialog, which) -> {
            GenericConfirmDialog.show(activity, xdrip.gs(R.string.zero_value) + " ?", xdrip.gs(R.string.are_you_sure), () -> zeroTreatmentValueByUUID(uuid));
            dialog.cancel();
        });
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

    private static void createNoteFromTreatmentUUID(final String uuid) {
        val t = Treatments.byuuid(uuid);
        if (t != null) {
            Home.startHomeWithExtra(xdrip.getAppContext(), Home.CREATE_TREATMENT_NOTE, "" + t.timestamp, "-1");
        }
    }

    private static void zeroTreatmentValueByUUID(final String uuid) {
        val t = Treatments.byuuid(uuid);
        if (t != null) {
            t.insulin = 0;
            t.save();
            Home.staticRefreshBGChartsOnIdle();
            UploaderQueue.newEntry("update", t);
            SyncService.startSyncService(3000); // sync in 3 seconds
        }
    }

}


