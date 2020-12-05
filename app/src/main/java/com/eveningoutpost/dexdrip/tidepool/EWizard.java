package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.models.Profile;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.google.gson.annotations.Expose;

// jamorham

public class EWizard extends BaseElement {
    private static final String TAG = "EWizard";
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

    private static boolean IsEWizardValid(Treatments treatment) {
        if(treatment.carbs < 0 || treatment.carbs > 1000) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (carbs) " + treatment.toS());
            return false;
        }
        
        double insulinCarbRatio = Profile.getCarbRatio(treatment.timestamp);
        if(insulinCarbRatio <= 0 || insulinCarbRatio > 250) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (insulinCarbRatio) " + treatment.toS());
            return false;
        }
        if(treatment.insulin < 0 || treatment.insulin >= 100) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (insulin) " + treatment.toS());
            return false;
        }
        return true;
    }

    // todo grt: change EBolus to ArrayList<InsulinInjection> when tidepool may support that
    public static EWizard fromTreatment(final Treatments treatment) {
        if(!IsEWizardValid(treatment)) {
            return null;
        }
        
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
