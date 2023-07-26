package com.eveningoutpost.dexdrip.evaluators;

/**
 * jamorham
 *
 * Determine if readings appear to have been high for a period of time and trigger an alert.
 * Filter out invalid datasets, for example backfilled data with lower values, but received
 * out of sequence with two end points that are both above threshold.
 */

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Date;
import java.util.List;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;

public class PersistentHigh {

    private static final String TAG = PersistentHigh.class.getSimpleName();
    private static final String PERSISTENT_HIGH_SINCE = "persistent_high_since";

    public static boolean checkForPersistentHigh() {

        // skip if not enabled
        if (!Pref.getBooleanDefaultFalse("persistent_high_alert_enabled")) return false;


        final List<BgReading> last = BgReading.latest(1);
        if ((last != null) && (last.size() > 0)) {

            final double highMarkMgDl = Home.convertToMgDlIfMmol(
                    JoH.tolerantParseDouble(Pref.getString("highValue", "170"), 170d));

            final long now = JoH.tsl();
            final long since = now - last.get(0).timestamp;
            // only process if last reading <10 mins
            if (since < MINUTE_IN_MS * 10) {
                // check if exceeding high
                if (last.get(0).getDg_mgdl() > highMarkMgDl) {

                    final double this_slope = last.get(0).getDg_slope() * MINUTE_IN_MS;
                    Log.d(TAG, "CheckForPersistentHigh: Slope: " + JoH.qs(this_slope)+ " "+JoH.dateTimeText(last.get(0).timestamp));

                    // if not falling
                    if (this_slope > 0 && !last.get(0).hide_slope) {
                        final long high_since = Pref.getLong(PERSISTENT_HIGH_SINCE, 0);
                        if (high_since == 0) {
                            // no previous persistent high so set start as now
                            Pref.setLong(PERSISTENT_HIGH_SINCE, now);
                            Log.d(TAG, "Registering start of persistent high at time now");
                        } else {
                            final long high_for_mins = (now - high_since) / MINUTE_IN_MS;
                            long threshold_mins;
                            try {
                                threshold_mins = Long.parseLong(Pref.getString("persistent_high_threshold_mins", "60"));
                            } catch (NumberFormatException e) {
                                threshold_mins = 60;
                                Home.toaststaticnext("Invalid persistent high for longer than minutes setting: using 60 mins instead");
                            }
                            if (high_for_mins > threshold_mins) {
                                // we have been high for longer than the threshold - raise alert

                                // except if alerts are disabled
                                if (Pref.getLong("alerts_disabled_until", 0) > new Date().getTime()) {
                                    Log.i(TAG, "checkforPersistentHigh: Notifications are currently disabled cannot alert!!");
                                    return false;
                                }

                                if (!dataQualityCheck(high_since, highMarkMgDl)) {
                                    Log.d(TAG, "Insufficient data quality to raise persistent high alert");
                                    return false;
                                }

                                Log.i(TAG, "Persistent high for: " + high_for_mins + " mins -> alerting");
                                Notifications.persistentHighAlert(xdrip.getAppContext(), true, xdrip.getAppContext().getString(R.string.persistent_high_for_greater_than) + (int) high_for_mins + xdrip.getAppContext().getString(R.string.space_mins));
                                return true;
                            } else {
                                Log.d(TAG, "Persistent high below time threshold at: " + high_for_mins);
                            }
                        }
                    }
                } else {
                    // not high - cancel any existing
                    if (Pref.getLong(PERSISTENT_HIGH_SINCE, 0) != 0) {
                        Log.i(TAG, "Cancelling previous persistent high as we are no longer high");
                        Pref.setLong(PERSISTENT_HIGH_SINCE, 0); // clear it
                        Notifications.persistentHighAlert(xdrip.getAppContext(), false, ""); // cancel it
                    }
                }
            }
        }
        return false; // actually we should probably return void as we do everything inside this method
    }

    public static boolean dataQualityCheck(final long since, final double highMarkMgDl) {

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.e(TAG, "Cannot raise persistent high alert as no active sensor!");
            return false;
        }
        if (since < sensor.started_at) {
            Log.e(TAG, "Cannot raise persistent high alert as high time pre-dates sensor start");
            return false;
        }
        final long duration = msSince(since);
        if (duration > Constants.DAY_IN_MS || duration < 0) {
            Log.e(TAG, "Cannot raise persistent high alert as duration doesn't make sense: " + JoH.niceTimeScalar(duration));
            return false;
        }

        final List<BgReading> readings = BgReading.latestForSensorAsc(2000, since, JoH.tsl(), Home.get_follower());
        if (readings == null) {
            Log.e(TAG, "Cannot raise persistent high alert as there are no readings for this sensor!");
            return false;
        }

        final int numberOfReadings = readings.size();

        if (numberOfReadings == 0) {
            Log.e(TAG, "Cannot raise persistent high alert as there are 0 readings for this sensor!");
            return false;
        }

        final long frequency = duration / numberOfReadings;
        //Log.d(TAG, "Frequency Calculated as: " + frequency);
        if (frequency > MINUTE_IN_MS * 15) {
            Log.e(TAG, "Cannot raise persistent high alert as readings frequency is: " + niceTimeScalar(frequency));
            return false;
        }

        for (final BgReading bgr : readings) {
            if (bgr.getDg_mgdl() < highMarkMgDl) {
                Log.e(TAG, "High not persistent as reading at: " + JoH.dateTimeText(bgr.timestamp) + " does not exceed " + JoH.qs(highMarkMgDl) + " mgdl / high mark");
                return false;
            }
        }

        return true;

    }
}
