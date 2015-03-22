package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Date;
import java.util.UUID;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 */
public class PebbleSync {
    //    CGM_ICON_KEY = 0x0,		// TUPLE_CSTRING, MAX 2 BYTES (10)
    //    CGM_BG_KEY = 0x1,		// TUPLE_CSTRING, MAX 4 BYTES (253 OR 22.2)
    //    CGM_TCGM_KEY = 0x2,		// TUPLE_INT, 4 BYTES (CGM TIME)
    //    CGM_TAPP_KEY = 0x3,		// TUPLE_INT, 4 BYTES (APP / PHONE TIME)
    //    CGM_DLTA_KEY = 0x4,		// TUPLE_CSTRING, MAX 5 BYTES (BG DELTA, -100 or -10.0)
    //    CGM_UBAT_KEY = 0x5,		// TUPLE_CSTRING, MAX 3 BYTES (UPLOADER BATTERY, 100)
    //    CGM_NAME_KEY = 0x6		// TUPLE_CSTRING, MAX 9 BYTES (xDrip)
    public static final UUID PEBBLEAPP_UUID = UUID.fromString("2c3f5ab3-7506-44e7-b8d0-2c63de32e1ec");
    public static final int ICON_KEY = 0;
    public static final int BG_KEY = 1;
    public static final int RECORD_TIME_KEY = 2;
    public static final int PHONE_TIME_KEY = 3;
    public static final int BG_DELTA_KEY = 4;
    public static final int UPLOADER_BATTERY_KEY = 5;
    public static final int NAME_KEY = 6;

    private Context mContext;
    private BgGraphBuilder bgGraphBuilder;
    private BgReading mBgReading;

    public PebbleDictionary buildDictionary() {
        PebbleDictionary dictionary = new PebbleDictionary();
        dictionary.addString(ICON_KEY, slopeOrdinal());
        dictionary.addString(BG_KEY, bgReading());
        dictionary.addUint32(RECORD_TIME_KEY, (int) (mBgReading.timestamp / 1000));
        dictionary.addUint32(PHONE_TIME_KEY, (int) (new Date().getTime() / 1000));
        dictionary.addString(BG_DELTA_KEY, bgDelta());
        dictionary.addString(UPLOADER_BATTERY_KEY, phoneBattery());
        dictionary.addString(NAME_KEY, "xDrip");
        return dictionary;
    }

    public void sendData(Context context, BgReading bgReading){
        mContext = context;
        bgGraphBuilder = new BgGraphBuilder(mContext);
        mBgReading = BgReading.last();
        sendDownload(buildDictionary());
    }

    public String bgReading() {
        return bgGraphBuilder.unitized_string(mBgReading.calculated_value);
    }

    public String bgDelta() {
        String deltaString = bgGraphBuilder.unitized_string((int)(mBgReading.calculated_value_slope * (5 * 60 * 1000)));
        if(mBgReading.calculated_value_slope > 0.1) {
            return ("+"+deltaString);
        } else if(mBgReading.calculated_value_slope > -0.1 && mBgReading.calculated_value_slope < 0.1) {
            return "0";
        } else {
            return deltaString;
        }
    }

    public String phoneBattery() {
        return String.valueOf(getBatteryLevel());
    }

    public String bgUnit() {
        return bgGraphBuilder.unit();
    }

    public void sendDownload(PebbleDictionary dictionary) {
        if (PebbleKit.isWatchConnected(mContext)) {
            if (dictionary != null && mContext != null) {
                Log.d("PEBBLE PUSHER", "Sending data to pebble");
                PebbleKit.sendDataToPebble(mContext, PEBBLEAPP_UUID, dictionary);
            }
        }
    }

    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) {
            return 50;
        }
        return (int)(((float)level / (float)scale) * 100.0f);
    }

    public String slopeOrdinal(){
        double slope_by_minute = mBgReading.calculated_value_slope * 60000;
        String arrow = "0";
        if (slope_by_minute <= (-3.5)) {
            arrow = "7";
        } else if (slope_by_minute <= (-2)) {
            arrow = "6";
        } else if (slope_by_minute <= (-1)) {
            arrow = "5";
        } else if (slope_by_minute <= (1)) {
            arrow = "4";
        } else if (slope_by_minute <= (2)) {
            arrow = "3";
        } else if (slope_by_minute <= (3.5)) {
            arrow = "2";
        } else {
            arrow = "1";
        }
        if(mBgReading.hide_slope) {
            arrow = "9";
        }
        return arrow;
    }
}

