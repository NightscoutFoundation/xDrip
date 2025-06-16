package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;

// jamorham

public class QuickSettingsDialogs {

    private static final String TAG = "QuickSettingsDialog";

    public static final String FINISH_ACTIVITY_ON_DIALOG_DISMISS = "FINISH_ON_DIALOG_DISMISS";
    private static AlertDialog dialog;

    public static void booleanSettingDialog(Activity activity, String setting, String title, String checkboxText, String message, final Runnable postRun) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_checkbox, null);
        dialogBuilder.setView(dialogView);

        final CheckBox cb = dialogView.findViewById(R.id.dialogCheckbox);
        cb.setText(checkboxText);
        cb.setChecked(Pref.getBooleanDefaultFalse(setting));

        final TextView tv = dialogView.findViewById(R.id.dialogCheckboxTextView);
        dialogBuilder.setTitle(title);
        tv.setText(message);
        dialogBuilder.setPositiveButton(R.string.done, (dialog, whichButton) -> {
            Pref.setBoolean(setting, cb.isChecked());
            if (postRun != null) postRun.run();
        });
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
            if (postRun != null) postRun.run();
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
        final boolean finishOnDismiss = activity.getIntent().getBooleanExtra(FINISH_ACTIVITY_ON_DIALOG_DISMISS, false);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_text_entry, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = dialogView.findViewById(R.id.dialogTextEntryeditText);

        if (input_type != 0) {
            edt.setInputType(input_type);
        }

        if (setting.equals("dex_txid")) {
            edt.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        }

        edt.setText(Pref.getString(setting, ""));

        final TextView tv = dialogView.findViewById(R.id.dialogTextEntryTextView);
        dialogBuilder.setTitle(title);
        tv.setText(message);
        dialogBuilder.setPositiveButton(R.string.done, (dialog, whichButton) -> {
            final String text = edt.getText().toString().trim();
            Pref.setString(setting, text);
            if (postRun != null) postRun.run();
        });
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
            if (postRun != null) postRun.run();
        });

        try {
            if (isDialogShowing()) dialog.dismiss();
        } catch (Exception e) {
            //
        }

        dialog = dialogBuilder.create();

        // finish the holding activity when dialog is dismissed if the flag is set
        if (finishOnDismiss) {
            dialog.setOnDismissListener(d -> {
                // Called when the dialog is dismissed by any means
                activity.finish();
            });
        }

        if (!BlueJayEntry.isNative()) {
            dialog.setOnShowListener(d -> {
                try {
                    edt.requestFocus();
                    edt.post(() -> {
                        // Move cursor to end of text
                        edt.setSelection(edt.getText().length());
                        // show keyboard automatically
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(edt, InputMethodManager.SHOW_IMPLICIT);
                    });
                  } catch (Exception e) {
                    UserError.Log.e(TAG, "Error setting input method focus: " + e);
                }
            });
        }

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
