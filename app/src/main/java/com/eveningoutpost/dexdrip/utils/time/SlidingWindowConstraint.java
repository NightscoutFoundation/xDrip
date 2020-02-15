package com.eveningoutpost.dexdrip.utils.time;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;

/**
 * Created by jamorham on 14/02/2018.
 *
 * Sliding Window Constraint
 *
 * Designed to facilitate a restriction of X units per time period
 *
 * For example, in the decision for popping up fingerprint authentication on meal boluses,
 * but allowing tiny boluses through, so long as they don't exceed a threshold.
 *
 */


public class SlidingWindowConstraint {

    private static final String SLIDING_WINDOW = "SLIDING_WINDOW";
    private static final String PERSIST_PREF = "SLIDING_PERSIST_";
    private static final boolean d = true;
    private static final Gson gson = new Gson();
    private static final Type recordsType = new TypeToken<List<Record>>() {
    }.getType();

    private final String identifier;
    private final String pref_identifier;
    private final double max;
    private final long period;
    private final boolean persist;
    private final List<Record> records = new LinkedList<>();

    // max = maximum value per period. period = ms period length. identifier = persistence id
    public SlidingWindowConstraint(double max, long period, String identifier) {
        this(max, period, identifier, false);
    }

    SlidingWindowConstraint(double max, long period, String identifier, boolean persist) {
        if (period < 1000) {
            throw new RuntimeException("period too small");
        }
        if (max < 0) {
            throw new RuntimeException("max too small");
        }
        if (identifier == null) {
            throw new RuntimeException("null identifier");
        }

        this.identifier = identifier;
        this.pref_identifier = PERSIST_PREF + identifier;
        this.period = period;
        this.max = max;
        this.persist = persist;

        // load saved data
        if (this.persist) {
            final String loaded_data = PersistentStore.getString(pref_identifier);
            if (!emptyString(loaded_data)) {
                fromJson(loaded_data);
            }
        }
    }

    // this is the best public method to use for checks as it is atomic and locks to a single thread
    public synchronized boolean checkAndAddIfAcceptable(double value) {
        if (!acceptable(value)) {
            return false;
        } else {
            add(value);
            return true;
        }
    }

    // check if a value is acceptable based on current window data set
    public boolean acceptable(double value) {
        if (d)
            android.util.Log.e("SlidingWindow", "checking:" + value + " total:" + totalRecords() + " max:" + max + " verdict: " + ((totalRecords() + value) <= max));
        return (totalRecords() + value) <= max;
    }

    // add an item to the window at time now
    public void add(double value) {
        add(value, tsl());
    }

    // add an item to the window at any point in time
    public void add(double value, long when) {
        synchronized (records) {
            records.add(new Record(when, value));
        }
        pruneRecords();
        saveRecords();
    }

    // save the records to the store
    private void saveRecords() {
        if (persist) {
            PersistentStore.setString(pref_identifier, toJson());
        }
    }

    // get sum of non-expired records held
    public double totalRecords() {
        double total = 0;
        final long expire_time = tsl() - period;
        synchronized (records) {
            for (Record record : records) {
                if (record.timestamp > expire_time) {
                    total += record.value;
                }
            }
        }
        return total;
    }

    // delete expired records
    private void pruneRecords() {
        final long expire_time = tsl() - period;
        synchronized (records) {
            pruneRecord(expire_time);
        }
    }

    // delete next expired record
    private void pruneRecord(long expire_time) {
        List<Record> deletion_list = null;
        for (Record record : records) {
            if (record.timestamp < expire_time) {
                if (deletion_list == null) deletion_list = new LinkedList<>();
                deletion_list.add(record);
            }
        }
        if (deletion_list != null) {
            records.removeAll(deletion_list);
        }
    }

    // get time stamp long
    private long tsl() {
        return System.currentTimeMillis();
    }

    // convert records to json string
    String toJson() {
        synchronized (records) {
            return gson.toJson(records, recordsType);
        }
    }

    // convert json string to records
    void fromJson(String json) {
        synchronized (records) {
            records.clear();
            records.addAll(gson.fromJson(json, recordsType));
        }
    }

    // return size of implementation records storage
    int testSizeOfStorage() {
        return records.size();
    }

    @RequiredArgsConstructor
    private class Record {
        final long timestamp;
        final double value;
    }

}
