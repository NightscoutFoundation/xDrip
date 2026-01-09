package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.List;

import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;
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
    final static boolean d = true; // debug flag
    private static SharedPreferences prefs;

    public static class DisplayGlucose {
        private Boolean stale = null;
        private Double highMark = null;
        private Double lowMark = null;
        public boolean doMgDl = true; // mgdl/mmol
        public double mgdl = -1;    // displayable mgdl figure
        public double unitized_value = -1; // in local units
        public double delta_mgdl = 0; // displayable delta mgdl figure
        public double slope = 0; // slope metric mgdl/ms
        public double noise = -1; // noise value
        public int warning = -1;  // warning level
        public long mssince = -1;
        public long timestamp = -1; // timestamp of reading
        public String unitized = "void";
        public String unitized_delta = "";
        public String unitized_delta_no_units = "";
        public String delta_arrow = ""; // unicode delta arrow
        public String delta_name = "";
        public String extra_string = "";
        public String plugin_name = ""; // plugin which generated this data
        public boolean from_plugin = false; // whether a plugin was used


        // Display getters - built in caching where appropriate

        public String minutesAgo() {
            return minutesAgo(false);
        }

        public String minutesAgo(boolean include_words) {
            final int minutes = ((int) (this.mssince / 60000));
            return Integer.toString(minutes) + (include_words ? (((minutes == 1) ? xdrip.getAppContext().getString(R.string.space_minute_ago) : xdrip.getAppContext().getString(R.string.space_minutes_ago))) : "");
        }

        // return boolean if data would be considered stale
        public boolean isStale() {
            if (this.stale == null) {
                this.stale = this.mssince > Home.stale_data_millis();
            }
            return this.stale;
        }

        // return boolean if data would be considered stale
        public boolean isReallyStale() {
                return this.mssince > (Home.stale_data_millis()*3);
        }


        // is this value above the "High" preference value
        public boolean isHigh() {
            if (this.highMark == null)
                this.highMark = JoH.tolerantParseDouble(prefs.getString("highValue", "170"), 170d);
            return this.unitized_value >= this.highMark;
        }

        // is this value below the "Low" preference value
        public boolean isLow() {
            if (this.lowMark == null)
                this.lowMark = JoH.tolerantParseDouble(prefs.getString("lowValue", "70"), 70d);
            return this.unitized_value <= this.lowMark;
        }

        // return strikeout string if data is high/low / stale
        public SpannableString spannableString(String str) {
            return spannableString(str, false);
        }

        // return a coloured strikeout string based on boolean
        public SpannableString spannableString(String str, boolean color) {
            final SpannableString ret = new SpannableString((str != null) ? str : "");
            if (isStale()) wholeSpan(ret, new StrikethroughSpan());
            if (color) {
                if (isLow()) {
                    wholeSpan(ret, new ForegroundColorSpan(getCol(ColorCache.X.color_low_bg_values)));
                } else if (isHigh()) {
                    wholeSpan(ret, new ForegroundColorSpan(getCol(ColorCache.X.color_high_bg_values)));
                } else {
                    wholeSpan(ret, new ForegroundColorSpan(getCol(ColorCache.X.color_inrange_bg_values)));
                }
            }
            return ret;
        }

        // set the whole spannable string to whatever this span is
        private void wholeSpan(SpannableString ret, Object what) {
            ret.setSpan(what, 0, ret.length(), 0);
        }

        public String humanSummary() {
            return unitized + " " + (doMgDl ? "mg/dl" : "mmol/l") + (isStale() ? ", " + minutesAgo(true).toLowerCase() : "");
        }

    }

    // note we don't support the original depreciated "predictive" mode
    // TODO stale data
    // TODO time ago
    // TODO check BGgraph data is current
    // TODO internalize delta handling to handle irregular periods and missing data plugins etc
    // TODO see getSlopeArrowSymbolBeforeCalibration for calculation method for arbitary slope
    // TODO option to process noise or not
    // TODO check what happens if there is only a single entry, especially regarding delta
    // TODO select by time


    public static DisplayGlucose getDisplayGlucose() {

        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        final DisplayGlucose dg = new DisplayGlucose(); // return value
        final boolean doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
        final boolean is_follower = Home.get_follower();

        dg.doMgDl = doMgdl;

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

        // if we are actively using a plugin, get the glucose calculation from there
        if ((plugin != null) && ((pcalibration = plugin.getCalibrationData()) != null) && (Pref.getBoolean("display_glucose_from_plugin", false))) {
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
        BgGraphBuilder.refreshNoiseIfOlderThan(dg.timestamp); // should this be conditional on whether bg_compensate_noise is set?
        dg.noise = BgGraphBuilder.last_noise;

        boolean bg_from_filtered = prefs.getBoolean("bg_from_filtered", false);
        // if noise has settled down then switch off filtered mode
        if ((bg_from_filtered) && (BgGraphBuilder.last_noise < BgGraphBuilder.NOISE_FORGIVE) && (prefs.getBoolean("bg_compensate_noise", false))) {
            bg_from_filtered = false;
            prefs.edit().putBoolean("bg_from_filtered", false).apply();
        }

        // TODO Noise uses plugin in bggraphbuilder
        if (compensateNoise()) {
            estimate = BgGraphBuilder.best_bg_estimate; // this maybe needs scaling based on noise intensity
            estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
            // TODO handle ratio when period is not dexcom period?
            double estimated_delta_by_minute = estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000);
            dg.slope = estimated_delta_by_minute / 60000;
            dg.unitized_delta_no_units = BgGraphBuilder.unitizedDeltaStringRaw(false, true, estimated_delta, doMgdl);
            // TODO optimize adding units
            dg.unitized_delta = BgGraphBuilder.unitizedDeltaStringRaw(true, true, estimated_delta, doMgdl);
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
            dg.slope = slope;
            slope_arrow = BgReading.slopeToArrowSymbol(slope * 60000); // slope by minute
            slope_name = BgReading.slopeName(slope * 60000);
            Log.d(TAG, "No noise option slope by minute: " + JoH.qs(slope * 60000, 5));
        }

        // TODO bit more work on deltas etc needed here
        if (bg_from_filtered) {
            estimate = filtered;
            warning_level = 2;
        }

        dg.unitized_value = BgGraphBuilder.unitized(estimate, doMgdl);
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

        // fail safe for excessive raw data values - this may want
        // to be moved one day
        if (!SensorSanity.isRawValueSane(lastBgReading.raw_data)) {
            dg.delta_arrow = "!";
            dg.unitized = ">!?";
            dg.mgdl = 0;
            dg.delta_mgdl = 0;
            dg.unitized_value = 0;
            dg.unitized_delta = "";
            dg.slope = 0;
            if (JoH.ratelimit("exceeding_max_raw", 120)) {
                UserError.Log.wtf(TAG, "Failing raw bounds validation: " + lastBgReading.raw_data);
            }
        }

        if (d)
            Log.d(TAG, "dg result: " + dg.unitized + " previous: " + BgGraphBuilder.unitized_string(previous_estimate, doMgdl));
        return dg;
    }

    public static boolean compensateNoise() {
        return (BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_TRIGGER
                || (BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_TRIGGER_ULTRASENSITIVE
                        && Pref.getBooleanDefaultFalse("engineering_mode")
                        && Pref.getBooleanDefaultFalse("bg_compensate_noise_ultrasensitive")
                ))
                && (BgGraphBuilder.best_bg_estimate > 0)
                && (BgGraphBuilder.last_bg_estimate > 0)
                && (prefs.getBoolean("bg_compensate_noise", false));
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

    private static double calculateSlope(double value1, long timestamp1, double value2, long timestamp2) {
        if (timestamp1 == timestamp2 || value1 == value2) {
            return 0;
        } else {
            return (value2 - value1) / (timestamp2 - timestamp1);
        }
    }


}
