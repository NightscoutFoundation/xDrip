package com.eveningoutpost.dexdrip.healthconnect;

import static com.eveningoutpost.dexdrip.healthconnect.Coroutines.suspendFunction;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HEALTH_CONNECT_RESPONSE_ID;
import static kotlin.jvm.internal.Reflection.createKotlinClass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;

import androidx.annotation.RequiresApi;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.changes.Change;
import androidx.health.connect.client.changes.UpsertionChange;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.BloodGlucoseRecord;
import androidx.health.connect.client.records.DistanceRecord;
import androidx.health.connect.client.records.ElevationGainedRecord;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.FloorsClimbedRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord;
import androidx.health.connect.client.records.HeightRecord;
import androidx.health.connect.client.records.HydrationRecord;
import androidx.health.connect.client.records.MealType;
import androidx.health.connect.client.records.NutritionRecord;
import androidx.health.connect.client.records.PowerRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.RestingHeartRateRecord;
import androidx.health.connect.client.records.SleepSessionRecord;
import androidx.health.connect.client.records.SpeedRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.health.connect.client.records.WheelchairPushesRecord;
import androidx.health.connect.client.records.metadata.Metadata;
import androidx.health.connect.client.request.ChangesTokenRequest;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;
import androidx.health.connect.client.units.BloodGlucose;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


import kotlin.reflect.KClass;
import lombok.RequiredArgsConstructor;
import lombok.val;

// jamorham

@RequiredArgsConstructor
@RequiresApi(api = Build.VERSION_CODES.O)
@SuppressWarnings("unchecked")
public class HealthGamut {

    private static final String TAG = HealthGamut.class.getSimpleName();

