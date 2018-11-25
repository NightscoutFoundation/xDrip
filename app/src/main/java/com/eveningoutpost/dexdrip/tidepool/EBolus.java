package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.Models.Treatments;
import com.google.gson.annotations.Expose;

// jamorham

public class EBolus extends BaseElement {

    @Expose
    public final String subType = "normal";
    @Expose
    public final double normal;
    @Expose
    public final double expectedNormal;

    {
        type = "bolus";
    }

    EBolus(double insulinDelivered, double insulinExpected, long timestamp, String uuid) {
        this.normal = insulinDelivered;
        this.expectedNormal = insulinExpected;
        populate(timestamp, uuid);
    }

    public static EBolus fromTreatment(Treatments treatment) {
        return new EBolus(treatment.insulin, treatment.insulin, treatment.timestamp, treatment.uuid);
    }

}
