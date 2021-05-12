package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;

import com.eveningoutpost.dexdrip.BluetoothScan;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.cgm.sharefollow.ShareFollowService;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.ui.dialog.QuickSettingsDialogs.booleanSettingDialog;
import static com.eveningoutpost.dexdrip.ui.dialog.QuickSettingsDialogs.textSettingDialog;

/**
 * Created by jamorham on 02/03/2018.
 */

public class DexCollectionHelper {

    private static final String TAG = DexCollectionHelper.class.getSimpleName();


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

            case SHFollow:
                textSettingDialog(activity,
                        "shfollow_user", "Dex Share Username",
                        "Enter Share Follower Username",
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        new Runnable() {
                            @Override
                            public void run() {
                                textSettingDialog(activity,
                                        "shfollow_pass", "Dex Share Password",
                                        "Enter Share Follower Password",
                                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                booleanSettingDialog(activity,
                                                        "dex_share_us_acct", "Select Servers", "My account is on USA servers", "Select whether using USA or rest-of-world account", new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Home.staticRefreshBGCharts();
                                                                ShareFollowService.resetInstanceAndInvalidateSession();
                                                                CollectionServiceStarter.restartCollectionServiceBackground();
                                                            }
                                                        });
                                            }
                                        });
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


}
