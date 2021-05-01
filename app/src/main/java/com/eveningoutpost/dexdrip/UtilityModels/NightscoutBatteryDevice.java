package com.eveningoutpost.dexdrip.UtilityModels;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.G5Model.Ob1DexTransmitterBattery;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a device type tracked by xDrip which can be uploaded to Nightscout devicestatus.
 *
 * Contains methods for identifying the current battery level and details for how the device's
 * status will be sent to Nightscout.
 *
 * @author James Woglom (j@wogloms.net)
 */
public enum NightscoutBatteryDevice {
    PHONE {
        @Override
        int getBatteryLevel(Context mContext) {
            Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level == -1 || scale == -1) {
                    return 50;
                }
                return (int) (((float) level / (float) scale) * 100.0f);
            } else return 50;
        }

        @Override
        String getDeviceName() {
            return Build.MANUFACTURER + " " + Build.MODEL;
        }
    },
    BRIDGE {
        @Override
        int getBatteryLevel(Context mContext) {
            return Pref.getInt("bridge_battery", -1);
        }

        @Override
        String getDeviceName() {
            return DexCollectionService.getBestLimitterHardwareName();
        }
    },
    PARAKEET {
        @Override
        int getBatteryLevel(Context mContext) {
            return Pref.getInt("parakeet_battery", -1);
        }

        @Override
        String getDeviceName() {
            return "Parakeet";
        }
    };


    /**
     * Returns the battery level of the device.
     *
     * @param mContext is supplied so that the phone battery can be retrieved.
     */
    abstract int getBatteryLevel(Context mContext);

    /**
     * Returns the JSON object containing the battery details.
     * Defaults to returning a JSONObject with integer key battery.
     *
     * @param mContext is supplied so that the phone battery can be retrieved.
     */
    public JSONObject getUploaderJson(Context mContext) throws JSONException {
        JSONObject uploader = new JSONObject();
        uploader.put("battery", getBatteryLevel(mContext));
        return uploader;
    }

    /**
     * Returns the device name sent to Nightscout for the device.
     */
    abstract String getDeviceName();

    /**
     * If true, the battery status is always uploaded to Nightscout.
     * If false, the battery status is only uploaded when it changes.
     *
     * Nightscout doesn't currently display a device's status if it thinks
     * that it is stale, so this has always been set to TRUE.
     */
    boolean alwaysSendBattery() {
        return true;
    }
}
