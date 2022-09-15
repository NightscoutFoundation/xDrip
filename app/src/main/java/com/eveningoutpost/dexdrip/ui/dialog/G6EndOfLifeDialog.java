package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.eveningoutpost.dexdrip.G5Model.DexTimeKeeper;
import static com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService.getTransmitterID;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

// Navid200

public class G6EndOfLifeDialog {
    // Inform the user that Transmitter Days is approaching maximum, or that it has passed.
    public static void show(final Activity activity, Runnable runnable) {
        String Title = activity.getString(R.string.reminder);
        String Message = activity.getString(R.string.TX_EOL_reminder);
        if (PersistentStore.getBoolean("TX_EOL")) { // Cannot start sensors
            Title = activity.getString(R.string.notification);
            Message = activity.getString(R.string.TX_EOL_notification);
        } else { // Approaching end of life
            Title = activity.getString(R.string.reminder);
            Message = activity.getString(R.string.TX_EOL_reminder);
            if (PersistentStore.getBoolean("TX_Mod")) { // modified transmitter
                Message = activity.getString(R.string.TX_EOL_reminder_mod);
            }
        }
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity)
                .setTitle(Title)
                .setMessage(Message);

        builder.setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runnable.run();
            }
        });

        if (PersistentStore.getBoolean("TX_EOL")) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
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
