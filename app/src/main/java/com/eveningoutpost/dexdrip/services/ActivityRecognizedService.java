package com.eveningoutpost.dexdrip.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import android.util.SparseArray;

import com.eveningoutpost.dexdrip.ErrorsActivity;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.ShotStateStore;
import com.eveningoutpost.dexdrip.utilitymodels.VehicleMode;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
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

    private static final String START_ACTIVITY_ACTION = "START_ACTIVITY_ACTION";
    private static final String STOP_ACTIVITY_ACTION = "STOP_ACTIVITY_ACTION";
    private static final String RESTART_ACTIVITY_ACTION = "RESTART_ACTIVITY_ACTION";
    private static final String INCOMING_ACTIVITY_ACTION = "INCOMING_ACTIVITY_ACTION";
    private static final String RECHECK_VEHICLE_MODE = "RECHECK_VEHICLE_MODE";
    private static final String PREFS_MOTION_INTERNAL = "motion_internal";
    private static final String PREFS_MOTION_TIME_SERIES = "time_series";
    private static final String PREFS_MOTION_VEHICLE_MODE = "vehicle_mode";
    private static final String REQUESTED = "requested";
    private static final String RECEIVED = "received";
    private static final String REQUESTED_ALL_TIME = "requested_all_time";
    private static final String RECEIVED_ALL_TIME = "received_all_time";
    private static final String VEHICLE_MODE_SINCE = "vehicle_mode_since";
    private static final String VEHICLE_MODE_LAST_ALERT = "vehicle_mode_last_alert";
    private static final String TAG = "ActivityRecognizer";
    private static final int VEHICLE_NOTIFICATION_ID = 37;
    private static final int VEHICLE_NOTIFICATION_ERROR_ID = 38;
    private static final boolean d = false;
    private static PowerManager.WakeLock wl_global;
    private static PowerManager.WakeLock wl_start;
    public static double last_data = -1;
    private static final double vehicle_mode_adjust_mgdl = 18;
    private static final int FREQUENCY = 1000;
    private static final int MAX_RECEIVED = 4;
    private static int received = 0;
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

    // TODO this is not used
    public static void reCheckVehicleMode(Context context) {
        if (d) Log.d(TAG, "recheck Vehicle mode");
        if (is_in_vehicle_mode()) {
            final Intent intent = new Intent(context, ActivityRecognizedService.class);
            intent.putExtra(RECHECK_VEHICLE_MODE, RECHECK_VEHICLE_MODE);
            context.startService(intent);
        }
    }


    public synchronized void start() {
        start(false);
    }

    public synchronized void start(boolean no_rate_limit) {
        if (Pref.getBoolean("use_remote_motion", false)) {
            Log.d(TAG, "Not starting as we are expecting remote instead of local motion");
            return;
        }
        if ((no_rate_limit) || (JoH.ratelimit("recognizer-start", 60))) {
            release_wl_start();
            wl_start = JoH.getWakeLock("recognizer-start", 60000);
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
        stopUpdates();
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
        prefs.edit().putString(name, value).apply();
    }

    private static long getInternalPrefsLong(String name) {
        init_prefs();
        return prefs.getLong(name, 0);
    }

    private static void setInternalPrefsLong(String name, long value) {
        init_prefs();
        prefs.edit().putLong(name, value).apply();
    }

    private static void incrementInternalPrefsLong(String name) {
        setInternalPrefsLong(name, getInternalPrefsLong(name) + 1);
    }

    private static long addInternalPrefsLong(String name_source, String name_destination) {
        final long f_val = getInternalPrefsLong(name_destination) + getInternalPrefsLong(name_source);
        setInternalPrefsLong(name_destination, f_val);
        return f_val;
    }

    private static void interpretRatio(Context context) {
        final long requested = getInternalPrefsLong(REQUESTED);
        final long received = getInternalPrefsLong(RECEIVED);

        if (requested > 0) {
            Log.d(TAG, "Requested: " + requested + " Received: " + received);
            if (requested == 10) {
                if (received < 4) {
                    UserError.Log.ueh(TAG, "Issuing full screen wakeup as req: " + getInternalPrefsLong(REQUESTED) + " rec: " + getInternalPrefsLong(RECEIVED));
                    Home.startHomeWithExtra(context, Home.HOME_FULL_WAKEUP, "1");
                }
            } else if (requested == 15) {
                if ((received < 4) && (!PowerStateReceiver.is_power_connected())) {
               disableMotionTrackingDueToErrors(context);
                }
            }

            if (requested > 20) {
                evaluateRequestReceivedCounters(false,context);
                resetRequestedReceivedCounters();
            }
        }
    }

    private static void disableMotionTrackingDueToErrors(Context context) {
        final long requested = getInternalPrefsLong(REQUESTED);
        final long received = getInternalPrefsLong(RECEIVED);
        Home.toaststaticnext("DISABLED MOTION TRACKING DUE TO FAILURES! See Error Log!");
        final String msg = "Had to disable motion tracking feature as it did not seem to be working and may be incompatible with your phone. Please report this to the developers using the send logs feature: " + requested + " vs " + received + " " + JoH.getDeviceDetails();
        UserError.Log.wtf(TAG, msg);
        UserError.Log.ueh(TAG, msg);
        Pref.setBoolean("motion_tracking_enabled", false);
        evaluateRequestReceivedCounters(true, context); // mark for disable
        setInternalPrefsLong(REQUESTED, 0);
        setInternalPrefsLong(RECEIVED, 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        Intent intent = new Intent(xdrip.getAppContext(), ErrorsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(xdrip.getAppContext(), 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        builder.setContentText("Shut down motion detection! See Error Logs - Please report to developer" + JoH.dateTimeText(JoH.tsl()));
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle("Problem with motion detection!");
        NotificationManagerCompat.from(context).notify(VEHICLE_NOTIFICATION_ERROR_ID, builder.build());
    }

    private static void evaluateRequestReceivedCounters(boolean disable,Context context) {
        final long requested_all_time = addInternalPrefsLong(REQUESTED, REQUESTED_ALL_TIME);
        final long received_all_time = addInternalPrefsLong(RECEIVED, RECEIVED_ALL_TIME);
        // TODO check connectivity
        // TODO use preferences for last send time - don't report more than once every 3 days

        final double ratio = ((received_all_time * 100) / requested_all_time);
        if (d)
            Log.d(TAG, "evaluteRequestReceived: " + requested_all_time + "/" + received_all_time + " " + JoH.qs(ratio, 2));
        /*
        disabled as was for early debug only
        if (JoH.ratelimit("evalute-request-received", 86400) || (disable)) {
            if (Pref.getBoolean("enable_crashlytics", true)) {
                new WebAppHelper(null).executeOnExecutor(xdrip.executor, xdrip.getAppContext().getString(R.string.wserviceurl) + "/joh-mreport/" + (disable ? 1 : 0) + "/" + requested_all_time + "/" + received_all_time + "/" + JoH.qs(ratio, 0) + "/" + JoH.base64encode(JoH.getDeviceDetails()));
            }
        }*/

        // mutually exclusive logic
        if ((!disable) && (requested_all_time > 100) && (ratio < 90 ) && (!PowerStateReceiver.is_power_connected()) && (JoH.isAnyNetworkConnected()) && (JoH.ratelimit("disable_motion", 86400))) {
            disableMotionTrackingDueToErrors(context);
        }

    }

    private static void resetRequestedReceivedCounters() {
        if (d) Log.d(TAG, "Resetting Request/Received counters");
        setInternalPrefsLong(REQUESTED, 0);
        setInternalPrefsLong(RECEIVED, 0);
    }

    // TODO refactor all the actual vehicle mode handling in to its own class
    // TODO would reversing the order of these items be more efficient?
    public static boolean is_in_vehicle_mode() {
        return (Pref.getBooleanDefaultFalse("motion_tracking_enabled") || VehicleMode.viaCarAudio())
                && VehicleMode.isEnabled() && getInternalPrefsString(PREFS_MOTION_VEHICLE_MODE).equals("true");
    }

    public static boolean raise_limit_due_to_vehicle_mode() {
        return is_in_vehicle_mode() && (Pref.getBoolean("raise_low_limit_in_vehicle_mode", false));
    }

    public static void set_vehicle_mode(boolean value) {
        setInternalPrefsString(PREFS_MOTION_VEHICLE_MODE, value ? "true" : "false");
        if (value) {
            setInternalPrefsLong(VEHICLE_MODE_SINCE, JoH.tsl());
        } else {
            final long duration = get_vehicle_mode_minutes();
            setInternalPrefsLong(VEHICLE_MODE_SINCE, -1);
            if (duration > 0) {
                UserError.Log.ueh(TAG, "Exiting vehicle mode after: " + duration + " minutes");
            }
        }
        VehicleMode.sendBroadcast();
    }

    private static int get_vehicle_mode_minutes() {
        final long duration = JoH.tsl() - getInternalPrefsLong(VEHICLE_MODE_SINCE);
        if (duration < 86400000) {
            return (int) (duration / 60000);
        } else {
            return -1;
        }
    }

    private static int get_minutes_since_last_alert() {
        final long duration = JoH.tsl() - getInternalPrefsLong(VEHICLE_MODE_LAST_ALERT);
        if (duration < 86400000) {
            return (int) (duration / 60000);
        } else {
            return -1;
        }
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
        if (d) Log.d(TAG, "Motion list final size: " + ret.size());

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

    private synchronized void requestUpdates(int frequency) {
        try {
            received = 0; // reset
            wl_global = JoH.getWakeLock("motion-wait", frequency * 5); // released later
            if (d) Log.d(TAG, "requestUpdates called: " + frequency);
            incrementInternalPrefsLong(REQUESTED);
            interpretRatio(this);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, frequency, get_pending_intent());
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception starting activity recognition: " + e.toString());
        }
    }

    private synchronized void stopUpdates() {
        try {
            if (d) Log.d(TAG, "stopUpdates called");
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, get_pending_intent());
            if (wl_global != null) {
                if (d) Log.d(TAG, "release wl_global");
                JoH.releaseWakeLock(wl_global);
                wl_global = null;
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception stopping activity recognition: " + e.toString());
        }
    }

    private void restart(int frequency) {
        if (Pref.getBoolean("use_remote_motion", false)) {
            if (d) Log.d(TAG, "Not re-starting as we are expecting remote instead of local motion");
            return;
        }

        if ((mApiClient == null) || (!mApiClient.isConnected())) {
            start(true);
        } else {
            requestUpdates(frequency);
        }
    }

    private synchronized void release_wl_start() {
        if (wl_start != null) {
            if (d) Log.d(TAG, "release wl_start");
            JoH.releaseWakeLock(wl_start);
            wl_start = null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        UserError.Log.e(TAG, "onConnected");
        requestUpdates(FREQUENCY);
        release_wl_start();
    }


    @Override
    public void onConnectionSuspended(int i) {
        UserError.Log.e(TAG, "onConnectionSuspended");
        start();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        UserError.Log.e(TAG, "onConnectionFailed: " + connectionResult.toString());
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.getResolution().send();
            } catch (NullPointerException e) {
                //
            } catch (Exception e) {
                UserError.Log.e(TAG, e.toString());
            }
        } else {
            if (connectionResult.getErrorCode() == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
                JoH.static_toast_long("Google Play Services update download needed for Motion");
                Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
                notificationIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms"));
                final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                JoH.showNotification("Google Update Needed","Google Play Services update download needed for Motion. Download update via Google Play Store and try motion again after installed.",contentIntent,60302,true,true,true);
                UserError.Log.ueh(TAG,"Google Play Services updated needed for motion - disabling motion for now");
                Pref.setBoolean("motion_tracking_enabled", false);

            }
            start();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG, 60000);
        try {
            if (intent.getStringExtra(START_ACTIVITY_ACTION) != null) {
                start(true);
            } else if (intent.getStringExtra(RESTART_ACTIVITY_ACTION) != null) {
                restart(FREQUENCY);
                checkVehicleRepeatNotification();
            } else if (intent.getStringExtra(STOP_ACTIVITY_ACTION) != null) {
                UserError.Log.uel(TAG, "Stopping service");
                stop();
            } else if (intent.getStringExtra(RECHECK_VEHICLE_MODE) != null) {
                checkVehicleRepeatNotification();
            } else if (ActivityRecognitionResult.hasResult(intent)) {
                if ((Pref.getBooleanDefaultFalse("motion_tracking_enabled"))) {
                    if (mApiClient == null) start();
                    ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                    final int topConfidence = handleDetectedActivities(result.getProbableActivities(), true, 0);
                    last_data = JoH.ts();
                    received++;
                    if (d)
                        Log.d(TAG, JoH.hourMinuteString() + " :: Packets received: " + received + " Top confidence: " + topConfidence);
                    if ((received > MAX_RECEIVED) || (topConfidence > 90))
                        stopUpdates(); // one hit only
                } else {
                    if (JoH.ratelimit("not-expected-activity", 1200)) {
                        UserError.Log.e(TAG, "Received ActivityRecognition we were not expecting!"); /// DEEEBUG
                        stop();
                    }
                }
            } else {
                // spoof from intent
                final String payload = intent.getStringExtra(INCOMING_ACTIVITY_ACTION);
                if ((payload != null) && (Pref.getBoolean("use_remote_motion", false))) {
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

    private int handleDetectedActivities(List<DetectedActivity> probableActivities, boolean from_local, long timestamp) {
        DetectedActivity topActivity = null;
        int topConfidence = 0;

        if (timestamp == 0) timestamp = JoH.tsl();
        incrementInternalPrefsLong(RECEIVED);

        for (DetectedActivity activity : probableActivities) {
            if ((activity.getType() != DetectedActivity.TILTING) && (activity.getType() != DetectedActivity.UNKNOWN)) {
                if (activity.getConfidence() > topConfidence) {
                    topActivity = activity;
                    topConfidence = activity.getConfidence();
                }
            }
        }
        if (topActivity != null) {
            if (d)
                UserError.Log.uel(TAG, "Top activity: " + topActivity.toString() + "req: " + getInternalPrefsLong(REQUESTED) + " rec: " + getInternalPrefsLong(RECEIVED));
            if ((topActivity.getType() != DetectedActivity.UNKNOWN) && (topActivity.getType() != DetectedActivity.TILTING)) {
                if (activityState == null) activityState = getLastStoredDetectedActivity();

                if (((topActivity.getConfidence() > 89) || ((lastactivity != null) && (topActivity.getType() == lastactivity.getType()) && ((lastactivity.getConfidence() + topActivity.getConfidence()) > 150)))
                        && ((activityState == null) || (activityState.getType() != topActivity.getType()))) {
                    if (Pref.getBoolean("motion_tracking_enabled", false)) {
                        UserError.Log.ueh(TAG, "Changed activity state from " + ((activityState == null) ? "null" : activityState.toString()) + " to: " + topActivity.toString());
                        activityState = topActivity;

                        if (Pref.getBoolean("plot_motion", true))
                            saveUpdatedActivityState(timestamp);

                        switch (topActivity.getType()) {

                            case DetectedActivity.IN_VEHICLE: {
                                UserError.Log.e(TAG, "Vehicle: " + topActivity.getConfidence());
                                // confidence condition above overrides this for non consolidated entries
                                if (topActivity.getConfidence() >= 75) {

                                    if (!VehicleMode.isVehicleModeActive()) VehicleMode.setVehicleModeActive(true);
                                    // also checks if vehicle mode enabled on this handset if get != set
                                    if (is_in_vehicle_mode()) {
                                        raise_vehicle_notification("In Vehicle Mode: " + JoH.dateTimeText(JoH.tsl()));
                                    }
                                }
                                break;
                            }
                            default:
                                if (is_in_vehicle_mode()) {
                                    set_vehicle_mode(false);
                                    cancel_vehicle_notification();
                                }
                                break;
                        }

                        if ((from_local) && Pref.getBoolean("motion_tracking_enabled", false) && (Pref.getBoolean("act_as_motion_master", false))) {
                            if (d) Log.d(TAG, "Sending update: " + activityState.getType());
                            GcmActivity.sendMotionUpdate(JoH.tsl(), activityState.getType());
                        }

                    } else {
                        UserError.Log.e(TAG, "Shutting down");
                        stop();
                    }
                } else {
                    if (JoH.ratelimit("check-vehicle-repeat", 60)) checkVehicleRepeatNotification();
                    if (d)
                        UserError.Log.uel(TAG, "Last: " + ((lastactivity == null) ? "null" : lastactivity.toString()) + " Current: " + topActivity.toString());
                }
                lastactivity = topActivity;
            }
        }
        return topConfidence;
    }

    private void raise_vehicle_notification(String msg) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentText(msg);
        builder.setSmallIcon(R.drawable.ic_launcher);
        if (VehicleMode.shouldPlaySound()) {
            setInternalPrefsLong(VEHICLE_MODE_LAST_ALERT, JoH.tsl());
            builder.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.labbed_musical_chime));
        }
        builder.setContentTitle(getString(R.string.app_name) + " " + "Vehicle mode");
        cancel_vehicle_notification();
        NotificationManagerCompat.from(this).notify(VEHICLE_NOTIFICATION_ID, builder.build());
    }

    private void cancel_vehicle_notification() {
        NotificationManagerCompat.from(this).cancel(VEHICLE_NOTIFICATION_ID);
    }

    private void checkVehicleRepeatNotification() {
        if (Pref.getBoolean("play_sound_in_vehicle_mode", false)) {
            if (is_in_vehicle_mode()) {
                if (Pref.getBoolean("repeat_sound_in_vehicle_mode", true)) {
                    if (get_minutes_since_last_alert() > 90) {
                        raise_vehicle_notification("Still in Vehicle mode, duration: " + get_vehicle_mode_minutes() + " mins");
                    }
                }
            }
        }
    }

    private static void startupInfo() {
        if (ShotStateStore.hasShot(Home.SHOWCASE_MOTION_DETECTION)) return;
        Home.startHomeWithExtra(xdrip.getAppContext(), Home.ACTIVITY_SHOWCASE_INFO, "");
    }


    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

            //if (d) Log.d(TAG, "Preference change listener fired: "+key);
            switch (key) {
                case "motion_tracking_enabled":
                    if (!prefs.getBoolean("motion_tracking_enabled", false)) {
                        if (d) Log.d(TAG, "Shutting down on preference change");
                        stopActivityRecogniser(xdrip.getAppContext());
                        set_vehicle_mode(false);
                    } else {
                        if (d) Log.d(TAG, "Starting on preference change");
                        resetRequestedReceivedCounters();
                        startActivityRecogniser(xdrip.getAppContext());

                        startupInfo();
                    }
                    break;
                case "act_as_motion_master":
                    if (prefs.getBoolean("act_as_motion_master", false)
                            && prefs.getBoolean("use_remote_motion", false)) {
                        if (d) Log.d(TAG, "Turning off remote motion");
                        prefs.edit().putBoolean("use_remote_motion", false).apply();
                    }
                    reStartActivityRecogniser(xdrip.getAppContext());
                    break;
                case "use_remote_motion":
                    if (prefs.getBoolean("use_remote_motion", false)
                            && prefs.getBoolean("act_as_motion_master", false)) {
                        if (d) Log.d(TAG, "Turning off motion master");
                        prefs.edit().putBoolean("act_as_motion_master", false).apply();

                    }
                    reStartActivityRecogniser(xdrip.getAppContext());
                    break;
            }

        }

    };

    protected static class motionDataWrapper {
        @Expose
        public ArrayList<motionData> entries;

        motionDataWrapper() {
            entries = new ArrayList<>();
        }
    }

    public static class motionData {

        private static final SparseArray<String> classification = new SparseArray<>();

        static {
            classification.put(DetectedActivity.IN_VEHICLE, "in vehicle");
            classification.put(DetectedActivity.ON_BICYCLE, "on bicycle");
            classification.put(DetectedActivity.ON_FOOT, "on foot");
            classification.put(DetectedActivity.RUNNING, "running");
            classification.put(DetectedActivity.STILL, "still");
            classification.put(DetectedActivity.TILTING, "tilting");
            classification.put(DetectedActivity.UNKNOWN, "unknown");
            classification.put(DetectedActivity.WALKING, "walking");
        }

        @Expose
        public long timestamp;
        @Expose
        public int activity;

        public motionData(long timestamp, int activity) {
            this.timestamp = timestamp;
            this.activity = activity;
        }

        public String toPrettyType() {
            final String value = classification.get(this.activity);
            return value != null ? value : "unclassified " + activity;
        }

    }
}
