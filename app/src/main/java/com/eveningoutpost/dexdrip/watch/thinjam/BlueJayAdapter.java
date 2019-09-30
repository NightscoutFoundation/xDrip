package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import android.preference.Preference;

import com.eveningoutpost.dexdrip.R;
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


}
