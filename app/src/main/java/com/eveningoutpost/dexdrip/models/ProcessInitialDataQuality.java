package com.eveningoutpost.dexdrip.models;

import androidx.databinding.BaseObservable;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.STALE_CALIBRATION_CUT_OFF;

/**
 * Created by jamorham on 01/10/2017.
 * <p>
 * Provides InitialDataQuality status object used to determine
 * whether we can process an initial calibration right now
 * <p>
 * Currently does not handle interpolation or interstitial lag
 */

public class ProcessInitialDataQuality {

    public static InitialDataQuality getInitialDataQuality() {
        // get uncalculated data
        JoH.clearCache();
        final List<BgReading> uncalculated = BgReading.latestUnCalculated(3);
        return getInitialDataQuality(uncalculated);
    }

    // Check if data looks suitable for initial calibration
    // List must be supplied with newest data first and without duplicates
    public static InitialDataQuality getInitialDataQuality(final List<BgReading> uncalculated) {
        final InitialDataQuality result = new InitialDataQuality();
        String alert = "";
        final Boolean service_running = DexCollectionType.getServiceRunningState();
        if (service_running != null) result.collector_running = service_running;

        // if there is no data at all within calibration cut off then return negative result
        if (!((uncalculated == null) || (uncalculated.size() < 1))) {
            // we have at least one record
            result.received_raw_data = true;
            result.last_activity = uncalculated.get(0).timestamp;

            // work out next likely time to receive a reading
            final long OUR_PERIOD = DEXCOM_PERIOD; // eventually to come from DexCollectionType
            result.next_activity_expected = result.last_activity + OUR_PERIOD;
            // if time already past based on last timestamp then work out in to the future based on period
            if (JoH.tsl() > result.next_activity_expected) {
                result.missed_last = true;
                final long offset = result.last_activity % OUR_PERIOD;
                result.next_activity_expected = ((JoH.tsl() / OUR_PERIOD) * OUR_PERIOD) + offset;

                // this logic could probably be improved
                while (result.next_activity_expected < JoH.tsl()) {
                    result.next_activity_expected += OUR_PERIOD;
                }

            }
            if (!(JoH.msSince(uncalculated.get(0).timestamp) > STALE_CALIBRATION_CUT_OFF)) {
                // we got some data now see if it is recent enough
                result.recent_data = true;
                boolean adjusted = true; // run one time
                while (adjusted) {
                    adjusted = false;
                    for (int i = 0; i < uncalculated.size(); i++) {
                        if (JoH.msSince(uncalculated.get(i).timestamp) > STALE_CALIBRATION_CUT_OFF) {
                            uncalculated.remove(i);
                            adjusted = true;
                            break;
                        }
                        if (!SensorSanity.isRawValueSane(uncalculated.get(i).raw_data)) {
                            uncalculated.remove(i);
                            adjusted = true;
                            alert = "  "+"Raw Sensor data is outside valid range! Sensor problem!";
                            break;
                        }

                    }
                }
                result.number_of_records_inside_window = uncalculated.size();
                // do we have enough good data?
                if (uncalculated.size() >= 3) {
                    if (JoH.msSince(uncalculated.get(2).timestamp) > STALE_CALIBRATION_CUT_OFF) {
                        result.advice = "Oldest of last 3 readings is more than " + JoH.niceTimeScalar(STALE_CALIBRATION_CUT_OFF) + " ago" + alert;
                    } else {
                        result.advice = "Readings look suitable for calibration" + alert;
                        result.pass = true;
                    }
                } else {
                    result.advice = "Need 3 recent readings, got only " + uncalculated.size() + " so far" + alert;
                }
            } else {
                result.advice = "No data received in last " + JoH.niceTimeScalar(STALE_CALIBRATION_CUT_OFF) + alert;
            }
        } else {
            result.advice = "No data received yet" + alert;
        }
        return result;
    }

    private static String gs(int id) {
        return xdrip.getAppContext().getString(id);
    }


    public static class InitialDataQuality extends BaseObservable {
        public boolean collector_running = false;
        public boolean received_raw_data = false;
        public boolean recent_data = false;
        public boolean pass = false;
        public boolean missed_last = false;
        public int number_of_records_inside_window = 0;
        public long last_activity = 0;
        public long next_activity_expected = 0;
        public String advice = "";

        public String getNextExpectedTill() {
            return JoH.niceTimeTill(next_activity_expected);
        }

    }

}
