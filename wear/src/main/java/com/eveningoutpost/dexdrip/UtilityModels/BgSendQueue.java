package com.eveningoutpost.dexdrip.UtilityModels;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.LocalBroadcastManager;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
//KS import com.eveningoutpost.dexdrip.GcmActivity;
//KS import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ListenerService;
import com.eveningoutpost.dexdrip.Models.BgReading;
//KS import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;


//KS following are not used on watch
/*
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.ShareModels.BgUploader;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareUploadPayload;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xDripWidget;
*/
import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/7/14.
 */
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
    public static List<BgSendQueue> mongoQueue() {
        return new Select()
                .from(BgSendQueue.class)
                .where("mongo_success = ?", false)
                .where("operation_type = ?", "create")
                .orderBy("_ID desc")
                .limit(30)
                .execute();
    }

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

    public static void handleNewBgReading(BgReading bgReading, String operation_type, Context context, boolean is_follower, boolean quick) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            if (!is_follower) addToQueue(bgReading, operation_type);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // all this other UI stuff probably shouldn't be here but in lieu of a better method we keep with it..

            //KS Following is not needed on watch
            /*
            if (!quick) {
                if (Home.activityVisible) {
                    Intent updateIntent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA);
                    context.sendBroadcast(updateIntent);
                }

                Context appContext = xdrip.getAppContext();//KS
                if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(appContext, xDripWidget.class)).length > 0) {//KS context
                    context.startService(new Intent(context, WidgetUpdateService.class));
                }
            }

            if (prefs.getBoolean("broadcast_data_through_intents", false)) {
                Log.i("SENSOR QUEUE:", "Broadcast data");
                final Bundle bundle = new Bundle();
                bundle.putDouble(Intents.EXTRA_BG_ESTIMATE, bgReading.calculated_value);

                //TODO: change back to bgReading.calculated_value_slope if it will also get calculated for Share data
                // bundle.putDouble(Intents.EXTRA_BG_SLOPE, bgReading.calculated_value_slope);
                bundle.putDouble(Intents.EXTRA_BG_SLOPE, BgReading.currentSlope());
                if (bgReading.hide_slope) {
                    bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, "9");
                } else {
                    bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, bgReading.slopeName());
                }
                bundle.putInt(Intents.EXTRA_SENSOR_BATTERY, getBatteryLevel(context));
                bundle.putLong(Intents.EXTRA_TIMESTAMP, bgReading.timestamp);

                //raw value
                double slope = 0, intercept = 0, scale = 0, filtered = 0, unfiltered = 0, raw = 0;
                Calibration cal = Calibration.last();
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
                Intent intent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE);
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);


                if (prefs.getBoolean("broadcast_data_through_intents_without_permission", false)) {
                    context.sendBroadcast(intent);
                } else {
                    context.sendBroadcast(intent, Intents.RECEIVER_PERMISSION);
                }

                //just keep it alive for 3 more seconds to allow the watch to be updated
                // TODO: change NightWatch to not allow the system to sleep.
                if ((!quick) && (prefs.getBoolean("excessive_wakelocks", false))) {
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "broadcstNightWatch").acquire(3000);
                }
            }

            // send to wear
            if ((!quick) && (prefs.getBoolean("wear_sync", false))) {
                context.startService(new Intent(context, WatchUpdaterService.class));
                if (prefs.getBoolean("excessive_wakelocks", false)) {
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "wear-quickFix3").acquire(15000);

                }
            }

            // send to pebble
            if ((!quick) && (prefs.getBoolean("broadcast_to_pebble", false) )
                    && (PebbleUtil.getCurrentPebbleSyncType(prefs) != 1)) {
                context.startService(new Intent(context, PebbleWatchSync.class));
            }

            if ((!is_follower) && (prefs.getBoolean("plus_follow_master", false))) {
                GcmActivity.syncBGReading(bgReading);
            }

            if ((!is_follower) && (!quick) && (prefs.getBoolean("share_upload", false))) {
                if (JoH.ratelimit("sending-to-share-upload",10)) {
                    Log.d("ShareRest", "About to call ShareRest!!");
                    String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
                    BgUploader bgUploader = new BgUploader(context);
                    bgUploader.upload(new ShareUploadPayload(receiverSn, bgReading));
                }
            }

            if (JoH.ratelimit("start-sync-service",30)) {
                context.startService(new Intent(context, SyncService.class));
            }

            //Text to speech
            //Log.d("BgToSpeech", "gonna call speak");
            if ((!quick) && (prefs.getBoolean("bg_to_speech", false)))
            {
                BgToSpeech.speak(bgReading.calculated_value, bgReading.timestamp);
            }
            */

            // if executing on watch; send to watchface
            if (prefs.getBoolean("enable_wearG5", false)) {//KS
                Log.d("BgSendQueue", "handleNewBgReading Broadcast BG data to watch");
                resendData(context);
                if (prefs.getBoolean("force_wearG5", false)) {
                    ListenerService.requestData(context);
                }
            }


        } finally {
            wakeLock.release();
        }
    }

    public static void sendToPhone(Context context) {//KS
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean("enable_wearG5", false) && prefs.getBoolean("force_wearG5", false)) {
            ListenerService.requestData(context);
        }
    }

    //KS start from WatchUpdaterService
    public static void resendData(Context context) {//KS
        long startTime = new Date().getTime() - (60000 * 60 * 24);
        Log.d("BgSendQueue", "resendData enter");

        BgReading last_bg = BgReading.last();
        if (last_bg != null) {
            Log.d("BgSendQueue", "resendData last_bg.timestamp:" +  JoH.dateTimeText(last_bg.timestamp));
        }

        List<BgReading> graph_bgs = BgReading.latestForGraph(60, startTime);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context.getApplicationContext());
        if (!graph_bgs.isEmpty()) {
            Log.d("BgSendQueue", "resendData graph_bgs size=" + graph_bgs.size());
            final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            DataMap entries = dataMap(last_bg, sharedPrefs, bgGraphBuilder, context);
            for (BgReading bg : graph_bgs) {
                dataMaps.add(dataMap(bg, sharedPrefs, bgGraphBuilder, context));
            }
            entries.putDataMapArrayList("entries", dataMaps);
            Log.d("BgSendQueue", "resendData entries=" + entries);

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", "ACTION_G5BG");
            messageIntent.putExtra("data", entries.toBundle());
            LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);
        }
    }

    private static DataMap dataMap(BgReading bg, SharedPreferences sPrefs, BgGraphBuilder bgGraphBuilder, Context context) {//KS
        Double highMark = Double.parseDouble(sPrefs.getString("highValue", "140"));
        Double lowMark = Double.parseDouble(sPrefs.getString("lowValue", "60"));
        DataMap dataMap = new DataMap();

        int battery = BgSendQueue.getBatteryLevel(context.getApplicationContext());

        dataMap.putString("sgvString", bgGraphBuilder.unitized_string(bg.calculated_value));
        dataMap.putString("slopeArrow", bg.slopeArrow());
        dataMap.putDouble("timestamp", bg.timestamp); //TODO: change that to long (was like that in NW)
        dataMap.putString("delta", bgGraphBuilder.unitizedDeltaString(true, true));
        dataMap.putString("battery", "" + battery);
        dataMap.putLong("sgvLevel", sgvLevel(bg.calculated_value, sPrefs, bgGraphBuilder));
        dataMap.putInt("batteryLevel", (battery>=30)?1:0);
        dataMap.putDouble("sgvDouble", bg.calculated_value);
        dataMap.putDouble("high", inMgdl(highMark, sPrefs));
        dataMap.putDouble("low", inMgdl(lowMark, sPrefs));
        //TODO: Add raw again
        //dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
        return dataMap;
    }


    // TODO: Integrate these helper methods into BGGraphBuilder.
    // TODO: clean them up  (no "if(boolean){return true; else return false;").
    // TODO: Make the needed methods in BgGraphBuilder static.

    public static long sgvLevel(double sgv_double, SharedPreferences prefs, BgGraphBuilder bgGB) {//KS change to static
        Double highMark = Double.parseDouble(prefs.getString("highValue", "140"));
        Double lowMark = Double.parseDouble(prefs.getString("lowValue", "60"));
        if(bgGB.unitized(sgv_double) >= highMark) {
            return 1;
        } else if (bgGB.unitized(sgv_double) >= lowMark) {
            return 0;
        } else {
            return -1;
        }
    }

    public static double inMgdl(double value, SharedPreferences sPrefs) {//KS change to static
        if (!doMgdl(sPrefs)) {
            return value * Constants.MMOLL_TO_MGDL;
        } else {
            return value;
        }

    }

    public static boolean doMgdl(SharedPreferences sPrefs) {
        String unit = sPrefs.getString("units", "mgdl");
        if (unit.compareTo("mgdl") == 0) {
            return true;
        } else {
            return false;
        }
    }
    //KS end from WatchUpdaterService

    public void markMongoSuccess() {
        this.mongo_success = true;
        save();
    }

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level == -1 || scale == -1) {
            return 50;
        }
        return (int) (((float) level / (float) scale) * 100.0f);
    }
}
