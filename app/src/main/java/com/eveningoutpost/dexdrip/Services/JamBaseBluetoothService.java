package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.bt.HandleBleScanException;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;


// jamorham base class for reactive bluetooth services

public abstract class JamBaseBluetoothService extends Service {

    private final PowerManager.WakeLock wl = JoH.getWakeLock("jam-bluetooth-generic", 1000);
    protected static boolean android_wear = false;
    protected static final String BUGGY_SAMSUNG_ENABLED = "buggy-samsung-enabled";
    protected String TAG = this.getClass().getSimpleName();
    private volatile boolean background_launch_waiting = false;
    protected static final long TOLERABLE_JITTER = 10000;

    protected ForegroundServiceStarter foregroundServiceStarter;
    protected Service service;

    protected String handleBleScanException(BleScanException bleScanException) {
        return HandleBleScanException.handle(TAG, bleScanException);
    }


    {
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                if (!e.getCause().toString().contains("OperationSuccess")) {
                    UserError.Log.e(TAG, "RxJavaError: " + e.getMessage());
                }
            } else {
                UserError.Log.wtf(TAG, "RxJavaError2:" + e.getClass().getCanonicalName() + " " + e.getMessage() + " " + JoH.backTrace(3));
            }
        });

    }

    protected void startInForeground() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
        foregroundServiceStarter.start();
        foregroundStatus();
    }

    protected void foregroundStatus() {
        Inevitable.task("jam-base-foreground-status", 2000, () -> UserError.Log.d("FOREGROUND", service.getClass().getSimpleName() + (JoH.isServiceRunningInForeground(service.getClass()) ? " is running in foreground" : " is not running in foreground")));
    }

    public void background_automata() {
        background_automata(100);
    }

    public synchronized void background_automata(final int timeout) {
        if (background_launch_waiting) {
            UserError.Log.d(TAG, "Blocked by existing background automata pending");
            return;
        }
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG + "-background", timeout + 1000);
        background_launch_waiting = true;
        new Thread(() -> {
            JoH.threadSleep(timeout);
            background_launch_waiting = false;
            automata();
            JoH.releaseWakeLock(wl);
        }).start();
    }

    protected synchronized boolean automata() {
        throw new RuntimeException("automata stub - not implemented!");
    }

    protected synchronized void extendWakeLock(long ms) {
        JoH.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    protected void releaseWakeLock() {
        JoH.releaseWakeLock(wl);
    }


    protected class OperationSuccess extends RuntimeException {
        public OperationSuccess(String message) {
            super(message);
            UserError.Log.d(TAG, "Operation Success: " + message);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void tryGattRefresh(RxBleConnection connection) {
        if (connection == null) return;
        if (JoH.ratelimit("gatt-refresh", 60)) {
            if (Pref.getBoolean("use_gatt_refresh", true)) {
                try {
                    if (connection != null)
                        UserError.Log.d(TAG, "Trying gatt refresh queue");
                    connection.queue((new GattRefreshOperation(0))).timeout(2, TimeUnit.SECONDS).subscribe(
                            readValue -> {
                                UserError.Log.d(TAG, "Refresh OK: " + readValue);
                            }, throwable -> {
                                UserError.Log.d(TAG, "Refresh exception: " + throwable);
                            });
                } catch (NullPointerException e) {
                    UserError.Log.d(TAG, "Probably harmless gatt refresh exception: " + e);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Got exception trying gatt refresh: " + e);
                }
            } else {
                UserError.Log.d(TAG, "Gatt refresh rate limited");
            }
        }
    }

    protected static class GattRefreshOperation implements RxBleCustomOperation<Void> {
        private long delay_ms = 500;

        GattRefreshOperation() {
        }

        public GattRefreshOperation(long delay_ms) {
            this.delay_ms = delay_ms;
        }

        @NonNull
        @Override
        public Observable<Void> asObservable(BluetoothGatt bluetoothGatt,
                                             RxBleGattCallback rxBleGattCallback,
                                             Scheduler scheduler) throws Throwable {

            return Observable.fromCallable(() -> refreshDeviceCache(bluetoothGatt))
                    .delay(delay_ms, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .subscribeOn(scheduler);
        }

        private Void refreshDeviceCache(final BluetoothGatt gatt) {
            UserError.Log.d("BaseBluetooth", "Gatt Refresh " + (JoH.refreshDeviceCache("BaseBluetooth", gatt) ? "succeeded" : "failed"));
            return null;
        }
    }

    protected static byte[] nn(final byte[] array) {
        if (array == null) {
            if (JoH.ratelimit("never-null", 60)) {
                UserError.Log.wtf("NeverNull", "Attempt to pass null!!! " + JoH.backTrace());
                return new byte[1];
            }
        }
        return array;
    }

    protected static void enableBuggySamsungIfNeeded(final String TAG) {
        if ((JoH.isSamsung() && PersistentStore.getLong(BUGGY_SAMSUNG_ENABLED) > 4)) {
            UserError.Log.d(TAG, "Enabling buggy samsung due to persistent metric");
            JoH.buggy_samsung = true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        UserError.Log.d("FOREGROUND", "Current Service: " + service.getClass().getSimpleName());
        startInForeground();
    }

}
