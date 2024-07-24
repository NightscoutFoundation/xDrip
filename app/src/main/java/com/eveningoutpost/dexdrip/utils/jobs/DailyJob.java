package com.eveningoutpost.dexdrip.utils.jobs;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.DailyIntentService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

/**
 * jamorham
 *
 * Scheduled daily job for cleanup / maintenance tasks
 *
 * Should run once per day at the best time period for battery conservation and user experience
 *
 */

public class DailyJob extends Job {

    public static final String TAG = "xDrip-Daily";

    @Override
    @NonNull
    protected Result onRunJob(@NonNull Job.Params params) {
        final long startTime = JoH.tsl();
        DailyIntentService.work();
        UserError.Log.uel(TAG, JoH.dateTimeText(JoH.tsl()) + " Job Ran - finished, duration: " + JoH.niceTimeScalar(JoH.msSince(startTime)));
        return Result.SUCCESS;
    }

    public static void schedule() {
        if (JoH.pratelimit("daily-job-schedule", 60000)) {
            UserError.Log.uel(TAG, JoH.dateTimeText(JoH.tsl()) + " Job Scheduled"); // Debug only
            new JobRequest.Builder(TAG)
                    .setPeriodic(Constants.DAY_IN_MS, Constants.HOUR_IN_MS * 12)
                    .setRequiresDeviceIdle(true)
                    .setRequiresCharging(true)
                    .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule();
        }
    }
}
