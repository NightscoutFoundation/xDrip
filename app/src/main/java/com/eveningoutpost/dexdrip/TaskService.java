package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 04/02/2016.
 */

import android.content.Intent;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;


public class TaskService extends GcmTaskService {

    private static final String TAG = "jamorham TaskService";


    @Override
    public void onInitializeTasks() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Log.d(TAG, "onRunTask: " + taskParams.getTag());

        String tag = taskParams.getTag();

        // Default result is success.
        int result = GcmNetworkManager.RESULT_SUCCESS;

        // Choose method based on the tag.
        if (GcmActivity.TASK_TAG_UNMETERED.equals(tag)) {
            result = doUnmeteredTask();
        } else if (GcmActivity.TASK_TAG_CHARGING.equals(tag)) {
            result = doChargingTask();
        }
        // Return one of RESULT_SUCCESS, RESULT_FAILURE, or RESULT_RESCHEDULE
        return result;
    }

    private int doUnmeteredTask() {
        if (GcmActivity.token != null) {
            if (JoH.pratelimit("unmetered-update", 43200)) {
                UpdateActivity.checkForAnUpdate(getApplicationContext());
            }
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private int doChargingTask() {
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}


