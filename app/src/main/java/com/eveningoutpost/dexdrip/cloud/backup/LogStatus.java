package com.eveningoutpost.dexdrip.cloud.backup;

import com.eveningoutpost.dexdrip.models.UserError;

public class LogStatus implements BackupStatus {
    private static final String TAG = "Backup";

    @Override
    public void status(String msg) {
        UserError.Log.e(TAG, "Status: " + msg);      // TODO reduce log priority in production
    }
}
