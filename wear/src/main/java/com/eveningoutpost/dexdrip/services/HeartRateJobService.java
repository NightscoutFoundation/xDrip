package com.eveningoutpost.dexdrip.services;

// jamorham

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;


import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Due to a critical bug in Android Wear 8.x affecting at least the Huawei Watch 2
 * where background process limits are applied to apk's which do not target sdk 26 or above
 * and where the activity IS visible and where no user settings have been applied to restrict
 * access. This is the exact opposite behavior described in the Android reference documentation.
 *
 * The service is terminated silently, invisibly bringing down the entire app every 30 seconds
 * causing massive power drain and corruption potential. This very significantly harms user
 * experience and is a huge regression in the framework.
 *
 * This class attempts to work around the issue.
 *
 */

public class HeartRateJobService extends JobIntentService {

    private static final String TAG = HeartRateService.class.getSimpleName();

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        UserError.Log.d(TAG, "OnHandleWork Enter (Oreo workaround)");

        final HeartRateService service = new HeartRateService().setInjectable();
        service.realOnHandleIntent(null);

        UserError.Log.d(TAG, "OnHandleWork Exit");
    }


    static void enqueueWork() {
        enqueueWork(xdrip.getAppContext(), HeartRateJobService.class, Constants.HEART_RATE_JOB_ID, new Intent());
    }

}
