package com.eveningoutpost.dexdrip.utils;

import android.os.SystemClock;

// JamOrHam
public class BlobCache {

    private final Object lock = new Object();
    private volatile Object blobCache = null;
    private static volatile long blobTime = 0;
    static volatile long blobParamX = 0;
    static volatile long blobParamY = 0;

    private final long timeout;

    public BlobCache(long timeout) {
        this.timeout = timeout;
    }

    public void set(Object o) {
        synchronized (lock) {
            blobCache = o;
            blobTime = SystemClock.elapsedRealtime();
        }
    }

    public Object get() {
        synchronized (lock) {
            if (isExpired()) {
                blobCache = null;
            }
            return blobCache;
        }
    }

    public void set(Object o, long x, long y) {
        synchronized (lock) {
            blobParamX = x;
            blobParamY = y;
            blobCache = o;
            blobTime = SystemClock.elapsedRealtime();
        }
    }

    public Object get(long x, long y) {
        synchronized (lock) {
            if (x != blobParamX || y != blobParamY) {
                return null;
            }
            return get();
        }
    }

    public void clear() {
        blobCache = null;
        blobTime = 0;
    }

    public boolean isExpired() {
        synchronized (lock) {
            return (blobCache == null || SystemClock.elapsedRealtime() - blobTime > timeout);
        }
    }

}

