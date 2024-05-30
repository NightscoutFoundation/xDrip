package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.models.Accuracy;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.stats.StatsResult;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPlugin;
import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;

// jamorham


public class StatusLine {

    private static final String TAG = "StatusLine";
    private static final boolean D = false;
    private static String cache;
    private static volatile long cacheTime;

    @NonNull
    public static String extraStatusLine() {
        if (JoH.msSince(cacheTime) > Constants.SECOND_IN_MS * 5) {
            cache = extraStatusLineReal();
            cacheTime = JoH.tsl();
            if (D) UserError.Log.d(TAG, "Cache Miss");
        } else {
            if (D) UserError.Log.d(TAG, "Cache Hit");
        }
        return cache;
    }

    public static void invalidateCache() {
        cacheTime = 0;
    }

    @NonNull
    private static String extraStatusLineReal() {

        final StringBuilder sb = new StringBuilder();
        Calibration lastCalibration;

        if (Pref.getBoolean("status_line_calibration_long", false) && ((lastCalibration = Calibration.lastValid()) != null)) {
            append(sb, "slope = ");
            sb.append(String.format("%.2f", lastCalibration.slope));
            sb.append(' ');
            sb.append("inter = ");
            sb.append(String.format("%.2f", lastCalibration.intercept));
        }

        if (Pref.getBoolean("status_line_calibration_short", false) && ((lastCalibration = Calibration.lastValid()) != null)) {
            append(sb, "s:");
            sb.append(String.format("%.2f", lastCalibration.slope));
            sb.append(' ');
            sb.append("i:");
            sb.append(String.format("%.2f", lastCalibration.intercept));
        }


        if (Pref.getBoolean("status_line_avg", false)
                || Pref.getBoolean("status_line_a1c_dcct", false)
                || Pref.getBoolean("status_line_a1c_ifcc", false)
                || Pref.getBoolean("status_line_in", false)
                || Pref.getBoolean("status_line_high", false)
                || Pref.getBoolean("status_line_low", false)
                || Pref.getBoolean("status_line_stdev", false)
                || Pref.getBoolean("status_line_carbs", false)
                || Pref.getBoolean("status_line_insulin", false)
                || Pref.getBoolean("status_line_royce_ratio", false)
                || Pref.getBoolean("status_line_accuracy", false)
                || Pref.getBoolean("status_line_capture_percentage", false)
                || Pref.getBoolean("status_line_realtime_capture_percentage", false)) {


            if (D) UserError.Log.d(TAG, "Getting StatsResult");

            final StatsResult statsResult = new StatsResult(Pref.getInstance(), Pref.getBooleanDefaultFalse("extra_status_stats_24h"));

            if (Pref.getBoolean("status_line_avg", false)) {
                append(sb, statsResult.getAverageUnitised());
            }
            if (Pref.getBoolean("status_line_a1c_dcct", false)) {
                append(sb, statsResult.getA1cDCCT());
            }
            if (Pref.getBoolean("status_line_a1c_ifcc", false)) {
                append(sb, statsResult.getA1cIFCC());
            }
            if (Pref.getBoolean("status_line_in", false)) {
                append(sb, statsResult.getInPercentage());
            }
            if (Pref.getBoolean("status_line_high", false)) {
                append(sb, statsResult.getHighPercentage());
            }
            if (Pref.getBoolean("status_line_low", false)) {
                append(sb, statsResult.getLowPercentage());
            }
            if (Pref.getBoolean("status_line_stdev", false)) {
                append(sb, statsResult.getStdevUnitised());
            }
            if (Pref.getBoolean("status_line_carbs", false)) {
                append(sb, "Carbs:" + Math.round(statsResult.getTotal_carbs()));
            }
            if (Pref.getBoolean("status_line_insulin", false)) {
                append(sb, "U:" + JoH.qs(statsResult.getTotal_insulin(), 2));
            }
            if (Pref.getBoolean("status_line_royce_ratio", false)) {
                append(sb, "C/I:" + JoH.qs(statsResult.getRatio(), 2));
            }
            if (Pref.getBoolean("status_line_capture_percentage", false)) {
                append(sb, statsResult.getCapturePercentage(false));
            }
            if (Pref.getBoolean("status_line_realtime_capture_percentage", false) &&
                    statsResult.canShowRealtimeCapture()) {
                append(sb, statsResult.getRealtimeCapturePercentage(false));
            }
            if (Pref.getBoolean("status_line_accuracy", false)) {
                final long accuracy_period = DAY_IN_MS * 3;
                final String accuracy_report = Accuracy.evaluateAccuracy(accuracy_period);
                if ((accuracy_report != null) && (accuracy_report.length() > 0)) {
                    append(sb, accuracy_report);
                } else {
                    final String accuracy = BloodTest.evaluateAccuracy(accuracy_period);
                    append(sb, ((accuracy != null) ? " " + accuracy : ""));
                }
            }

        } // if using stats result

        if (Pref.getBoolean("status_line_pump_reservoir", false)) {
            append(sb, PumpStatus.getBolusIoBString());
            sb.append(PumpStatus.getReservoirString());
            sb.append(PumpStatus.getBatteryString());
        }

        if (Pref.getBooleanDefaultFalse("status_line_external_status")) {
            append(sb, ExternalStatusService.getLastStatusLine());
        }

        if (Pref.getBoolean("extra_status_calibration_plugin", false)) {
            final CalibrationAbstract plugin = getCalibrationPluginFromPreferences(); // make sure do this only once
            if (plugin != null) {
                final CalibrationAbstract.CalibrationData pcalibration = plugin.getCalibrationData();
                if (sb.length() > 0) sb.append("\n"); // not tested on the widget yet
                if (pcalibration != null)
                    sb.append("(" + plugin.getAlgorithmName() + ") s:" + JoH.qs(pcalibration.slope, 2) + " i:" + JoH.qs(pcalibration.intercept, 2));
                BgReading bgReading = BgReading.last();
                if (bgReading != null) {
                    final boolean doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
                    sb.append(" \u21D2 " + BgGraphBuilder.unitized_string(plugin.getGlucoseFromSensorValue(bgReading.age_adjusted_raw_value), doMgdl) + " " + BgGraphBuilder.unit(doMgdl));
                }
            }

            // If we are using the plugin as the primary then show xdrip original as well
            if (Pref.getBooleanDefaultFalse("display_glucose_from_plugin") || Pref.getBooleanDefaultFalse("use_pluggable_alg_as_primary")) {
                final CalibrationAbstract plugin_xdrip = getCalibrationPlugin(PluggableCalibration.Type.xDripOriginal); // make sure do this only once
                if (plugin_xdrip != null) {
                    final CalibrationAbstract.CalibrationData pcalibration = plugin_xdrip.getCalibrationData();
                    if (sb.length() > 0)
                        sb.append("\n"); // not tested on the widget yet
                    if (pcalibration != null)
                        sb.append("(" + plugin_xdrip.getAlgorithmName() + ") s:" + JoH.qs(pcalibration.slope, 2) + " i:" + JoH.qs(pcalibration.intercept, 2));
                    BgReading bgReading = BgReading.last();
                    if (bgReading != null) {
                        final boolean doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
                        sb.append(" \u21D2 " + BgGraphBuilder.unitized_string(plugin_xdrip.getGlucoseFromSensorValue(bgReading.age_adjusted_raw_value), doMgdl) + " " + BgGraphBuilder.unit(doMgdl));
                    }
                }
            }

        }

        if (Pref.getBoolean("status_line_time", false)) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            append(sb, sdf.format(new Date()));
        }
        return sb.toString();

    }

    private static void append(final StringBuilder sb, final String value) {
        if (sb.length() != 0) sb.append(' ');
        sb.append(value);
    }
}
