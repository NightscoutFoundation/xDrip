package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.List;

import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;

/**
 * Created by jamorham on 17/10/2016.
 * <p>
 * Try to get the best usable glucose values from available data
 * and calibration algorithms.
 * <p>
 * Designed to replace duplication within code and allow
 * for unified value to be shown that may come from multiple
 * sources.
 */

public class BestGlucose {

    final static String TAG = "BestGlucose";

    public static class DisplayGlucose {
        public double mgdl = -1;
        public double delta_mgdl = 0;
        public int warning = -1;
        public long mssince = -1;
        public long timestamp = -1;
        public boolean stale = true;
        public String unitized = "void";
        public String unitized_delta = "";
        public String unitized_delta_no_units = "";
        public String delta_arrow = "";
        public String delta_name = "";
        public String extra_string = "";
        public String plugin_name = "";
        public boolean from_plugin = false;

        public String minutesAgo() {
            return minutesAgo(false);
        }

        public String minutesAgo(boolean include_words) {
            final int minutes = ((int) (this.mssince / 60000));
            return Integer.toString(minutes) + (include_words ? (((minutes == 1) ? xdrip.getAppContext().getString(R.string.space_minute_ago) : xdrip.getAppContext().getString(R.string.space_minutes_ago))) : "");
        }

    }

    // note we don't support the original depreciated "predictive" mode
    // TODO stale data
    // TODO time ago
    // TODO check BGgraph data is current
    // TODO internalize delta handling to handle irregular periods and missing data plugins etc
    // TODO see getSlopeArrowSymbolBeforeCalibration for calculation method for arbitary slope
    // TODO option to process noise or not

    public static DisplayGlucose getDisplayGlucose() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        final boolean doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
        final boolean is_follower = Home.get_follower();
        final DisplayGlucose dg = new DisplayGlucose(); // return value

        List<BgReading> last_2 = BgReading.latest(2);

        final BgReading lastBgReading = BgReading.last(is_follower);
        if (lastBgReading == null) return null;

        final CalibrationAbstract.CalibrationData pcalibration;
        final CalibrationAbstract plugin = getCalibrationPluginFromPreferences();

        double estimate = -1;
        double filtered = -1;
        long timestamp = -1;
        double previous_estimate = -1;
        double previous_filtered = -1;
        long previous_timestamp = -1;

        estimate = lastBgReading.calculated_value; // normal first
        filtered = lastBgReading.filtered_calculated_value;
        timestamp = lastBgReading.timestamp;

        if (last_2.size() == 2) {
            previous_estimate = last_2.get(1).calculated_value;
            previous_filtered = last_2.get(1).filtered_calculated_value;
            previous_timestamp = last_2.get(1).timestamp;
        }

        dg.mssince = JoH.msSince(lastBgReading.timestamp);

        dg.timestamp = lastBgReading.timestamp;
        // TODO set stale or use getter maybe
        dg.stale = dg.mssince > Home.stale_data_millis();

        // if we are actively using a plugin, get the glucose calculation from there
        if ((plugin != null) && ((pcalibration = plugin.getCalibrationData()) != null) && (Home.getPreferencesBoolean("display_glucose_from_plugin", false))) {
            dg.plugin_name = plugin.getAlgorithmName();
            Log.d(TAG, "Using plugin: " + dg.plugin_name);
            dg.from_plugin = true;
            estimate = plugin.getGlucoseFromBgReading(lastBgReading, pcalibration);
            filtered = plugin.getGlucoseFromFilteredBgReading(lastBgReading, pcalibration);

            // also try to update the previous values in the same way
            if (last_2.size() == 2) {
                previous_estimate = plugin.getGlucoseFromBgReading(last_2.get(1), pcalibration);
                previous_filtered = plugin.getGlucoseFromFilteredBgReading(last_2.get(1), pcalibration);
            }

        }

        int warning_level = 0;
        String slope_arrow = "";
        String slope_name = "";
        String extrastring = "";
        double estimated_delta = 0;

        // TODO refresh bggraph if needed based on cache - observe

        boolean bg_from_filtered = prefs.getBoolean("bg_from_filtered", false);
        // if noise has settled down then switch off filtered mode
        if ((bg_from_filtered) && (BgGraphBuilder.last_noise < BgGraphBuilder.NOISE_FORGIVE) && (prefs.getBoolean("bg_compensate_noise", false))) {
            bg_from_filtered = false;
            prefs.edit().putBoolean("bg_from_filtered", false).apply();
        }

