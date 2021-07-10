package com.eveningoutpost.dexdrip.UtilityModels;

import android.support.annotation.NonNull;

import com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify;

/**
 * Created by jamorham on 05/10/2017.
 *
 * Implementation of PrefsView
 */

public class PrefsViewImpl extends ObservableArrayMapNoNotify<String, Boolean> implements PrefsView {

    private Runnable runnable;

    public boolean getbool(String name) {
        return Pref.getBooleanDefaultFalse(name);
    }

    public void setbool(String name, boolean value) {
        Pref.setBoolean(name, value);
        super.put(name, value);
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
        Boolean value = super.get(key);
        if (value == null) {
            value = getbool((String) key);
            super.putNoNotify((String) key, value);
        }
        return value;
    }

    @Override
    public Boolean put(String key, Boolean value) {
        if (!(super.get(key).equals(value))) {
            Pref.setBoolean(key, value);
            super.put(key, value);
            doRunnable();
        }
        return value;
    }

    public void put(Object key, boolean value) {
        if (!(super.get(key).equals(value))) {
            super.put((String) key, value);
        }
    }

}
