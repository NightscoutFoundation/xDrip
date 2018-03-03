package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

/**
 * Created by jamorham on 02/03/2018.
 */

public class DexCollectionHelper {


    public static void assistance(Activity activity, DexCollectionType type) {

        switch (type) {

            case DexcomG5:
            case DexbridgeWixel:
                textSettingDialog(activity,
                        "dex_txid", activity.getString(R.string.dexcom_transmitter_id),
                        activity.getString(R.string.enter_your_transmitter_id_exactly),
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                break;

            // TODO G4 Share Receiver

            // TODO Parakeet / Wifi ??

            // TODO Bluetooth devices without active device -> bluetooth scan

            // TODO Helper apps not installed? Prompt for installation

        }


    }

    // TODO this can move to its own utility class
    public static void textSettingDialog(Activity activity, String setting, String title, String message, int input_type) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_text_entry, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.dialogTextEntryeditText);

        if (input_type != 0) {
            edt.setInputType(input_type);
        }

        edt.setText(Pref.getString(setting, ""));

        final TextView tv = (TextView) dialogView.findViewById(R.id.dialogTextEntryTextView);
        dialogBuilder.setTitle(title);
        tv.setText(message);
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String text = edt.getText().toString().trim();
                Pref.setString(setting, text);
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        dialogBuilder.create().show();
    }


}
