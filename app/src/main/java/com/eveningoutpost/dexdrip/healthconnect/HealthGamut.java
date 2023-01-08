package com.eveningoutpost.dexdrip.healthconnect;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.HEALTH_CONNECT_RESPONSE_ID;
import static kotlin.jvm.internal.Reflection.createKotlinClass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.changes.Change;
import androidx.health.connect.client.changes.UpsertionChange;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.BloodGlucoseRecord;
import androidx.health.connect.client.records.DistanceRecord;
import androidx.health.connect.client.records.ExerciseEventRecord;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.MealType;
import androidx.health.connect.client.records.NutritionRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.SleepSessionRecord;
import androidx.health.connect.client.records.SpeedRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.metadata.Metadata;
import androidx.health.connect.client.request.ChangesTokenRequest;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;
import androidx.health.connect.client.units.BloodGlucose;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jamorham.javakotlininterop.Coroutines;
import kotlin.reflect.KClass;
import lombok.RequiredArgsConstructor;
import lombok.val;

// jamorham

@RequiredArgsConstructor
@RequiresApi(api = Build.VERSION_CODES.O)
@SuppressWarnings("unchecked")
public class HealthGamut {

    private static final String TAG = HealthGamut.class.getSimpleName();

    private static final HealthPermission[] permissionList = {
            HealthPermission.createReadPermission(createKotlinClass(ExerciseSessionRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(ExerciseEventRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(SleepSessionRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(StepsRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(SpeedRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(DistanceRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(TotalCaloriesBurnedRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(HeartRateRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(BloodGlucoseRecord.class)),
            HealthPermission.createWritePermission(createKotlinClass(BloodGlucoseRecord.class)),
            HealthPermission.createReadPermission(createKotlinClass(NutritionRecord.class)),
            HealthPermission.createWritePermission(createKotlinClass(NutritionRecord.class))
    };

    private static final List<KClass<? extends Record>> recordList = new LinkedList<>();

    static {
        recordList.add(createKotlinClass(StepsRecord.class));
        recordList.add(createKotlinClass(HeartRateRecord.class));
    }

    private static final Set<HealthPermission> permissions = new HashSet<>(Arrays.asList(permissionList));
    private static final Set<? extends KClass<? extends Record>> records = new HashSet<>(recordList);

    private static volatile String token = null;

    private final Context context;
    private HealthConnectClient client;

    private final Coroutines coroutines = new Coroutines();

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "Needs above android 8");
            return false;
        }
        if (HealthConnectClient.isAvailable(context)) {
            client = HealthConnectClient.getOrCreate(context);

            client.getPermissionController().getGrantedPermissions(permissions, coroutines.getContinuation((result, throwable) -> {
                try {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }

                    if (!permissions.equals(result)) {
                        Log.d(TAG, "Need permissions!");
                        val permsIntent = PermissionController.createRequestPermissionResultContract().createIntent(context, permissions);
                        JoH.runOnUiThread(() -> {
                            try {
                                if (context instanceof Activity) {
                                    ((Activity) context).startActivityForResult(permsIntent, HEALTH_CONNECT_RESPONSE_ID);
                                } else {
                                    JoH.static_toast_long("ERROR: Health connect needs permissions! - try from settings menu again");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Cannot start permissions request: " + e);
                            }
                        });

                    } else {
                        Log.d(TAG, "Got permissions!");
                        if (HealthConnectEntry.receiveEnabled()) {
                            getAllData();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get permissions: " + e);
                }
            }));

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

    public static void init(Activity activity) {
        new HealthGamut(activity).init();
    }

    public static void ping() {
        new HealthGamut(xdrip.getAppContext()).init();
    }

    public static void sendGlucoseStatic(final BgReading bg) {
        new HealthGamut(xdrip.getAppContext()).sendGlucose(bg);
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
                    BloodGlucoseRecord.SpecimenSource.INTERSTITIAL_FLUID,
                    MealType.UNKNOWN, null, new Metadata());
            list.add(record);
            client.insertRecords(list, coroutines.getContinuation((result, throwable) -> {
                Log.d(TAG, "Insert result: " + result.getRecordIdsList().size());
            }));
        } else {
            Log.e(TAG, "Could not send Glucose");
        }
    }
}
