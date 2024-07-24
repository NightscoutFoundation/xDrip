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

import androidx.databinding.ObservableField;
import android.text.SpannableString;
import android.util.Log;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.adapters.SpannableSerializer;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.lang.reflect.Method;
import java.util.HashMap;

import lombok.Setter;
import lombok.val;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

public class NanoStatus {

    private static final String TAG = "NanoStatus";
    private static final String LAST_COLLECTOR_STATUS_STORE = "LAST_COLLECTOR_STATUS_STORE";
    private static final String REMOTE_COLLECTOR_STATUS_STORE = "REMOTE_COLLECTOR_STATUS_STORE";
    private static final HashMap<Class<?>, Method> cache = new HashMap<>();
    private static final boolean D = false;

    private final String parameter;
    private final int freqMs;
    private final SpannableString empty = new SpannableString("");
    private volatile boolean running = false;
    private volatile Thread myThread;
    @Setter
    private Runnable doveTail;
    public final ObservableField<String> watch = new ObservableField<>();
    public final ObservableField<SpannableString> color_watch = new ObservableField<>();

    private static String lastException = "";

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
            case "sensor-expiry":
                return getLocalOrRemoteSensorExpiry();
            default:
                return new SpannableString("Invalid module type");
        }
    }

    private static SpannableString getLocalOrRemoteSensorExpiry() {
        if (Home.get_follower()) {
            return getRemote("sensor-expiry");
        }
        return SensorDays.get().getSpannable();
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
              val exceptionString = e + " " + service.getSimpleName();
                if (!exceptionString.equals(lastException)) {
                    Log.d(TAG, "reflection exception: " + exceptionString);
                    lastException = exceptionString;
                }
            }
        }
        return null;
    }


    public static void keepFollowerUpdated() {
        keepFollowerUpdated(true);
    }

    public static void keepFollowerUpdated(final boolean ratelimits) {
        keepFollowerUpdated("", 0); // legacy defaults to collector
        keepFollowerUpdated("sensor-expiry", ratelimits ? 3600 : 0);
    }

    public static void keepFollowerUpdated(final String prefix, final int rateLimit) {
        try {
            if (Home.get_master()) {
                UserError.Log.d(TAG, "keepfollower updated called: " + prefix + " " + rateLimit);
                if (rateLimit == 0 || JoH.pratelimit("keep-follower-updated" + prefix, rateLimit)) {
                    final String serialized = SpannableSerializer.serializeSpannableString(nanoStatusColor(prefix.equals("") ? "collector" : prefix));
                    if (PersistentStore.updateStringIfDifferent(LAST_COLLECTOR_STATUS_STORE + prefix, serialized)) {
                        Inevitable.task("update-follower-to-nanostatus" + prefix, 500, () ->
                                GcmActivity.sendNanoStatusUpdate(prefix, PersistentStore.getString(LAST_COLLECTOR_STATUS_STORE + prefix)));
                    }
                } else {
                    UserError.Log.d(TAG, "Ratelimiting keepFollowerUpdated check on " + prefix + " @ " + rateLimit);
                }
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception serializing: " + e);
        }
    }

    public static void setRemote(final String json) {
        setRemote("", json);
    }

    public static void setRemote(final String prefix, final String json) {
        PersistentStore.setString(REMOTE_COLLECTOR_STATUS_STORE + prefix, json);
    }

    public static SpannableString getRemote() {
        return getRemote("");
    }

    public static SpannableString getRemote(final String prefix) {
        // TODO apply timeout?
        try {
            val result = PersistentStore.getString(REMOTE_COLLECTOR_STATUS_STORE + prefix);
            if (emptyString(result)) return new SpannableString("");
            return SpannableSerializer.unserializeSpannableString(result);
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
