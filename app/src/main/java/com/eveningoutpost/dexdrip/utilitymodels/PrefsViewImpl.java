package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify;

import lombok.val;

/**
 * Created by jamorham on 05/10/2017.
 * <p>
 * Implementation of PrefsView
 */

public class PrefsViewImpl extends ObservableArrayMapNoNotify<String, Boolean> implements PrefsView {

    private Runnable runnable;

    public boolean getbool(String name) {
        if (name == null) return false;
        return PrefHandle.parse(name).getBoolean();
    }

    public void setbool(String name, boolean value) {
        val handle = PrefHandle.parse(name);
        Pref.setBoolean(handle.key, value);
        super.put(handle.key, value);
        doRunnable();
    }

    public void togglebool(String name) {
        setbool(name, !getbool(name));
    }

    public PrefsViewImpl setRefresh(final Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    private void doRunnable() {
        if (runnable != null) {
            runnable.run();
        }
    }

    @NonNull
    @Override
    public Boolean get(Object key) {
        val handle = PrefHandle.parse((String) key);
        Boolean value = super.get(handle.key);
        if (value == null) {
            value = getbool((String) key);
            super.putNoNotify(handle.key, value);
        }
        return value;
    }

    @Override
    public Boolean put(String key, Boolean value) {
        val handle = PrefHandle.parse(key);
        if (!(super.get(handle.key).equals(value))) {
            Pref.setBoolean(handle.key, value);
            super.put(handle.key, value);
            doRunnable();
        }
        return value;
    }

    public void put(Object key, boolean value) {
        val handle = PrefHandle.parse((String) key);
        if (!(super.get(handle.key).equals(value))) {
            super.put(handle.key, value);
        }
    }

    public static class PrefHandle {
        public final String key;
        public final boolean defaultValue;

        public PrefHandle(final String key, final boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        boolean getBoolean() {
            return Pref.getBoolean(key, defaultValue);
        }

        public static PrefHandle parse(final String identifier) {
            if (identifier == null) {
                return null;
            }

            final String[] parts = identifier.split(":", 2);
            if (parts.length == 2) {
                return new PrefHandle(parts[0], Boolean.parseBoolean(parts[1]));
            } else {
                return new PrefHandle(identifier, false);
            }
        }
    }

}
