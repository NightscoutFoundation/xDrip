package com.eveningoutpost.dexdrip.store;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jamorham on 08/11/2017.
 * <p>
 * Fast implementation of KeyStore interface
 * Uses an in-memory database for short lived data elements
 * Content is expired as per normal garbage collection
 * Neutral defaults favoured over null return values
 * Static creation for fastest shared instance access
 */

public class FastStore implements KeyStore {

    private static final FastStore mFastStore = new FastStore();
    private static final ConcurrentHashMap<String, String> stringStore = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> longStore = new ConcurrentHashMap<>();

    // we trade substitution flexibility at the expense of some object creation

    private FastStore() {
        // use getInstance!
    }

    public static FastStore getInstance() {
        return mFastStore;
    }

    // interface methods

    public String getS(String key) {
        if (stringStore.containsKey(key)) return stringStore.get(key);
        return "";
    }

    public void putS(String key, String value) {
        stringStore.put(key, value);
    }

    public long getL(String key) {
        if (longStore.containsKey(key)) return longStore.get(key);
        return 0;
    }

    public void putL(String key, long value) {
        longStore.put(key, value);
    }

}


