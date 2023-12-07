package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenHeight;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenWidth;
import static com.eveningoutpost.dexdrip.utils.QRcodeUtils.createQRCodeBitmap;
import static com.eveningoutpost.dexdrip.utils.QRcodeUtils.qrmarker;
import static com.eveningoutpost.dexdrip.utils.QRcodeUtils.serializeBinaryPrefsMap;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewImpl;
import com.eveningoutpost.dexdrip.utilitymodels.desertsync.RouteTools;
import com.eveningoutpost.dexdrip.databinding.ActivityDisplayQrcodeBinding;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.val;

public class DisplayQRCode extends BaseAppCompatActivity {


    private static final String TAG = "jamorham qr";
    private static String send_url;
    private final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    private static DisplayQRCode mInstance;
    private Map<String, String> prefsMap = new HashMap<>();
    private Map<String, byte[]> binaryPrefsMap = new HashMap<>();
    private String mapChecksum = "empty";



    private ActivityDisplayQrcodeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        binding = ActivityDisplayQrcodeBinding.inflate(getLayoutInflater());
        binding.setPrefs(new PrefsViewImpl());
        binding.setViewmodel(new ViewModel());
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);
        processIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        processIntent(intent);
    }


    private void processIntent(final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "xdrip_plus_desert_sync_qr":
                        desertSyncSettings(null);
                        break;
                    case "xdrip_plus_keks_qr":
                        showGKey(null);
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        mInstance = null; // GC?
        super.onDestroy();
    }

    public synchronized void xdripPlusSyncSettings(View view) {
        prefsMap.put("custom_sync_key", prefs.getString("custom_sync_key", ""));
        prefsMap.put("use_custom_sync_key", Boolean.toString(prefs.getBoolean("use_custom_sync_key", false)));
        showQRCode();
    }

    public synchronized void connectionSettings(View view) {
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

    public synchronized void alarmSettings(View view) {
        prefsMap.clear();
        prefsMap.put("bg_alert_profile", prefs.getString("bg_alert_profile", "ascending"));
        prefsMap.put("smart_snoozing", Boolean.toString(prefs.getBoolean("smart_snoozing", true)));
        prefsMap.put("smart_alerting", Boolean.toString(prefs.getBoolean("smart_alerting", true)));
        prefsMap.put("calibration_notifications", Boolean.toString(prefs.getBoolean("calibration_notifications", true)));
        // need to support alert profiles
        showQRCode();
    }

    public synchronized void desertSyncSettings(View view) {
        prefsMap.clear();
        prefsMap.put("desert_sync_enabled", Boolean.toString(true));
        prefsMap.put("desert_sync_master_ip", RouteTools.getBestInterfaceAddress());
        prefsMap.put("dex_collection_method", "Follower");
        prefsMap.put("custom_sync_key", Pref.getString("custom_sync_key", ""));
        prefsMap.put("use_custom_sync_key", Boolean.toString(Pref.getBoolean("use_custom_sync_key", false)));
        prefsMap.put("desert_use_https", Boolean.toString(Pref.getBooleanDefaultFalse("desert_use_https")));
        prefsMap.put("xdrip_webservice_secret", Pref.getString("xdrip_webservice_secret", ""));
        showQRCode();
    }


    public boolean generateKeksBinaryPrefs() {
        binaryPrefsMap.clear();
        mapChecksum = "error";
        try {
            val digest = MessageDigest.getInstance("SHA-256");
            val tem = "keks_p";
            for (int i = 1; i < 4; i++) {
                val pn = tem + i;
                val bb = JoH.hexStringToByteArray(Pref.getStringDefaultBlank(pn));
                if (bb == null || bb.length == 0) {
                    Log.d(TAG, "Null or empty at: " + i);
                    return false;
                }
                val px = "b__" + pn;
                binaryPrefsMap.put(px, bb);
                digest.update(px.getBytes(StandardCharsets.UTF_8));
                digest.update(bb);
            }
            mapChecksum = JoH.bytesToHex(digest.digest());
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Got exception making binary prefs map " + e);
        }
        return false;
    }

    public synchronized void showGKey(View view) {
        showQRCode2("G Key settings\n\n" + Preferences.getMapKeysString(binaryPrefsMap).replace("\n", " ") + "\n\nHash: " + mapChecksum.substring(0, 16));
    }

    public static synchronized void uploadBytes(byte[] result, final int callback_option) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("uploadBytes", 1200000);
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
                                        Log.d(TAG, "Got unhandled reply: " + reply);
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


    private void showQRCode2(final String hint) {
        val bytes = serializeBinaryPrefsMap(binaryPrefsMap);
        Log.d(TAG, "QR bytes: " + bytes.length);
        val bytesc = JoH.compressBytesToBytes(bytes);
        Log.d(TAG, "QR bytes: " + bytesc.length);
        val scale = (getScreenWidth() > getScreenHeight()) ? 0.8d : 1;
        val desiredPixels = (int) (Math.min(getScreenWidth(), getScreenHeight()) * scale);
        try {
            val bitmap = createQRCodeBitmap(bytesc, desiredPixels, desiredPixels);
            binding.getViewmodel().showQr.set(false);
            binding.getViewmodel().narrative.set(JoH.dateTimeText(JoH.tsl()) + "\n" + Build.MANUFACTURER + " " + Build.MODEL + "\n" + hint);
            binding.getViewmodel().qrbitmap.set(new BitmapDrawable(xdrip.getAppContext().getResources(), bitmap));
            binding.getViewmodel().showQr.set(true);
        } catch (WriterException e) {
            Log.e(TAG, "ERROR: " + e);
        }
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


    public class ViewModel {
        public final ObservableBoolean showQr = new ObservableBoolean();
        public final ObservableField<Drawable> qrbitmap = new ObservableField<>();
        public final ObservableBoolean showGkey = new ObservableBoolean();
        public final ObservableField<String> narrative = new ObservableField<>();

        {
            showGkey.set(generateKeksBinaryPrefs());
        }

    }
}
