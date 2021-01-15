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
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.DesertSync;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.RollCall;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.InstalledApps;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.DisplayQRCode;
import com.eveningoutpost.dexdrip.utils.SdcardImportExport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.primitives.Bytes;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
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

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Created by jamorham on 11/01/16.
 */


public class GcmActivity extends FauxActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String TASK_TAG_CHARGING = "charging";
    static final String TASK_TAG_UNMETERED = "unmetered";
    private static final String TAG = "jamorham gcmactivity";
    public static long last_sync_request = 0;
    public static long last_sync_fill = 0;
    private static int bg_sync_backoff = 0;
    private static long last_ping_request = 0;
    private static long last_rlcl_request = 0;
    private static long cool_down_till = 0;
    public static AtomicInteger msgId = new AtomicInteger(1);
    public static String token = null;
    public static String senderid = null;
    public static final List<GCM_data> gcm_queue = new ArrayList<>();
    private static final Object queue_lock = new Object();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    public static boolean cease_all_activity = false;
    private static boolean cease_all_checked = false;
    public static volatile long last_ack = -1;
    public static volatile long last_send = -1;
    public static volatile long last_send_previous = -1;
    private static final long MAX_ACK_OUTSTANDING_MS = 3600000;
    private static int recursion_depth = 0;
    private static int last_bridge_battery = -1;
    private static int last_parakeet_battery = -1;
    private static final int MAX_RECURSION = 30;
    private static final int MAX_QUEUE_SIZE = 300;
    private static final int RELIABLE_MAX_PAYLOAD = 1800;
    private static final int RELIABLE_MAX_BINARY_PAYLOAD = 1400;
    private static final boolean d = false; // debug


    private static SensorCalibrations[] getSensorCalibrations(String json) {
        SensorCalibrations[] sensorCalibrations = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, SensorCalibrations[].class);
        Log.d(TAG, "After fromjson sensorCalibrations are " + sensorCalibrations.toString());
        return sensorCalibrations;
    }

    private static String sensorAndCalibrationsToJson(Sensor sensor, int limit) {
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

    static NewCalibration getNewCalibration(String json) {
        NewCalibration[] newCalibrationArray = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, NewCalibration[].class);
        if (newCalibrationArray != null) {
            Log.e(TAG, "After fromjson NewCalibration are " + newCalibrationArray.toString());
        } else {
            Log.e(TAG, "Error creating newCalibrationArray");
            return null;
        }
        return newCalibrationArray[0];
    }

    private static String newCalibrationToJson(double bgValue, String uuid, long offset) {
        NewCalibration newCalibrationArray[] = new NewCalibration[1];
        NewCalibration newCalibration = new NewCalibration();
        newCalibration.bgValue = bgValue;
        newCalibration.uuid = uuid;
        newCalibration.timestamp = JoH.tsl();
        newCalibration.offset = offset;
        newCalibrationArray[0] = newCalibration;

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();

        String output = gson.toJson(newCalibrationArray);
        Log.d(TAG, "newCalibrationToJson Created the string " + output);
        return output;
    }

    static void upsertSensorCalibratonsFromJson(String json) {
        Log.i(TAG, "upsertSensorCalibratonsFromJson called");
        SensorCalibrations[] sensorCalibrations = getSensorCalibrations(json);
        for (SensorCalibrations SensorCalibration : sensorCalibrations) {
            Sensor.upsertFromMaster(SensorCalibration.sensor);
            for (Calibration calibration : SensorCalibration.calibrations) {
                Log.d(TAG, "upsertSensorCalibratonsFromJson updating calibration " + calibration.uuid);
                Calibration.upsertFromMaster(calibration);
            }
        }
    }

    static synchronized void queueAction(String reference) {
        synchronized (queue_lock) {
            Log.d(TAG, "Received ACK, Queue Size: " + GcmActivity.gcm_queue.size() + " " + reference);
            last_ack = JoH.tsl();
            for (GCM_data datum : gcm_queue) {
                String thisref = datum.bundle.getString("action") + datum.bundle.getString("payload");
                if (thisref.equals(reference)) {
                    gcm_queue.remove(gcm_queue.indexOf(datum));
                    Log.d(TAG, "Removing acked queue item: " + reference + " Queue size now: " + gcm_queue.size());
                    break;
                }
            }
            queueCheckOld(xdrip.getAppContext());
        }
    }

    static void queueCheckOld(Context context) {
        queueCheckOld(context, false);
    }

    private static void queueCheckOld(Context context, boolean recursive) {

        if (context == null) {
            Log.e(TAG, "Can't process old queue as null context");
            return;
        }

        if (overHeated()) {
            Log.e(TAG, "Can't process old queue as in cool down state");
            return;
        }

        final long MAX_QUEUE_AGE = (5 * 60 * 60 * 1000); // 5 hours
        final long MIN_QUEUE_AGE = (30000);
        final long MAX_RESENT = 7;
        final long timenow = JoH.tsl();
        boolean queuechanged = false;
        if (!recursive) recursion_depth = 0;
        synchronized (queue_lock) {
            for (GCM_data datum : gcm_queue) {
                if (datum != null) {
                    if (overHeated()) break;
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
                } else {
                    UserError.Log.wtf(TAG, "Null datum in gcm_queue - should be impossible!");
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

    private static void checkCease() {
        if ((!cease_all_checked) && (!cease_all_activity)) {
            cease_all_activity = Pref.getBooleanDefaultFalse("disable_all_sync");
            cease_all_checked = true;
        }
    }

    private static String sendMessage(final String action, final String payload) {
        return sendMessage(myIdentity(), action, payload);
    }

    private static String sendMessage(final String action, final byte[] bpayload) {
        return sendMessage(myIdentity(), action, bpayload);
    }

    private static String sendMessage(final String identity, final String action, final String payload) {
        checkCease();
        if (cease_all_activity) return null;
        if (identity == null) return null;
        new Thread() {
            @Override
            public void run() {
                sendMessageNow(identity, action, payload, null);
            }
        }.start();
        return "sent async";
    }

    private static String sendMessage(final String identity, final String action, final byte[] bpayload) {
        checkCease();
        if (cease_all_activity) return null;
        if (identity == null) return null;
        new Thread() {
            @Override
            public void run() {
                sendMessageNow(identity, action, "", bpayload);
            }
        }.start();
        return "sent async";
    }

    public synchronized static void syncBGReading(BgReading bgReading) {
        if (bgReading == null) {
            UserError.Log.wtf(TAG, "Cannot sync null bgreading - should never occur");
            return;
        }
        Log.d(TAG, "syncBGReading called");
        if (JoH.ratelimit("gcm-bgs-batch", 15)) {
            GcmActivity.sendMessage("bgs", bgReading.toJSON(true));
        } else {
            PersistentStore.appendBytes("gcm-bgs-batch-queue", bgReading.toMessage());
            PersistentStore.setLong("gcm-bgs-batch-time", JoH.tsl());
            processBgsBatch(false);
        }
    }

    // called only from interactive or evaluated new data
    public synchronized static void syncBloodTests() {
        Log.d(TAG, "syncBloodTests called");
        if (Home.get_master_or_follower()) {
            if (JoH.ratelimit("gcm-btmm-send", 4)) {
                final byte[] this_btmm = BloodTest.toMultiMessage(BloodTest.last(12));
                if (JoH.differentBytes("gcm-btmm-last-send", this_btmm)) {
                    sendMessage("btmm", JoH.compressBytesforPayload(this_btmm));
                    Home.staticRefreshBGCharts();
                } else {
                    Log.d(TAG, "btmm message is identical to previously sent");
                }
            }
        }
    }

    private synchronized static void processBgsBatch(boolean send_now) {
        final byte[] value = PersistentStore.getBytes("gcm-bgs-batch-queue");
        Log.d(TAG, "Processing BgsBatch: length: " + value.length + " now:" + send_now);
        if ((send_now) || (value.length > (RELIABLE_MAX_BINARY_PAYLOAD - 100))) {
            if (value.length > 0) {
                PersistentStore.setString("gcm-bgs-batch-queue", "");
                GcmActivity.sendMessage("bgmm", value);
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
        Log.d(TAG, "syncsensor backtrace: " + JoH.backTrace());
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
            if (d)
                Log.d(TAG, "sensor json size: limit: " + limit + " len: " + CipherUtils.compressEncryptString(json).length());
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
        if ((JoH.tsl() - last_ping_request) > (60 * 1000 * 15)) {
            last_ping_request = JoH.tsl();
            Log.d(TAG, "Sending ping");
            if (JoH.pratelimit("gcm-ping", 1199))
                GcmActivity.sendMessage("ping", new RollCall().populate().toS());
        } else {
            Log.d(TAG, "Already requested ping recently");
        }
    }

    public static void desertPing() {
        if (JoH.pratelimit("gcm-desert-ping", 300)) {
            GcmActivity.sendMessage("ping", new RollCall().populate().toS());
        } else {
            Log.d(TAG, "Already requested desert ping recently");
        }
    }

    public static void requestRollCall() {
        if (JoH.tsl() - last_rlcl_request > (60 * 1000)) {
            last_rlcl_request = JoH.tsl();
            if (JoH.pratelimit("gcm-rlcl", 3600))
                GcmActivity.sendMessage("rlcl", new RollCall().populate().toS());
        }
    }

    static void sendLocation(final String location) {
        if (JoH.pratelimit("gcm-plu", 180)) {
            GcmActivity.sendMessage("plu", location);
        }
    }

    public static void sendSensorBattery(final int battery) {
        if (JoH.pratelimit("gcm-sbu", 3600)) {
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

    public static void sendParakeetBattery(final int battery) {
        if (battery != last_parakeet_battery) {
            if (JoH.pratelimit("gcm-pbu", 1800)) {
                GcmActivity.sendMessage("pbu", Integer.toString(battery));
                last_parakeet_battery = battery;
            }
        }
    }

    public static void sendNotification(String title, String message) {
        if (JoH.pratelimit("gcm-not", 30)) {
            GcmActivity.sendMessage("not", title.replaceAll("\\^", "") + "^" + message.replaceAll("\\^", ""));
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
        if ((Home.get_master() || Home.get_follower()) && (Pref.getBooleanDefaultFalse("send_snooze_to_remote"))
                && (JoH.pratelimit("gcm-sra-maybe", 5))) {
            if (Pref.getBooleanDefaultFalse("confirm_snooze_to_remote")) {
                Home.startHomeWithExtra(xdrip.getAppContext(), Home.HOME_FULL_WAKEUP, "1");
                Home.startHomeWithExtra(xdrip.getAppContext(), Home.SNOOZE_CONFIRM_DIALOG, "");
            } else {
                sendRealSnoozeToRemote();
                UserError.Log.ueh(TAG, "Sent snooze to remote");
            }
        }
    }

    static void sendSnoozeToRemoteWithConfirm(final Context context) {
        final long when = JoH.tsl();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(xdrip.getAppContext().getString(R.string.confirm_remote_snooze));
        builder.setMessage(xdrip.getAppContext().getString(R.string.are_you_sure_you_wish_to_snooze_all_other_devices_in_your_sync_group));
        builder.setPositiveButton(xdrip.getAppContext().getString(R.string.yes_send_it), new DialogInterface.OnClickListener() {
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

        builder.setNegativeButton(xdrip.getAppContext().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (alert.isShowing()) {
                        alert.dismiss();
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Got exception trying to auto-dismiss dialog: " + e);
                }
            }
        };

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 120000);


    }

    public static void sendMotionUpdate(final long timestamp, final int activity) {
        if (JoH.pratelimit("gcm-amu", 5)) {
            sendMessage("amu", Long.toString(timestamp) + "^" + Integer.toString(activity));
        }
    }

    public static void sendPumpStatus(String json) {
        if (JoH.pratelimit("gcm-psu", 180)) {
            sendMessage("psu", json);
        }
    }

    public static void sendNanoStatusUpdate(final String prefix, final String json) {
        if (JoH.pratelimit("gcm-nscu" + prefix, 30)) {
            UserError.Log.d(TAG, "Sending nano status update: " + prefix + " " + json);
            sendMessage("nscu" + prefix, json);
        }
    }

    public static void sendMimeoGraphUpdate(final String json) {
        if (JoH.pratelimit("gcm-mimg", 180)) {
            UserError.Log.d(TAG, "Sending mimeograph key update: " + json);
            sendMessage("mimg", json);
        }
    }


    public static void requestBGsync() {
        if (token != null) {
            if ((JoH.tsl() - last_sync_request) > (60 * 1000 * (5 + bg_sync_backoff))) {
                last_sync_request = JoH.tsl();
                final BgReading bgReading = BgReading.last();
                if (JoH.pratelimit("gcm-bfr", 299)) {
                    GcmActivity.sendMessage("bfr", bgReading != null ? "" + bgReading.timestamp : "");
                }
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

    static synchronized void syncBGTable2() {
        if (!Sensor.isActive()) return;
        new Thread() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = JoH.getWakeLock("syncBGTable", 300000);
                //if ((JoH.ts() - last_sync_fill) > (60 * 1000 * (5 + bg_sync_backoff))) {
                if (JoH.pratelimit("last-sync-fill", 60 * (5 + bg_sync_backoff))) {
                    last_sync_fill = JoH.tsl();
                    bg_sync_backoff++;

                    // Since this is a big update, also update sensor and calibrations
                    syncSensor(Sensor.currentSensor(), true);

                    final List<BgReading> bgReadings = BgReading.latestForGraph(300, JoH.tsl() - (24 * 60 * 60 * 1000));

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

    static void processBFPbundle(String bundle) {
        String[] bundlea = bundle.split("\\^");
        for (String bgr : bundlea) {
            BgReading.bgReadingInsertFromJson(bgr, false);
        }
        GcmActivity.requestSensorBatteryUpdate();
        Home.staticRefreshBGCharts();
    }

    static void requestSensorBatteryUpdate() {
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
        if ((thistreatment.uuid == null) || (thistreatment.uuid.length() < 5)) return;
        final String json = thistreatment.toJSON();
        sendMessage(myIdentity(), "nt", json);
    }

    static void send_ping_reply() {
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

    public static void push_stop_master_sensor() {
        sendMessage("ssom", "challenge string");
    }

    public static void push_start_master_sensor() {
        sendMessage("rsom", JoH.tsl() + "");
    }

    public static void push_external_status_update(long timestamp, String statusLine) {
        if (JoH.ratelimit("gcm-esup", 30)) {
            sendMessage("esup", timestamp + "^" + statusLine);
        }
    }

    static String myIdentity() {
        // TODO prefs override possible
        return GoogleDriveInterface.getDriveIdentityString();
    }

    static void pushTreatmentFromPayloadString(String json) {
        if (json.length() < 3) return;
        Log.d(TAG, "Pushing json from GCM: " + json);
        Treatments.pushTreatmentFromJson(json);
    }

    static void pushCalibration(String bg_value, String seconds_ago) {
        if ((bg_value.length() == 0) || (seconds_ago.length() == 0)) return;
        if (Home.get_master()) {
            // For master, we now send the entire table, no need to send this specific table each time
            return;
        }
        if (Home.get_follower()) {
            final String currenttime = Double.toString(new Date().getTime());
            final String tosend = currenttime + " " + bg_value + " " + seconds_ago;
            sendMessage(myIdentity(), "cal", tosend);
        }
    }

    static void pushCalibration2(double bgValue, String uuid, long offset) {
        Log.i(TAG, "pushCalibration2 called: " + JoH.qs(bgValue, 1) + " " + uuid + " " + offset);
        if (Home.get_master_or_follower()) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
            final String unit = prefs.getString("units", "mgdl");

            if (unit.compareTo("mgdl") != 0) {
                bgValue = bgValue * Constants.MMOLL_TO_MGDL;
            }

            if ((bgValue < 40) || (bgValue > 400)) {
                Log.wtf(TAG, "Invalid out of range calibration glucose mg/dl value of: " + bgValue);
                JoH.static_toast_long("Calibration out of range: " + bgValue + " mg/dl");
                return;
            }
            final String json = newCalibrationToJson(bgValue, uuid, offset);
            GcmActivity.sendMessage(myIdentity(), "cal2", json);
        }
    }
    public static void pushLibreBlock(String libreBlock) {
        Log.i(TAG, "libreBlock called: " + libreBlock);
        if (!Home.get_master()) {
            return;
        }
        if (!JoH.pratelimit("libre-allhouse", 10)) {
            // Do not create storm of packets.
            Log.e(TAG, "Rate limited start libre-allhouse");
            return;
        }

        GcmActivity.sendMessage(myIdentity(), "libreBlock", libreBlock);
    }

    public static void clearLastCalibration(String uuid) {
        sendMessage(myIdentity(), "clc", uuid);
    }

    private static synchronized String sendMessageNow(String identity, String action, String payload, byte[] bpayload) {

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

            if (overHeated()) {
                UserError.Log.e(TAG, "Cannot send message due to cool down period: " + action + " till: " + JoH.dateTimeText(cool_down_till));
                return "";
            }

            final Bundle data = new Bundle();
            data.putString("action", action);
            data.putString("identity", identity);

            if (action.equals("sensorupdate")) {
                final String ce_payload = CipherUtils.compressEncryptString(payload);
                Log.i(TAG, "sensor length CipherUtils.encryptBytes ce_payload length: " + ce_payload.length());
                data.putString("payload", ce_payload);
                if (d) Log.d(TAG, "sending data len " + ce_payload.length() + " " + ce_payload);
            } else {
                if ((bpayload != null) && (bpayload.length > 0)) {
                    data.putString("payload", CipherUtils.encryptBytesToString(Bytes.concat(bpayload, JoH.bchecksum(bpayload)))); // don't double sum
                } else if (payload.length() > 0) {
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
            if (last_ack == -1) last_ack = JoH.tsl();
            last_send_previous = last_send;
            last_send = JoH.tsl();
            msg = "Sent message OK " + messageid;
            DesertSync.fromGCM(data);
        } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
        }
        Log.d(TAG, "Return msg in SendMessage: " + msg);
        return msg;
    }

    private static boolean shouldAddQueue(final Bundle data) {
        if (data == null) return false;
        final String action = data.getString("action");
        if (action == null) return false;
        switch (action) {
            // one shot action types where multi queuing is not needed
            case "ping":
            case "rlcl":
            case "sbr":
            case "bfr":
            case "nscu":
            case "nscusensor-expiry":
            case "esup":
                synchronized (queue_lock) {
                    for (GCM_data qdata : gcm_queue) {
                        try {
                            if (qdata.bundle.getString("action").equals(action)) {
                                Log.d(TAG, "Skipping queue add for duplicate action: " + action);
                                qdata.bundle = data; // update with latest
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

    private static void fmSend(Bundle data) {
        final FirebaseMessaging fm = FirebaseMessaging.getInstance();
        if (senderid != null) {
            fm.send(new RemoteMessage.Builder(senderid + "@gcm.googleapis.com")
                    .setMessageId(Integer.toString(msgId.incrementAndGet()))
                    .setData(JoH.bundleToMap(data))
                    .build());
        } else {
            Log.wtf(TAG, "senderid is null");
        }
    }

    private void tryGCMcreate() {
        Log.d(TAG, "try GCMcreate");
        checkCease();
        if (cease_all_activity) return;

        if (!InstalledApps.isGooglePlayInstalled(xdrip.getAppContext())) {
            if (JoH.pratelimit("gms-missing-msg", 86400)) {
                final String msg = "Google Play services - not installed!\nInstall it or disable xDrip+ sync options";
                JoH.static_toast_long(msg);
                Home.toaststaticnext(msg);
            }
            cease_all_activity = true;
            return;
        }

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

        final Boolean play_result = checkPlayServices();
        if (play_result == null) {
            Log.d(TAG, "Indeterminate result for play services");
            PlusSyncService.backoff_a_lot();
        } else if (play_result) {
            final Intent intent = new Intent(xdrip.getAppContext(), RegistrationIntentService.class);
            xdrip.getAppContext().startService(intent);
        } else {
            cease_all_activity = true;
            final String msg = "ERROR: Connecting to Google Services - check google login or reboot?";
            JoH.static_toast_long(msg);
            Home.toaststaticnext(msg);
        }
    }

    // for starting FauxActivity
    public void jumpStart() {
        Log.d(TAG, "jumpStart() called");
        if (JoH.ratelimit("gcm-jumpstart", 5)) {
            onCreate(null);
        } else {
            Log.d(TAG, "Ratelimiting jumpstart");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            if (Pref.getBooleanDefaultFalse("disable_all_sync")) {
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
        LocalBroadcastManager.getInstance(xdrip.getAppContext()).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(PreferencesNames.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        try {
            LocalBroadcastManager.getInstance(xdrip.getAppContext()).unregisterReceiver(mRegistrationBroadcastReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Exception onPause: ", e);
        }
        super.onPause();
    }

    static void checkSync(final Context context) {
        if ((GcmActivity.last_ack > -1) && (GcmActivity.last_send_previous > 0)) {
            if (GcmActivity.last_send_previous > GcmActivity.last_ack) {
                if (Pref.getLong("sync_warning_never", 0) == 0) {

                    if (PreferencesNames.SYNC_VERSION.equals("1") && JoH.isOldVersion(context)) {
                        final long since_send = JoH.tsl() - GcmActivity.last_send_previous;
                        if (since_send > 60000) {
                            if (!DesertSync.isEnabled()) {
                                final long ack_outstanding = JoH.tsl() - GcmActivity.last_ack;
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
                                            builder.setNeutralButton(gs(R.string.maybe_later), new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                            builder.setNegativeButton("NO, Never", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                    Pref.setLong("sync_warning_never", JoH.tsl());
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
    }

    static void coolDown() {
        cool_down_till = JoH.tsl() + Constants.MINUTE_IN_MS * 20;
        Log.wtf(TAG, "Too many messages, activating cool down till: " + JoH.dateTimeText(cool_down_till));
    }

    static boolean overHeated() {
        return (cool_down_till != 0 && JoH.msSince(cool_down_till) < 0);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */

    private static Boolean checkPlayServices() {
        return checkPlayServices(xdrip.getAppContext(), null);
    }

    static Boolean checkPlayServices(Context context, Activity activity) {
        checkCease();
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
                            //apiAvailability.showErrorNotification(context, resultCode);
                            Home.startHomeWithExtra(context, Home.GCM_RESOLUTION_ACTIVITY, "1");
                            return null;
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
        public long timestamp;
        private int resent;

        private GCM_data(Bundle data) {
            bundle = data;
            timestamp = JoH.tsl();
            resent = 0;
        }
    }
}

class SensorCalibrations {
    @Expose
    Sensor sensor;

    @Expose
    List<Calibration> calibrations;
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

