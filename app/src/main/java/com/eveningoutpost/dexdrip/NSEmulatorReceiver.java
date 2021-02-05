
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.PumpStatus;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertFromJson;

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
                                                    }   catch (JSONException e) {
                                                        // Intentionly ignoring ecxeption.
                                                    }
                                                    if(process_id == -1 || process_id == android.os.Process.myPid()) {
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
                                Log.e(TAG, "recieved message XDRIP_DECODE_FARM_RESULT");
                                handleOop2DecodeFarmResult(bundle);
                                break;
                                
                            case Intents.XDRIP_DECODE_BLE_RESULT:
                                Log.e(TAG, "recieved message XDRIP_DECODE_BLE_RESULT");
                                handleOop2DecodeBleResult(bundle);
                                break;
                                
                            case Intents.XDRIP_BLUETOOTH_ENABLE_RESULT:
                                Log.e(TAG, "recieved message XDRIP_BLUETOOTH_ENABLE_RESULT");
                                handleOop2BlutoothEnableResult(bundle);
                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                break;
                        }
                    } finally {
                        JoH.benchmark("NSEmulator process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }
    private JSONObject extractParams(Bundle bundle) {
        final String json = bundle.getString("json");
        if (json == null) {
            Log.e(TAG, "json == null returning");
            return null;
        }
        JSONObject json_object;
        try {
            json_object = new JSONObject(json);
            int process_id = json_object.getInt("ROW_ID");
            if(process_id != android.os.Process.myPid()) {
                Log.d(TAG, "Ignoring OOP result since process id is wrong " + process_id);
                return null;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Got JSON exception: " + e);
            return null;
        }
        return json_object;
        
    }
    
    private void handleOop2DecodeFarmResult(Bundle bundle) {
        if(Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.e(TAG, "External OOP algorithm is on, ignoring decoded data.");
            return;
        }
        JSONObject json_object = extractParams(bundle);
        if(json_object == null) {
            return;
        }
        String decoded_buffer;
        String patchUidString;
        String patchInfoString;
        String tagId;
        long CaptureDateTime;
        try {
            decoded_buffer = json_object.getString(Intents.DECODED_BUFFER);
            patchUidString = json_object.getString(Intents.PATCH_UID);
            patchInfoString = json_object.getString(Intents.PATCH_INFO);
            tagId = json_object.getString(Intents.TAG_ID);
            CaptureDateTime = json_object.getLong(Intents.LIBRE_DATA_TIMESTAMP);
        } catch (JSONException e) {
            Log.e(TAG, "Error JSONException ", e);
            return;
        }
        if(decoded_buffer == null) {
            Log.e(TAG, "Error could not get decoded_buffer");
            return;
        }
        
        // Does this throws exception???
        byte[] farm_data = Base64.decode(decoded_buffer, Base64.NO_WRAP);
        byte []patchUid = Base64.decode(patchUidString, Base64.NO_WRAP);
        byte []patchInfo = Base64.decode(patchInfoString, Base64.NO_WRAP);
        LibreOOPAlgorithm.handleOop2DecryptFarmResult(tagId, CaptureDateTime, farm_data, patchUid, patchInfo);
    }
    
    private void handleOop2DecodeBleResult(Bundle bundle) {
        if(Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.e(TAG, "External OOP algorithm is on, ignoring ble decrypted data.");
            return;
        }
        JSONObject json_object = extractParams(bundle);
        if(json_object == null) {
            return;
        }
        String decoded_buffer;
        String patchUidString;
        long CaptureDateTime;
        try {
            decoded_buffer = json_object.getString(Intents.DECODED_BUFFER);
            patchUidString = json_object.getString(Intents.PATCH_UID);
            CaptureDateTime = json_object.getLong(Intents.LIBRE_DATA_TIMESTAMP);
        } catch (JSONException e) {
            Log.e(TAG, "Error JSONException ", e);
            return;
        }
        if(decoded_buffer == null) {
            Log.e(TAG, "Error could not get decoded_buffer");
            return;
        }
        
        // Does this throws exception???
        byte[] ble_data = Base64.decode(decoded_buffer, Base64.NO_WRAP);
        byte []patchUid = Base64.decode(patchUidString, Base64.NO_WRAP);
        LibreOOPAlgorithm.handleDecodedBleResult(CaptureDateTime, ble_data, patchUid);
    }
    
    private void handleOop2BlutoothEnableResult(Bundle bundle) {
        if(Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.e(TAG, "External OOP algorithm is on, ignoring data.");
            return;
        }
        JSONObject json_object = extractParams(bundle);
        if(json_object == null) {
            return;
        }
        String btUunlockBufferString;
        String nfcUnlockBufferString;
        String patchUidString;
        String patchInfoString;
        String deviceName;

        
        try {
            btUunlockBufferString = json_object.getString(Intents.BT_UNLOCK_BUFFER);
            nfcUnlockBufferString = json_object.getString(Intents.NFC_UNLOCK_BUFFER);
            patchUidString = json_object.getString(Intents.PATCH_UID);
            patchInfoString = json_object.getString(Intents.PATCH_INFO);
            deviceName = json_object.getString(Intents.DEVICE_NAME);
        } catch (JSONException e) {
            Log.e(TAG, "Error JSONException ", e);
            return;
        }

        // Does this throws exception???
        byte[] bt_unlock_buffer = Base64.decode(btUunlockBufferString, Base64.NO_WRAP);
        byte[] nfc_unlock_buffer = Base64.decode(nfcUnlockBufferString, Base64.NO_WRAP);
        byte []patchUid = Base64.decode(patchUidString, Base64.NO_WRAP);
        byte []patchInfo = Base64.decode(patchInfoString, Base64.NO_WRAP);
        LibreOOPAlgorithm.handleOop2BlutoothEnableResult(bt_unlock_buffer, nfc_unlock_buffer, patchUid, patchInfo, deviceName);
    }


    public static BgReading bgReadingInsertFromData(long timestamp, double sgv, double slope, boolean do_notification) {
        Log.d(TAG, "bgReadingInsertFromData called timestamp = " + timestamp + " bg = " + sgv + " time =" + JoH.dateTimeText(timestamp));
        final JSONObject faux_bgr = new JSONObject();
        try {
            faux_bgr.put("timestamp", timestamp);
            faux_bgr.put("calculated_value", sgv);
            faux_bgr.put("filtered_calculated_value", sgv);
            faux_bgr.put("calculated_value_slope", slope);
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
        return bgReadingInsertFromJson(faux_bgr.toString(), do_notification, true); // notify and force sensor
    }
}
