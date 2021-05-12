package com.eveningoutpost.dexdrip.tidepool;

// jamorham

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.gson.annotations.Expose;


import java.util.LinkedList;
import java.util.List;

public class ESensorGlucose extends BaseElement {

    private static final String TAG = "TidepoolESensorGlucose";

    @Expose
    String units;
    @Expose
    int value;

    ESensorGlucose() {
        this.type = "cbg";
        this.units = "mg/dL";
    }

    static boolean isBgReadingValid(final BgReading bgReading) {
        if(bgReading.calculated_value >= 39 && bgReading.calculated_value <= 500) {
            return true;
        }
        UserError.Log.e(TAG, "Ignoring invalid bg " + bgReading.toS());
        return false;
    }

    static ESensorGlucose fromBgReading(final BgReading bgReading) {
        final ESensorGlucose sensorGlucose = new ESensorGlucose();
        sensorGlucose.populate(bgReading.timestamp, bgReading.uuid);
        sensorGlucose.value = (int) bgReading.calculated_value; // TODO best glucose?
        return sensorGlucose;
    }

    static List<ESensorGlucose> fromBgReadings(final List<BgReading> bgReadingList) {
        if (bgReadingList == null) return null;
        final List<ESensorGlucose> results = new LinkedList<>();
        for (BgReading bgReading : bgReadingList) {
            if(isBgReadingValid(bgReading)) {
                results.add(fromBgReading(bgReading));
            }
        }
        return results;
    }

}
