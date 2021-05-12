package com.eveningoutpost.dexdrip.store;

/**
 * Created by jamorham on 08/11/2017.
 *
 * KeyStore is a persistence interface allowing storage and retrieval of primitive data types
 * referenced by a String based key.
 *
 * Implementations may choose how long data is retained for.
 * Typical usage might include caching expensive function results
 *
 */

public interface KeyStore {

    // java type erasure prevents us using generics and then implementing multiple generic interfaces
    // so storage types we are interested in get their own interface methods

    void putS(String key, String value);

    String getS(String key);

    void putL(String key, long value);

    long getL(String key);


}
