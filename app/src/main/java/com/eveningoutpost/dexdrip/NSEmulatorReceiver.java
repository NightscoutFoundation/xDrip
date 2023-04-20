
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.models.BgReading.bgReadingInsertFromJson;
import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Created by jamorham on 14/11/2016.
 */

public class NSEmulatorReceiver extends BroadcastReceiver {

    private static final String TAG = "jamorham nsemulator";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static SharedPreferences prefs;
    private static final Object lock = new Object();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("nsemulator-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.d(TAG, "NSEmulator onReceiver: " + intent.getAction());
                        JoH.benchmark(null);
                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        //  BundleScrubber.scrub(bundle);
                        final String action = intent.getAction();


                        if ((bundle != null) && (debug)) {
                            UserError.Log.d(TAG, "Action: " + action);
                            JoH.dumpBundle(bundle, TAG);
                        }

                        if (action == null) return;

                        switch (action) {
                            case Intents.XDRIP_PLUS_NS_EMULATOR:

                                // in future this could have its own data source perhaps instead of follower
                                if (!Home.get_follower() && DexCollectionType.getDexCollectionType() != DexCollectionType.NSEmulator &&
                                        !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
                                    Log.e(TAG, "Received NSEmulator data but we are not a follower or emulator receiver");
                                    return;
                                }

                                if (!Home.get_follower()) {
                                    // must be NSEmulator here ???? Not true anymore.
                                    if (!Sensor.isActive()) {
                                        // warn about problems running without a sensor record
                                        Home.toaststaticnext("Please use: Start Sensor from the menu for best results!");
                                    }
                                }

                                if (bundle == null) break;

                                Log.d(TAG, "Receiving NSEmulator broadcast");

                                final String collection = bundle.getString("collection");
                                if (collection == null) return;

                                switch (collection) {

                                    case "entries":
                                        final String data = bundle.getString("data");

                                        if ((data != null) && (data.length() > 0)) {
                                            try {
                                                final JSONArray json_array = new JSONArray(data);
                                                // if this array is >1 in length then it is from OOP otherwise something like AAPS
                                                if (json_array.length() > 1) {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    int process_id = -1;
                                                    try {
                                                        process_id = json_object.getInt("ROW_ID");
                                                    } catch (JSONException e) {
                                                        // Intentionly ignoring ecxeption.
                                                    }
                                                    if (process_id == -1 || process_id == android.os.Process.myPid()) {
                                                        LibreOOPAlgorithm.handleData(json_array.getString(1));
                                                    } else {
                                                        Log.d(TAG, "Ignoring OOP result since process id is wrong " + process_id);
                                                    }

                                                } else {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    final String type = json_object.getString("type");
                                                    switch (type) {
                                                        case "sgv":
                                                            double slope = 0;
                                                            try {
                                                                slope = BgReading.slopefromName(json_object.getString("direction"));
                                                            } catch (JSONException e) {
                                                                //
                                                            }
                                                            bgReadingInsertFromData(json_object.getLong("date"),
                                                                    json_object.getDouble("sgv"), slope, true);

                                                            break;
                                                        default:
                                                            Log.e(TAG, "Unknown entries type: " + type);
                                                    }
                                                }


                                            } catch (JSONException e) {
                                                Log.e(TAG, "Got JSON exception: " + e);
                                            }

                                        }
                                        break;

                                    case "devicestatus":
                                        final String ddata = bundle.getString("data");

                                        if ((ddata != null) && (ddata.length() > 0)) {
                                            try {
                                                Log.d(TAG, "Got device status data: " + ddata);
                                                final JSONArray json_array = new JSONArray(ddata);
                                                final JSONObject json_object = json_array.getJSONObject(0);
                                                final JSONObject json_pump_object = json_object.getJSONObject("pump");

                                                try {
                                                    final double reservoir = json_pump_object.getDouble("reservoir");
                                                    PumpStatus.setReservoir(reservoir);

                                                } catch (JSONException e) {
                                                    Log.d(TAG, "Got exception when processing reservoir: " + e);
                                                }

                                                try {
                                                    final JSONObject battery_object = json_pump_object.getJSONObject("battery");
                                                    final double battery_percent = battery_object.getDouble("percent");
                                                    PumpStatus.setBattery(battery_percent);

                                                } catch (JSONException e) {
                                                    Log.d(TAG, "Got exception when processing battery: " + e);
                                                }

                                                try {
                                                    final JSONObject iob_object = json_pump_object.getJSONObject("iob");
                                                    final double bolus_iob = iob_object.getDouble("bolusiob");
                                                    PumpStatus.setBolusIoB(bolus_iob);

                                                } catch (JSONException e) {
                                                    Log.d(TAG, "Got exception when processing iob: " + e);
                                                }

                                            } catch (JSONException e) {
                                                Log.e(TAG, "Got JSON exception: " + e);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Got processing exception: " + e);
                                            }
                                            PumpStatus.syncUpdate();
                                        }
                                        break;

                                    default:
                                        Log.d(TAG, "Unprocessed collection: " + collection);

                                }

                                break;
                            case Intents.XDRIP_DECODE_FARM_RESULT:
                                Log.i(TAG, "recieved message XDRIP_DECODE_FARM_RESULT");
                                handleOop2DecodeFramResult(bundle);
                                break;

                            case Intents.XDRIP_DECODE_BLE_RESULT:
                                Log.i(TAG, "recieved message XDRIP_DECODE_BLE_RESULT");
                                handleOop2DecodeBleResult(bundle);
                                break;

                            case Intents.XDRIP_BLUETOOTH_ENABLE_RESULT:
                                Log.i(TAG, "recieved message XDRIP_BLUETOOTH_ENABLE_RESULT");
                                handleOop2BluetoothEnableResult(bundle);
                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                break;
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Caught Exception handling intent", e );
                    }finally {
                        JoH.benchmark("NSEmulator process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }
    private double getOOP2Version(final Bundle bundle) {
        final Double version = bundle.getDouble(Intents.OOP2_VERSION_NAME, 0);
        return version;
    }

    private JSONObject extractParams(final Bundle bundle) {
        if (bundle == null) {
            Log.e(TAG, "Null bundle passed to extract params");
            return null;
        }
        double version = getOOP2Version(bundle);
        boolean calibrate_raw = Pref.getString("calibrate_external_libre_2_algorithm_type", "calibrate_raw").equals("calibrate_raw");
        Log.d(TAG, "oop2 version = " + version + " calibarate_raw " + calibrate_raw);
        if(version < 1.2 && !calibrate_raw) {
            // Versions before 1.2 had a bug or missed features which allows them only to work on raw mode.
            JoH.static_toast_long(gs(R.string.please_update_OOP2_or_move_to_calibrate_based_on_raw_mode));
            Log.ueh(TAG, "OOP2 is too old to use with no calibration mode. Please update OOP2 or move to 'calibrate based on raw' mode.");
            return null;
        }

        final String json = bundle.getString("json");
        if (json == null) {
            Log.e(TAG, "json == null returning");
            return null;
        }
        JSONObject json_object;
        try {
            json_object = new JSONObject(json);
            int process_id = json_object.getInt("ROW_ID");
            if (process_id != android.os.Process.myPid()) {
                Log.d(TAG, "Ignoring OOP result since process id is wrong " + process_id);
                return null;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Got JSON exception: " + e);
            return null;
        }
        return json_object;

    }

    private void handleOop2DecodeFramResult(Bundle bundle) {
        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.e(TAG, "External OOP algorithm is on, ignoring decoded data.");
            return;
        }
        JSONObject json_object = extractParams(bundle);
        if (json_object == null) {
            return;
        }
        String decoded_buffer;
        String patchUidString;
        String patchInfoString;
        String tagId;
        long CaptureDateTime;
        int[] trend_bg_vals = null;
        int[] history_bg_vals = null;

        try {
            decoded_buffer = json_object.getString(Intents.DECODED_BUFFER);
            patchUidString = json_object.getString(Intents.PATCH_UID);
            patchInfoString = json_object.getString(Intents.PATCH_INFO);
            tagId = json_object.getString(Intents.TAG_ID);
            CaptureDateTime = json_object.getLong(Intents.LIBRE_DATA_TIMESTAMP);
            if (json_object.has(Intents.TREND_BG) && json_object.has(Intents.HISTORIC_BG)) {
                JSONArray computed_bg = json_object.getJSONArray(Intents.TREND_BG);
                trend_bg_vals = new int[computed_bg.length()];
                for (int i = 0; i < computed_bg.length(); i++) {
                    trend_bg_vals[i] = computed_bg.getInt(i);
                }
                computed_bg = json_object.getJSONArray(Intents.HISTORIC_BG);
                history_bg_vals = new int[computed_bg.length()];
                for (int i = 0; i < computed_bg.length(); i++) {
                    history_bg_vals[i] = computed_bg.getInt(i);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error JSONException ", e);
            return;
        }
        if (decoded_buffer == null) {
            Log.e(TAG, "Error could not get decoded_buffer");
            return;
        }

        // Does this throws exception???
        byte[] fram_data = Base64.decode(decoded_buffer, Base64.NO_WRAP);
        byte[] patchUid = Base64.decode(patchUidString, Base64.NO_WRAP);
        byte[] patchInfo = Base64.decode(patchInfoString, Base64.NO_WRAP);
        LibreOOPAlgorithm.handleOop2DecodeFramResult(tagId, CaptureDateTime, fram_data, patchUid, patchInfo, trend_bg_vals, history_bg_vals);
    }

    private void handleOop2DecodeBleResult(Bundle bundle) {
        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.e(TAG, "External OOP algorithm is on, ignoring ble decrypted data.");
            return;
        }
        JSONObject json_object = extractParams(bundle);
        if (json_object == null) {
            return;
        }
        String decoded_buffer;
        String patchUidString;
        JSONArray computed_bg;
        long CaptureDateTime;

        int[] trend_bg_vals = null;
        int[] history_bg_vals = null;

        try {
            decoded_buffer = json_object.getString(Intents.DECODED_BUFFER);
            patchUidString = json_object.getString(Intents.PATCH_UID);
            CaptureDateTime = json_object.getLong(Intents.LIBRE_DATA_TIMESTAMP);
            if (json_object.has(Intents.TREND_BG) && json_object.has(Intents.HISTORIC_BG)) {
                computed_bg = json_object.getJSONArray(Intents.TREND_BG);
                trend_bg_vals = new int[computed_bg.length()];
                for (int i = 0; i < computed_bg.length(); i++) {
                    trend_bg_vals[i] = computed_bg.getInt(i);
                }
                computed_bg = json_object.getJSONArray(Intents.HISTORIC_BG);
                history_bg_vals = new int[computed_bg.length()];
                for (int i = 0; i < computed_bg.length(); i++) {
                    history_bg_vals[i] = computed_bg.getInt(i);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error JSONException ", e);
            return;
        }
        if (decoded_buffer == null) {
            Log.e(TAG, "Error could not get decoded_buffer");
            return;
        }

        Sensor.createDefaultIfMissing();

        // Does this throws exception???
        byte[] ble_data = Base64.decode(decoded_buffer, Base64.NO_WRAP);
        byte[] patchUid = Base64.decode(patchUidString, Base64.NO_WRAP);
        LibreOOPAlgorithm.handleDecodedBleResult(CaptureDateTime, ble_data, patchUid, trend_bg_vals, history_bg_vals);
    }

    private void handleOop2BluetoothEnableResult(Bundle bundle) {
        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.e(TAG, "External OOP algorithm is on, ignoring data.");
            return;
        }
        JSONObject json_object = extractParams(bundle);
        if (json_object == null) {
            return;
        }
        String btUnlockBufferString;
        String nfcUnlockBufferString;
        String patchUidString;
        String patchInfoString;
        String deviceName;
        JSONArray btUnlockBufferArray;
        int unlockCount = -1;
        ArrayList<byte[]> unlockBufferArray = new ArrayList<byte[]>();


        try {
            btUnlockBufferString = json_object.getString(Intents.BT_UNLOCK_BUFFER);
            nfcUnlockBufferString = json_object.getString(Intents.NFC_UNLOCK_BUFFER);
            patchUidString = json_object.getString(Intents.PATCH_UID);
            patchInfoString = json_object.getString(Intents.PATCH_INFO);
            deviceName = json_object.getString(Intents.DEVICE_NAME);
            if (json_object.has(Intents.CONNECTION_INDEX)) {
                unlockCount = json_object.getInt(Intents.CONNECTION_INDEX);
                btUnlockBufferArray = json_object.getJSONArray(Intents.BT_UNLOCK_BUFFER_ARRAY);
                for (int i = 0; i < btUnlockBufferArray.length(); i++) {
                    String btUnlockBuffer = btUnlockBufferArray.getString(i);
                    unlockBufferArray.add(Base64.decode(btUnlockBuffer, Base64.NO_WRAP));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error JSONException ", e);
            return;
        }

        byte[] bt_unlock_buffer = Base64.decode(btUnlockBufferString, Base64.NO_WRAP);
        byte[] nfc_unlock_buffer = Base64.decode(nfcUnlockBufferString, Base64.NO_WRAP);
        byte[] patchUid = Base64.decode(patchUidString, Base64.NO_WRAP);
        byte[] patchInfo = Base64.decode(patchInfoString, Base64.NO_WRAP);

        LibreOOPAlgorithm.handleOop2BluetoothEnableResult(bt_unlock_buffer, nfc_unlock_buffer, patchUid, patchInfo, deviceName, unlockBufferArray, unlockCount);
    }


    public static BgReading bgReadingInsertFromData(long timestamp, double sgv, double slope, boolean do_notification) {
        Log.d(TAG, "bgReadingInsertFromData called timestamp = " + timestamp + " bg = " + sgv + " time =" + JoH.dateTimeText(timestamp));
        final JSONObject faux_bgr = new JSONObject();
        try {
            faux_bgr.put("timestamp", timestamp);
            faux_bgr.put("calculated_value", sgv);
            faux_bgr.put("filtered_calculated_value", sgv);
            faux_bgr.put("calculated_value_slope", slope);
            faux_bgr.put("source_info", "NSEmulator Follow");
            // sanity checking???
            // fake up some extra data
            faux_bgr.put("raw_data", sgv);
            faux_bgr.put("age_adjusted_raw_value", sgv);
            faux_bgr.put("filtered_data", sgv);

            faux_bgr.put("uuid", UUID.randomUUID().toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            UserError.Log.e(TAG, "bgReadingInsertFromData Got JSON exception: " + e);
            return null;
        }

        Log.d(TAG, "Received NSEmulator SGV: " + faux_bgr);
        Sensor.createDefaultIfMissing();
        return bgReadingInsertFromJson(faux_bgr.toString(), do_notification, true); // notify and force sensor
    }
}
