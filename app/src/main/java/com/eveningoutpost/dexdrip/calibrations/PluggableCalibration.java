package com.eveningoutpost.dexdrip.calibrations;

import android.preference.ListPreference;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * Class for managing calibration plugins
 */

public class PluggableCalibration {

    private static final String TAG = "PluggableCalibration";
    private static final Map<Type, CalibrationAbstract> memory_cache = new HashMap<>();
    private static CalibrationAbstract current_plugin_cache = null;

    // get calibration plugin instance by type
    public static CalibrationAbstract getCalibrationPlugin(Type t) {
        if (memory_cache.containsKey(t)) {
            return memory_cache.get(t);
        }
        CalibrationAbstract plugin = null;
        switch (t) {
            case None:
                break;
            case Datricsae:
                plugin = new Datricsae();
                break;
            case FixedSlopeExample:
                plugin = new FixedSlopeExample();
                break;
            case xDripOriginal:
                plugin = new XDripOriginal();
                break;
            case Last7UnweightedA:
                plugin = new LastSevenUnweightedA();
                break;

            // add new plugins here and also to the enum below

            default:
                Log.e(TAG, "Unhandled plugin type: " + t.toString()+" "+ JoH.backTrace());
                break;
        }
        memory_cache.put(t, plugin);
        return plugin;
    }


    // enum for supported calibration plugins
    public enum Type {

        None("None"),
        Datricsae("Datricsae"),
        FixedSlopeExample("FixedSlopeExample"),
        xDripOriginal("xDripOriginal"),
        Last7UnweightedA("Last7UnweightedA");

        // add new algorithms here and also in to getCalibrationPlugin() above


        String internalName;
        private static final Map<String, Type> mapToInternalName;

        static {
            mapToInternalName = new HashMap<>();
            for (Type t : values()) {
                mapToInternalName.put(t.internalName, t);
            }
        }

        Type(String name) {
            this.internalName = name;
        }

        public static Type getTypeByName(String t) {
            if (mapToInternalName.containsKey(t))
                return mapToInternalName.get(t);
            else
                return None;
        }
    }

    // populate a ListPreference with plugin choices
    public static void setListPreferenceData(ListPreference p) {
        final Type[] types = Type.values();
        final CharSequence[] entries = new CharSequence[types.length];
        final CharSequence[] entryValues = new CharSequence[types.length];

        for (int i = 0; i < types.length; i++) {
            // Not sure exactly of what the overhead of this will be
            // perhaps we should save it to a cache...
            final CalibrationAbstract plugin = getCalibrationPlugin(types[i]);

            entries[i] = (plugin != null) ? plugin.getNiceNameAndDescription() : "None";
            entryValues[i] = types[i].toString();
        }
        p.setEntries(entries);
        p.setEntryValues(entryValues);
    }

    // get calibration plugin instance by name
    public static CalibrationAbstract getCalibrationPluginByName(String t) {
        return getCalibrationPlugin(Type.getTypeByName(t));
    }

    // get calibration plugin instance from preference setting
    public static synchronized CalibrationAbstract getCalibrationPluginFromPreferences() {
        if (current_plugin_cache == null) {
            current_plugin_cache = getCalibrationPluginByName(Pref.getString("current_calibration_plugin", "None"));
        }
        return current_plugin_cache;
    }

    public static synchronized void invalidatePluginCache() {
        current_plugin_cache = null;
        memory_cache.clear();
        Log.d(TAG, "Invalidated Plugin Cache");
    }

    // lazy helper function
    public static CalibrationAbstract.CalibrationData getCalibrationData() {
        try {
            return getCalibrationPluginFromPreferences().getCalibrationData();
        } catch (NullPointerException e) {
            return null;
        }
    }

    // lazy helper function
    public static double getGlucoseFromBgReading(BgReading bgReading) {
        try {
            final CalibrationAbstract plugin = getCalibrationPluginFromPreferences();
            final CalibrationAbstract.CalibrationData cd = plugin.getCalibrationData();
            return plugin.getGlucoseFromBgReading(bgReading, cd);
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public static BgReading mungeBgReading(BgReading bgReading) {
        try {
            final CalibrationAbstract plugin = getCalibrationPluginFromPreferences();
            final CalibrationAbstract.CalibrationData cd = plugin.getCalibrationData();
            bgReading.calculated_value = plugin.getGlucoseFromBgReading(bgReading, cd);
            bgReading.filtered_calculated_value = plugin.getGlucoseFromFilteredBgReading(bgReading, cd);
            return bgReading;
        } catch (NullPointerException e) {
            return bgReading;
        }
    }

    // lazy helper function
    public static boolean newCloseSensorData() {
        try {
            return getCalibrationPluginFromPreferences().newCloseSensorData();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // lazy helper function
    public static boolean newFingerStickData() {
        try {
            return getCalibrationPluginFromPreferences().newFingerStickData();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // lazy helper function
    public static boolean invalidateCache() {
        try {
            return getCalibrationPluginFromPreferences().invalidateCache();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // lazy helper function
    public static synchronized boolean invalidateAllCaches() {
        try {
            for (Object o : memory_cache.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                try {
                    final CalibrationAbstract ca = (CalibrationAbstract) entry.getValue();
                    ca.invalidateCache();
                    Log.d(TAG,"Invalidate cache for plugin: "+ca.getAlgorithmName());
                } catch (Exception e) {
                    //
                }
            }
            return getCalibrationPluginFromPreferences().invalidateCache();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // lazy helper function
    public static synchronized boolean invalidateCache(String tag) {
        try {
            return getCalibrationPluginByName(tag).invalidateCache();
        } catch (NullPointerException e) {
            return false;
        }
    }
}
