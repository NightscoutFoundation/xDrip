package com.eveningoutpost.dexdrip.alert;

import static com.eveningoutpost.dexdrip.models.JoH.cancelNotification;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalarNatural;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SENSORY_EXPIRY_NOTIFICATION_ID;

import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import lombok.val;

/**
 * JamOrHam
 *
 * Sensor Expiry alert. Triggers when passing threshold times when user should be able to notice
 */

public class SensorExpiry extends BaseAlert {

    private static final String TAG = SensorExpiry.class.getSimpleName();
    private static final long NOT_ALERTED = Long.MAX_VALUE;
    private static final long[] THRESHOLDS = {
            // need to be in ascending order so first hit is first applicable to avoid multiple triggers
            Constants.HOUR_IN_MS * 2,
            Constants.HOUR_IN_MS * 6,
            Constants.HOUR_IN_MS * 12,
            Constants.HOUR_IN_MS * 24,
    };

    private static final Persist.Long remaining = new Persist.Long("PREF_SENSOR_EXPIRE_ALERT");
    private static final Persist.Long alerted = new Persist.Long("PREF_SENSOR_EXPIRE_ALERTED");

    public SensorExpiry() {
        super("Sensor Expiry", When.ChargeChange, When.ScreenOn);
    }

    @Override
    public boolean activate() {
        val expiry = niceTimeScalarNatural(SensorDays.get().getRemainingSensorPeriodInMs());
        val notificationId = SENSORY_EXPIRY_NOTIFICATION_ID;
        cancelNotification(notificationId);
        val expireMsg = String.format("Sensor will expire in %s", expiry); // TODO i18n and format string
        showNotification("Sensor expiring", expireMsg, null, notificationId, null, true, true, null, null, null, true);
        Treatments.create_note("Warning: " + expireMsg, tsl()); // TODO i18n but note classifier also needs updating for that
        return true;
    }

    @Override
    public boolean isMet() {
        val sd = SensorDays.get();
        if (sd.isValid()) {
            val now = sd.getRemainingSensorPeriodInMs();
            val last = remaining.get();

            try {
                if (now > (last + Constants.HOUR_IN_MS)) {
                    Log.d(TAG, "Period rewound to: " + niceTimeScalar(now) + " was " + niceTimeScalar(last));
                    alerted.set(NOT_ALERTED);
                } else if (last > now) {
                    Log.d(TAG, "Period reduced to: " + niceTimeScalar(now) + " was " + niceTimeScalar(last));
                    val lastAlerted = alerted.get();
                    for (val threshold : THRESHOLDS) {
                        if (now <= threshold && threshold < lastAlerted) {
                            alerted.set(threshold);
                            return true;
                        }
                    }
                } else {
                    Log.d(TAG, "Now and last identical: " + niceTimeScalar(now));
                }

            } finally {
                remaining.set(now);
            }

        } else {
            Log.d(TAG, "Cannot evaluate as sensor days invalid");
        }
        return false;
    }

}
