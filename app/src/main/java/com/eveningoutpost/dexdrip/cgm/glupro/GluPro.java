package com.eveningoutpost.dexdrip.cgm.glupro;

import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;

import android.content.Intent;
import android.util.Pair;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Glucose Profile helper class
 */
public class GluPro {

    private static final String TAG = GluPro.class.getSimpleName();
    private static final String GLU_PRO_START_UTC = "GLU-PRO-START-UTC";
    private static final String PENDING_CALIBRATION_TIMESTAMP = "glupro-pending-calibration-timestamp";
    private static final String PENDING_CALIBRATION_GLUCOSE = "glupro-pending-calibration-glucose";

    public static long getStart() {
        return PersistentStore.getLong(GLU_PRO_START_UTC);
    }

    public static void setStart(long start) {
        PersistentStore.setLong(GLU_PRO_START_UTC, start);
    }

    public static boolean acceptCommands() {
        return DexCollectionType.getDexCollectionType() == DexCollectionType.GluPro;
    }

    static void startControlActivity() {
        UserError.Log.d(TAG, "Starting control activity");
        val context = xdrip.getAppContext();
        Intent intent = new Intent(context, GluProActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static synchronized void addCalibration(final int glucose, long timestamp) {
        if (acceptCommands()) {
            if (glucose == 0 || JoH.ratelimit("glupro-calibration-cooldown", 5)) {
                PersistentStore.setLong(PENDING_CALIBRATION_TIMESTAMP, timestamp);
                PersistentStore.setLong(PENDING_CALIBRATION_GLUCOSE, glucose);
                if (glucose != 0) {
                    UserError.Log.d(TAG, "Received calibration request: " + glucose + " @ " + dateTimeText(timestamp));
                    Inevitable.task("glupro-ping", 1000, GluPro::calibratePing);
                }
            }
        }
    }

    // clear any pending calibration
    public static synchronized void clearCalibration() {
        addCalibration(0, 0);
    }

    // return any pending calibration
    public static synchronized Pair<Long, Integer> getCalibration() {
        final long timestamp = PersistentStore.getLong(PENDING_CALIBRATION_TIMESTAMP);
        final long glucose = PersistentStore.getLong(PENDING_CALIBRATION_GLUCOSE);
        if (glucose == 0 || timestamp == 0 || msSince(timestamp) > Constants.HOUR_IN_MS * 8) {
            return null;
        } else {
            return new Pair<>(timestamp, (int) glucose);
        }

    }

    // start the service when we get a calibration added
    public static void calibratePing() {
        if (acceptCommands()) {
            JoH.startService(GluProService.class);
        }
    }
}
