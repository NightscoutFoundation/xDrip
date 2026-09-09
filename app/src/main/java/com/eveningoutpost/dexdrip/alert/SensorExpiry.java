package com.eveningoutpost.dexdrip.alert;

import static com.eveningoutpost.dexdrip.models.JoH.cancelNotification;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalarNatural;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SENSORY_EXPIRY_NOTIFICATION_ID;
import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.GENERAL_CHANNEL;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.Localized;
import com.eveningoutpost.dexdrip.xdrip;

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

    static final String NOTE_PREFIX = "Warning: ";
    // a note which took longer than this to reach us is too late to be worth alerting on
    private static final long MAX_NOTE_AGE = Constants.MINUTE_IN_MS * 30;

    private static final Persist.Long remaining = new Persist.Long("PREF_SENSOR_EXPIRE_ALERT");
    private static final Persist.Long alerted = new Persist.Long("PREF_SENSOR_EXPIRE_ALERTED");
    private static final Persist.Long followerNotified = new Persist.Long("PREF_SENSOR_EXPIRE_FOLLOWER_NOTIFIED");

    public SensorExpiry() {
        super("Sensor Expiry", When.ChargeChange, When.ScreenOn);
    }

    @Override
    public boolean activate() {
        val expiry = niceTimeScalarNatural(SensorDays.get().getRemainingSensorPeriodInMs(), 1);
        val expireMsg = xdrip.gs(R.string.sensor_will_expire_in, expiry);
        showExpiryNotification(expireMsg);
        Treatments.create_note(NOTE_PREFIX + expireMsg, tsl()); // TODO i18n but note classifier also needs updating for that
        UserError.Log.uel(TAG, "Sensor will expire soon");
        return true;
    }

    private static void showExpiryNotification(final String message) {
        val notificationId = SENSORY_EXPIRY_NOTIFICATION_ID;
        cancelNotification(notificationId);
        showNotification(xdrip.gs(R.string.sensor_expiring), message, null, notificationId, GENERAL_CHANNEL, true, true, null, null, null, true);
    }

    /**
     * Show the same notification the master showed when the note arrives
     */
    public static void evaluateReceivedNote(final String note, final long timestamp) {
        if (note == null) return;
        if (!Home.get_follower()) return;
        if (!Pref.getBooleanDefaultFalse("alert_raise_for_sensor_expiry")) return;
        if (!isExpiryNote(note)) return;
        if (msSince(timestamp) > MAX_NOTE_AGE) {
            Log.d(TAG, "Ignoring expiry note as too old: " + JoH.dateTimeText(timestamp));
            return;
        }
        if (!firstSightOf(timestamp)) {
            Log.d(TAG, "Already notified for expiry note: " + JoH.dateTimeText(timestamp));
            return;
        }
        showExpiryNotification(messageFromNote(note));
        UserError.Log.uel(TAG, "Sensor will expire soon (from master)");
    }

    /**
     * Only alert once for each note.
     */
    static boolean firstSightOf(final long timestamp) {
        if (timestamp <= followerNotified.get()) return false;
        followerNotified.set(timestamp);
        return true;
    }

    /**
     * Check if this is a note about sensor expiry
     */
    static boolean isExpiryNote(final String note) {
        return Localized.appearsIn(R.string.sensor_will_expire_in, note);
    }

    // notes can be joined onto each other, so show just the expiry message
    static String messageFromNote(final String note) {
        val index = note.indexOf(NOTE_PREFIX);
        return index >= 0 ? note.substring(index + NOTE_PREFIX.length()) : note;
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
