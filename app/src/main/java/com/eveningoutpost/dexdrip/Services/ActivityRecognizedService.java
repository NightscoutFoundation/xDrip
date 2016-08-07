package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jamorham on 11/07/2016.
 */

public class ActivityRecognizedService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String START_ACTIVITY_ACTION = "START_ACTIVITY_ACTION";
    public static final String STOP_ACTIVITY_ACTION = "STOP_ACTIVITY_ACTION";
    public static final String RESTART_ACTIVITY_ACTION = "RESTART_ACTIVITY_ACTION";
    public static final String INCOMING_ACTIVITY_ACTION = "INCOMING_ACTIVITY_ACTION";
    private static final String PREFS_MOTION_INTERNAL = "motion_internal";
    private static final String PREFS_MOTION_TIME_SERIES = "time_series";
    private static final String PREFS_MOTION_VEHICLE_MODE = "vehicle_mode";
    private static final String TAG = "ActivityRecognizer";
    private static final boolean d = true;
    public static double last_data = -1;
    private static final double vehicle_mode_adjust_mgdl = 18;
    private static SharedPreferences prefs;
    private static DetectedActivity lastactivity;
    public static DetectedActivity activityState;
    private static GoogleApiClient mApiClient;
    private static PendingIntent mPendingIntent;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    public static void startActivityRecogniser(Context context) {
        if (d) Log.d(TAG, "Start Activity called");
        final Intent intent = new Intent(context, ActivityRecognizedService.class);
        intent.putExtra(ActivityRecognizedService.START_ACTIVITY_ACTION, ActivityRecognizedService.START_ACTIVITY_ACTION);
        context.startService(intent);
    }

    public static void reStartActivityRecogniser(Context context) {
        if (d) Log.d(TAG, "Restart Activity called");
        final Intent intent = new Intent(context, ActivityRecognizedService.class);
        intent.putExtra(ActivityRecognizedService.RESTART_ACTIVITY_ACTION, ActivityRecognizedService.RESTART_ACTIVITY_ACTION);
        context.startService(intent);
    }

    public static void stopActivityRecogniser(Context context) {
        if (d) Log.d(TAG, "Stop Activity called");
        final Intent intent = new Intent(context, ActivityRecognizedService.class);
        intent.putExtra(ActivityRecognizedService.STOP_ACTIVITY_ACTION, ActivityRecognizedService.STOP_ACTIVITY_ACTION);
        context.startService(intent);
    }

    public static void spoofActivityRecogniser(Context context, String data) {
        if (d) Log.d(TAG, "Spoofing: " + data);
        final Intent intent = new Intent(context, ActivityRecognizedService.class);
        intent.putExtra(ActivityRecognizedService.INCOMING_ACTIVITY_ACTION, data);
        context.startService(intent);
    }

    public synchronized void start() {
        start(false);
    }

    public synchronized void start(boolean no_rate_limit) {
        if (Home.getPreferencesBoolean("use_remote_motion", false)) {
            Log.d(TAG, "Not starting as we are expecting remote instead of local motion");
            return;
        }
        if ((no_rate_limit) || (JoH.ratelimit("recognizer-start", 60))) {
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

    public synchronized void stop() {
        try {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, get_pending_intent());
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception stopping activity recognition: " + e.toString());
        }
    }

    private PendingIntent get_pending_intent() {
        if (mPendingIntent == null) {
            final Intent intent = new Intent(this, ActivityRecognizedService.class);
            mPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mPendingIntent;
    }

    private static void init_prefs() {
        if (prefs == null) prefs = xdrip.getAppContext()
                .getSharedPreferences(PREFS_MOTION_INTERNAL, Context.MODE_PRIVATE);
    }

    private static String getInternalPrefsString(String name) {
        init_prefs();
        return prefs.getString(name, "");
    }

    private static void setInternalPrefsString(String name, String value) {
        init_prefs();
        prefs.edit().putString(name, value).commit(); // TODO check if commit needed
    }

    public static boolean is_in_vehicle_mode() {
        return Home.getPreferencesBoolean("vehicle_mode_enabled", false) && getInternalPrefsString(PREFS_MOTION_VEHICLE_MODE).equals("true");
    }

    public static boolean raise_limit_due_to_vehicle_mode() {
        return is_in_vehicle_mode() && (Home.getPreferencesBoolean("raise_low_limit_in_vehicle_mode", false));
    }

    public static void set_vehicle_mode(boolean value) {
        setInternalPrefsString(PREFS_MOTION_VEHICLE_MODE, value ? "true" : "false");
    }

    private static int getLastStoredActivity() {
        final motionDataWrapper motion_list = loadActivityTimeSeries();
        if (motion_list.entries.size() > 0) {
            if (d) {
                for (motionData item : motion_list.entries) {
                    Log.d(TAG, "data item: " + JoH.dateTimeText(item.timestamp) + " = " + item.activity);
                }
            }
            // get last element assuming presorted
            return motion_list.entries.get(motion_list.entries.size() - 1).activity;
        } else {
            return -1;
        }
    }

    public static ArrayList<motionData> getForGraph(long start, long end) {
        final motionDataWrapper motion_list = loadActivityTimeSeries();
        final ArrayList<motionData> ret = new ArrayList<>();
        Log.d(TAG, "Motion list original size: " + motion_list.entries.size() + " start: " + JoH.dateTimeText(start) + " end:" + JoH.dateTimeText(end));

        for (motionData item : motion_list.entries) {
            if ((item.timestamp >= start) && (item.timestamp <= end)) {
                ret.add(item);
            }
        }
        Log.d(TAG, "Motion list final size: " + ret.size());

        return ret;
    }

    public static double getVehicle_mode_adjust_mgdl() {
        return vehicle_mode_adjust_mgdl; // static for now
    }

    private static DetectedActivity getLastStoredDetectedActivity() {
        final int activity = getLastStoredActivity();
        return (activity == -1) ? null : new DetectedActivity(activity, 102);
    }

    private static motionDataWrapper loadActivityTimeSeries() {
        final String stored = getInternalPrefsString(PREFS_MOTION_TIME_SERIES);

        final motionDataWrapper motion_list;
        if (stored.length() > 0) {
            final Gson gson = new Gson();
            motion_list = gson.fromJson(stored, motionDataWrapper.class);
        } else {
            motion_list = new motionDataWrapper();
        }
        return motion_list;
    }

    private static void saveUpdatedActivityState(long timestamp) {

        final motionDataWrapper motion_list = loadActivityTimeSeries();

        if (motion_list.entries.size() > 20) {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < motion_list.entries.size(); i++) {
                    if ((JoH.tsl() - motion_list.entries.get(i).timestamp) > (86400000)) {
                        motion_list.entries.remove(i);
                        break;
                    }
                }
            }
        }

        final boolean out_of_order = (motion_list.entries.size() > 0) && timestamp < motion_list.entries.get(motion_list.entries.size() - 1).timestamp;

        motion_list.entries.add(new motionData(timestamp, activityState.getType()));

        // sort if data was out of order
        if (out_of_order) {
            if (d) Log.d(TAG, "Sorting data...");
            Collections.sort(motion_list.entries, new Comparator<motionData>() {
                public int compare(motionData left, motionData right) {
                    if (left.timestamp == right.timestamp) return 0;
                    return ((left.timestamp - right.timestamp) > 0) ? 1 : -1;
                }
            });
        }

        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeSpecialFloatingPointValues()
                .create();

        setInternalPrefsString(PREFS_MOTION_TIME_SERIES, gson.toJson(motion_list));

        //getLastStoredActivity(); // DEEEBUGG

    }

    private void restart(int frequency) {
        if (Home.getPreferencesBoolean("use_remote_motion", false)) {
            Log.d(TAG, "Not re-starting as we are expecting remote instead of local motion");
            return;
        }

        if ((mApiClient == null) || (!mApiClient.isConnected())) {
            start(true);
        } else {
            // ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient,get_pending_intent());
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, frequency, get_pending_intent());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        UserError.Log.e(TAG, "onConnected");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 3000, get_pending_intent());
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
            if (intent.getStringExtra(START_ACTIVITY_ACTION) != null) {
                start(true);
            } else if (intent.getStringExtra(RESTART_ACTIVITY_ACTION) != null) {
                restart(3000);
            } else if (intent.getStringExtra(STOP_ACTIVITY_ACTION) != null) {
                UserError.Log.uel(TAG, "Stopping service");
                stop();
            } else if (ActivityRecognitionResult.hasResult(intent)) {
                if (mApiClient == null) start();
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                handleDetectedActivities(result.getProbableActivities(), true, 0);
                last_data = JoH.ts();
            } else {
                // spoof from intent
                final String payload = intent.getStringExtra(INCOMING_ACTIVITY_ACTION);
                if ((payload != null) && (Home.getPreferencesBoolean("use_remote_motion", false))) {
                    try {
                        final String amup[] = payload.split("\\^");
                        final long timestamp = Long.parseLong(amup[0]);
                        final int activity = Integer.parseInt(amup[1]);
                        final List<DetectedActivity> incoming_list = new ArrayList<>();
                        incoming_list.add(new DetectedActivity(activity, 101));
                        handleDetectedActivities(incoming_list, false, timestamp);
                    } catch (Exception e) {
                        Log.wtf(TAG, "Exception processing incoming motion: " + e.toString());
                    }
                }
            }

        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities, boolean from_local, long timestamp) {
        DetectedActivity topActivity = null;
        int topConfidence = 0;

        if (timestamp == 0) timestamp = JoH.tsl();

        for (DetectedActivity activity : probableActivities) {
            if ((activity.getType() != DetectedActivity.TILTING) && (activity.getType() != DetectedActivity.UNKNOWN)) {
                if (activity.getConfidence() > topConfidence) {
                    topActivity = activity;
                    topConfidence = activity.getConfidence();
                }
            }
        }
        if (topActivity != null) {
            if (d) UserError.Log.uel(TAG, "Top activity: " + topActivity.toString());
            if ((topActivity.getType() != DetectedActivity.UNKNOWN) && (topActivity.getType() != DetectedActivity.TILTING)) {
                if (activityState == null) activityState = getLastStoredDetectedActivity();
                if ((topActivity.getConfidence() > 89)
                        && ((activityState == null) || (activityState.getType() != topActivity.getType()))) {
                    if (Home.getPreferencesBoolean("motion_tracking_enabled", false)) {
                        UserError.Log.ueh(TAG, "Changed activity state from " + ((activityState == null) ? "null" : activityState.toString()) + " to: " + topActivity.toString());
                        activityState = topActivity;

                        if (Home.getPreferencesBoolean("plot_motion", true))
                            saveUpdatedActivityState(timestamp);

                        switch (topActivity.getType()) {

                            case DetectedActivity.IN_VEHICLE: {
                                UserError.Log.e(TAG, "Vehicle: " + topActivity.getConfidence());
                                // confidence condition above overrides this
                                if (topActivity.getConfidence() >= 75) {

                                    if (!is_in_vehicle_mode()) set_vehicle_mode(true);
                                    // also checks if vehicle mode enabled on this handset if get != set
                                    if (is_in_vehicle_mode()) {
                                        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                                        builder.setContentText("In Vehicle: " + JoH.dateTimeText(JoH.tsl()));
                                        builder.setSmallIcon(R.drawable.ic_launcher);
                                        if (Home.getPreferencesBoolean("play_sound_in_vehicle_mode", false)) {
                                            builder.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.labbed_musical_chime));
                                        }
                                        builder.setContentTitle(getString(R.string.app_name) + " " + "Vehicle mode");
                                        NotificationManagerCompat.from(this).notify(0, builder.build());
                                    }
                                }
                                break;
                            }
                            default:
                                if (is_in_vehicle_mode()) set_vehicle_mode(false);
                                break;
                        }

                        if ((from_local) && (Home.getPreferencesBoolean("act_as_motion_master", false))) {
                            Log.d(TAG, "Sending update: " + activityState.getType());
                            GcmActivity.sendMotionUpdate(JoH.tsl(), activityState.getType());
                        }

                    } else {
                        UserError.Log.e(TAG, "Shutting down");
                        stop();
                    }
                } else {
                    UserError.Log.uel(TAG, "Last: " + ((lastactivity == null) ? "null" : lastactivity.toString()) + " Current: " + topActivity.toString());
                }
                lastactivity = topActivity;
            }
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

            Log.d(TAG, "Preference change listener fired");
            if (key.equals("motion_tracking_enabled")) {
                if (!prefs.getBoolean("motion_tracking_enabled", false)) {
                    if (d) Log.d(TAG, "Shutting down on preference change");
                    stopActivityRecogniser(xdrip.getAppContext());
                } else {
                    if (d) Log.d(TAG, "Starting on preference change");
                    startActivityRecogniser(xdrip.getAppContext());
                }
            } else if (key.equals("act_as_motion_master")) {
                if (prefs.getBoolean("act_as_motion_master", false)
                        && prefs.getBoolean("use_remote_motion", false)) {
                    if (d) Log.d(TAG, "Turning off remote motion");
                    prefs.edit().putBoolean("use_remote_motion", false).apply();

                }
            } else if (key.equals("use_remote_motion")) {
                if (prefs.getBoolean("use_remote_motion", false)
                        && prefs.getBoolean("act_as_motion_master", false)) {
                    if (d) Log.d(TAG, "Turning off motion master");
                    prefs.edit().putBoolean("act_as_motion_master", false).apply();

                }
            }

        }

    };

    protected static class motionDataWrapper {
        @Expose
        public ArrayList<motionData> entries;

        motionDataWrapper() {
            entries = new ArrayList<motionData>();
        }
    }

    public static class motionData {
        @Expose
        public long timestamp;
        @Expose
        public int activity;

        public motionData(long timestamp, int activity) {
            this.timestamp = timestamp;
            this.activity = activity;
        }

    }
}
