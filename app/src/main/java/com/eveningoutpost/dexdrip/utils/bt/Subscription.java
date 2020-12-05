package com.eveningoutpost.dexdrip.utils.bt;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * jamorham
 *
 * wrapper class to smooth rxandroidble migration
 */

@RequiredArgsConstructor
public class Subscription implements Disposable {

    private final Disposable disposable;
    @Getter
    private volatile boolean unsubscribed;

    public synchronized void unsubscribe() {
        dispose();
        unsubscribed = true;
    }


    @Override
    public void dispose() {
        disposable.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposable.isDisposed();
    }


    public static void addErrorHandler(final String TAG) {
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

}

