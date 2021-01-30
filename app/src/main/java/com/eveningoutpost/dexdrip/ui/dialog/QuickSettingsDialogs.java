package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

// jamorham

public class QuickSettingsDialogs {

    private static final String TAG = "QuickSettingsDialog";
    private static AlertDialog dialog;


    public static void booleanSettingDialog(Activity activity, String setting, String title, String checkboxText, String message, final Runnable postRun) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_checkbox, null);
        dialogBuilder.setView(dialogView);

        final CheckBox cb = (CheckBox) dialogView.findViewById(R.id.dialogCheckbox);
        cb.setText(checkboxText);
        cb.setChecked(Pref.getBooleanDefaultFalse(setting));

        final TextView tv = (TextView) dialogView.findViewById(R.id.dialogCheckboxTextView);
        dialogBuilder.setTitle(title);
        tv.setText(message);
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Pref.setBoolean(setting, cb.isChecked());
                if (postRun != null) postRun.run();
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (postRun != null) postRun.run();
            }
        });

        try {
            if (isDialogShowing()) dialog.dismiss();
        } catch (Exception e) {
            //
        }

        dialog = dialogBuilder.create();
        try {
            dialog.show();
        } catch (Exception e) {
            UserError.Log.e(TAG, "Could not show dialog: " + e);
        }
    }


    public static void textSettingDialog(Activity activity, String setting, String title, String message, int input_type, final Runnable postRun) {
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
                if (postRun != null) postRun.run();
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (postRun != null) postRun.run();
            }
        });

        try {
            if (isDialogShowing()) dialog.dismiss();
        } catch (Exception e) {
            //
        }

        dialog = dialogBuilder.create();
        try {
            dialog.show();
        } catch (Exception e) {
            UserError.Log.e(TAG, "Could not show dialog: " + e);
        }
    }


    public static boolean isDialogShowing() {
        return (dialog != null) && dialog.isShowing();
    }

}
