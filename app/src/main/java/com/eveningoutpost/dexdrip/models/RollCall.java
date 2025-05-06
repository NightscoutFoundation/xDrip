package com.eveningoutpost.dexdrip.models;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.cloud.jamcm.Pusher;
import com.eveningoutpost.dexdrip.utilitymodels.BridgeBattery;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utilitymodels.desertsync.RouteTools;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

/**
 * Created by jamorham on 20/01/2017.
 */

public class RollCall {

    private static final String TAG = "RollCall";
    private static final int MAX_SSID_LENGTH = 20;
    private static volatile HashMap<String, RollCall> indexed;

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
    @Expose
    String ssid;
    @Expose
    String mhint;
    @Expose
    int battery = -1;
    @Expose
    int bridge_battery = -1;

    // not set by instantiation
    @Expose
    String hash;
    @Expose
    Long last_seen;

    @Expose
    int cloud;

    final long created = JoH.tsl();

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

        if (DesertSync.isEnabled()) {
            try {
                this.ssid = wifiString();
            } catch (Exception e) {
                //
            }
            try {
                if (this.role.equals("Master")) {
                    this.mhint = RouteTools.getBestInterfaceAddress();
                }
            } catch (Exception e) {
                //
            }
        }
    }

    // populate with values from this device
    public RollCall populate() {
        this.battery = getBatteryLevel();
        this.bridge_battery = BridgeBattery.getBestBridgeBattery();
        this.cloud = Pusher.enabled() ? 1 : 0;
        return this;
    }

    private boolean batteryValid() {
        return battery != -1;
    }

    private boolean bridgeBatteryValid() {
        return bridge_battery > 0;
    }

    private static String wifiString() {
        String ssid = JoH.getWifiSSID();
        if (ssid != null && ssid.length() > MAX_SSID_LENGTH) {
            ssid = ssid.substring(0, 20);
        }
        return ssid;
    }

    @SuppressWarnings("ConstantConditions")
    private static int getBatteryLevel() {
        final Intent batteryIntent = xdrip.getAppContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        try {
            final int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            final int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level == -1 || scale == -1) {
                return -1;
            }
            return (int) (((float) level / (float) scale) * 100.0f);
        } catch (NullPointerException e) {
            return -1;
        }
    }


    private String getRemoteIpStatus() {
        if (mhint != null) {
            return "\n" + mhint;
        }
        return "";
    }

    private String getRemoteWifiIndicate(final String our_wifi_ssid) {
        if (emptyString(our_wifi_ssid)) return "";
        if (emptyString(ssid)) return "";
        if (!our_wifi_ssid.equals(ssid)) return "\n" + ssid;
        return "";
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
        try {
            return gson.fromJson(json, RollCall.class);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception processing fromJson() " + e);
            UserError.Log.e(TAG, "json = " + json);
            return null;
        }
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

    public static String getBestMasterHintIP() {
        // TODO some intelligence regarding wifi ssid
        //final String our_wifi_ssid = wifiString();
        if (indexed == null) loadIndex();
        RollCall bestMatch = null;
        for (Map.Entry entry : indexed.entrySet()) {
            final RollCall rc = (RollCall) entry.getValue();
            if (!rc.role.equals("Master")) continue;
            if (emptyString(rc.mhint)) continue;
            if (bestMatch == null || rc.last_seen > bestMatch.last_seen) {
                bestMatch = rc;
            }
        }
        UserError.Log.d(TAG, "Returning best master hint ip: " + (bestMatch != null ? bestMatch.toS() : "no match"));
        return bestMatch != null ? bestMatch.mhint : null;
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
        if (indexed == null) loadIndex();
        GcmActivity.requestRollCall();
        // TODO sort data
        final boolean engineering = Home.get_engineering_mode();
        final boolean desert_sync = DesertSync.isEnabled();
        final String our_wifi_ssid = desert_sync ? wifiString() : "";
        final List<StatusItem> lf = new ArrayList<>();
        for (Map.Entry entry : indexed.entrySet()) {
            final RollCall rc = (RollCall) entry.getValue();
            // TODO refactor with stringbuilder
            lf.add(new StatusItem(rc.role + (desert_sync ? rc.getRemoteWifiIndicate(our_wifi_ssid) : "") + (engineering ? ("\n" + JoH.niceTimeSince(rc.last_seen) + " ago") : ""), rc.bestName() + (desert_sync ? rc.getRemoteIpStatus() : "") + (rc.batteryValid() ? ("\n" + rc.battery + "%") : "") + (engineering && rc.bridgeBatteryValid() ? (" " + rc.bridge_battery+"%") : "")));
        }

        Collections.sort(lf, new Comparator<StatusItem>() {
            public int compare(StatusItem left, StatusItem right) {
                int val = right.name.replaceFirst("\n.*$", "").compareTo(left.name.replaceFirst("\n.*$", "")); // descending sort ignore second line
                if (val == 0) val = left.value.compareTo(right.value); // ascending sort
                return val;
            }
        });
        // TODO could scan for duplicates and append serial to bestName

        return new ArrayList<>(lf);
    }

}
