package com.eveningoutpost.dexdrip.utils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.integration.android.IntentIntegrator;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DisplayQRCode extends BaseAppCompatActivity {

    public static final String qrmarker = "xdpref:";
    private static final String TAG = "jamorham qr";
    private static String send_url;
    private SharedPreferences prefs;
    private static DisplayQRCode mInstance;
    private Map<String, String> prefsMap = new HashMap<String, String>();

    public static Map<String, String> decodeString(String data) {
        try {
            if (data.startsWith(qrmarker)) {
                data = data.substring(qrmarker.length());
                Log.d(TAG, "String to uncompress: " + data);
                data = JoH.uncompressString(data);
                //Log.d(TAG, "Json after decompression: " + data);
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
        mInstance = this;
        setContentView(R.layout.activity_display_qrcode);
        JoH.fixActionBar(this);
    }

    @Override
    protected void onDestroy() {
        mInstance = null; // GC?
        super.onDestroy();
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

    public static void uploadBytes(byte[] result, final int callback_option) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("uploadBytes",1200000);
        if ((result != null) && (result.length > 0)) {
            final byte[] mykey = CipherUtils.getRandomKey();

            byte[] crypted_data = CipherUtils.encryptBytes(JoH.compressBytesToBytes(result), mykey);
            if ((crypted_data != null) && (crypted_data.length > 0)) {
                Log.d(TAG, "Before: " + result.length + " After: " + crypted_data.length);

                final OkHttpClient client = new OkHttpClient();

                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(30, TimeUnit.SECONDS);
                client.setWriteTimeout(30, TimeUnit.SECONDS);

                toast("Preparing");

                try {
                    send_url = xdrip.getAppContext().getString(R.string.wserviceurl) + "/joh-setsw";
                    final String bbody = Base64.encodeToString(crypted_data, Base64.NO_WRAP);
                    Log.d(TAG, "Upload Body size: " + bbody.length());
                    final RequestBody formBody = new FormEncodingBuilder()
                            .add("data", bbody)
                            .build();
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                final Request request = new Request.Builder()
                                        .header("User-Agent", "Mozilla/5.0 (jamorham)")
                                        .header("Connection", "close")
                                        .url(send_url)
                                        .post(formBody)
                                        .build();
                                Log.i(TAG, "Uploading data");
                                Response response = client.newCall(request).execute();
                                if (response.isSuccessful()) {
                                    final String reply = response.body().string();
                                    Log.d(TAG, "Got success response length: " + reply.length() + " " + reply);
                                    if ((reply.length() == 35) && (reply.startsWith("ID:"))) {
                                        switch (callback_option) {
                                            case 1: {
                                                if (mInstance != null) {
                                                    mInstance.display_final_all_settings_qr_code(reply.substring(3, 35), mykey);
                                                } else {
                                                    Log.e(TAG, "mInstance null");
                                                }
                                                break;
                                            }
                                            case 2: {
                                                GcmActivity.backfillLink(reply.substring(3, 35), JoH.bytesToHex(mykey));
                                                break;
                                            }
                                            default: {
                                                toast("Invalid callback option on upload");
                                            }
                                        }
                                    } else {
                                        Log.d(TAG,"Got unhandled reply: "+reply);
                                        toast(reply);
                                    }
                                } else {
                                    toast("Error please try again");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Got exception in execute: " + e.toString());
                                e.printStackTrace();
                                toast("Error with connection");
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    toast(e.getMessage());
                    Log.e(TAG, "General exception: " + e.toString());
                } finally {
                    JoH.releaseWakeLock(wl);
                }
            } else {
                toast("Something went wrong preparing the settings");
            }
        } else {
            toast("Could not read data somewhere");
        }
    }

    public void allSettings(View view) {
        prefsMap.clear();
        byte[] result = SdcardImportExport.getPreferencesFileAsBytes(getApplicationContext());
        if ((result != null) && (result.length > 0)) {
            uploadBytes(result, 1);
        } else {
            toast("Could not read preferences file");
        }
    }

    private void display_final_all_settings_qr_code(final String uid, final byte[] mykey) {
        Log.d(TAG, "Displaying final qr code: " + uid);
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    prefsMap.put(getString(R.string.all_settings_wizard), "t");
                    prefsMap.put("wizard_uuid", uid);
                    prefsMap.put("wizard_key", CipherUtils.bytesToHex(mykey));
                    showQRCode();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Got exception displaying final qrcode: " + e.toString());
        }
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
            mInstance = null;
            finish();
        } catch (Exception e) {
            Log.d(TAG, "Error finishing " + e.toString());
        }
    }


    private static void toast(final String msg) {
        JoH.static_toast_short(msg);
    }

}
