package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;

import com.eveningoutpost.dexdrip.Models.UserError;

public class DailyIntentService extends IntentService {
    // DAILY TASKS CAN GO IN HERE!

    public DailyIntentService() {
        super("DailyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        UserError.cleanup();
    }

  }
