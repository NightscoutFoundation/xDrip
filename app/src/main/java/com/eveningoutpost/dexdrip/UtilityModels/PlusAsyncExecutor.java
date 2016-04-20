
package com.eveningoutpost.dexdrip.UtilityModels;

import android.support.annotation.NonNull;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.Executor;

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
    private static final HashMap<String, Queue<Runnable>> taskQueues = new HashMap<String, Queue<Runnable>>();
    private static final HashMap<String, Runnable> currentTask = new HashMap<String, Runnable>();


    public synchronized void execute(@NonNull final Runnable r) {

        final String queueId = JoH.backTraceShort(0); // TODO probably a better way to get a queue name

        // Create the queue if it doesn't exist yet
        if (!taskQueues.containsKey(queueId)) {
            Log.d(TAG, "New task queue for: " + queueId);
            taskQueues.put(queueId, new ArrayDeque<Runnable>());
            currentTask.put(queueId, null);
        }

        // enqueue this current runnable to the respective queue
        taskQueues.get(queueId).offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    // each task will try to call the next when done
                    next(queueId);
                }
            }
        });

        final int qsize = taskQueues.get(queueId).size();

        // Log if tasks are backlogged
        if (qsize > 1) {
            Log.i(TAG, "Task queue size: " + qsize + " on queue: " + queueId);
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
        Runnable task = currentTask.get(queueId);
        if (task != null) {
            Log.d(TAG, " New thread: " + queueId);
            (new Thread(task)).start();
        } else {
            Log.d(TAG, "Queue empty: " + queueId);
        }

    }
}
