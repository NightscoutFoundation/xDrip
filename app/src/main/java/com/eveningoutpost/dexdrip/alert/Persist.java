package com.eveningoutpost.dexdrip.alert;

import static com.eveningoutpost.dexdrip.UtilityModels.PersistentStore.getLong;
import static com.eveningoutpost.dexdrip.UtilityModels.PersistentStore.getString;
import static com.eveningoutpost.dexdrip.UtilityModels.PersistentStore.setLong;
import static com.eveningoutpost.dexdrip.UtilityModels.PersistentStore.setString;

import lombok.RequiredArgsConstructor;

/**
 * JamOrHam
 *
 * Generic persistence property helper class
 */

public class Persist {

    @RequiredArgsConstructor
    public static class String {
        private final java.lang.String pref;

        public java.lang.String get() {
            return getString(pref);
        }

        public void set(final java.lang.String value) {
            setString(pref, value);
        }
    }

    @RequiredArgsConstructor
    public static class Long {
        private final java.lang.String pref;

        public long get() {
            return getLong(pref);
        }

        public void set(final long value) {
            setLong(pref, value);
        }
    }
}
