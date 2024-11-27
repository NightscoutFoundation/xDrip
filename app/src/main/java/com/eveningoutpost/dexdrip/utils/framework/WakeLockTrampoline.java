package com.eveningoutpost.dexdrip.utils.framework;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.SparseArray;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.Locale;

/**
 * jamorham
 *
 * Under certain circumstances the framework can release a wakelock when a service based pending
 * intent fires.
 *
 * To avoid this, we trampoline off a broadcast intent, taking care to respect Oreo rules on
 * background services.
 */

public class WakeLockTrampoline extends BroadcastReceiver {

    private static final String TAG = "WakeLockTrampoline";
    private static final String SERVICE_PARAMETER = "SERVICE_PARAM";
    private static final HashMap<String, Class> cache = new HashMap<>();
    private static final SparseArray<String> collision = new SparseArray<>();
    private static final boolean D = true;

    /**
     * When we receive the broadcast callback we extract the required service and start it.
     * The framework only releases the wakelock when onReceive returns.
     */

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onReceive(final Context context, final Intent broadcastIntent) {
        JoH.getWakeLock(TAG, 1000); // deliberately not released
        final String serviceName = broadcastIntent.getStringExtra(SERVICE_PARAMETER);

        UserError.Log.d(TAG, "Trampoline ignition for: " + serviceName);
        if (serviceName == null) {
            UserError.Log.wtf(TAG, "Incorrectly passed pending intent with null service parameter!");
            return;
        }
        final Class serviceClass = getClassFromName(serviceName);
        if (serviceClass == null) {
            UserError.Log.wtf(TAG, "Could not resolve service class for: " + serviceName);
            return;
        }

        final Intent serviceIntent = new Intent(context, serviceClass);
        final String function = broadcastIntent.getStringExtra("function");
        if (function != null) serviceIntent.putExtra("function", function);

        ComponentName startResult;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            //    && BuildConfig.targetSDK >= Build.VERSION_CODES.N
                && ForegroundServiceStarter.shouldRunCollectorInForeground()) {
            try {
                UserError.Log.d(TAG, String.format("Starting oreo foreground service: %s", serviceIntent.getComponent().getClassName()));
            } catch (NullPointerException e) {
                UserError.Log.d(TAG, "Null pointer exception in startServiceCompat");
            }
            startResult = context.startForegroundService(serviceIntent);
        } else {
            startResult = context.startService(serviceIntent);
        }
        if (D) UserError.Log.d(TAG, "Start result: " + startResult);

    }

    // wrap a service in a broadcast trampoline
    public static PendingIntent getPendingIntent(final Class serviceClass) {
        return getPendingIntent(serviceClass, 0);
    }

    // wrap a service in a broadcast trampoline
    public static PendingIntent getPendingIntent(final Class serviceClass, final int id) {
        return getPendingIntent(serviceClass, id, null);
    }

    // wrap a service in a broadcast trampoline
    public static synchronized PendingIntent getPendingIntent(final Class serviceClass, final int id, final String function) {
        final String name = serviceClass.getCanonicalName();
        final int scheduleId = name.hashCode() + id;

        final Intent intent = new Intent(xdrip.getAppContext(), WakeLockTrampoline.class).putExtra(SERVICE_PARAMETER, name);
        if (function != null) intent.putExtra("function",function);
        cache.put(name, serviceClass);

        if (D)
            UserError.Log.d(TAG, String.format(Locale.US, "Schedule %s ID: %d (%d)", name, scheduleId, id));

        final String existing = collision.get(scheduleId);
        if (existing == null) {
            collision.put(scheduleId, name);
            if (D) UserError.Log.d(TAG, "New schedule id: " + scheduleId + " for " + name);
        } else {
            if (!existing.equals(name)) {
                UserError.Log.wtf(TAG, String.format(Locale.US, "Collision between: %s vs %s (%d) @ %d", existing, name, id, scheduleId));
            } else {
                if (D)
                    UserError.Log.d(TAG, "Recurring schedule id: " + scheduleId + " for " + existing);
            }
        }
        return PendingIntent.getBroadcast(xdrip.getAppContext(), scheduleId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Class getClassFromName(final String name) {
        Class result = cache.get(name);
        if (result == null) {
            // result by reflection if cache empty due to garbage collection
            try {
                result = Class.forName(name);
            } catch (ClassNotFoundException e) {
                //
            }
        }
        return result;
    }

}
