package com.eveningoutpost.dexdrip.tidepool;

// jamorham

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.google.gson.annotations.Expose;

import java.util.LinkedList;
import java.util.List;

public class ESensorGlucose extends BaseElement {


    @Expose
    String units;
    @Expose
    int value;

    ESensorGlucose() {
        this.type = "cbg";
        this.units = "mg/dL";
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
            results.add(fromBgReading(bgReading));
        }
        return results;
    }

}
