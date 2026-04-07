package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify;

/**
 * Created by jamorham on 04/07/2018.
 *
 * Observable map with transparent persistence
 */

public class PrefsViewString extends ObservableArrayMapNoNotify<String, String> {

    public String getString(String name) {
        return Pref.getString(name, "");
    }

    public void setString(String name, String value) {
        Pref.setString(name, value);
        super.put(name, value);
    }

    @NonNull
    @Override
    public String get(Object key) {
        String value = super.get(key);
        if (value == null) {
            value = getString((String) key);
            super.putNoNotify((String) key, value);
        }
        return value;
    }

    @Override
    public String put(String key, String value) {
        String current = super.get(key);
        if (current == null || !current.equals(value)) {
            setString(key, value);
        }
        return value;
    }


}