    private static final String[] fullPermissionList = {
            HealthPermission.getReadPermission(createKotlinClass(ExerciseSessionRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(SleepSessionRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(StepsRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(SpeedRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(DistanceRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(TotalCaloriesBurnedRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(HeartRateRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(HeartRateVariabilityRmssdRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(RestingHeartRateRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(ElevationGainedRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(FloorsClimbedRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(HeightRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(WeightRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(WheelchairPushesRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(PowerRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(BloodGlucoseRecord.class)),
            HealthPermission.getWritePermission(createKotlinClass(BloodGlucoseRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(HydrationRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(NutritionRecord.class)),
            HealthPermission.getWritePermission(createKotlinClass(NutritionRecord.class))
    };

    private static final String[] minimalPermissionList = {
            HealthPermission.getReadPermission(createKotlinClass(StepsRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(HeartRateRecord.class)),
            HealthPermission.getReadPermission(createKotlinClass(BloodGlucoseRecord.class)),
            HealthPermission.getWritePermission(createKotlinClass(BloodGlucoseRecord.class))
    };
    private static final List<KClass<? extends Record>> recordList = new LinkedList<>();

    static {
        recordList.add(createKotlinClass(StepsRecord.class));
        recordList.add(createKotlinClass(HeartRateRecord.class));
    }

    private static final Set<String> permissions = new HashSet<>(Arrays.asList(fullPermissionList));
    private static final Set<String> minimalPermissions = new HashSet<>(Arrays.asList(minimalPermissionList));
    private static final Set<? extends KClass<? extends Record>> records = new HashSet<>(recordList);

    private static volatile String token = null;

    private final Context context;
    private HealthConnectClient client;

    private final Coroutines coroutines = Coroutines.INSTANCE;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "Needs above android 8");
            return false;
        }

        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            client = HealthConnectClient.getOrCreate(context);
            try {
                suspendFunction(Coroutines::getGrantedPermissions).apply(client, (result, throwable) -> {
                    try {
                        if (throwable != null) {
                            throw new RuntimeException(throwable);
                        }

                        if (!result.containsAll(minimalPermissions)) {
                            Log.d(TAG, "Need permissions!");

                            if (Build.VERSION.SDK_INT >= 34) {
                                askPermsNew();
                            } else {
                                askPermsOld();
                            }

                        } else {
                            Log.d(TAG, "Got permissions!");
                            getAllDataIfEnabled();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get permissions: " + e);
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, "got exception in permissions get granted: " + e);
            }
            return true;
        } else {
            Log.e(TAG, "Companion app not available - asking for installation");

            val url = Uri.parse("market://details")
                    .buildUpon()
                    .appendQueryParameter("id", "com.google.android.apps.healthdata")
                    .appendQueryParameter("url", "healthconnect://onboarding")
                    .build();
            val intent = new Intent(Intent.ACTION_VIEW, url);

            JoH.runOnUiThread(() -> {
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, HEALTH_CONNECT_RESPONSE_ID);
                } else {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }

            });
        }
        return false;
    }

    private void askPermsNew() {
        val permsIntent = PermissionController.createRequestPermissionResultContract().createIntent(context, permissions);
        JoH.runOnUiThread(() -> {
            try {
                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) context,
                            Objects.requireNonNull(permsIntent.getStringArrayExtra("androidx.activity.result.contract.extra.PERMISSIONS")), HEALTH_CONNECT_RESPONSE_ID);
                } else {
                    JoH.static_toast_long(xdrip.gs(R.string.google_health_connect_needs_perms));
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot start permissions request: " + e);
            }
        });
    }

    private void askPermsOld() {
        val permsIntent = PermissionController.createRequestPermissionResultContract().createIntent(context, permissions);
        JoH.runOnUiThread(() -> {
            try {
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(permsIntent, HEALTH_CONNECT_RESPONSE_ID);
                } else {
                    JoH.static_toast_long(xdrip.gs(R.string.google_health_connect_needs_perms));
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot start permissions request: " + e);
            }
        });
    }

    public void openPermissionManager() {
        JoH.runOnUiThread(() -> {
            try {
                if (context instanceof Activity) {
                    if (Build.VERSION.SDK_INT >= 34) {
                        val intent =
                                new Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                                        .putExtra(Intent.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
                        ((Activity) context).startActivity(intent);
                    } else {
                        val intent = new Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS");
                        ((Activity) context).startActivity(intent);
                    }

                } else {
                    JoH.static_toast_long(xdrip.gs(R.string.google_health_connect_needs_perms));
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot start permissions request: " + e);
            }
        });
    }

    public static HealthGamut init(Activity activity) {
        val instance = new HealthGamut(activity);
        instance.init();
        return instance;
    }

    public static void ping() {
        new HealthGamut(xdrip.getAppContext()).init();
    }

    public static void sendGlucoseStatic(final BgReading bg) {
        new HealthGamut(xdrip.getAppContext()).sendGlucose(bg);
    }

    public void getAllDataIfEnabled() {
        if (HealthConnectEntry.receiveEnabled()) {
            getAllData();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public synchronized void getAllData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        val startTime = Instant.now().minus(1, ChronoUnit.DAYS);
        val endTime = Instant.now();
        val reply = new DataReply();

        try {
            if (token != null) {
                if (JoH.ratelimit("health-connect-use-token", 1)) {
                    client.getChanges(token, coroutines.getContinuation((result, throwable) -> {
                        token = null;
                        if (throwable != null) {
                            Log.e(TAG, "Got exception during changes: " + throwable);
                            return;
                        }
                        try {
                            token = result.getNextChangesToken();
                            reply.stepsRecords = new LinkedList<>();
                            reply.heartRateRecords = new LinkedList<>();
                            int ups = 0;
                            for (Change change : result.getChanges()) {
                                if (change instanceof UpsertionChange) {
                                    ups++;
                                    UpsertionChange cu = (UpsertionChange) change;
                                    val record = cu.getRecord();
                                    if (record instanceof StepsRecord) {
                                        reply.stepsRecords.add((StepsRecord) record);
                                    }
                                    if (record instanceof HeartRateRecord) {
                                        reply.heartRateRecords.add((HeartRateRecord) record);
                                    }
                                } else {
                                    Log.d(TAG, "Unhandled change record of type: " + change.getClass().getSimpleName());
                                }
                            }
                            Log.d(TAG, "Processed changes of count: " + ups);
                            if (ups > 0) {
                                ReadReplyProcessor.process(reply);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception processing changes: " + e);
                        }
                    }));
                }
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception trying to get changes: " + e);
        }

        client.readRecords(new ReadRecordsRequest<StepsRecord>(createKotlinClass(StepsRecord.class),
                TimeRangeFilter.between(startTime, endTime),
                Collections.emptySet(),
                false,
                1000,
                null), coroutines.getContinuation((result, throwable) -> {

            try {
                if (throwable != null) {
                    throw new RuntimeException(throwable);
                }
                reply.stepsRecords = result.getRecords();

                client.readRecords(new ReadRecordsRequest<HeartRateRecord>(createKotlinClass(HeartRateRecord.class),
                        TimeRangeFilter.between(startTime, endTime),
                        Collections.emptySet(),
                        false,
                        1000,
                        null), coroutines.getContinuation((result2, throwable2) -> {

                    try {
                        if (throwable2 != null) {
                            throw new RuntimeException(throwable2);
                        }
                        reply.heartRateRecords = result2.getRecords();

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to read hr records: " + e);
                    }
                }));

                ReadReplyProcessor.process(reply);

                client.getChangesToken(new ChangesTokenRequest(records, new HashSet<>()),
                        coroutines.getContinuation((tokenResult, throwable3) -> {
                            if (throwable3 != null) {
                                throw new RuntimeException(throwable3);
                            }

                            Log.d(TAG, "Changes token: " + tokenResult);
                            token = tokenResult;
                        }));


            } catch (Exception e) {
                Log.e(TAG, "Failed to read records: " + e);
            }
        }));
    }

    public void sendGlucose(final BgReading bg) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        if (init()) {
            val list = new LinkedList<BloodGlucoseRecord>();
            val record = new BloodGlucoseRecord(Instant.ofEpochMilli(bg.timestamp),
                    null, BloodGlucose.milligramsPerDeciliter(bg.calculated_value),
                    BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                    MealType.MEAL_TYPE_UNKNOWN, BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN, new Metadata());
            list.add(record);
            client.insertRecords(list, coroutines.getContinuation((result, throwable) -> {
                try {
                    Log.d(TAG, "Insert result: " + result.getRecordIdsList().size());
                } catch (Exception e) {
                    Log.e(TAG, "Got exception on insert: " + e);
                }
            }));
        } else {
            Log.e(TAG, "Could not send Glucose");
        }
    }
}
