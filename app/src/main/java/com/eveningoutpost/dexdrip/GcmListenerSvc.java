package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 11/01/16.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Base64;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.DesertSync;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.RollCall;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utilitymodels.WholeHouse;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.utils.WebAppHelper;
import com.eveningoutpost.dexdrip.utils.bt.Mimeograph;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.eveningoutpost.dexdrip.models.JoH.isAnyNetworkConnected;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;

public class GcmListenerSvc extends JamListenerSvc {

    private static final String TAG = "jamorham GCMlis";
    private static final String EXTRA_WAKE_LOCK_ID = "android.support.content.wakelockid";
    public static long lastMessageReceived = 0;
    private static byte[] staticKey;

    public static int lastMessageMinutesAgo() {
        return (int) ((JoH.tsl() - GcmListenerSvc.lastMessageReceived) / 60000);
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        if (lastMessageReceived > 0)
            l.add(new StatusItem("Network traffic", JoH.niceTimeSince(lastMessageReceived) + " ago"));
        return l;
    }

    // TODO probably needs alternate workaround for google play bug
    /*@Override
    protected Intent zzf(Intent interceptedIntent) {
        // intercept and fix google play services wakelocking bug
        try {
            if (!Pref.getBooleanDefaultFalse("excessive_wakelocks")) {
                completeWakefulIntent(interceptedIntent);
                final Bundle extras = interceptedIntent.getExtras();
                if (extras != null) extras.remove(EXTRA_WAKE_LOCK_ID);
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Error patching play services: " + e);
        }
        return super.zzf(interceptedIntent);
    }*/


    @Override
    public void onSendError(String msgID, Exception exception) {
        boolean unexpected = true;
        if (exception.getMessage().equals("TooManyMessages")) {
            if (isAnyNetworkConnected() && googleReachable()) {
                GcmActivity.coolDown();
            }
            unexpected = false;
        }
        if (unexpected || JoH.ratelimit("gcm-expected-error", 86400)) {
            Log.e(TAG, "onSendError called" + msgID, exception);
        }
    }

    private static boolean googleReachable() {
        return false; // TODO we need a method for this to properly handle cooldown default to false to disable functionality
    }


    @Override
    public void onDeletedMessages() {
        Log.e(TAG, "onDeletedMessages: ");
    }

    @Override
    public void onMessageSent(String msgID) {
        Log.i(TAG, "onMessageSent: " + msgID);
    }

