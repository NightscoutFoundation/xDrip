package com.eveningoutpost.dexdrip.calibrations;

import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.HOUR_IN_MS;

/**
 *
 * jamorham
 *
 * Forward calibrations to interested algorithms
 *
 */

public class NativeCalibrationPipe {

    private static final String TAG = NativeCalibrationPipe.class.getSimpleName();

    public static void addCalibration(final int glucose, final long timestamp) {

        final long since = msSince(timestamp);
        if (since < 0) {
            final String msg = "Cannot send calibration in future to transmitter: " + glucose + " @ " + JoH.dateTimeText(timestamp);
            JoH.static_toast_long(msg);
            UserError.Log.wtf(TAG, msg);
            return;
        }
        if (since > HOUR_IN_MS) {
            final String msg = "Cannot send calibration older than 1 hour to transmitter: " + glucose + " @ " + JoH.dateTimeText(timestamp);
            JoH.static_toast_long(msg);
            UserError.Log.wtf(TAG, msg);
            return;
        }
        if ((glucose < 40 || glucose > 400)) {
            final String msg = "Calibration glucose value out of range: " + glucose;
            JoH.static_toast_long(msg);
            UserError.Log.wtf(TAG, msg);
            return;
        }

        UserError.Log.uel(TAG, "Queuing Calibration for transmitter: " + BgGraphBuilder.unitized_string_with_units_static(glucose) + " " + JoH.dateTimeText(timestamp));

        // Send to potential listeners
        Ob1G5StateMachine.addCalibration(glucose, timestamp);
        Medtrum.addCalibration(glucose, timestamp);

        PersistentStore.setLong("last-calibration-pipe-timestamp", JoH.tsl());

    }

    public static void removePendingCalibration(final int glucose) {
        if (Ob1G5StateMachine.deleteFirstQueueCalibration(glucose)) {
            JoH.static_toast_long("Deleted pending calibration for: " + Unitized.unitized_string_static(glucose));
        }
    }
}
