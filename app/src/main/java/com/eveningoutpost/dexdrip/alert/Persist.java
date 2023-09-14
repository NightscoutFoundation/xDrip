package com.eveningoutpost.dexdrip.alert;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.PersistentStore.getDouble;
import static com.eveningoutpost.dexdrip.utilitymodels.PersistentStore.getLong;
import static com.eveningoutpost.dexdrip.utilitymodels.PersistentStore.getString;
import static com.eveningoutpost.dexdrip.utilitymodels.PersistentStore.setDouble;
import static com.eveningoutpost.dexdrip.utilitymodels.PersistentStore.setLong;
import static com.eveningoutpost.dexdrip.utilitymodels.PersistentStore.setString;

import java.security.InvalidParameterException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * JamOrHam
 * <p>
 * Generic persistence property helper class
 */

public class Persist {

    private static final java.lang.String PREF_TIMEOUT = "TIMEOUT__";

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

    @RequiredArgsConstructor
    public static class Double {
        private final java.lang.String pref;

        public java.lang.Double get() {
            return getDouble(pref);
        }

        public void set(final double value) {
            setDouble(pref, value);
        }
    }

    public static class LongTimeout extends Long {

        private final long millis;

        public LongTimeout(final java.lang.String pref, final long millis) {
            super(pref);
            this.millis = millis;
        }

        public void set() {
            set(tsl());
        }

        public boolean expired() {
            return msSince(get()) > millis;
        }
    }

    public static class DoubleTimeout extends Double {

        private final LongTimeout timeout;

        public DoubleTimeout(final java.lang.String pref, final long millis) {
            super(pref);
            timeout = new LongTimeout(PREF_TIMEOUT + pref, millis);
        }

        @Override
        public void set(final double value) {
            super.set(value);
            timeout.set();
        }

        @Override
        public java.lang.Double get() {
            return !timeout.expired() ? super.get() : null;
        }

        public void set(@NonNull final java.lang.Double value) {
            if (value == null) {
                throw new InvalidParameterException("Sorry null not permitted here");
            }
            set((double)value);
        }

    }

    public static class StringTimeout extends String {

        private final LongTimeout timeout;

        public StringTimeout(final java.lang.String pref, final long millis) {
            super(pref);
            timeout = new LongTimeout(PREF_TIMEOUT + pref, millis);
        }

        @Override
        public void set(final java.lang.String value) {
            super.set(value);
            timeout.set();
        }

        @Override
        public java.lang.String get() {
            return !timeout.expired() ? super.get() : null;
        }

    }

}
