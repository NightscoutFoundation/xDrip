package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.PowerManager;
import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.NORM_PRIORITY;

/**
 * xDrip plus AsyncExecutor
 * multi-queue Executor for AsyncTask
 * <p/>
 * Created by jamorham on 19/04/2016.
 * <p/>
 * AsyncTask works only on a single thread, any lock-up will bring down
 * everything else using that thread. This splits each calling task in to
 * its own single thread with deadlock detection and reporting.
 */

public class PlusAsyncExecutor implements Executor {

    private static final String TAG = "jamorham exec";
    private static final ConcurrentHashMap<String, Queue<Runnable>> taskQueues = new ConcurrentHashMap<>();
    private static final HashMap<String, Runnable> currentTask = new HashMap<>();

    private static final AtomicInteger wlocks = new AtomicInteger(0);

    public synchronized void execute(@NonNull final Runnable r) {

        final String queueId = JoH.backTraceShort(0); // TODO probably a better way to get a queue name

        // Create the queue if it doesn't exist yet
        if (!taskQueues.containsKey(queueId)) {
            Log.d(TAG, "New task queue for: " + queueId);
            taskQueues.put(queueId, new ArrayDeque<>());
            currentTask.remove(queueId);
        }

        final int qsize = taskQueues.get(queueId).size();

        // Log if tasks are backlogged
        if (qsize > 0) {
            Log.i(TAG, "Task queue size: " + qsize + " on queue: " + queueId);
        }

        // if queue isnt broken then add task to it
        if (qsize < 20) {
            // enqueue this current runnable to the respective queue
            taskQueues.get(queueId).offer(new Runnable() {
                public void run() {
                    //final PowerManager pm = (PowerManager) xdrip.getAppContext().getSystemService(Context.POWER_SERVICE);
                    //final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, queueId);
                    final PowerManager.WakeLock wl = JoH.getWakeLock(queueId, 3600000);
                    try {
                        //wl.acquire(3600000); // failsafe value = 60 mins
                        final int locksnow = wlocks.incrementAndGet();
                        if (locksnow > 1)
                            Log.d(TAG, queueId + " Acquire Wakelocks total: " + locksnow);
                        r.run();
                    } finally {
                        // each task will try to call the next when done
                        next(queueId);
                        JoH.releaseWakeLock(wl); // will stack wakelocks
                        final int locksnow = wlocks.decrementAndGet();
                        if (locksnow != 0)
                            Log.d(TAG, queueId + " Release Wakelocks total: " + locksnow);
                    }
                }
            });

        } else {
            Log.e(TAG, "Queue so backlogged we are not extending! " + queueId);
        }

        // if we are not busy then run the queue
        if (currentTask.get(queueId) == null) {
            next(queueId);
        } else if (qsize > 2) {
            // report deadlock if queue is stacking up
            final String err = JoH.hourMinuteString() + " Task deadlock on: " + queueId + "! @" + qsize;
            Log.e(TAG, err);
            Home.toaststaticnext(err);
        }

    }

    // process the next runnable in the named queue
    // create a single thread for each queue
    private synchronized void next(String queueId) {
        currentTask.put(queueId, taskQueues.get(queueId).poll());
        final Runnable task = currentTask.get(queueId);
        if (task != null) {
            Log.d(TAG, " New thread: " + queueId);
            final Thread t = (new Thread(task));
            t.setPriority(NORM_PRIORITY - 1);
            t.start();
        } else {
            Log.d(TAG, "Queue empty: " + queueId);
        }

    }
}
