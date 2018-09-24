package com.eveningoutpost.dexdrip.UtilityModels;

// jamorham

import android.databinding.ObservableField;
import android.text.SpannableString;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.lang.reflect.Method;
import java.util.HashMap;

public class NanoStatus {

    private static final String TAG = "NanoStatus";
    private static final HashMap<Class<?>, Method> cache = new HashMap<>();
    private static final boolean D = false;

    private final String parameter;
    private final int freqMs;
    private final SpannableString empty = new SpannableString("");
    private volatile boolean running = false;
    private volatile Thread myThread;
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
        return nanoStatusColor(module).toString();
    }

    public static SpannableString nanoStatusColor(final String module) {
        switch (module) {
            case "collector":
                return collectorNano(DexCollectionType.getCollectorServiceClass());
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

}
