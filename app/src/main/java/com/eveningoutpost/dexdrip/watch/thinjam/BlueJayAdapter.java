package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import android.preference.Preference;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

public class BlueJayAdapter {

    public static int screenTimeoutValueToSeconds(final int value) {
        switch (value) {
            case 0:
                return 2;
            case 1:
                return 4;
            case 2:
                return 6;
            case 3:
                return 8;
            case 4:
                return 10;
            case 5:
                return 12;
            case 6:
                return 16;
            case 7:
                return 20;
            default:
                return -1; // invalid

        }
    }

    public static Preference.OnPreferenceChangeListener sBindPreferenceTitleAppendToBlueJayTimeoutValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            boolean do_update = false;
            // detect not first run
            if (preference.getTitle().toString().contains("(")) {
                do_update = true;
            }

            try {
                final int ivalue = (int) value;
                if (ivalue > -1) {
                    final String seconds = xdrip.gs(R.string.unit_seconds);
                    preference.setTitle(preference.getTitle().toString().replaceAll("  \\([a-z0-9A-Z ]+" + seconds + "\\)$", "") + "  (" + screenTimeoutValueToSeconds(ivalue) + " " + seconds + ")");
                    if (do_update) {
                        preference.getEditor().putInt(preference.getKey(), ivalue).apply(); // update prefs now
                    }
                }
            } catch (Exception e) {
                //
            }
            return true;
        }
    };


    public static Preference.OnPreferenceChangeListener changeToPhoneSlotListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            try {
                if ((boolean) value) {
                    // setting to true
                    if (preference.getSharedPreferences().getBoolean("bluejay_run_phone_collector", true)) {
                        JoH.static_toast_long("Must disable phone collector first!");
                        return false;
                    }
                    if (BlueJay.getMac() == null) {
                        JoH.static_toast_long("Needs a connected BlueJay");
                        return false;
                    }
                    if (BlueJayInfo.getInfo(BlueJay.getMac()).buildNumber < 51) {
                        JoH.static_toast_long("Needs BlueJay firmware at least version 51");
                        return false;
                    }
                }
            } catch (Exception e) {
                //
            }
            return true;
        }
    };

    private static boolean alwaysAllowPhoneSlot() {
        final int specifiedSlot = Pref.getBooleanDefaultFalse("engineering_mode") ? Pref.getStringToInt("dex_specified_slot", -1) : -1;
        return specifiedSlot == 3;
    }

    public static Preference.OnPreferenceChangeListener changeToPhoneCollectorListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            try {
                if ((boolean) value) {
                    // setting to true
                    if (!alwaysAllowPhoneSlot()) {
                        if (preference.getSharedPreferences().getBoolean("bluejay_run_as_phone_collector", false)) {
                            JoH.static_toast_long("Must disable BlueJay using phone slot first!");
                            return false;
                        }
                    }
                }

            } catch (Exception e) {
                //
            }
            return true;
        }
    };

}
