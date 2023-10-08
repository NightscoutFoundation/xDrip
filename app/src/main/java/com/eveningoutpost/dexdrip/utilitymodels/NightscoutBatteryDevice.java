package com.eveningoutpost.dexdrip.utilitymodels;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.g5model.Ob1DexTransmitterBattery;
import com.eveningoutpost.dexdrip.services.DexCollectionService;

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
    },

    DEXCOM_TRANSMITTER {
        /**
         * This is only used as a check before uploading that we were able to
         * obtain data for the transmitter in NightscoutUploader.
         */
        @Override
        int getBatteryLevel(Context mContext) {
            Ob1DexTransmitterBattery b = new Ob1DexTransmitterBattery();
            if (b.isPresent()) {
                return b.voltageA();
            }
            return -1;
        }

        /**
         * Returns a JSON blob such as:
         * {
         *     "days": 50,
         *     "daysEstimate": "50" or "50 / 100" or "50 100",
         *     "status": "UNKNOWN" or "BRICKED" or "LOW" or "OK",
         *     "voltagea": 285,
         *     "voltagea_warning": true,
         *     "voltageb": 275,
         *     "voltageb_warning": true,
         *     "resistance": 100 or null,
         *     "resistance_status": "GOOD" "NORMAL" "WARNING" "BAD" or "UNKNOWN",
         *     "temperature": 75 or null,
         *     "battery": "50 days (voltage: 285/275)"
         * }
         */
        @Override
        public JSONObject getUploaderJson(Context mContext) throws JSONException {
            JSONObject uploader = new JSONObject();

            Ob1DexTransmitterBattery b = new Ob1DexTransmitterBattery();

            if (!b.isPresent()) return null;

            uploader.put("days", b.days());
            uploader.put("daysEstimate", b.daysEstimate());
            uploader.put("status", b.status().name());
            uploader.put("voltagea", b.voltageA());
            if (b.voltageAWarning()) {
                uploader.put("voltagea_warning", true);
            }
            uploader.put("voltageb", b.voltageB());
            if (b.voltageBWarning()) {
                uploader.put("voltageb_warning", true);
            }
            if (b.resistanceStatus() != Ob1DexTransmitterBattery.ResistanceStatus.UNKNOWN) {
                uploader.put("resistance", b.resistance());
                uploader.put("resistance_status", b.resistanceStatus().name());
            }
            uploader.put("temperature", b.temperature());

            // What nightscout will normally show in the UI
            uploader.put("battery", b.days() + " days (voltage: " + b.voltageA() + "/" + b.voltageB() + ")");

            // Epoch timestamp representing when the battery was last queried.
            uploader.put("lastQueried", b.lastQueried());

            uploader.put("type", name());
            return uploader;
        }

        /**
         * Only sends to Nightscout when there is a change in the voltage A value.
         */
        @Override
        boolean alwaysSendBattery() {
            return false;
        }

        @Override
        String getDeviceName() {
            if (Ob1G5StateMachine.usingG6()) {
                return "G6 Transmitter";
            }
            return "G5 Transmitter";
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
     * Defaults to returning a JSONObject with integer key battery (required by Nightscout)
     * and a string key type with the value of the enum (e.g. PHONE, DEXCOM_TRANSMITTER)
     * @param mContext is supplied so that the phone battery can be retrieved.
     */
    public JSONObject getUploaderJson(Context mContext) throws JSONException {
        JSONObject uploader = new JSONObject();
        uploader.put("battery", getBatteryLevel(mContext));
        uploader.put("type", name());
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
