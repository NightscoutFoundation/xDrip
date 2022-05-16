package com.eveningoutpost.dexdrip.utils.jobs;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import lombok.val;

/**
 * JamOrHam
 * Sequential background queue processor
 */
public class BackgroundQueue extends HandlerThread {

    private static final boolean D = false;
    private static final String TAG = BackgroundQueue.class.getSimpleName();

    {
        if (D) UserError.Log.e(TAG, "Instantiated!");
    }

    public BackgroundQueue(String tag, int threadPriorityBackground) {
        super(tag, threadPriorityBackground);
    }

    private static class Singleton {
        private static final BackgroundQueue INSTANCE = new BackgroundQueue(TAG, THREAD_PRIORITY_BACKGROUND).go();
    }

    public static BackgroundQueue getInstance() {
        return BackgroundQueue.Singleton.INSTANCE;
    }

    public static void post(final Runnable runnable) {
        val handler = getInstance().getHandler();
        if (handler != null) {
            handler.post(runnable);
        } else {
            UserError.Log.e(TAG, "Handler not ready yet, posting to ui thread instead");
            JoH.runOnUiThread(runnable);
        }
    }

    public long xcounter = 0;

    public static void postDelayed(Runnable runnable, final long delay) {
        if (D) {
            val counter = getInstance().xcounter++;
            val origRunnable = runnable;
            runnable = () -> {
                UserError.Log.d(TAG, "Execute: " + counter);
                origRunnable.run();
            };
        }
        val handler = getInstance().getHandler();
        if (handler != null) {
            if (D) UserError.Log.d(TAG, "Handler ready");
            handler.postDelayed(runnable, delay);
        } else {
            UserError.Log.e(TAG, "Handler not ready yet, posting to ui thread delayed instead");
            JoH.runOnUiThreadDelayed(runnable, delay);
        }
    }

    private volatile Handler myHandler;

    Handler getHandler() {
        if (myHandler == null) {
            synchronized (BackgroundQueue.class) {
                if (myHandler == null) {
                    if (D) {
                        UserError.Log.e(TAG, "Creating new handler");
                    }
                    myHandler = new MyHandler(getLooper());
                }
            }
        }
        return myHandler;
    }

    BackgroundQueue go() {
        UserError.Log.e(TAG, "go called");
        this.start();
        return this;
    }

    static class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            UserError.Log.e(TAG, "Got message: " + msg);
        }

    }
}


