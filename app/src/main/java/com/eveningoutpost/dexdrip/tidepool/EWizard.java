package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.google.gson.annotations.Expose;

// jamorham

public class EWizard extends BaseElement {

    @Expose
    public String units = "mg/dL";
    @Expose
    public double carbInput;
    @Expose
    public double insulinCarbRatio;
    @Expose
    public EBolus bolus;

    EWizard() {
        type = "wizard";
    }

    public static EWizard fromTreatment(final Treatments treatment) {
        final EWizard result = (EWizard)new EWizard().populate(treatment.timestamp, treatment.uuid);
        result.carbInput = treatment.carbs;
        result.insulinCarbRatio = Profile.getCarbRatio(treatment.timestamp);
        if (treatment.insulin > 0) {
            result.bolus = new EBolus(treatment.insulin, treatment.insulin, treatment.timestamp, treatment.uuid);
        } else {
            result.bolus = new EBolus(0.0001,0.0001, treatment.timestamp, treatment.uuid); // fake insulin record
        }
        return result;
        }

}
