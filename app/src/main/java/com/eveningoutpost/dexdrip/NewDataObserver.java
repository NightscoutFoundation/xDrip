package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.ShareModels.BgUploader;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareUploadPayload;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.VehicleMode;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.tidepool.TidepoolEntry;
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.watch.lefun.LeFun;
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry;
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayRemote;
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

/**
 * Created by jamorham on 01/01/2018.
 *
 * Handle triggering data updates on enabled modules
 */

public class NewDataObserver {

    private static final String TAG = "NewDataObserver";

    // TODO after restructuring so that the triggering is organized by data type,
    // TODO move appropriate functions in to their responsible classes

    // when we receive new glucose reading we want to propagate
    public static void newBgReading(BgReading bgReading, boolean is_follower) {

        sendToPebble();
        sendToWear();
        sendToAmazfit();
        sendToLeFun();
        sendToMiBand();
        sendToBlueJay();
        sendToRemoteBlueJay();
        Notifications.start();
        uploadToShare(bgReading, is_follower);
        textToSpeech(bgReading, null);
        LibreBlock.UpdateBgVal(bgReading.timestamp, bgReading.calculated_value);
        LockScreenWallPaper.setIfEnabled();
        TidepoolEntry.newData();

    }

    // when we receive a new external status broadcast
    public static void newExternalStatus(boolean receivedLocally) {

        final String statusLine = ExternalStatusService.getLastStatusLine();
        if (statusLine.length() > 0) {
            // send to wear
            if (Pref.getBooleanDefaultFalse("wear_sync")) {
                startWatchUpdaterService(xdrip.getAppContext(), WatchUpdaterService.ACTION_SEND_STATUS, TAG, "externalStatusString", statusLine);
            }
            // send to pebble
            sendToPebble();
            sendToAmazfit();
            sendStatusToBlueJay();

            // don't send via GCM if received via GCM!
            if (receivedLocally) {
                // SEND TO GCM
                GcmActivity.push_external_status_update(JoH.tsl(), statusLine);

            }
        }

    }

    // send data to pebble if enabled
    private static void sendToPebble() {
        if (Pref.getBooleanDefaultFalse("broadcast_to_pebble") && (PebbleUtil.getCurrentPebbleSyncType() != 1)) {
            JoH.startService(PebbleWatchSync.class);
        }
    }

    // send data to Amazfit if enabled
    private static void sendToAmazfit() {
        if (Pref.getBoolean("pref_amazfit_enable_key", true)) {
            Amazfitservice.start("xDrip_synced_SGV_data");
        }
    }

    private static void sendToLeFun() {
        if (LeFunEntry.isEnabled()) {
            Inevitable.task("poll-le-fun-for-bg", DexCollectionType.hasBluetooth() ? 2000 : 500, LeFun::showLatestBG); // delay enough for BT to finish on collector
        }
    }

    private static void sendToMiBand() {
        if (MiBandEntry.isEnabled()) {
            Inevitable.task("poll-miband-for-bg", DexCollectionType.hasBluetooth() ? 2000 : 500, MiBandEntry::showLatestBG); // delay enough for BT to finish on collector
        }
    }

    private static void sendToBlueJay() {
        if (BlueJayEntry.isEnabled()) {
            Inevitable.task("poll-bluejay-for-bg", DexCollectionType.hasBluetooth() ? 2000 : 500, BlueJay::showLatestBG); // delay enough for BT to finish on collector
        }
    }

    private static void sendStatusToBlueJay() {
        if (BlueJayEntry.isEnabled()) {
            Inevitable.task("poll-bluejay-for-status", 1000, BlueJay::showStatusLine);
        }
    }

    private static void sendToRemoteBlueJay() {
        if (BlueJayEntry.isRemoteEnabled()) {
            Inevitable.task("poll-bluejay-remote-for-bg", DexCollectionType.hasBluetooth() ? 2000 : 500, BlueJayRemote::sendLatestBG); // delay enough for BT to finish on collector
        }
    }

    // send to wear
    // data is already synced via UploaderQueue but this will send the display glucose
    private static void sendToWear() {
        if ((Pref.getBooleanDefaultFalse("wear_sync")) && !Home.get_forced_wear()) {//KS not necessary since MongoSendTask sends UploaderQueue.newEntry BG to WatchUpdaterService.sendWearUpload
            JoH.startService(WatchUpdaterService.class);
            // I don't think this wakelock is really needed anymore
            if (Pref.getBoolean("excessive_wakelocks", false)) {
                JoH.getWakeLock("wear-quickFix3", 15000); // dangling wakelock
            }
        }
    }

    // speak value
    private static void textToSpeech(BgReading bgReading, BestGlucose.DisplayGlucose dg) {
        //Text to speech
        if (Pref.getBooleanDefaultFalse("bg_to_speech") || VehicleMode.shouldSpeak()) {
            if (dg == null) dg = BestGlucose.getDisplayGlucose();
            if (dg != null) {
                BgToSpeech.speak(dg.mgdl, dg.timestamp, dg.delta_name);
            } else {
                BgToSpeech.speak(bgReading.calculated_value, bgReading.timestamp, bgReading.slopeName());
            }
        }
    }

    // share uploader
    private static void uploadToShare(BgReading bgReading, boolean is_follower) {
        if ((!is_follower) && (Pref.getBooleanDefaultFalse("share_upload"))) {
            if (JoH.ratelimit("sending-to-share-upload", 10)) {
                UserError.Log.d("ShareRest", "About to call ShareRest!!");
                String receiverSn = Pref.getString("share_key", "SM00000000").toUpperCase();
                BgUploader bgUploader = new BgUploader(xdrip.getAppContext());
                bgUploader.upload(new ShareUploadPayload(receiverSn, bgReading));
            }
        }
    }
}
