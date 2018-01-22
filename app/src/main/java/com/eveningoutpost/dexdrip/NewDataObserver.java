package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.ShareModels.BgUploader;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareUploadPayload;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
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
        uploadToShare(bgReading, is_follower);
        textToSpeech(bgReading, null);

    }

    // when we receive a new external status broadcast
    public static void newExternalStatus() {

        final String statusLine = ExternalStatusService.getLastStatusLine();
        if (statusLine.length() > 0) {
            // send to wear
            if (Pref.getBooleanDefaultFalse("wear_sync")) {
                startWatchUpdaterService(xdrip.getAppContext(), WatchUpdaterService.ACTION_SEND_STATUS, TAG, "externalStatusString", statusLine);
            }
            // send to pebble
            sendToPebble();
            // TODO should we also be syncing wear here?
        }

    }

    // send data to pebble if enabled
    private static void sendToPebble() {
        if (Pref.getBooleanDefaultFalse("broadcast_to_pebble") && (PebbleUtil.getCurrentPebbleSyncType() != 1)) {
            JoH.startService(PebbleWatchSync.class);
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
        if (Pref.getBooleanDefaultFalse("bg_to_speech")) {
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
