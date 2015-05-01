package com.eveningoutpost.dexdrip.UtilityModels;

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
import com.eveningoutpost.dexdrip.ShareModels.ShareRest;
import com.eveningoutpost.dexdrip.widgetUpdateService;

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
                .orderBy("_ID asc")
                .limit(30)
                .execute();
    }

    public static void addToQueue(BgReading bgReading, String operation_type, Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();


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
        context.startService(new Intent(context, widgetUpdateService.class));

        if (prefs.getBoolean("cloud_storage_mongodb_enable", false) || prefs.getBoolean("cloud_storage_api_enable", false)) {
            Log.w("SENSOR QUEUE:", String.valueOf(bgSendQueue.mongo_success));
            if (operation_type.compareTo("create") == 0) {
                MongoSendTask task = new MongoSendTask(context, bgSendQueue);
                task.execute();
            }
        }

        if(prefs.getBoolean("broadcast_data_through_intents", false)) {
            Log.i("SENSOR QUEUE:", "Broadcast data");
            final Bundle bundle = new Bundle();
            bundle.putDouble(Intents.EXTRA_BG_ESTIMATE, bgReading.calculated_value);
            bundle.putDouble(Intents.EXTRA_BG_SLOPE, bgReading.calculated_value_slope);
            if(bgReading.hide_slope) {
                bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, "9");
            } else {
                bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, bgReading.slopeName());
            }
            bundle.putInt(Intents.EXTRA_SENSOR_BATTERY, getBatteryLevel(context));
            bundle.putLong(Intents.EXTRA_TIMESTAMP, bgReading.timestamp);

            Intent intent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE);
            intent.putExtras(bundle);
            context.sendBroadcast(intent, Intents.RECEIVER_PERMISSION);
        }

        if(prefs.getBoolean("broadcast_to_pebble", false)) {
            PebbleSync pebbleSync = new PebbleSync();
            pebbleSync.sendData(context, bgReading);
        }

        if(prefs.getBoolean("share_upload", false)) {
            ShareRest shareRest = new ShareRest(context);
            Log.w("ShareRest", "About to call ShareRest!!");
            shareRest.sendBgData(bgReading);
        }
        wakeLock.release();
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
