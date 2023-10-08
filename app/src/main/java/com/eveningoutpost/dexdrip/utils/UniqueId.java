package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

/**
 * JamOrHam
 * Per installation unique id
 * Can be used for differentiating xDrip instances accessing a shared resource
 */

public class UniqueId {

    private static final String TAG = UniqueId.class.getSimpleName();
    private static final String PERSISTENT_KEY = "unique-id-persist";
    private static String id = null;

    // get arbitrary length value up to 32 characters (128 bits)
    public static String get(final int len) {

        if (id == null) {
            synchronized (UniqueId.class) {
                if (id == null) {
                    id = PersistentStore.getString(PERSISTENT_KEY, null);
                    if (id == null) {
                        id = CipherUtils.getRandomHexKey().toLowerCase();
                        PersistentStore.setString(PERSISTENT_KEY, id);
                    }
                }
            }
        }
        return id.substring(0, Math.min(len, id.length()));
    }

    // get standard 64 bit value
    public static String get() {
        return get(16);
    }

    static void clear() {
        synchronized (UniqueId.class) {
            PersistentStore.removeItem(PERSISTENT_KEY);
            id = null;
        }
    }

}
