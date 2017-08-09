package com.eveningoutpost.dexdrip.Models;

import android.os.Build;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jamorham on 20/01/2017.
 */

public class RollCall {

    private static final String TAG = "RollCall";
    private static HashMap<String, RollCall> indexed;

    @Expose
    String device_manufactuer;
    @Expose
    String device_model;
    @Expose
    String device_serial;
    @Expose
    String device_name;
    @Expose
    String android_version;
    @Expose
    String xdrip_version;
    @Expose
    String role;

    // not set by instantiation
    @Expose
    String hash;
    @Expose
    Long last_seen;


    public RollCall() {
        this.device_manufactuer = Build.MANUFACTURER;
        this.device_model = Build.MODEL;
        this.device_name = JoH.getLocalBluetoothName(); // sanity check length
        this.device_serial = Build.SERIAL;
        this.android_version = Build.VERSION.RELEASE;
        this.xdrip_version = JoH.getVersionDetails();

        if (Home.get_follower()) {
            this.role = "Follower";
        } else if (Home.get_master()) {
            this.role = "Master";
        } else {
            this.role = "None";
        }
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }

    public String getHash() {
        if (this.hash == null) {
            this.hash = CipherUtils.getSHA256(this.device_manufactuer + this.android_version + this.device_model + this.device_serial);
        }
        return this.hash;
    }

    public String bestName() {
        if ((device_name != null) && (device_name.length() > 2)) {
            return device_name;
        }
        return (!device_manufactuer.equals("unknown") ? device_manufactuer + " " : "") + device_model;
    }

    public static RollCall fromJson(String json) {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.fromJson(json, RollCall.class);
    }

    public synchronized static void Seen(String item_json) {
        try {
            UserError.Log.d(TAG, "Processing Seen: " + item_json);
            Seen(fromJson(item_json));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception processing Seen() " + e);
        }
    }

    private synchronized static void Seen(RollCall item) {
        // sanity check object contains some data
        if (item == null) return;
        if ((item.android_version == null) || (item.android_version.length() == 0)) return;
        if (indexed == null) loadIndex();
        indexed.put(item.getHash(), item);
        item.last_seen = JoH.tsl();
        saveIndex();
    }

    private static final String ROLLCALL_SAVED_INDEX = "RollCall-saved-index";

    private static void saveIndex() {
        final Gson gson = new GsonBuilder().create();
        final String[] array = new String[indexed.size()];
        int i = 0;
        for (Map.Entry entry : indexed.entrySet()) {
            array[i++] = (((RollCall) entry.getValue()).toS());
        }
        PersistentStore.setString(ROLLCALL_SAVED_INDEX, gson.toJson(array));
        UserError.Log.d(TAG, "Saving");
    }

    private synchronized static void loadIndex() {
        UserError.Log.d(TAG, "Loading index");
        final String loaded = PersistentStore.getString(ROLLCALL_SAVED_INDEX);
        final HashMap<String, RollCall> hashmap = new HashMap<>();
        try {
            if ((loaded != null) && (loaded.length() > 0)) {
                final Gson gson = new GsonBuilder().create();
                final String[] array = gson.fromJson(loaded, String[].class);
                if (array != null) {
                    for (String json : array) {
                        RollCall item = gson.fromJson(json, RollCall.class);
                        hashmap.put(item.getHash(), item);
                    }
                }
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error loading index: " + e);
        }
        indexed = hashmap;
        UserError.Log.d(TAG, "Loaded: count: " + hashmap.size());
    }


    public static void pruneOld(int depth) {
        if (indexed == null) loadIndex();
        if (depth > 10) return;
        boolean changed = false;
        for (Map.Entry entry : indexed.entrySet()) {
            RollCall rc = (RollCall) entry.getValue();
            long since = JoH.msSince(rc.last_seen);

            if ((since < 0) || (since > (1000 * 60 * 60 * 24))) {
                UserError.Log.d(TAG, "Pruning entry: " + rc.bestName());
                indexed.remove(entry.getKey().toString());
                changed = true;
                break;
            }
        }
        if (changed) {
            saveIndex();
            pruneOld(depth + 1);
        }
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        if (indexed == null) loadIndex();
        GcmActivity.requestRollCall();
        // TODO sort data
        final boolean engineering = Home.get_engineering_mode();

        final List<StatusItem> lf = new ArrayList<>();
        for (Map.Entry entry : indexed.entrySet()) {
            RollCall rc = (RollCall) entry.getValue();
            lf.add(new StatusItem(rc.role + (engineering ? ("\n" + JoH.niceTimeSince(rc.last_seen) + " ago") : ""), rc.bestName()));
        }

        Collections.sort(lf, new Comparator<StatusItem>() {
            public int compare(StatusItem left, StatusItem right) {
                int val = right.name.replaceFirst("\n.*$","").compareTo(left.name.replaceFirst("\n.*$","")); // descending sort ignore second line
                if (val == 0) val = left.value.compareTo(right.value); // ascending sort
                return val;
            }
        });
        // TODO could scan for duplicates and append serial to bestName

        l.addAll(lf);
        return l;
    }

}
