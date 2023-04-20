package com.eveningoutpost.dexdrip.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.g5model.G6CalibrationParameters;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;

// jamorham

public class G6CalibrationCodeDialog {


    // ask the user for the code, check if its valid, only start sensor if it is
    public static void ask(Activity activity, Runnable runnable) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.g6_sensor_code);
        builder.setMessage(R.string.please_enter_printed_calibration_code);

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String code = input.getText().toString().trim();
                if (G6CalibrationParameters.checkCode(code)) {
                    G6CalibrationParameters.setCurrentSensorCode(code);
                    JoH.static_toast_long(activity.getString(R.string.code_accepted));
                    if (runnable != null) runnable.run();
                } else {
                    JoH.static_toast_long(activity.getString(R.string.invalid_or_unsupported_code));
                }
            }
        });

        builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (dialog != null)
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();

    }
}


