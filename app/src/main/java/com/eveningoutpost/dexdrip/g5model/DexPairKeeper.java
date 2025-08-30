package com.eveningoutpost.dexdrip.g5model;

import static com.eveningoutpost.dexdrip.models.JoH.defaultGsonInstance;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.val;

// jamorham
// keep track of pairings we have seen
public class DexPairKeeper {

    private static final String TAG = DexPairKeeper.class.getSimpleName();
    private static final String PAIR_KEEPER_PREF = "OB1_PAIR_KEEPER";
    private static final Type listType = new TypeToken<List<PairKeeper>>() {
    }.getType();
    private final List<PairKeeper> items = new ArrayList<>();

    {
        load();
    }

    // returns  false if we know the supplied mac is not for the txid
    public boolean check(final String txid, final String mac) {
        if (txid == null || mac == null) {
            log("Null mac or txid passed to check - allowing...");
            return true;
        }
        synchronized (items) {
            // search list in reverse order as oldest items are first in the list
            for (int i = items.size() - 1; i >= 0; i--) {
                val item = items.get(i);
                if (item.mac.equalsIgnoreCase(mac)) {
                    if (!item.txid.equalsIgnoreCase(txid)) {
                        log("Registered a mismatch for search txid: " + txid + " found mac: " + mac + " but we know as: " + item.txid);
                        return false; // mismatch
                    } else {
                        return true; // mac and txid match
                    }
                }
            }
            // we don't know this mac
            return true;
        }
    }

    public boolean macExists(final String mac) {
        if (mac == null) {
            log("Mac passed to macExists is null");
            return false;
        }
        synchronized (items) {
            for (int i = items.size() - 1; i >= 0; i--) {
                val item = items.get(i);
                if (item.mac.equalsIgnoreCase(mac)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean macEnded(final String mac) {
        if (mac == null) {
            log("Mac passed to macEnded is null");
            return false;
        }
        synchronized (items) {
            for (int i = items.size() - 1; i >= 0; i--) {
                val item = items.get(i);
                if (item.mac.equalsIgnoreCase(mac)) {
                    return item.ended();
                }
            }
        }
        return false;
    }

    public boolean add(final String txid, final String mac) {
        if (txid == null) {
            log("Txid passed to add is null!");
            return false;
        }
        if (mac == null) {
            log("MAC passed to add is null!");
            return false;
        }
        if (mac.length() != 17) {
            log("Invalid MAC passed: " + mac);
            return false;
        }
        if (txid.length() < 4) {
            log("Invalid txid passed: " + txid);
            return false;
        }
        synchronized (items) {
            for (val item : items) {
                if (item.mac.equalsIgnoreCase(mac)) {
                    return false; // already exists
                }
            }
            val item = new PairKeeper(txid, mac);
            items.add(item);
            log("Added: " + item);
            save();
            return true;
        }
    }


    public static class PairKeeper {
        @Expose
        public String txid;
        @Expose
        public String mac;
        @Expose
        public long created;
        @Expose
        public long lastSeen;


        // don't store record any more after this time
        public boolean expired() {
            return JoH.msSince(created) > Constants.MONTH_IN_MS * 12;
        }

        // sensor session will have finished
        public boolean ended() {
            return JoH.msSince(created) > Constants.DAY_IN_MS * 16;
        }

        @Override
        public String toString() {
            return "PairKeeper: " + txid + " -> " + mac + "  added: " + JoH.dateTimeText(created);
        }

        public PairKeeper() {
        }

        public PairKeeper(String txid, String mac) {
            this.txid = txid;
            this.mac = mac;
            this.created = JoH.tsl();
            this.lastSeen = this.created;
        }

    }

    private void load() {
        synchronized (items) {
            items.clear();
            try {
                //noinspection unchecked
                val toAdd = (List<PairKeeper>) (defaultGsonInstance()
                        .fromJson(PersistentStore.getString(PAIR_KEEPER_PREF), listType));
                if (toAdd != null) {
                    items.addAll(toAdd);
                }
            } catch (Exception e) {
                log("Exception during loading: " + e);
            }
        }
        log("Loaded: " + items + " items");
    }

    private void save() {
        try {
            prune();
            synchronized (items) {
                PersistentStore.setString(PAIR_KEEPER_PREF, defaultGsonInstance().toJson(items));
            }
        } catch (Exception e) {
            log("Got exception saving: " + e);
        }
    }

    private void prune() {
        val remove = new LinkedList<PairKeeper>();
        synchronized (items) {
            for (val item : items) {
                if (item.expired()) {
                    remove.add(item);
                    log("Expiring pair: " + item);
                }
            }
            items.removeAll(remove);
        }
    }

    public static void clearAll() {
        PersistentStore.setString(PAIR_KEEPER_PREF, "");
    }


    private static void log(String msg) {
        if (Pref.getBooleanDefaultFalse("engineering_mode")) {
            UserError.Log.e(TAG, msg);
        } else {
            UserError.Log.d(TAG, msg);
        }
    }

}
