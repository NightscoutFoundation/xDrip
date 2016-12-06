package com.eveningoutpost.dexdrip;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.DisplayQRCode;
import com.eveningoutpost.dexdrip.utils.SdcardImportExport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jamorham on 11/01/16.
 */
class SensorCalibrations {
    @Expose
    Sensor sensor;
    
    @Expose
    List <Calibration> calibrations;
}

class NewCalibration {
    @Expose
    double bgValue; // Always in mgdl
    
    @Expose
    long timestamp;
    
    @Expose
    long offset;
    
    @Expose
    String uuid;
}

public class GcmActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String TASK_TAG_CHARGING = "charging";
    public static final String TASK_TAG_UNMETERED = "unmetered";
    private static final String TAG = "jamorham gcmactivity";
    public static double last_sync_request = 0;
    public static double last_sync_fill = 0;
    private static int bg_sync_backoff = 0;
    private static double last_ping_request = 0;
    public static AtomicInteger msgId = new AtomicInteger(1);
    public static String token = null;
    public static String senderid = null;
    public static final List<GCM_data> gcm_queue = new ArrayList<>();
    private static final Object queue_lock = new Object();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    public static boolean cease_all_activity = false;
    public static double last_ack = -1;
    public static double last_send = -1;
    public static double last_send_previous = -1;
    private static final long MAX_ACK_OUTSTANDING_MS = 3600000;
    private static int recursion_depth = 0;
    private static int last_bridge_battery = -1;
    private static final int MAX_RECURSION = 30;
    private static final int MAX_QUEUE_SIZE = 300;
    private static final int RELIABLE_MAX_PAYLOAD = 1800;
    private static final boolean d = false; // debug


    public static SensorCalibrations []  getSensorCalibrations(String json) {
        SensorCalibrations[] sensorCalibrations = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, SensorCalibrations[].class);
        Log.d(TAG, "After fromjson sensorCalibrations are " + sensorCalibrations.toString());
        return sensorCalibrations;
    }

    public static String sensorAndCalibrationsToJson(Sensor sensor, int limit) {
        SensorCalibrations[] sensorCalibrations = new SensorCalibrations[1];
        sensorCalibrations[0] = new SensorCalibrations();
        sensorCalibrations[0].sensor = sensor;
        sensorCalibrations[0].calibrations = Calibration.getCalibrationsForSensor(sensor, limit);
        if (d) Log.d(TAG, "calibrations size " + sensorCalibrations[0].calibrations.size());
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();

        String output = gson.toJson(sensorCalibrations);
        if (d) Log.d(TAG, "sensorAndCalibrationsToJson created the string " + output);
        return output;
    }
    
    public static NewCalibration getNewCalibration(String json) {
        NewCalibration []newCalibrationArray = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, NewCalibration[].class);
        if(newCalibrationArray != null) {
            Log.e(TAG, "After fromjson NewCalibration are " + newCalibrationArray.toString());
        } else {
            Log.e(TAG,"Error creating newCalibrationArray");
            return null;
        }
        return newCalibrationArray[0];
    }
    
    public static String newCalibrationToJson(double bgValue, String uuid) {
        NewCalibration newCalibrationArray[] = new NewCalibration[1];
        NewCalibration newCalibration = new NewCalibration();
        newCalibration.bgValue = bgValue;
        newCalibration.uuid = uuid;
        newCalibration.timestamp = JoH.tsl();
        newCalibration.offset = 0;
        newCalibrationArray[0] = newCalibration;
        
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        
        String output =  gson.toJson(newCalibrationArray);
        Log.d(TAG, "newCalibrationToJson Created the string " + output);
        return output;
    }

    public static void upsertSensorCalibratonsFromJson(String json) {
        Log.i(TAG, "upsertSensorCalibratonsFromJson called");
        SensorCalibrations [] sensorCalibrations = getSensorCalibrations(json);
        for (SensorCalibrations SensorCalibration : sensorCalibrations ) {
            Sensor.upsertFromMaster(SensorCalibration.sensor);
            for (Calibration calibration : SensorCalibration.calibrations) {
                Log.d(TAG, "upsertSensorCalibratonsFromJson updating calibration " + calibration.uuid);
                Calibration.upsertFromMaster(calibration);
            }
        }
    }
    
    public static synchronized void queueAction(String reference) {
        synchronized (queue_lock) {
            Log.d(TAG, "Received ACK, Queue Size: " + GcmActivity.gcm_queue.size() + " " + reference);
            last_ack = JoH.ts();
            for (GCM_data datum : gcm_queue) {
                String thisref = datum.bundle.getString("action") + datum.bundle.getString("payload");
                if (thisref.equals(reference)) {
                    gcm_queue.remove(gcm_queue.indexOf(datum));
                    Log.d(TAG, "Removing acked queue item: " + reference);
                    break;
                }
            }
            queueCheckOld(xdrip.getAppContext());
        }
    }

    public static void queueCheckOld(Context context) {
        queueCheckOld(context, false);
    }

    public static void queueCheckOld(Context context, boolean recursive) {

        if (context == null) {
            Log.e(TAG, "Can't process old queue as null context");
            return;
        }

        final double MAX_QUEUE_AGE = (5 * 60 * 60 * 1000); // 5 hours
        final double MIN_QUEUE_AGE = (15000);
        final double MAX_RESENT = 10;
        Double timenow = JoH.ts();
        boolean queuechanged = false;
        if (!recursive) recursion_depth = 0;
        synchronized (queue_lock) {
            for (GCM_data datum : gcm_queue) {
                if ((timenow - datum.timestamp) > MAX_QUEUE_AGE
                        || datum.resent > MAX_RESENT) {
                    queuechanged = true;
                    Log.i(TAG, "Removing old unacknowledged queue item: resent: " + datum.resent);
                    gcm_queue.remove(gcm_queue.indexOf(datum));
                    break;
                } else if (timenow - datum.timestamp > MIN_QUEUE_AGE) {
                    try {
                        Log.i(TAG, "Resending unacknowledged queue item: " + datum.bundle.getString("action") + datum.bundle.getString("payload"));
                        datum.resent++;
                        GoogleCloudMessaging.getInstance(context).send(senderid + "@gcm.googleapis.com", Integer.toString(msgId.incrementAndGet()), datum.bundle);
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception during resend: " + e.toString());
                    }
                    break;
                }
            }
        }
        if (queuechanged) {
            recursion_depth++;
            if (recursion_depth < MAX_RECURSION) {
                queueCheckOld(context, true);
            } else {
                Log.e(TAG, "Max recursion exceeded!");
            }
        }
    }

    private static String sendMessage(final String action, final String payload) {
        if (cease_all_activity) return null;
        return sendMessage(myIdentity(), action, payload);
    }

    private static String sendMessage(final String identity, final String action, final String payload) {
        if (cease_all_activity) return null;
        if (identity == null) return null;
        new Thread() {
            @Override
            public void run() {
                sendMessageNow(identity, action, payload);
            }
        }.start();
        return "sent async";
    }

    public synchronized static void syncBGReading(BgReading bgReading) {
        Log.d(TAG, "syncBGReading called");
        if (JoH.ratelimit("gcm-bgs-batch", 15)) {
            GcmActivity.sendMessage("bgs", bgReading.toJSON(true));
        } else {
            PersistentStore.appendString("gcm-bgs-batch-queue", bgReading.toJSON(false), "^");
            PersistentStore.setLong("gcm-bgs-batch-time", JoH.tsl());
            processBgsBatch(false);
        }
    }

    private synchronized static void processBgsBatch(boolean send_now) {
        final String value = PersistentStore.getString("gcm-bgs-batch-queue");
        Log.d(TAG, "Processing BgsBatch: length: " + value.length() + " now:" + send_now);
        if ((send_now) || (value.length() > 700)) {
            if (value.length() > 0) {
                PersistentStore.setString("gcm-bgs-batch-queue", "");
                GcmActivity.sendMessage("bgs", value);
            }
            Log.d(TAG, "Sent batch");
        } else {
            JoH.runOnUiThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    if (JoH.msSince(PersistentStore.getLong("gcm-bgs-batch-time")) > 4000) {
                        Log.d(TAG, "Progressing BGSbatch due to timeout");
                        processBgsBatch(true);
                    }
                }
            }, 5000);
        }
    }

    public static synchronized void syncSensor(Sensor sensor, boolean forceSend) {
        Log.d(TAG,"syncsensor backtrace: "+JoH.backTrace());
        Log.i(TAG, "syncSensor called");
        if (sensor == null) {
            Log.e(TAG, "syncSensor sensor is null");
            return;
        }
        if ((!forceSend) && !JoH.pratelimit("GcmSensorCalibrationsUpdate", 300)) {
            Log.i(TAG, "syncSensor not sending data, because of rate limiter");
            return;
        }

        // automatically find a suitable volume of payload data
        for (int limit = 9; limit > 0; limit--) {
            final String json = sensorAndCalibrationsToJson(sensor, limit);
            if (d) Log.d(TAG, "sensor json size: limit: " + limit + " len: " + CipherUtils.compressEncryptString(json).length());
            if (CipherUtils.compressEncryptString(json).length() <= RELIABLE_MAX_PAYLOAD) {
                final String json_hash = CipherUtils.getSHA256(json);
                if (!forceSend || !PersistentStore.getString("last-syncsensor-json").equals(json_hash)) {
                    PersistentStore.setString("last-syncsensor-json", json_hash);
                    GcmActivity.sendMessage(GcmActivity.myIdentity(), "sensorupdate", json);
                } else {
                    Log.d(TAG, "syncSensor: data is duplicate of last data: " + json);
                    break;
                }
                break; // send only one
            }
        }
    }

    public static void requestPing() {
        if ((JoH.ts() - last_ping_request) > (60 * 1000 * 15)) {
            last_ping_request = JoH.ts();
            Log.d(TAG, "Sending ping");
            if (JoH.pratelimit("gcm-ping",1199)) GcmActivity.sendMessage("ping", "");
        } else {
            Log.d(TAG, "Already requested ping recently");
        }
    }

    public static void sendLocation(final String location) {
        if (JoH.pratelimit("gcm-plu", 180)) {
            GcmActivity.sendMessage("plu", location);
        }
    }

    public static void sendSensorBattery(final int battery) {
        if (JoH.pratelimit("gcm-sbu", 300)) {
            GcmActivity.sendMessage("sbu", Integer.toString(battery));
        }
    }

    public static void sendBridgeBattery(final int battery) {
        if (battery != last_bridge_battery) {
            if (JoH.pratelimit("gcm-bbu", 1800)) {
                GcmActivity.sendMessage("bbu", Integer.toString(battery));
                last_bridge_battery = battery;
            }
        }
    }

    private static void sendRealSnoozeToRemote() {
        if (JoH.pratelimit("gcm-sra", 60)) {
            String wifi_ssid = JoH.getWifiSSID();
            if (wifi_ssid == null) wifi_ssid = "";
            sendMessage("sra", Long.toString(JoH.tsl()) + "^" + JoH.base64encode(wifi_ssid));
        }
    }

    public static void sendSnoozeToRemote() {
        if ((Home.get_master() || Home.get_follower()) && (Home.getPreferencesBooleanDefaultFalse("send_snooze_to_remote"))
                && (JoH.pratelimit("gcm-sra-maybe", 5))) {
            if (Home.getPreferencesBooleanDefaultFalse("confirm_snooze_to_remote")) {
                Home.startHomeWithExtra(xdrip.getAppContext(), Home.HOME_FULL_WAKEUP, "1");
                Home.startHomeWithExtra(xdrip.getAppContext(), Home.SNOOZE_CONFIRM_DIALOG, "");
            } else {
                sendRealSnoozeToRemote();
                UserError.Log.ueh(TAG, "Sent snooze to remote");
            }
        }
    }

    public static void sendSnoozeToRemoteWithConfirm(final Context context) {
        final long when = JoH.tsl();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Remote Snooze");
        builder.setMessage("Are you sure you wish to snooze all other devices in your sync group?");
        builder.setPositiveButton("YES, send it!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ((JoH.tsl() - when) < 120000) {
                    sendRealSnoozeToRemote();
                    UserError.Log.ueh(TAG, "Sent snooze to remote after confirmation");
                } else {
                    JoH.static_toast_long("Took too long to confirm! Ignoring!");
                    UserError.Log.ueh(TAG, "Ignored snooze confirmation as took > 2 minutes to confirm!");
                }
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void sendMotionUpdate(final long timestamp, final int activity) {
        if (JoH.pratelimit("gcm-amu", 5)) {
            sendMessage("amu", Long.toString(timestamp) + "^" + Integer.toString(activity));
        }
    }


    public static void requestBGsync() {
        if (token != null) {
            if ((JoH.ts() - last_sync_request) > (60 * 1000 * (5 + bg_sync_backoff))) {
                last_sync_request = JoH.ts();
                if (JoH.pratelimit("gcm-bfr",299)) GcmActivity.sendMessage("bfr", "");
                bg_sync_backoff++;
            } else {
                Log.d(TAG, "Already requested BGsync recently, backoff: " + bg_sync_backoff);
                if (JoH.ratelimit("check-queue", 20)) {
                    queueCheckOld(xdrip.getAppContext());
                }
            }
        } else {
            Log.d(TAG, "No token for BGSync");
        }
    }

    public static void syncBGTable2() {
        new Thread() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = JoH.getWakeLock("syncBGTable", 300000);
                if ((JoH.ts() - last_sync_fill) > (60 * 1000 * (5 + bg_sync_backoff))) {
                    last_sync_fill = JoH.ts();
                    bg_sync_backoff++;

                    // Since this is a big update, also update sensor and calibrations
                    syncSensor(Sensor.currentSensor(), true);

                    final List<BgReading> bgReadings = BgReading.latestForGraph(300, JoH.ts() - (24 * 60 * 60 * 1000));

                    StringBuilder stringBuilder = new StringBuilder();
                    for (BgReading bgReading : bgReadings) {
                        String myrecord = bgReading.toJSON(false);
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append("^");
                        }
                        stringBuilder.append(myrecord);
                    }
                    final String mypacket = stringBuilder.toString();
                    Log.d(TAG, "Total BGreading sync packet size: " + mypacket.length());
                    if (mypacket.length() > 0) {
                        DisplayQRCode.uploadBytes(mypacket.getBytes(Charset.forName("UTF-8")), 2);
                    } else {
                        Log.i(TAG, "Not uploading data due to zero length");
                    }
                } else {
                    Log.d(TAG, "Ignoring recent sync request, backoff: " + bg_sync_backoff);
                }
                JoH.releaseWakeLock(wl);
            }
        }.start();
    }

    // callback function
    public static void backfillLink(String id, String key) {
        Log.d(TAG, "sending bfb message: " + id);
        sendMessage("bfb", id + "^" + key);
    }

    public static void processBFPbundle(String bundle) {
        String[] bundlea = bundle.split("\\^");
        for (String bgr : bundlea) {
            BgReading.bgReadingInsertFromJson(bgr, false);
        }
        GcmActivity.requestSensorBatteryUpdate();
        Home.staticRefreshBGCharts();
    }

    public static void requestSensorBatteryUpdate() {
        if (Home.get_follower() && JoH.pratelimit("SensorBatteryUpdateRequest", 1200)) {
            Log.d(TAG, "Requesting Sensor Battery Update");
            GcmActivity.sendMessage("sbr", ""); // request sensor battery update
        }
    }
    
    public static void requestSensorCalibrationsUpdate() {
        if (Home.get_follower() && JoH.pratelimit("SensorCalibrationsUpdateRequest", 300)) {
            Log.d(TAG, "Requesting Sensor and calibrations Update");
            GcmActivity.sendMessage("sensor_calibrations_update", "");
        }
    }

    public static void pushTreatmentAsync(final Treatments thistreatment) {
        new Thread() {
            @Override
            public void run() {
                push_treatment(thistreatment);
            }
        }.start();
    }

    private static void push_treatment(Treatments thistreatment) {
        String json = thistreatment.toJSON();
        sendMessage(myIdentity(), "nt", json);
    }

    public static void send_ping_reply() {
        Log.d(TAG, "Sending ping reply");
        sendMessage(myIdentity(), "q", "");
    }

    public static void push_delete_all_treatments() {
        Log.i(TAG, "Sending push for delete all treatments");
        sendMessage(myIdentity(), "dat", "");
    }

    public static void push_delete_treatment(Treatments treatment) {
        Log.i(TAG, "Sending push for specific treatment");
        sendMessage(myIdentity(), "dt", treatment.uuid);
    }

    public static String myIdentity() {
        // TODO prefs override possible
        return GoogleDriveInterface.getDriveIdentityString();
    }

    public static void pushTreatmentFromPayloadString(String json) {
        if (json.length() < 3) return;
        Log.d(TAG, "Pushing json from GCM: " + json);
        Treatments.pushTreatmentFromJson(json);
    }

    public static void pushCalibration(String bg_value, String seconds_ago) {
        if ((bg_value.length() == 0) || (seconds_ago.length() == 0)) return;
        if (Home.get_master()) {
            // For master, we now send the entire table, no need to send this specific table each time
            return;
        }
        String currenttime = Double.toString(new Date().getTime());
        String tosend = currenttime + " " + bg_value + " " + seconds_ago;
        sendMessage(myIdentity(), "cal", tosend);
    }
    
    public static void pushCalibration2(double bgValue, String uuid) {
        Log.i(TAG, "pushCalibration2 called");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        final String unit = prefs.getString("units", "mgdl");
        
        if (unit.compareTo("mgdl") != 0) {
            bgValue = bgValue * Constants.MMOLL_TO_MGDL;
        }
        
        if ((bgValue < 40) || (bgValue > 400)) {
            Log.wtf(TAG, "Invalid out of range calibration glucose mg/dl value of: " + bgValue);
            JoH.static_toast_long("Calibration out of range: " + bgValue + " mg/dl");
            return ;
        }
        String json = newCalibrationToJson(bgValue, uuid);
        GcmActivity.sendMessage(myIdentity(), "cal2", json);
    }

    public static void clearLastCalibration() {
        sendMessage(myIdentity(), "clc", "");
    }

    private static synchronized String sendMessageNow(String identity, String action, String payload) {

        Log.i(TAG, "Sendmessage called: " + identity + " " + action + " " + payload);
        String msg;
        try {

            if (xdrip.getAppContext() == null) {
                Log.e(TAG, "mContext is null cannot sendMessage");
                return "";
            }

            if (identity == null) {
                Log.e(TAG, "identity is null cannot sendMessage");
                return "";
            }

            final Bundle data = new Bundle();
            data.putString("action", action);
            data.putString("identity", identity);

            if(action.equals("sensorupdate") ) {
                final String ce_payload = CipherUtils.compressEncryptString(payload);
                Log.i(TAG, "sensor length CipherUtils.encryptBytes ce_payload length: " + ce_payload.length());
                data.putString("payload", ce_payload);
                if (d) Log.d(TAG, "sending data len " + ce_payload.length()+ " " + ce_payload);
            } else {
                if (payload.length() > 0) {
                    data.putString("payload", CipherUtils.encryptString(payload));
                } else {
                    data.putString("payload", "");
                }
            }

            if (gcm_queue.size() < MAX_QUEUE_SIZE) {
                if (shouldAddQueue(data)) {
                    gcm_queue.add(new GCM_data(data));
                }
            } else {
                Log.e(TAG, "Queue size exceeded");
                Home.toaststaticnext("Maximum Sync Queue size Exceeded!");
            }
            final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(xdrip.getAppContext());
            if (token == null) {
                Log.e(TAG, "GCM token is null - cannot sendMessage");
                return "";
            }
            String messageid = Integer.toString(msgId.incrementAndGet());
            gcm.send(senderid + "@gcm.googleapis.com", messageid, data);
            if (last_ack == -1) last_ack = JoH.ts();
            last_send_previous = last_send;
            last_send = JoH.ts();
            msg = "Sent message OK " + messageid;
        } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
        }
        Log.d(TAG, "Return msg in SendMessage: " + msg);
        return msg;
    }

    private static boolean shouldAddQueue(Bundle data) {
        final String action = data.getString("action");
        if (action == null) return false;
        switch (action) {
            // one shot action types where multi queuing is not needed
            case "ping":
            case "sbr":
            case "bfr":
                synchronized (queue_lock) {
                    for (GCM_data qdata : gcm_queue) {
                        try {
                            if (qdata.bundle.getString("action").equals(action)) {
                                Log.d(TAG, "Skipping queue add for duplicate action: " + action);
                                return false;
                            }
                        } catch (NullPointerException e) {
                            //
                        }
                    }
                }
                return true;
            default:
                return true;
        }
    }

    public void tryGCMcreate() {
        Log.d(TAG, "try GCMcreate");
        if (cease_all_activity) return;
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.i(TAG, "Token retrieved and sent");
                } else {
                    Log.e(TAG, "Error with token");
                }
            }
        };

        if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        } else {
            cease_all_activity = true;
            final String msg = "ERROR: Connecting to Google Services - check google login or reboot?";
            JoH.static_toast(this, msg, Toast.LENGTH_LONG);
            Home.toaststaticnext(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            if (Home.getPreferencesBooleanDefaultFalse("disable_all_sync")) {
                cease_all_activity = true;
                Log.d(TAG, "Sync services disabled");
            }
            if (cease_all_activity) {
                finish();
                return;
            }
            Log.d(TAG, "onCreate");
            tryGCMcreate();
        } catch (Exception e) {
            Log.e(TAG, "Got exception in GCMactivity Oncreate: ", e);
        } finally {
            try {
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Exception when finishing: " + e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cease_all_activity) return;
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(PreferencesNames.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Exception onPause: ", e);
        }
        super.onPause();
    }

    public static void checkSync(final Context context) {
        if ((GcmActivity.last_ack > -1) && (GcmActivity.last_send_previous > 0)) {
            if (GcmActivity.last_send_previous > GcmActivity.last_ack) {
                if (Home.getPreferencesLong("sync_warning_never", 0) == 0) {
                    if (PreferencesNames.SYNC_VERSION.equals("1") && JoH.isOldVersion(context)) {
                        final double since_send = JoH.ts() - GcmActivity.last_send_previous;
                        if (since_send > 60000) {
                            final double ack_outstanding = JoH.ts() - GcmActivity.last_ack;
                            if (ack_outstanding > MAX_ACK_OUTSTANDING_MS) {
                                if (JoH.ratelimit("ack-failure", 7200)) {
                                    if (JoH.isAnyNetworkConnected()) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                        builder.setTitle("Possible Sync Problem");
                                        builder.setMessage("It appears we haven't been able to send/receive sync data for the last: " + JoH.qs(ack_outstanding / 60000, 0) + " minutes\n\nDo you want to perform a reset of the sync system?");
                                        builder.setPositiveButton("YES, Do it!", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                JoH.static_toast(context, "Resetting...", Toast.LENGTH_LONG);
                                                SdcardImportExport.forceGMSreset();
                                            }
                                        });
                                        builder.setNeutralButton("Maybe Later", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                        builder.setNegativeButton("NO, Never", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                Home.setPreferencesLong("sync_warning_never", (long) JoH.ts());
                                            }
                                        });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */

    private boolean checkPlayServices() {
        return checkPlayServices(this, null);
    }

    public static boolean checkPlayServices(Context context, Activity activity) {
        if (cease_all_activity) return false;
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            try {
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    if (activity != null) {
                        apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                                .show();
                    } else {
                        if (JoH.ratelimit(Home.GCM_RESOLUTION_ACTIVITY, 60)) {
                            Home.startHomeWithExtra(context, Home.GCM_RESOLUTION_ACTIVITY, "1");
                        } else {
                            Log.e(TAG, "Ratelimit exceeded for " + Home.GCM_RESOLUTION_ACTIVITY);
                        }
                    }
                } else {
                    final String msg = "This device is not supported for play services.";
                    Log.i(TAG, msg);
                    JoH.static_toast_long(msg);
                    cease_all_activity = true;
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resolving google play - probably no google");
                cease_all_activity = true;
            }
            return false;
        }
        return true;
    }

    private static class GCM_data {
        public Bundle bundle;
        public Double timestamp;
        private int resent;

        private GCM_data(Bundle data) {
            bundle = data;
            timestamp = JoH.ts();
            resent = 0;
        }
    }
}

