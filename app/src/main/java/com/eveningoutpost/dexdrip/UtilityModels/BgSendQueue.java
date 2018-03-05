package com.eveningoutpost.dexdrip.UtilityModels;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.Noise;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NewDataObserver;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.xDripWidget;
import com.rits.cloning.Cloner;

import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */
@Deprecated
@Table(name = "BgSendQueue", id = BaseColumns._ID)
public class BgSendQueue extends Model {

    @Column(name = "bgReading", index = true)
    public BgReading bgReading;

    @Column(name = "success", index = true)
    public boolean success;

    @Column(name = "mongo_success", index = true)
    public boolean mongo_success;

    @Column(name = "operation_type")
    public String operation_type;

    /*
        public static List<BgSendQueue> queue() {
            return new Select()
                    .from(BgSendQueue.class)
                    .where("success = ?", false)
                    .orderBy("_ID asc")
                    .limit(20)
                    .execute();
        }
    */
    @Deprecated
    public static List<BgSendQueue> mongoQueue() {
        return new Select()
                .from(BgSendQueue.class)
                .where("mongo_success = ?", false)
                .where("operation_type = ?", "create")
                .orderBy("_ID desc")
                .limit(30)
                .execute();
    }

    @Deprecated
    public static List<BgSendQueue> cleanQueue() {
        return new Delete()
                .from(BgSendQueue.class)
                .where("mongo_success = ?", true)
                .where("operation_type = ?", "create")
                .execute();
    }

    @Deprecated
    private static void addToQueue(BgReading bgReading, String operation_type) {
        BgSendQueue bgSendQueue = new BgSendQueue();
        bgSendQueue.operation_type = operation_type;
        bgSendQueue.bgReading = bgReading;
        bgSendQueue.success = false;
        bgSendQueue.mongo_success = false;
        bgSendQueue.save();
        Log.d("BGQueue", "New value added to queue!");
    }

    public static void handleNewBgReading(BgReading bgReading, String operation_type, Context context) {
        handleNewBgReading(bgReading, operation_type, context, false);
    }

    public static void handleNewBgReading(BgReading bgReading, String operation_type, Context context, boolean is_follower) {
        handleNewBgReading(bgReading, operation_type, context, is_follower, false);
    }

