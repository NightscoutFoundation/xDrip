package com.eveningoutpost.dexdrip.alert;

import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * JamOrHam
 * <p>
 * Registry for Pollable alert objects
 */

public class Registry {

    private static final String TAG = "AlertRegistry";

    @Getter
    private static final List<Pollable> registry = new ArrayList<>();

    // TODO allow for adjusting list and available vs enabled lists
    // TODO add sorting of registry

    static {
        refresh();
    }

    public static void refresh() {
        synchronized (registry) {
            registry.clear();
            if (Pref.getBooleanDefaultFalse("alert_raise_for_sensor_expiry")) {
                registry.add(new SensorExpiry());
            }
            //  addGlucoseAlerts();
            // sort();
        }
    }

    public static void remove(final Pollable alert) {
        synchronized (registry) {
            Log.d(TAG, "Removing " + alert);
            registry.remove(alert);
        }
    }

    private static void clear() {
        synchronized (registry) {
            registry.clear();
        }
    }

    /*private  static void addGlucoseAlerts() {
       synchronized (registry) {
           registry.add(new CustomAlert("Ultra Low")
                   .setHighAlert(false)
                   .setSmoothedRate(false)
                   .setRateOfChange(0d)
                   .setThreshold(100));
           registry.add(new CustomAlert("Ultra High")
                   .setHighAlert(true)
                   .setSmoothedRate(false)
                   .setRateOfChange(0d)
                   .setThreshold(300));
       }
    }*/

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (key.startsWith("alert_")) {
            Log.d(TAG, "Refreshing due to settings change");
            refresh();
        }
    };

}
