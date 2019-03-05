package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BluetoothScan;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by jamorham on 02/03/2018.
 */

public class DexCollectionHelper {

    private static final String TAG = DexCollectionHelper.class.getSimpleName();
    private static AlertDialog dialog;

    public static void assistance(Activity activity, DexCollectionType type) {

        switch (type) {

            // g6 is currently a pseudo type which enables required g6 settings and then sets g5
            case DexcomG6:
                Ob1G5CollectionService.setG6Defaults();

                DexCollectionType.setDexCollectionType(DexCollectionType.DexcomG5);
                // intentional fall thru

            case DexcomG5:
                final String pref = "dex_txid";
                textSettingDialog(activity,
                        pref, activity.getString(R.string.dexcom_transmitter_id),
                        activity.getString(R.string.enter_your_transmitter_id_exactly),
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        new Runnable() {
                            @Override
                            public void run() {
                                // InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS does not seem functional here
                                Pref.setString(pref, Pref.getString(pref, "").toUpperCase());
                                Home.staticRefreshBGCharts();
                                CollectionServiceStarter.restartCollectionServiceBackground();
                            }
                        });
                break;

            case DexbridgeWixel:
                textSettingDialog(activity,
                        "dex_txid", activity.getString(R.string.dexcom_transmitter_id),
                        activity.getString(R.string.enter_your_transmitter_id_exactly),
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        new Runnable() {
                            @Override
                            public void run() {
                                bluetoothScanIfNeeded();
                            }
                        });
                break;


            case NSFollow:
                textSettingDialog(activity,
                        "nsfollow_url", "Nightscout Follow URL",
                        "Web address for following",
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        new Runnable() {
                            @Override
                            public void run() {
                                Home.staticRefreshBGCharts();
                                CollectionServiceStarter.restartCollectionServiceBackground();
                            }
                        });
                break;


            case LimiTTer:
                bluetoothScanIfNeeded();
                break;

            case BluetoothWixel:
                bluetoothScanIfNeeded();
                break;

            case DexcomShare:
                bluetoothScanIfNeeded();
                break;

            case Medtrum:
                bluetoothScanIfNeeded();
                break;

            // TODO G4 Share Receiver

            // TODO Parakeet / Wifi ??

            // TODO Bluetooth devices without active device -> bluetooth scan

            // TODO Helper apps not installed? Prompt for installation

        }


    }

    public static void bluetoothScanIfNeeded() {
        if (ActiveBluetoothDevice.first() == null) {
            xdrip.getAppContext().startActivity(new Intent(xdrip.getAppContext(), BluetoothScan.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    // TODO this can move to its own utility class
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
