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
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.ShareModels.ShareRest;
import com.eveningoutpost.dexdrip.widgetUpdateService;
import com.eveningoutpost.dexdrip.xDripWidget;

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

    public static BgSendQueue nextBgJob() {
        return new Select()
                .from(BgSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
    }

    public static List<BgSendQueue> queue() {
        return new Select()
                .from(BgSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID asc")
                .limit(20)
                .execute();
    }
    public static List<BgSendQueue> mongoQueue() {
        return new Select()
                .from(BgSendQueue.class)
                .where("mongo_success = ?", false)
                .where("operation_type = ?", "create")
                .orderBy("_ID desc")
                .limit(30)
                .execute();
    }

    public static void addToQueue(BgReading bgReading, String operation_type, Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            BgSendQueue bgSendQueue = new BgSendQueue();
            bgSendQueue.operation_type = operation_type;
            bgSendQueue.bgReading = bgReading;
            bgSendQueue.success = false;
            bgSendQueue.mongo_success = false;
            bgSendQueue.save();
            Log.d("BGQueue", "New value added to queue!");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            Intent updateIntent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA);
            context.sendBroadcast(updateIntent);

            if(AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0){
                context.startService(new Intent(context, widgetUpdateService.class));
            }

            if (prefs.getBoolean("broadcast_data_through_intents", false)) {
                Log.i("SENSOR QUEUE:", "Broadcast data");
                final Bundle bundle = new Bundle();
                bundle.putDouble(Intents.EXTRA_BG_ESTIMATE, bgReading.calculated_value);
                bundle.putDouble(Intents.EXTRA_BG_SLOPE, bgReading.calculated_value_slope);
                if (bgReading.hide_slope) {
                    bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, "9");
                } else {
                    bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, bgReading.slopeName());
                }
                bundle.putInt(Intents.EXTRA_SENSOR_BATTERY, getBatteryLevel(context));
                bundle.putLong(Intents.EXTRA_TIMESTAMP, bgReading.timestamp);

                Intent intent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE);
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);


                context.sendBroadcast(intent, Intents.RECEIVER_PERMISSION);

                //just keep it alive for 3 more seconds to allow the watch to be updated
                // TODO: change NightWatch to not allow the system to sleep.
                powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "broadcstNightWatch").acquire(3000);

            }

            if(prefs.getBoolean("broadcast_to_pebble", false)) {
                context.startService(new Intent(context, PebbleSync.class));
            }

            if (prefs.getBoolean("share_upload", false)) {
                Log.w("ShareRest", "About to call ShareRest!!");
                Intent shareIntent = new Intent(context, ShareRest.class);
                shareIntent.putExtra("BgUuid", bgReading.uuid);
                context.startService(shareIntent);
            }
            context.startService(new Intent(context, SyncService.class));
        } finally {
            wakeLock.release();
        }
    }

    public void markMongoSuccess() {
        mongo_success = true;
        save();
    }

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) {
            return 50;
        }
        return (int)(((float)level / (float)scale) * 100.0f);
    }
}