    // TODO extract to non depreciated class
    public static void handleNewBgReading(BgReading bgReading, String operation_type, Context context, boolean is_follower, boolean quick) {

        final PowerManager.WakeLock wakeLock = JoH.getWakeLock("sendQueue", 120000);
        try {
            if (!is_follower) {
              //  addToQueue(bgReading, operation_type);
                UploaderQueue.newEntry(operation_type, bgReading);
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // all this other UI stuff probably shouldn't be here but in lieu of a better method we keep with it..

            if (!quick) {
                if (Home.activityVisible) {
                    context.sendBroadcast(new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA));
                }

                if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0) {
                    //context.startService(new Intent(context, WidgetUpdateService.class));
                    JoH.startService(WidgetUpdateService.class);
                }
            }

            // TODO extract to separate class/method and put in to new data observer
            BestGlucose.DisplayGlucose dg = null;
            if (prefs.getBoolean("broadcast_data_through_intents", false)) {
                Log.i("SENSOR QUEUE:", "Broadcast data");
                final Bundle bundle = new Bundle();

                // TODO this cannot handle out of sequence data due to displayGlucose taking most recent?!
                // TODO can we do something with munging for quick data and getDisplayGlucose for non quick?
                // use display glucose if enabled and available

                final int noiseBlockLevel = Noise.getNoiseBlockLevel();
                bundle.putInt(Intents.EXTRA_NOISE_BLOCK_LEVEL, noiseBlockLevel);

                if ((prefs.getBoolean("broadcast_data_use_best_glucose", false)) && ((dg = BestGlucose.getDisplayGlucose()) != null)) {
                    bundle.putDouble(Intents.EXTRA_NOISE, dg.noise);
                    bundle.putInt(Intents.EXTRA_NOISE_WARNING, dg.warning);

                    if (dg.noise <= noiseBlockLevel) {
                        bundle.putDouble(Intents.EXTRA_BG_ESTIMATE, dg.mgdl);
                        bundle.putDouble(Intents.EXTRA_BG_SLOPE, dg.slope);

                        // hide slope possibly needs to be handled properly
                        if (bgReading.hide_slope) {
                            bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, "9"); // not sure if this is right has been this way for a long time
                        } else {
                            bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, dg.delta_name);
                        }
                    } else {
                        final String msg = "Not locally broadcasting due to noise block level of: " + noiseBlockLevel + " and noise of; " + JoH.roundDouble(dg.noise, 1);
                        UserError.Log.e("LocalBroadcast", msg);
                        JoH.static_toast_long(msg);
                    }
                } else {

                    // better to use the display glucose version above
                    bundle.putDouble(Intents.EXTRA_NOISE, BgGraphBuilder.last_noise);
                    if (BgGraphBuilder.last_noise <= noiseBlockLevel) {
                        // standard xdrip-classic data set
                        bundle.putDouble(Intents.EXTRA_BG_ESTIMATE, bgReading.calculated_value);


                        //TODO: change back to bgReading.calculated_value_slope if it will also get calculated for Share data
                        // bundle.putDouble(Intents.EXTRA_BG_SLOPE, bgReading.calculated_value_slope);
                        bundle.putDouble(Intents.EXTRA_BG_SLOPE, BgReading.currentSlope());
                        if (bgReading.hide_slope) {
                            bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, "9"); // not sure if this is right but has been this way for a long time
                        } else {
                            bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, bgReading.slopeName());
                        }
                    } else {
                        final String msg = "Not locally broadcasting due to noise block level of: " + noiseBlockLevel + " and noise of; " + JoH.roundDouble(BgGraphBuilder.last_noise, 1);
                        UserError.Log.e("LocalBroadcast", msg);
                        JoH.static_toast_long(msg);
                    }
                }

                bundle.putInt(Intents.EXTRA_SENSOR_BATTERY, PowerStateReceiver.getBatteryLevel(context));
                bundle.putLong(Intents.EXTRA_TIMESTAMP, bgReading.timestamp);

                //raw value
                double slope = 0, intercept = 0, scale = 0, filtered = 0, unfiltered = 0, raw = 0;
                final Calibration cal = Calibration.lastValid();
                if (cal != null) {
                    // slope/intercept/scale like uploaded to NightScout (NightScoutUploader.java)
                    if (cal.check_in) {
                        slope = cal.first_slope;
                        intercept = cal.first_intercept;
                        scale = cal.first_scale;
                    } else {
                        slope = 1000 / cal.slope;
                        intercept = (cal.intercept * -1000) / (cal.slope);
                        scale = 1;
                    }
                    unfiltered = bgReading.usedRaw() * 1000;
                    filtered = bgReading.ageAdjustedFiltered() * 1000;
                }
                //raw logic from https://github.com/nightscout/cgm-remote-monitor/blob/master/lib/plugins/rawbg.js#L59
                if (slope != 0 && intercept != 0 && scale != 0) {
                    if (filtered == 0 || bgReading.calculated_value < 40) {
                        raw = scale * (unfiltered - intercept) / slope;
                    } else {
                        double ratio = scale * (filtered - intercept) / slope / bgReading.calculated_value;
                        raw = scale * (unfiltered - intercept) / slope / ratio;
                    }
                }
                bundle.putDouble(Intents.EXTRA_RAW, raw);
                final Intent intent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE);
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                if (Pref.getBooleanDefaultFalse("broadcast_data_through_intents_without_permission")) {
                    context.sendBroadcast(intent);
                } else {
                    context.sendBroadcast(intent, Intents.RECEIVER_PERMISSION);
                }

                // TODO I don't really think this is needed anymore
                if (!quick && Pref.getBooleanDefaultFalse("excessive_wakelocks")) {
                    // just keep it alive for 3 more seconds to allow the watch to be updated
                    // dangling wakelock
                    JoH.getWakeLock("broadcstNightWatch", 3000);
                }
            } // if broadcasting

            // now is done with an uploader queue instead
          /*  // send to wear
            if ((!quick) && (prefs.getBoolean("wear_sync", false)) && !Home.get_forced_wear()) {//KS not necessary since MongoSendTask sends UploaderQueue.newEntry BG to WatchUpdaterService.sendWearUpload
                context.startService(new Intent(context, WatchUpdaterService.class));
                if (prefs.getBoolean("excessive_wakelocks", false)) {
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "wear-quickFix3").acquire(15000);

                }
            }*/

            // send to pebble
            /*if ((!quick) && (prefs.getBoolean("broadcast_to_pebble", false) )
                    && (PebbleUtil.getCurrentPebbleSyncType(prefs) != 1)) {
                context.startService(new Intent(context, PebbleWatchSync.class));
            }*/

            if (!quick) {
                NewDataObserver.newBgReading(bgReading, is_follower);
                LibreBlock.UpdateBgVal(bgReading.timestamp, bgReading.calculated_value); // TODO move this to NewDataObserver
            }

            if ((!is_follower) && (prefs.getBoolean("plus_follow_master", false))) {
                if (prefs.getBoolean("display_glucose_from_plugin", false))
                {
                    // TODO does this currently ignore noise or is noise properly calculated on the follower?
                    // munge bgReading for follower TODO will probably want extra option for this in future
                    // TODO we maybe don't need deep clone for this! Check how value will be used below
                    GcmActivity.syncBGReading(PluggableCalibration.mungeBgReading(new Cloner().deepClone(bgReading)));
                } else {
                    // send as is
                    GcmActivity.syncBGReading(bgReading);
                }
            }

            // process the uploader queue
            if (JoH.ratelimit("start-sync-service", 30)) {
                JoH.startService(SyncService.class);
            }


        } finally {
            wakeLock.release();
        }
    }

    @Deprecated
    public void markMongoSuccess() {
        this.mongo_success = true;
        save();
    }

}
