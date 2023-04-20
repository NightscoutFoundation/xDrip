package com.eveningoutpost.dexdrip.utilitymodels;

// jamorham

import com.eveningoutpost.dexdrip.models.BgReading;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class Unitized {

    // TODO move static BgGraphBuilder methods here

    public static boolean usingMgDl() {
        return Pref.getString("units", "mgdl").equals("mgdl");
    }

    public static double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }

    public static double mgdlConvert(double mmol) {
        return mmol * Constants.MMOLL_TO_MGDL;
    }

    public static double unitized(double value, boolean doMgdl) {
        if (doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }


    public static String unitized_string_static(double value) {
        return unitized_string(value, Pref.getString("units", "mgdl").equals("mgdl"));
    }

    public static String unitized_string_with_units_static(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return unitized_string(value, domgdl)+" "+(domgdl ? "mg/dl" : "mmol/l");
    }

    public static String unitized_string_with_units_static_short(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return unitized_string(value, domgdl)+" "+(domgdl ? "mgdl" : "mmol");
    }

    public static String unitized_string_static_no_interpretation_short(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        final DecimalFormat df = new DecimalFormat("#");
        if (domgdl) {
            df.setMaximumFractionDigits(0);
        } else {
            df.setMaximumFractionDigits(1);
        }
        return df.format(unitized(value, domgdl)) + " " + (domgdl ? "mgdl" : "mmol");
    }

    public static String unitized_string(double value, boolean doMgdl) {
        final DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            return "HIGH";
        } else if (value >= 40) {
            if (doMgdl) {
                df.setMaximumFractionDigits(0);
                return df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                //next line ensures mmol/l value is XX.x always.  Required by PebbleWatchSync, and probably not a bad idea.
                df.setMinimumFractionDigits(1);
                return df.format(mmolConvert(value));
            }
        } else if (value > 12) {
            return "LOW";
        } else {
            switch ((int) value) {
                case 0:
                    return "??0";
                case 1:
                    return "?SN";
                case 2:
                    return "??2";
                case 3:
                    return "?NA";
                case 5:
                    return "?NC";
                case 6:
                    return "?CD";
                case 9:
                    return "?AD";
                case 12:
                    return "?RF";
                default:
                    return "???";
            }
        }
    }


    public static String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean is_follower, boolean doMgdl) {

        List<BgReading> last2 = BgReading.latest(2,is_follower);
        if (last2.size() < 2 || last2.get(0).timestamp - last2.get(1).timestamp > 20 * 60 * 1000) {
            // don't show delta if there are not enough values or the values are more than 20 mintes apart
            return "???";
        }

        double value = BgReading.currentSlope(is_follower) * 5 * 60 * 1000;

        return unitizedDeltaStringRaw(showUnit, highGranularity, value, doMgdl);
    }


    public static String unitizedDeltaStringRaw(boolean showUnit, boolean highGranularity,double value, boolean doMgdl) {


        if (Math.abs(value) > 100) {
            // a delta > 100 will not happen with real BG values -> problematic sensor data
            return "ERR";
        }

        // TODO: allow localization from os settings once pebble doesn't require english locale
        DecimalFormat df = new DecimalFormat("#", new DecimalFormatSymbols(Locale.ENGLISH));
        String delta_sign = "";
        if (value > 0) {
            delta_sign = "+";
        }
        if (doMgdl) {

            if (highGranularity) {
                df.setMaximumFractionDigits(1);
            } else {
                df.setMaximumFractionDigits(0);
            }

            return delta_sign + df.format(unitized(value,doMgdl)) + (showUnit ? " mg/dl" : "");
        } else {
            // only show 2 decimal places on mmol/l delta when less than 0.1 mmol/l
            if (highGranularity && (Math.abs(value) < (Constants.MMOLL_TO_MGDL * 0.1))) {
                df.setMaximumFractionDigits(2);
            } else {
                df.setMaximumFractionDigits(1);
            }

            df.setMinimumFractionDigits(1);
            df.setMinimumIntegerDigits(1);
            return delta_sign + df.format(unitized(value,doMgdl)) + (showUnit ? " mmol/l" : "");
        }
    }


    public static String unit(boolean doMgdl) {
        if (doMgdl) {
            return "mg/dl";
        } else {
            return "mmol";
        }
    }




}
