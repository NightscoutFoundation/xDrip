package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Unitized.usingMgDl;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.g5model.DexSessionKeeper;
import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Noise;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import lombok.val;

// created by jamorham

public class BroadcastGlucose {

    private static final String TAG = "BroadcastGlucose";
    private static long lastTimestamp = 0;
    private static long dexStartedAt = 0;
    private static boolean connectedToG7 = false;
    private static boolean connectedToG6 = false;
    private static boolean usingG6OrG7 = false;

    public static void sendLocalBroadcast(final BgReading bgReading) {
        if (SendXdripBroadcast.enabled()) {
            final Bundle bundle = new Bundle();
            if (bgReading != null) {
                if (bgReading.calculated_value == 0) {
                    UserError.Log.wtf(TAG, "Refusing to broadcast reading with calculated value of 0");
                    return;
                }

                if (Math.abs(bgReading.timestamp - lastTimestamp) < MINUTE_IN_MS) {
                    val msg = String.format("Refusing to broadcast a reading with close timestamp to last broadcast:  %s (%d) vs %s (%d) ", dateTimeText(lastTimestamp), lastTimestamp, dateTimeText(bgReading.timestamp), bgReading.timestamp);
                    if (bgReading.timestamp == lastTimestamp) {
                        UserError.Log.d(TAG, msg);
                    } else {
                        UserError.Log.wtf(TAG, msg);
                    }
                    return;
                }

                val sensor = Sensor.currentSensor();
                if (sensor == null) {
                    UserError.Log.wtf(TAG, "Refusing to broadcast a reading as no sensor is active");
                    return;
                }

                if (bgReading.timestamp <= sensor.started_at) {
                    UserError.Log.wtf(TAG, "Refusing to broadcast a reading before sensor start:  " + dateTimeText(sensor.started_at) + " vs " + dateTimeText(bgReading.timestamp));
                    return;
                }

                lastTimestamp = bgReading.timestamp;

                BestGlucose.DisplayGlucose dg;

                UserError.Log.i("SENSOR QUEUE:", "Broadcast data");

                String collectorName = getBestCollectorHardwareName();
                if (collectorName.equals("G6 Native") || collectorName.equals("G7")) {
                    if (collectorName.equals("G7")) {
                        collectorName = "G6 Native"; // compatibility for older AAPS
                    }
                    collectorName += " / G5 Native"; // compatibility for older AAPS
                }
                bundle.putString(Intents.XDRIP_DATA_SOURCE_DESCRIPTION, collectorName);
                if (bgReading.source_info != null) {
                    bundle.putString(Intents.XDRIP_DATA_SOURCE_INFO, bgReading.source_info);
                }

                bundle.putString(Intents.XDRIP_VERSION_INFO, BuildConfig.buildVersion + " " + BuildConfig.VERSION_NAME);

                try {
                    final Calibration calib = Calibration.lastValid();
                    final long last_calibration_timestamp = calib != null ? calib.timestamp : -1;
                    bundle.putString(Intents.XDRIP_CALIBRATION_INFO, last_calibration_timestamp + "");
                } catch (Exception e) {
                    //
                }

                // TODO this cannot handle out of sequence data due to displayGlucose taking most recent?!
                // TODO can we do something with munging for quick data and getDisplayGlucose for non quick?
                // use display glucose if enabled and available

                final int noiseBlockLevel = Noise.getNoiseBlockLevel();
                bundle.putInt(Intents.EXTRA_NOISE_BLOCK_LEVEL, noiseBlockLevel);
                bundle.putString(Intents.EXTRA_NS_NOISE_LEVEL, bgReading.noise);
                if ((Pref.getBoolean("broadcast_data_use_best_glucose", false))
                        && !bgReading.isBackfilled()
                        && (msSince(bgReading.timestamp) < MINUTE_IN_MS * 5)
                        && ((dg = BestGlucose.getDisplayGlucose()) != null)) {

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

                        final boolean is_oop = DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(null);
                        if (dg.from_plugin || is_oop) {
                            bundle.putString(Intents.XDRIP_CALIBRATION_PLUGIN, (is_oop ? "OOP " : "") + dg.plugin_name);
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

                bundle.putInt(Intents.EXTRA_SENSOR_BATTERY, BridgeBattery.getBestBridgeBattery());
                if (getBestCollectorHardwareName().equals("G7") || getBestCollectorHardwareName().equals("G6 Native")) { // If we are using G7 or One+, or G6 in native mode
                    usingG6OrG7 = true;
                }
                if (getBestCollectorHardwareName().equals("G7") && FirmwareCapability.isDeviceAltOrAlt2OrAlt3(getTransmitterID())) { // If we are using G7 or One+ and there is connectivity
                    connectedToG7 = true;
                }
                if (getBestCollectorHardwareName().equals("G6 Native") && FirmwareCapability.isTransmitterG6(getTransmitterID())) { // If we are using a G6 in native mode and there is connectivity
                    connectedToG6 = true;
                }
                if (usingG6OrG7) { // If we are using G7 or G6 in native mode
                    if (connectedToG6 || connectedToG7) { // Only if there is connectivity
                        dexStartedAt = DexSessionKeeper.getStart(); // Session start time reported by the Dexcom transmitter
                        if (dexStartedAt > 0) { // Only if dexStartedAt is valid
                            bundle.putLong(Intents.EXTRA_SENSOR_STARTED_AT, dexStartedAt);
                        }
                    }
                } else { // If we are not using G7, One+ or G6 in native mode
                    bundle.putLong(Intents.EXTRA_SENSOR_STARTED_AT, sensor.started_at);
                }
                bundle.putLong(Intents.EXTRA_TIMESTAMP, bgReading.timestamp);

                addDisplayInformation(bundle);

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

                addCollectorStatus(bundle);
                final Intent intent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE);
                SendXdripBroadcast.send(intent, bundle);
                //JoH.dumpBundle(bundle, TAG);
            } else {
                // just status update
                addCollectorStatus(bundle);
                final Intent intent = new Intent(Intents.ACTION_STATUS_UPDATE);
                SendXdripBroadcast.send(intent, bundle);
            }
        }
    }

    private static void addCollectorStatus(final Bundle bundle) {
        final String result = NanoStatus.nanoStatus("collector");
        if (result != null) {
            bundle.putString(Intents.EXTRA_COLLECTOR_NANOSTATUS, result);
        }
    }

    private static void addDisplayInformation(final Bundle bundle) {
        bundle.putString(Intents.EXTRA_DISPLAY_UNITS, Unitized.unit(usingMgDl()));
    }
}
