package com.eveningoutpost.dexdrip.calibrations;

import android.preference.ListPreference;

import com.eveningoutpost.dexdrip.Home;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * Class for managing calibration plugins
 */

public class PluggableCalibration {

    // get calibration plugin instance by type
    public static CalibrationAbstract getCalibrationPlugin(Type t) {
        switch (t) {
            case Datricsae:
                return new Datricsae();
            case FixedSlopeExample:
                return new FixedSlopeExample();
            case xDripOriginal:
                return new XDripOriginal();

            // add new plugins here and also to the enum below

            default:
                return null;
        }
    }


    // enum for supported calibration plugins
    public enum Type {

        None("None"),
        Datricsae("Datricsae"),
        FixedSlopeExample("FixedSlopeExample"),
        xDripOriginal("xDripOriginal");

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
    public static CalibrationAbstract getCalibrationPluginFromPreferences() {
        return getCalibrationPluginByName(Home.getPreferencesStringWithDefault("current_calibration_plugin", "None"));
    }

    // lazy helper function
    public static boolean newCloseSensorData() {
        try {
            return PluggableCalibration.getCalibrationPluginFromPreferences().newCloseSensorData();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // lazy helper function
    public static boolean newFingerStickData() {
        try {
            return PluggableCalibration.getCalibrationPluginFromPreferences().newFingerStickData();
        } catch (NullPointerException e) {
            return false;
        }
    }
}
