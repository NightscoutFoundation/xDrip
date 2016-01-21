package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DisplayQRCode extends Activity {

    public static final String qrmarker = "xdpref:";
    private static final String TAG = "jamorham qr";
    private SharedPreferences prefs;
    private Map<String, String> prefsMap = new HashMap<String, String>();

    public static Map<String, String> decodeString(String data) {
        try {
            if (data.startsWith(qrmarker)) {
                data = data.substring(qrmarker.length());
                Log.d(TAG, "String to uncompress: " + data);
                data = JoH.uncompressString(data);
                Log.d(TAG, "Json after decompression: " + data);
                Map<String, String> mymap = new Gson().fromJson(data, new TypeToken<HashMap<String, String>>() {
                }.getType());
                return mymap;

            } else {
                Log.e(TAG, "No qrmarker on qrcode");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception during decodingString: " + e.toString());
            return null;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_qrcode);
    }

    public void xdripPlusSyncSettings(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefsMap.put("custom_sync_key", prefs.getString("custom_sync_key", ""));
        prefsMap.put("use_custom_sync_key", Boolean.toString(prefs.getBoolean("use_custom_sync_key", false)));
        showQRCode();
    }

    public void connectionSettings(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefsMap.clear();
        prefsMap.put("wifi_recievers_addresses", prefs.getString("wifi_recievers_addresses", ""));
        prefsMap.put("dex_collection_method", prefs.getString("dex_collection_method", "BluetoothWixel"));
        prefsMap.put("highValue", prefs.getString("highValue", "170"));
        prefsMap.put("lowValue", prefs.getString("lowValue", "70"));
        prefsMap.put("units", prefs.getString("units", "mgdl"));
        prefsMap.put("custom_sync_key", prefs.getString("custom_sync_key", ""));
        prefsMap.put("use_custom_sync_key", Boolean.toString(prefs.getBoolean("use_custom_sync_key", false)));
        showQRCode();
    }

    public void alarmSettings(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefsMap.clear();
        prefsMap.put("bg_alert_profile", prefs.getString("bg_alert_profile", "ascending"));
        prefsMap.put("smart_snoozing", Boolean.toString(prefs.getBoolean("smart_snoozing", true)));
        prefsMap.put("smart_alerting", Boolean.toString(prefs.getBoolean("smart_alerting", true)));
        prefsMap.put("calibration_notifications", Boolean.toString(prefs.getBoolean("calibration_notifications", true)));
        // need to support alert profiles
        showQRCode();
    }

    private void showQRCode() {
        String mystring = new JSONObject(prefsMap).toString();
        Log.d(TAG, "Serialized: " + mystring);
        String compressedstring = JoH.compressString(mystring);
        Log.d(TAG, "Compressed: " + compressedstring);

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.shareText(qrmarker + compressedstring);
    }

    public void closeNow(View view) {
        try {
            finish();
        } catch (Exception e) {
            Log.d(TAG, "Error finishing " + e.toString());
        }
    }

}
