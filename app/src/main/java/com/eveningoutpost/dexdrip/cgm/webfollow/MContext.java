package com.eveningoutpost.dexdrip.cgm.webfollow;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;

import java.util.List;

import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * Web Follower master context script
 */

@Builder
public class MContext implements Exposed {

    private static final String TAG = "WebFollow";
    private static final String PREF_MCONTEXT = "PREF_MCONTEXT-";

    public volatile String domain;
    public volatile String aUrl;
    public volatile String bUrl;
    public volatile String cUrl;
    public volatile String dUrl;
    public volatile String eUrl;
    public volatile String fUrl;
    public volatile String gUrl;
    public volatile String hUrl;
    public volatile String pid;
    public volatile String version;
    public volatile String product;
    public volatile String agent;
    public volatile String token;
    public volatile String country;
    public volatile Double lvalue;
    public volatile long ltime;
    public volatile String[] cmd;
    public volatile List<Object> history;
    public volatile int skip;
    public volatile boolean quit;
    public volatile Long lastErrorTime;
    public volatile String lastError;


    public String toJson() {
        return new Gson().toJson(this);
    }

    public Object getOByName(final String ref) throws NoSuchFieldException, IllegalAccessException {
        return this.getClass().getDeclaredField(ref).get(this);
    }

    public Double getDByName(final String ref) throws NoSuchFieldException, IllegalAccessException {
        return (Double) getOByName(ref);
    }

    public String getByName(final String ref) throws NoSuchFieldException, IllegalAccessException {
        return (String) getOByName(ref);
    }

    public void setByName(final String ref, final Object value) throws NoSuchFieldException, IllegalAccessException {
        this.getClass().getDeclaredField(ref).set(this, value);
    }

    public void save(final String id) {
        PersistentStore.setString(PREF_MCONTEXT + id, toJson());
        log("Saving: " + id + " -> " + toJson());
    }

    void doAction(final String action) {
        log("STEP: action = " + action);

        val actionS = action.replaceAll("[0-9]", "");
        int actionN = 1;
        try {
            actionN = Integer.parseInt(action.replaceAll("[a-z]", ""));
        } catch (NumberFormatException e) {
            //
        }
        switch (actionS) {
            case "skip":
                log("Skipping " + actionN);
                skip = actionN;
                break;
            case "quit":
                log("Quitting");
                quit = true;
        }
    }

    public void log(final String msg) {
        if (xdrip.isRunningTest()) {
            System.out.println(msg);
        } else {
            UserError.Log.d(TAG, msg);
        }
    }

    public void loge(final String msg, boolean alert) {
        lastError = msg;
        lastErrorTime = JoH.tsl();
        if (xdrip.isRunningTest()) {
            System.out.println("ERROR: " + msg);
        } else {
            UserError.Log.e(TAG, msg);
            if (alert) {
                JoH.static_toast_long(msg);
            }
        }
    }

    public void loge(final String msg) {
        loge(msg, false);
    }

    public void evaluateSingle(final long ltime, final Double lvalue) {
        if (ltime > 1623075881000L && lvalue != null && lvalue > 35d && ltime <= JoH.tsl()) {
            if (BgReading.getForPreciseTimestamp(ltime, DexCollectionType.getCurrentDeduplicationPeriod(), false) == null) {
                log("Inserting new value: " + lvalue + " " + JoH.dateTimeText(ltime));
                BgReading.bgReadingInsertFromG5(lvalue, ltime);
            }
        }
    }

    public void evaluateSingle() {
        evaluateSingle(this.ltime, this.lvalue);
        this.ltime = 0;
        this.lvalue = null;
    }

    public boolean shouldQuit() {
        if (quit) {
            quit = false;
            return true;
        }
        return false;
    }

    public boolean shouldSkip() {
        val skip = this.skip;
        if (skip > 0) {
            this.skip = skip - 1;
            return true;
        }
        return false;
    }

    public static MContext fromJson(final String json) {
        if (json == null) return null;
        return new Gson().fromJson(json, MContext.class);
    }

    public static MContext load(final String id) {
        return fromJson(PersistentStore.getString(PREF_MCONTEXT + id, null));
    }

    public static MContext revive() {
        MContext context = load("active");
        if (context == null) {
            context = load("template");
        }
        return context;
    }

    public static void delete(final String id) {
        PersistentStore.removeItem(PREF_MCONTEXT + id);
    }

    public static void invalidate(final boolean template) {
        delete("active");
        if (template) {
            delete("template");
        }
    }
}