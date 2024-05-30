package com.eveningoutpost.dexdrip.utils.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;

// jamorham

public class XDripJobCreator implements JobCreator {

    private static final String TAG = "XDripJobCreator";
    private static final boolean D = false;

    public XDripJobCreator() {
        if (D) UserError.Log.uel(TAG, "Start: " + JoH.dateTimeText(JoH.tsl()));
    }

    /**
     * All new Job classes need to be included in the factory method below
     */
    @Override
    @Nullable
    public Job create(@NonNull final String tag) {
        if (D) UserError.Log.ueh("JobCreator", JoH.dateTimeText(JoH.tsl()) + " Passed: " + tag);
        switch (tag) {
/*
            case CloudSyncJob.TAG:
                return new CloudSyncJob();

*/

            case DailyJob.TAG:
                return new DailyJob();

            default:
                UserError.Log.wtf(TAG, "Failed to match Job: " + tag + " requesting cancellation");
                try {
                    JobManager.instance().cancelAllForTag(tag);
                } catch (Exception e) {
                    //
                }
                return null;
        }
    }
}





