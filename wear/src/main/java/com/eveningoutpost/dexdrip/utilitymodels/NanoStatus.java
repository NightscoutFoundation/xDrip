package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * NanoStatus allows access to a static class based status interface
 *
 * It can push to observable fields, ui elements and remote followers
 *
 * Use of dovetailing means runnables can also be attached
 *
 * This allows us to provide dynamic UI updates behind a layer of abstraction for classes and
 * services which have not yet been instantiated or which maintain a static singleton state
 *
 */


import android.text.SpannableString;
import android.util.Log;

import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Method;
import java.util.HashMap;

import lombok.Setter;

public class NanoStatus {

    private static final String TAG = "NanoStatus";
    private static final String LAST_COLLECTOR_STATUS_STORE = "LAST_COLLECTOR_STATUS_STORE";
    private static final String REMOTE_COLLECTOR_STATUS_STORE = "REMOTE_COLLECTOR_STATUS_STORE";
    private static final HashMap<Class<?>, Method> cache = new HashMap<>();
    private static final boolean D = false;
    private static Gson muhGson;

    private final String parameter;
    private final int freqMs;
    private final SpannableString empty = new SpannableString("");
    private volatile boolean running = false;
    private volatile Thread myThread;
    @Setter
    private Runnable doveTail;
    public ObservableField<String> watch = new ObservableField<>();
    public ObservableField<SpannableString> color_watch = new ObservableField<>();


    public NanoStatus(final String parameter, final int freqMs) {
        this.parameter = parameter;
        this.freqMs = freqMs;
        updateWatch();
        if (freqMs > 0) {
            running = true;
            startRefresh();
        }
    }

    public void setRunning(final boolean state) {
        final boolean oldState = running;
        running = state;
        if (state && !oldState) {
            startRefresh();
        }
    }

    private synchronized void startRefresh() {
        UserError.Log.d(TAG, "startRefresh");
        if (myThread != null) {
            try {
                if (D) UserError.Log.d(TAG, "sending interrupt");
                myThread.interrupt();
            } catch (Exception e) {
                if (D) UserError.Log.d(TAG, " interrupt exception " + e);
            }
        }
        myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running && !Thread.interrupted()) {
                    try {
                        Thread.sleep(freqMs);
                    } catch (InterruptedException e) {
                        //
                        if (D) UserError.Log.d(TAG, "Sleep interrupted - quiting");
                        return;
                    }
                    if (D) UserError.Log.d(TAG, "Updating");
                    updateWatch();
                    if (doveTail != null) {
                        doveTail.run();
                    }
                }
                if (D) UserError.Log.d(TAG, "Stopping");
            }
        });
        myThread.setPriority(Thread.NORM_PRIORITY - 1);
        myThread.start();

    }

    private void updateWatch() {
        final SpannableString result = nanoStatusColor(parameter);
        color_watch.set(result != null ? result : empty);
        watch.set(result != null ? result.toString() : "");
    }


    public static String nanoStatus(final String module) {
        final SpannableString result = nanoStatusColor(module);
        return result != null ? result.toString() : null;
    }

    public static SpannableString nanoStatusColor(final String module) {
        switch (module) {
            case "collector":
                return collectorNano(DexCollectionType.getCollectorServiceClass());
            case "mtp-configure":
                return collectorNano(getClassByName(".utilitymodels.MtpConfigure"));
            default:
                return new SpannableString("Invalid module type");
        }
    }


    static SpannableString collectorNano(final Class<?> service) {
        if (service != null) {
            try {
                try {
                    return (SpannableString) cache.get(service).invoke(null);
                } catch (NullPointerException e) {
                    final Method method = service.getMethod("nanoStatus");
                    cache.put(service, method);
                    return (SpannableString) method.invoke(null);
                }

            } catch (Exception e) {
                Log.d(TAG, "reflection exception: " + e + " " + service.getSimpleName());
            }
        }
        return null;
    }

    private static void gsonInstance() {
        if (muhGson == null) {
            muhGson = new GsonBuilder().create();
        }
    }

    public static void keepFollowerUpdated() {
        try {
            if (Home.get_master()) {
                gsonInstance();
                final String serialized = muhGson.toJson(nanoStatusColor("collector"));
                if (PersistentStore.updateStringIfDifferent(LAST_COLLECTOR_STATUS_STORE, serialized)) {
                    Inevitable.task("update-follower-to-nanostatus", 500, new Runnable() {
                        @Override
                        public void run() {
                            GcmActivity.sendNanoStatusUpdate(PersistentStore.getString(LAST_COLLECTOR_STATUS_STORE));
                        }
                    });
                }
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception serializing: " + e);
        }
    }

    public static void setRemote(final String json) {
        PersistentStore.setString(REMOTE_COLLECTOR_STATUS_STORE, json);
    }

    public static SpannableString getRemote() {
        // TODO apply timeout?
        try {
            gsonInstance();
            return muhGson.fromJson(PersistentStore.getString(REMOTE_COLLECTOR_STATUS_STORE), SpannableString.class);
        } catch (Exception e) {
            return new SpannableString("");
        }
    }

    private static Class<?> getClassByName(final String name) {
        try {
            return Class.forName(BuildConfig.APPLICATION_ID + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
