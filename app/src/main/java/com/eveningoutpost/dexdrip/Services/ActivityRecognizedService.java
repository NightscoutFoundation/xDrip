package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by jamorham on 11/07/2016.
 */

public class ActivityRecognizedService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String START_ACTIVITY_ACTION = "START_ACTIVITY_ACTION";
    private static final String TAG = "ActivityRecognizer";
    public static double last_data = -1;
    private static DetectedActivity lastactivity;
    public static DetectedActivity activityState;
    private static GoogleApiClient mApiClient;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    public static void startActivityRecogniser(Context context) {
        Intent intent = new Intent(context, ActivityRecognizedService.class);
        intent.putExtra(ActivityRecognizedService.START_ACTIVITY_ACTION, ActivityRecognizedService.START_ACTIVITY_ACTION);
        context.startService(intent);
    }

    public synchronized void start() {
        if (JoH.ratelimit("recognizer-start", 60)) {
            UserError.Log.e(TAG, "Restarting API");
            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mApiClient.connect();
        } else {
            UserError.Log.e(TAG, "Couldn't restart API due to ratelimit");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        UserError.Log.e(TAG, "onConnected");
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 6000, pendingIntent);
    }


    @Override
    public void onConnectionSuspended(int i) {
        UserError.Log.e(TAG, "onConnectionSuspended");
        start();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        UserError.Log.e(TAG, "onConnectionFailed");
        start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG, 60000);
        try {
            if ((mApiClient == null) || intent.getStringExtra(ActivityRecognizedService.START_ACTIVITY_ACTION) != null) {
                start();
            }
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                handleDetectedActivities(result.getProbableActivities());
                last_data = JoH.ts();
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        DetectedActivity topActivity = null;
        int topConfidence = 0;

        for (DetectedActivity activity : probableActivities) {
            if ((activity.getType()!=DetectedActivity.TILTING)&&(activity.getType()!=DetectedActivity.UNKNOWN)) {
                if (activity.getConfidence() > topConfidence) {
                    topActivity = activity;
                    topConfidence = activity.getConfidence();
                }
            }
        }
        if (topActivity != null) {
            UserError.Log.uel(TAG, "Top activity: " + topActivity.toString());
            if ((topActivity.getType() != DetectedActivity.UNKNOWN) && (topActivity.getType()!= DetectedActivity.TILTING))
            {
                if ((topActivity.getConfidence()>89)
                && ((activityState==null) || (activityState.getType()!=topActivity.getType())))
                {
                    UserError.Log.ueh(TAG,"Changed activity state from "+((activityState == null) ? "null" : activityState.toString())+" to: "+topActivity.toString());
                    activityState=topActivity;

                    switch (topActivity.getType()) {

                        case DetectedActivity.IN_VEHICLE: {
                            UserError.Log.e(TAG, "Vehicle: " + topActivity.getConfidence());
                            if (topActivity.getConfidence() >= 75) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                                builder.setContentText("Vehicle ping");
                                builder.setSmallIcon(R.drawable.ic_launcher);
                                builder.setContentTitle(getString(R.string.app_name));
                                NotificationManagerCompat.from(this).notify(0, builder.build());
                            }
                            break;
                        }
                    }
                } else {
                    UserError.Log.uel(TAG,"Last: "+((lastactivity==null) ? "null" : lastactivity.toString())+" Current: "+topActivity.toString());
                }
                lastactivity=topActivity;
            }
        }
    }
}
