package com.eveningoutpost.dexdrip.Models;

import android.databinding.BaseObservable;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.List;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.STALE_CALIBRATION_CUT_OFF;

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
        final List<BgReading> uncalculated = BgReading.latestUnCalculated(3);
        return getInitialDataQuality(uncalculated);
    }

    // Check if data looks suitable for initial calibration
    // List must be supplied with newest data first and without duplicates
    public static InitialDataQuality getInitialDataQuality(final List<BgReading> uncalculated) {
        final InitialDataQuality result = new InitialDataQuality();

        final Boolean service_running = DexCollectionType.getServiceRunningState();
        if (service_running != null) result.collector_running = service_running;

        // if there is no data at all within calibration cut off then return negative result
        if (!((uncalculated == null) || (uncalculated.size() < 1))) {
            result.received_raw_data = true;
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
                    }
                }
                result.number_of_records_inside_window = uncalculated.size();
                // do we have enough good data?
                if (uncalculated.size() >= 3) {
                    if (JoH.msSince(uncalculated.get(2).timestamp) > STALE_CALIBRATION_CUT_OFF) {
                        result.advice = "Oldest of last 3 readings is more than " + JoH.niceTimeScalar(STALE_CALIBRATION_CUT_OFF) + " ago";
                    } else {
                        result.advice = "Readings look suitable for calibration";
                        result.pass = true;
                    }
                } else {
                    result.advice = "Need 3 recent readings, got only " + uncalculated.size() + " so far";
                }
            } else {
                result.advice = "No data received in last " + JoH.niceTimeScalar(STALE_CALIBRATION_CUT_OFF);
            }
        } else {
            result.advice = "No data received yet";
        }
        return result;
    }


    public static class InitialDataQuality extends BaseObservable {
        public boolean collector_running = false;
        public boolean received_raw_data = false;
        public boolean recent_data = false;
        public boolean pass = false;
        public int number_of_records_inside_window = 0;
        public String advice = "";

    }

}
