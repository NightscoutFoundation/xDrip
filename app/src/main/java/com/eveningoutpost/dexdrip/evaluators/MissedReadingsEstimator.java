package com.eveningoutpost.dexdrip.evaluators;

// jamorham

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.DEXCOM_PERIOD;

public class MissedReadingsEstimator {

    public static int estimate() {

        final BgReading bgReading = BgReading.last();
        final long since = bgReading != null ? JoH.msSince(bgReading.timestamp) : Constants.DAY_IN_MS;
        return (int) (since / DEXCOM_PERIOD);
    }

}
