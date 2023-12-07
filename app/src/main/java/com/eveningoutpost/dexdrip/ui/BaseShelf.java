package com.eveningoutpost.dexdrip.ui;

import androidx.databinding.ObservableArrayMap;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jamorham on 21/10/2017.
 *
 * Base class for ViewShelf implementations
 * uses observable map
 *
 */

public abstract class BaseShelf implements ViewShelf {

    public ObservableArrayMap<String, Boolean> included = new ObservableArrayMap<>();

    final HashMap<String, String> map = new HashMap<>();
    final HashMap<String, Boolean> defaults = new HashMap<>();
    String PREFS_PREFIX = null;


    // live read an item
    public boolean get(String id) {
        return included.get(id);
    }

    // live read an item
    public boolean getDefaultFalse(String id) {
        final Boolean result = included.get(id);
        return result == null ? false : result;
    }

    // live set an item
    public void set(String id, boolean value) {
        included.put(id, value);
    }

    // persistently set an item
    public void pset(String id, boolean value) {
        set(id, value);
        spb(id, value);
    }

    // toggle an item
    public void ptoggle(String id) {
        if (!included.containsKey(id)) {
            throw new NullPointerException("we don't know key " + id);
        }
        pset(id, !included.get(id));
    }

    // load everything from persistent store
    void populate() {
        for (Map.Entry<String, String> es : map.entrySet()) {
            set(es.getKey(), gpb(es.getKey()));
        }
    }

    // implementation for saving to persistent store
    private void spb(String id, boolean value) {
        Pref.setBoolean(PREFS_PREFIX + id, value);
    }

    // implementation for loading from persistent store
    private boolean gpb(String id) {
        return Pref.getBoolean(PREFS_PREFIX + id, defaults.containsKey(id) ? defaults.get(id) : false);
    }


}