    @Override
    public void onMessageReceived(RemoteMessage rmessage) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-onMsgRec", 120000);
        try {
            if (rmessage == null) return;
            if (GcmActivity.cease_all_activity) return;
            String from = rmessage.getFrom();

            final Bundle data = new Bundle();
            for (Map.Entry<String, String> entry : rmessage.getData().entrySet()) {
                data.putString(entry.getKey(), entry.getValue());
            }

            if (from == null) {
                if (isInjectable()) {
                    from = data.getString("yfrom");
                }
                if (from == null) {
                    from = "null";
                }
            }
            String message = data.getString("message");

            Log.d(TAG, "From: " + from);
            if (message != null) {
                Log.d(TAG, "Message: " + message);
            } else {
                message = "null";
            }

            final Bundle notification = data.getBundle("notification");
            if (notification != null) {
                Log.d(TAG, "Processing notification bundle");
                try {
                    sendNotification(notification.getString("body"), notification.getString("title"));
                } catch (NullPointerException e) {
                    Log.d(TAG, "Null pointer exception within sendnotification");
                }
            }

            if (from.startsWith(getString(R.string.gcmtpc))) {

                String xfrom = data.getString("xfrom");
                String payload = data.getString("datum", data.getString("payload"));
                String action = data.getString("action");

                if ((xfrom != null) && (xfrom.equals(GcmActivity.token))) {
                    GcmActivity.queueAction(action + payload);
                    return;
                }

                String[] tpca = from.split("/");
                if ((tpca[2] != null) && (tpca[2].length() > 30) && (!tpca[2].equals(GcmActivity.myIdentity()))) {
                    Log.e(TAG, "Received invalid channel: " + from + " instead of: " + GcmActivity.myIdentity());
                    if ((GcmActivity.myIdentity() != null) && (GcmActivity.myIdentity().length() > 30)) {
                        try {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(tpca[2]);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception unsubscribing: " + e.toString());
                        }
                    }
                    return;
                }

                if (!isInjectable()) {
                    if (!DesertSync.fromGCM(data)) {
                        UserError.Log.d(TAG, "Skipping inbound data due to duplicate detection");
                        return;
                    }
                }

                byte[] bpayload = null;
                if (payload == null) payload = "";
                if (action == null) action = "null";

                if (payload.length() > 16) {
                    if (GoogleDriveInterface.keyInitialized()) {

                        // handle binary message types
                        switch (action) {

                            case "btmm":
                            case "bgmm":
                                bpayload = CipherUtils.decryptStringToBytes(payload);
                                if (JoH.checkChecksum(bpayload)) {
                                    bpayload = Arrays.copyOfRange(bpayload, 0, bpayload.length - 4);
                                    Log.d(TAG, "Binary payload received: length: " + bpayload.length + " orig: " + payload.length());
                                } else {
                                    Log.e(TAG, "Invalid binary payload received, possible key mismatch: ");
                                    bpayload = null;
                                }
                                payload = "binary";
                                break;

                            default:

                                if (action.equals("sensorupdate")) {
                                    try {
                                        Log.i(TAG, "payload for sensorupdate " + payload);
                                        byte[] inbytes = Base64.decode(payload, Base64.NO_WRAP);
                                        byte[] inbytes1 = JoH.decompressBytesToBytes(CipherUtils.decryptBytes(inbytes));
                                        payload = new String(inbytes1, "UTF-8");
                                        Log.d(TAG, "inbytes size = " + inbytes.length + " inbytes1 size " + inbytes1.length + "payload len " + payload.length());
                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(TAG, "Got unsupported encoding on UTF8 " + e.toString());
                                        payload = "";
                                    }
                                } else {
                                    String decrypted_payload = CipherUtils.decryptString(payload);
                                    if (decrypted_payload.length() > 0) {
                                        payload = decrypted_payload;
                                    } else {
                                        Log.e(TAG, "Couldn't decrypt payload!");
                                        payload = "";
                                        Home.toaststaticnext("Having problems decrypting incoming data - check keys");
                                    }
                                }
                        }
                    } else {
                        Log.e(TAG, "Couldn't decrypt as key not initialized");
                        payload = "";
                    }
                } else {
                    if (payload.length() > 0)
                        UserError.Log.wtf(TAG, "Got short payload: " + payload + " on action: " + action);
                }

                Log.i(TAG, "Got action: " + action + " with payload: " + payload);
                lastMessageReceived = JoH.tsl();


                // new treatment
                if (action.equals("nt")) {
                    Log.i(TAG, "Attempting GCM push to Treatment");
                    if (Home.get_master_or_follower() && Home.follower_or_accept_follower())
                        GcmActivity.pushTreatmentFromPayloadString(payload);
                } else if (action.equals("dat")) {
                    Log.i(TAG, "Attempting GCM delete all treatments");
                    if (Home.get_master_or_follower() && Home.follower_or_accept_follower())
                        Treatments.delete_all();
                } else if (action.equals("dt")) {
                    Log.i(TAG, "Attempting GCM delete specific treatment");
                    if (Home.get_master_or_follower() && Home.follower_or_accept_follower())
                        Treatments.delete_by_uuid(filter(payload));
                } else if (action.equals("clc")) {
                    Log.i(TAG, "Attempting to clear last calibration");
                    if (Home.get_master_or_follower() && Home.follower_or_accept_follower()) {
                        if (payload.length() > 0) {
                            Calibration.clearCalibrationByUUID(payload);
                        } else {
                            Calibration.clearLastCalibration();
                        }
                    }
                } else if (action.equals("cal")) {
                    if (Home.get_master_or_follower() && Home.follower_or_accept_follower()) {
                        String[] message_array = filter(payload).split("\\s+");
                        if ((message_array.length == 3) && (message_array[0].length() > 0)
                                && (message_array[1].length() > 0) && (message_array[2].length() > 0)) {
                            // [0]=timestamp [1]=bg_String [2]=bgAge
                            Intent calintent = new Intent();
                            calintent.setClassName(getString(R.string.local_target_package), "com.eveningoutpost.dexdrip.AddCalibration");
                            calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            long timediff = (long) ((new Date().getTime() - Double.parseDouble(message_array[0])) / 1000);
                            Log.i(TAG, "Remote calibration latency calculated as: " + Long.toString(timediff) + " seconds");
                            if (timediff > 0) {
                                message_array[2] = Long.toString(Long.parseLong(message_array[2]) + timediff);
                            }
                            Log.i(TAG, "Processing remote CAL " + message_array[1] + " age: " + message_array[2]);
                            calintent.putExtra("timestamp", JoH.tsl());
                            calintent.putExtra("bg_string", message_array[1]);
                            calintent.putExtra("bg_age", message_array[2]);
                            calintent.putExtra("cal_source", "gcm cal packet");
                            if (timediff < 3600) {
                                getApplicationContext().startActivity(calintent);
                            }
                        } else {
                            Log.e(TAG, "Invalid CAL payload");
                        }
                    }
                } else if (action.equals("cal2")) {
                    Log.i(TAG, "Received cal2 packet");
                    if (Home.get_master() && Home.follower_or_accept_follower()) {
                        final NewCalibration newCalibration = GcmActivity.getNewCalibration(payload);
                        if (newCalibration != null) {
                            final Intent calintent = new Intent();
                            calintent.setClassName(getString(R.string.local_target_package), "com.eveningoutpost.dexdrip.AddCalibration");
                            calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            long timediff = (long) ((new Date().getTime() - newCalibration.timestamp) / 1000);
                            Log.i(TAG, "Remote calibration latency calculated as: " + timediff + " seconds");
                            Long bg_age = newCalibration.offset;
                            if (timediff > 0) {
                                bg_age += timediff;
                            }
                            Log.i(TAG, "Processing remote CAL " + newCalibration.bgValue + " age: " + bg_age);
                            calintent.putExtra("timestamp", JoH.tsl());
                            calintent.putExtra("bg_string", "" + (Pref.getString("units", "mgdl").equals("mgdl") ? newCalibration.bgValue : newCalibration.bgValue * Constants.MGDL_TO_MMOLL));
                            calintent.putExtra("bg_age", "" + bg_age);
                            calintent.putExtra("cal_source", "gcm cal2 packet");
                            if (timediff < 3600) {
                                getApplicationContext().startActivity(calintent);
                            } else {
                                Log.w(TAG, "warninig ignoring calibration because timediff is " + timediff);
                            }
                        }
                    } else {
                        Log.e(TAG, "Received cal2 packet packet but we are not a master, so ignoring it");
                    }

                } else if (action.equals("ping")) {
                    if (payload.length() > 0) {
                        RollCall.Seen(payload);
                    }
                    // don't respond to wakeup pings
                } else if (action.equals("rlcl")) {
                    if (Home.get_master_or_follower()) {
                        if (payload.length() > 0) {
                            RollCall.Seen(payload);
                        }
                        GcmActivity.requestPing();
                    }
                } else if (action.equals("p")) {
                    GcmActivity.send_ping_reply();
                } else if (action.equals("q")) {
                    Home.toaststatic("Received ping reply");
                } else if (action.equals("plu")) {
                    // process map update
                    if (Home.get_follower()) {
                        MapsActivity.newMapLocation(payload, (long) JoH.ts());
                    }
                } else if (action.equals("sbu")) {
                    if (Home.get_follower()) {
                        Log.i(TAG, "Received sensor battery level update");
                        Sensor.updateBatteryLevel(Integer.parseInt(payload), true);
                        TransmitterData.updateTransmitterBatteryFromSync(Integer.parseInt(payload));
                    }
                } else if (action.equals("bbu")) {
                    if (Home.get_follower()) {
                        Log.i(TAG, "Received bridge battery level update");
                        Pref.setInt("bridge_battery", Integer.parseInt(payload));
                        CheckBridgeBattery.checkBridgeBattery();
                    }
                } else if (action.equals("pbu")) {
                    if (Home.get_follower()) {
                        Log.i(TAG, "Received parakeet battery level update");
                        Pref.setInt("parakeet_battery", Integer.parseInt(payload));
                        CheckBridgeBattery.checkParakeetBattery();
                    }
                } else if (action.equals("psu")) {
                    if (Home.get_follower()) {
                        Log.i(TAG, "Received pump status update");
                        PumpStatus.fromJson(payload);
                    }
                } else if (action.startsWith("nscu")) {
                    if (Home.get_follower()) {
                        Log.i(TAG, "Received nanostatus update: " + action);
                        NanoStatus.setRemote(action.replaceAll("^nscu", ""), payload);
                    }
                } else if (action.equals("not")) {
                    if (Home.get_follower()) {
                        try {
                            final int GCM_NOTIFICATION_ITEM = 543;
                            final String[] payloadA = payload.split("\\^");
                            final String title = payloadA[0];
                            final String body = payloadA[1];
                            final PendingIntent pendingIntent = android.app.PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                            showNotification(title, body, pendingIntent, GCM_NOTIFICATION_ITEM, true, true, false);
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "Error showing follower notification with payload: " + payload);
                        }
                    }
                } else if (action.equals("sbr")) {
                    if ((Home.get_master()) && JoH.ratelimit("gcm-sbr", 300)) {
                        Log.i(TAG, "Received sensor battery request");
                        if (Sensor.currentSensor() != null) {
                            try {
                                TransmitterData td = TransmitterData.last();
                                if ((td != null) && (td.sensor_battery_level != 0)) {
                                    GcmActivity.sendSensorBattery(td.sensor_battery_level);
                                } else {
                                    GcmActivity.sendSensorBattery(Sensor.currentSensor().latest_battery_level);
                                }
                            } catch (NullPointerException e) {
                                Log.e(TAG, "Cannot send sensor battery as sensor is null");
                            }
                        } else {
                            Log.d(TAG, "No active sensor so not sending anything.");
                        }
                    }
                } else if (action.equals("amu")) {
                    if ((Pref.getBoolean("motion_tracking_enabled", false)) && (Pref.getBoolean("use_remote_motion", false))) {
                        if (!Pref.getBoolean("act_as_motion_master", false)) {
                            ActivityRecognizedService.spoofActivityRecogniser(getApplicationContext(), payload);
                        } else {
                            Home.toaststaticnext("Receiving motion updates from a different master! Make only one the master!");
                        }
                    }
                } else if (action.equals("sra")) {
                    if ((Home.get_follower() || Home.get_master())) {
                        if (Pref.getBooleanDefaultFalse("accept_remote_snoozes")) {
                            try {
                                long snoozed_time = 0;
                                String sender_ssid = "";
                                try {
                                    snoozed_time = Long.parseLong(payload);
                                } catch (NumberFormatException e) {
                                    String ii[] = payload.split("\\^");
                                    snoozed_time = Long.parseLong(ii[0]);
                                    if (ii.length > 1) sender_ssid = JoH.base64decode(ii[1]);
                                }
                                if (!Pref.getBooleanDefaultFalse("remote_snoozes_wifi_match") || JoH.getWifiFuzzyMatch(sender_ssid, JoH.getWifiSSID())) {
                                    if (Math.abs(JoH.tsl() - snoozed_time) < 300000) {
                                        if (JoH.pratelimit("received-remote-snooze", 30)) {
                                            AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1, false);
                                            UserError.Log.ueh(TAG, "Accepted remote snooze");
                                            JoH.static_toast_long("Received remote snooze!");
                                        } else {
                                            Log.e(TAG, "Rate limited remote snooze");
                                        }
                                    } else {
                                        UserError.Log.uel(TAG, "Ignoring snooze as outside 5 minute window, sync lag or clock difference");
                                    }
                                } else {
                                    UserError.Log.uel(TAG, "Ignoring snooze as wifi network names do not match closely enough");
                                }
                            } catch (Exception e) {
                                UserError.Log.e(TAG, "Exception processing remote snooze: " + e);
                            }
                        } else {
                            UserError.Log.uel(TAG, "Rejecting remote snooze");
                        }
                    }
                } else if (action.equals("bgs")) {
                    Log.i(TAG, "Received BG packet(s)");
                    if (Home.get_follower() || WholeHouse.isEnabled()) {
                        final String bgs[] = payload.split("\\^");
                        for (String bgr : bgs) {
                            BgReading.bgReadingInsertFromJson(bgr);
                        }
                        if (Pref.getBooleanDefaultFalse("follower_chime") && JoH.pratelimit("bgs-notify", 1200)) {
                            JoH.showNotification("New glucose data @" + JoH.hourMinuteString(), "Follower Chime: will alert whenever it has been more than 20 minutes since last", null, 60311, true, true, true);
                        }
                    } else {
                        Log.e(TAG, "Received remote BG packet but we are not set as a follower");
                    }
                    // Home.staticRefreshBGCharts();
                } else if (action.equals("bfb")) {
                    final String bfb[] = payload.split("\\^");
                    if (Pref.getString("dex_collection_method", "").equals("Follower")) {
                        Log.i(TAG, "Processing backfill location packet as we are a follower");
                        staticKey = CipherUtils.hexToBytes(bfb[1]);
                        final Handler mainHandler = new Handler(getMainLooper());
                        final Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    new WebAppHelper(new GcmListenerSvc.ServiceCallback()).executeOnExecutor(xdrip.executor, getString(R.string.wserviceurl) + "/joh-getsw/" + bfb[0]);
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception processing run on ui thread: " + e);
                                }
                            }
                        };
                        mainHandler.post(myRunnable);
                    } else {
                        Log.i(TAG, "Ignoring backfill location packet as we are not follower");
                    }
                } else if (action.equals("bfr")) {
                    if (Pref.getBooleanDefaultFalse("plus_follow_master")) {
                        Log.i(TAG, "Processing backfill location request as we are master");
                        final long remoteRecent = JoH.tolerantParseLong(payload, 0);
                        final BgReading bgReading = BgReading.last();
                        if (bgReading != null && bgReading.timestamp > remoteRecent) {
                            GcmActivity.syncBGTable2();
                        } else {
                            // TODO reduce logging priority
                            UserError.Log.e(TAG, "We do not have any more recent data to offer than: " + (bgReading != null ? JoH.dateTimeText(bgReading.timestamp) : "no data"));
                        }
                    }
                } else if (action.equals("sensorupdate")) {
                    Log.i(TAG, "Received sensorupdate packet(s)");
                    if (Home.get_follower() || WholeHouse.isEnabled()) {
                        GcmActivity.upsertSensorCalibratonsFromJson(payload);
                    } else {
                        Log.e(TAG, "Received sensorupdate packets but we are not set as a follower");
                    }
                } else if (action.equals("sensor_calibrations_update")) {
                    if (Home.get_master()) {
                        Log.i(TAG, "Received request for sensor calibration update");
                        GcmActivity.syncSensor(Sensor.currentSensor(), false);
                    }
                } else if (action.equals("mimg")) {
                    if (Home.get_master() && WholeHouse.isLive()) {
                        Mimeograph.putXferFromJson(payload);
                    }
                } else if (action.equals("btmm")) {
                    if (Home.get_master_or_follower() && Home.follower_or_accept_follower()) {
                        BloodTest.processFromMultiMessage(bpayload);
                    } else {
                        Log.i(TAG, "Receive multi blood test but we are neither master or follower");
                    }
                } else if (action.equals("bgmm")) {
                    if (Home.get_follower()) {
                        BgReading.processFromMultiMessage(bpayload);
                    } else {
                        Log.i(TAG, "Receive multi glucose readings but we are not a follower");
                    }
                } else if (action.equals("esup")) {
                    if (Home.get_master_or_follower()) {
                        final String[] segments = payload.split("\\^");
                        try {
                            ExternalStatusService.update(Long.parseLong(segments[0]), segments[1], false);
                        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                            UserError.Log.wtf(TAG, "Could not split esup payload");
                        }
                    }
                } else if (action.equals("ssom")) {
                    if (Home.get_master()) {
                        if (payload.equals("challenge string")) {
                            if (Pref.getBoolean("plus_accept_follower_actions", true)) {
                                UserError.Log.i(TAG, "Stopping sensor by remote");
                                StopSensor.stop();
                            } else {
                                UserError.Log.w(TAG, "Stop sensor by follower rejected because follower actions are disabled");
                            }
                        } else {
                            UserError.Log.wtf(TAG, "Challenge string failed in ssom");
                        }
                    }
                } else if (action.equals("rsom")) {
                    if (Home.get_master()) {
                        try {
                            final Long timestamp = Long.parseLong(payload);
                            StartNewSensor.startSensorForTime(timestamp);
                        } catch (NumberFormatException | NullPointerException e) {
                            UserError.Log.wtf(TAG, "Exception processing rsom timestamp");
                        }
                    }
                } else if (action.equals("libreBlock") || action.equals("libreBlck")) {
                    HandleLibreBlock(payload);
                } else {
                    Log.e(TAG, "Received message action we don't know about: " + action);
                }
            } else {
                // direct downstream message.
                Log.i(TAG, "Received downstream message: " + message);
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void HandleLibreBlock(final String payload) {
        LibreBlock lb = LibreBlock.createFromExtendedJson(payload);
        if (lb == null) {
            return;
        }

        if (lb.timestamp == 0) {
            UserError.Log.e(TAG, "Corrupt libre block from sync");
            return;
        }

        if (LibreBlock.getForTimestamp(lb.timestamp) != null) {
            // We already seen this one.
            return;
        }
        LibreBlock.Save(lb);

        PersistentStore.setString("LibreSN", lb.reference);

        if (Home.get_master()) {
            if (SensorSanity.checkLibreSensorChangeIfEnabled(lb.reference)) {
                Log.e(TAG, "Problem with Libre Serial Number - not processing");
                return;
            }

            NFCReaderX.HandleGoodReading(lb.reference, lb.blockbytes, lb.timestamp, false, lb.patchUid, lb.patchInfo);
        }
    }

    private void sendNotification(String body, String title) {
        Intent intent = new Intent(this, Home.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder notificationBuilder = (Notification.Builder) new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private String filter(String source) {
        if (source == null) return null;
        return source.replaceAll("[^a-zA-Z0-9 _.-]", "");
    }

    public class ServiceCallback implements Preferences.OnServiceTaskCompleted {
        @Override
        public void onTaskCompleted(byte[] result) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-gcm-callback", 60000);
            try {
                if (result.length > 0) {
                    if ((staticKey == null) || (staticKey.length != 16)) {
                        Log.e(TAG, "Error processing security key");
                    } else {
                        byte[] plainbytes = JoH.decompressBytesToBytes(CipherUtils.decryptBytes(result, staticKey));
                        staticKey = null;
                        UserError.Log.d(TAG, "Plain bytes size: " + plainbytes.length);
                        if (plainbytes.length > 0) {
                            GcmActivity.processBFPbundle(new String(plainbytes, 0, plainbytes.length, "UTF-8"));
                        } else {
                            Log.e(TAG, "Error processing data - empty");
                        }
                    }
                } else {
                    Log.e(TAG, "Error processing - no data - try again?");
                }
            } catch (Exception e) {
                Log.e(TAG, "Got error in BFP callback: " + e.toString());
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }
    }

}

