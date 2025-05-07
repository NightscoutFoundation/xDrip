package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BloodSugarUpdateReceiver extends BroadcastReceiver {
    public static final String ACTION_UPDATE_BLOOD_SUGAR = "com.example.ACTION_UPDATE_BLOOD_SUGAR";

    private final BloodSugarUpdateListener mListener;

    public BloodSugarUpdateReceiver(BloodSugarUpdateListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_UPDATE_BLOOD_SUGAR)) {
            String bloodSugarValue = intent.getStringExtra("blood_sugar_value");
            mListener.onBloodSugarUpdate(bloodSugarValue);
        }
    }

    public interface BloodSugarUpdateListener {
        void onBloodSugarUpdate(String bloodSugarValue);
    }
}
