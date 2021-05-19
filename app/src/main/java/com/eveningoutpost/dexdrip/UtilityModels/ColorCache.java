package com.eveningoutpost.dexdrip.UtilityModels;

import android.graphics.Color;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.UserError;

import java.util.EnumMap;

import lombok.Getter;

/**
 * Created by jamorham on 11/03/2016.
 */

public class ColorCache {

    //  map of colors
    private static final EnumMap<X, Integer> the_cache = new EnumMap<>(X.class);
    private static final String TAG = "jamorham colorcache";
    private static final boolean debug = false;

    public static void invalidateCache() {
        the_cache.clear();
        if (debug) Log.i(TAG, "Cache cleared");
    }

    public static int getCol(final X color) {
        if (!the_cache.containsKey(color)) {
            try {
                the_cache.put(color, Pref.getInt(color.internalName, 0xABCDEF));
            } catch (ClassCastException e) {
                UserError.Log.wtf(TAG, "Cannot set initial value - preference type likely wrong for: " + color.internalName + " " + e);
                the_cache.put(color, Color.GRAY);
            }
            if (debug)
                UserError.Log.d(TAG, "Setting cache for color: " + color.internalName + " / " + Pref.getInt(color.internalName, 1234));
        }
        return the_cache.get(color);
    }

    public enum X {

        color_high_values("color_high_values"),
        color_bad_values("color_bad_values"),
        color_inrange_values("color_inrange_values"),
        color_low_values("color_low_values"),
        color_filtered("color_filtered"),
        color_treatment("color_treatment"),
        color_treatment_dark("color_treatment_dark"),
        color_predictive("color_predictive"),
        color_predictive_dark("color_predictive_dark"),
        color_average1_line("color_average1_line"),
        color_average2_line("color_average2_line"),
        color_target_line("color_target_line"),
        color_calibration_dot_background("color_calibration_dot_background"),
        color_calibration_dot_foreground("color_calibration_dot_foreground"),
        color_treatment_dot_background("color_treatment_dot_background"),
        color_treatment_dot_foreground("color_treatment_dot_foreground"),
        color_home_chart_background("color_home_chart_background"),
        color_notification_chart_background("color_notification_chart_background"),
        color_widget_chart_background("color_widget_chart_background"),
        color_secondary_glucose_value("color_secondary_glucose_value"),
        color_step_counter1("color_step_counter1"),
        color_step_counter2("color_step_counter2"),
        color_upper_flair_bar("color_upper_flair_bar"),
        color_lower_flair_bar("color_lower_flair_bar"),
        color_heart_rate1("color_heart_rate1"),
        color_smb_icon("color_smb_icon"),
        color_smb_line("color_smb_line"),
        color_number_wall("color_number_wall"),
        color_number_wall_shadow("color_number_wall_shadow"),
        color_basal_tbr("color_basal_tbr");

        @Getter
        String internalName;

        X(String name) {
            this.internalName = name;
        }
    }
}
