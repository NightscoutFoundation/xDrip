package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 04/02/2016.
 */

import static com.eveningoutpost.dexdrip.GcmActivity.token;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.eveningoutpost.dexdrip.cloud.jamcm.Legacy;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity;


public class TaskService extends Worker {

    private static final String TAG = "jamorham TaskService";

    public TaskService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private boolean doUnmeteredTask() {
        if (token != null) {
            if (JoH.pratelimit("unmetered-update", 43200)) {
                UpdateActivity.checkForAnUpdate(getApplicationContext());
            }
            Legacy.migration(token);
        }
        return true;
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean result = doUnmeteredTask();
        return result ? Result.success() : Result.failure();
    }
}


