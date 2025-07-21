package com.eveningoutpost.dexdrip.cloud.jamcm;

import com.eveningoutpost.dexdrip.models.UserError;

import java.util.Arrays;

import lombok.val;

/**
 * JamOrHam
 */

public class MessageStatistics {

    private static final String TAG = MessageStatistics.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int bucketCount = 12;   // 12 buckets, one per 5-minute interval in 60 minutes
    private static final int bucketDurationMillis = 5 * 60 * 1000; // 5 minutes in milliseconds
    private final int[] buckets;          // Buckets for each 5-minute interval
    private long lastTimestamp;           // Last time a message was processed

    private int total;

    private final String name;

    public MessageStatistics(String name) {
        this.buckets = new int[bucketCount];
        this.lastTimestamp = System.currentTimeMillis();
        this.name = name;
    }

    private void resetOldBuckets(long currentTimeMillis) {

        final long bucketCurrent = currentTimeMillis / bucketDurationMillis;
        final long bucketlast = lastTimestamp / bucketDurationMillis;

        final int bucketsToReset = (int) (Math.min(bucketCurrent - bucketlast, bucketCount));

        if (bucketsToReset > 0) {

            int currentBucket = (int) (currentTimeMillis / bucketDurationMillis % bucketCount);
            for (int i = 0; i < bucketsToReset; i++) {
                int bucketToReset = (currentBucket - i + bucketCount) % bucketCount; // Handle wrap-around
                buckets[bucketToReset] = 0; // Reset the old bucket
                log("Resetting bucket " + bucketToReset);
            }

            lastTimestamp = currentTimeMillis;
        }
    }

    public void onMessage() {
        long currentTimeMillis = System.currentTimeMillis();

        int currentBucket = (int) (currentTimeMillis / bucketDurationMillis % bucketCount);
        resetOldBuckets(currentTimeMillis);
        buckets[currentBucket]++;
        total++;
        log("Current bucket: " + currentBucket + " -> " + buckets[currentBucket] + " total: " + total);
    }

    public int getTotalMessagesInLastHour() {
        return Arrays.stream(buckets).sum();
    }

    public int getTotalMessages() {
        return total;
    }

    public int getMessagesInLast5Minutes() {
        long currentTimeMillis = System.currentTimeMillis();
        int currentBucket = (int) (currentTimeMillis / bucketDurationMillis % bucketCount);
        return buckets[currentBucket];
    }

    public int getMessagesInLastNMinutes(int minutes) {
        int numBuckets = (int) Math.ceil(minutes / 5.0);
        long currentTimeMillis = System.currentTimeMillis();
        resetOldBuckets(currentTimeMillis);

        int totalMessages = 0;
        int currentBucket = (int) (currentTimeMillis / bucketDurationMillis % bucketCount);

        for (int i = 0; i < numBuckets; i++) {
            int bucketIndex = (currentBucket - i + bucketCount) % bucketCount;
            totalMessages += buckets[bucketIndex];
        }

        return totalMessages;
    }

    private void log(final String msg) {
        if (DEBUG) {
            UserError.Log.d(TAG, name + " " + msg);
        }
    }
}