        // TODO Noise uses plugin in bggraphbuilder
        if ((BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_TRIGGER)
                && (BgGraphBuilder.best_bg_estimate > 0)
                && (BgGraphBuilder.last_bg_estimate > 0)
                && (prefs.getBoolean("bg_compensate_noise", false))) {
            estimate = BgGraphBuilder.best_bg_estimate; // this maybe needs scaling based on noise intensity
            estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
            // TODO handle ratio when period is not dexcom period?
            double estimated_delta_by_minute = estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000);
            dg.unitized_delta_no_units = BgGraphBuilder.unitizedDeltaStringRaw(false, true, estimated_delta_by_minute, doMgdl);
            // TODO optimize adding units
            dg.unitized_delta = BgGraphBuilder.unitizedDeltaStringRaw(true, true, estimated_delta_by_minute, doMgdl);
            slope_arrow = BgReading.slopeToArrowSymbol(estimated_delta_by_minute); // delta by minute
            slope_name = BgReading.slopeName(estimated_delta_by_minute);
            extrastring = "\u26A0"; // warning symbol !
            warning_level = 1;

            if ((BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_HIGH) && (DexCollectionType.hasFiltered())) {
                bg_from_filtered = true; // force filtered mode
            }

        } else {
            // TODO ignores plugin
            //dg.unitized_delta = BgGraphBuilder.unitizedDeltaString(true, true, is_follower , doMgdl);
            dg.unitized_delta_no_units = unitizedDeltaString(false, true, doMgdl, estimate, timestamp, previous_estimate, previous_timestamp);
            estimated_delta = estimate - previous_estimate; // TODO time stretch adjustment?
            // TODO optimize adding units
            dg.unitized_delta = unitizedDeltaString(true, true, doMgdl, estimate, timestamp, previous_estimate, previous_timestamp);
            long time_delta = timestamp - previous_timestamp;
            if (time_delta < 0) Log.wtf(TAG, "Time delta is negative! : " + time_delta);
            //slope_arrow = lastBgReading.slopeArrow(); // internalize this for plugins
            double slope = calculateSlope(estimate, timestamp, previous_estimate, previous_timestamp);
            slope_arrow = BgReading.slopeToArrowSymbol(slope * 60000); // slope by minute
            slope_name = BgReading.slopeName(slope*60000);
            Log.d(TAG, "No noise option slope by minute: " + (slope * 60000));
        }

        // TODO bit more work on deltas etc needed here
        if (bg_from_filtered) {
            estimate = filtered;
            warning_level = 2;
        }



        final String stringEstimate = BgGraphBuilder.unitized_string(estimate, doMgdl);
        if ((lastBgReading.hide_slope) || (bg_from_filtered)) {
            slope_arrow = "";
            slope_name = "NOT COMPUTABLE";
        }

        dg.mgdl = estimate;
        dg.delta_mgdl = estimated_delta;
        dg.warning = warning_level;
        dg.unitized = stringEstimate;
        dg.delta_arrow = slope_arrow;
        dg.extra_string = extrastring;
        dg.delta_name = slope_name;

        Log.d(TAG, "dg result: " + dg.unitized);
        return dg;
    }

    public static String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean doMgdl, double value1, long timestamp1, double value2, long timestamp2) {

        // timestamps invalid or too far apart return ???
        if ((timestamp1 < 0)
                || (timestamp2 < 0)
                || (value1 < 0)
                || (value2 < 0)
                || (timestamp2 > timestamp1)
                || (timestamp1 - timestamp2 > (20 * 60 * 1000)))
            return "???";

        double value = calculateSlope(value1, timestamp1, value2, timestamp2) * 5 * 60 * 1000;

        return BgGraphBuilder.unitizedDeltaStringRaw(showUnit, highGranularity, value, doMgdl);
    }

    public static double calculateSlope(double value1, long timestamp1, double value2, long timestamp2) {
        if (timestamp1 == timestamp2 || value1 == value2) {
            return 0;
        } else {
            return (value2 - value1) / (timestamp2 - timestamp1);
        }
    }


}
