package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.FoodType;
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
    public double fatsInput;
    @Expose
    public double insulinFatsRatio;
    @Expose
    public double proteinsInput;
    @Expose
    public double insulinProteinsRatio;
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
        
        double insulinCarbRatio = Profile.getFoodRatio(treatment.timestamp, FoodType.CARBS);
        if(insulinCarbRatio <= 0 || insulinCarbRatio > 250) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (insulinCarbRatio) " + treatment.toS());
            return false;
        }
        if(treatment.fats < 0 || treatment.fats > 1000) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (fats) " + treatment.toS());
            return false;
        }

        double insulinFatsRatio = Profile.getFoodRatio(treatment.timestamp, FoodType.FATS);
        if(insulinFatsRatio <= 0 || insulinFatsRatio > 250) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (insulinFatsRatio) " + treatment.toS());
            return false;
        }

        if(treatment.proteins < 0 || treatment.proteins > 1000) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (proteins) " + treatment.toS());
            return false;
        }

        double insulinProteinsRatio = Profile.getFoodRatio(treatment.timestamp, FoodType.PROTEINS);
        if(insulinProteinsRatio <= 0 || insulinProteinsRatio > 250) {
            UserError.Log.e(TAG, "Ignoring invalid treatment (insulinProteinsRatio) " + treatment.toS());
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
        result.insulinCarbRatio = Profile.getFoodRatio(treatment.timestamp, FoodType.CARBS);
        result.fatsInput = treatment.fats;
        result.insulinFatsRatio = Profile.getFoodRatio(treatment.timestamp, FoodType.FATS);
        result.proteinsInput = treatment.proteins;
        result.insulinProteinsRatio = Profile.getFoodRatio(treatment.timestamp, FoodType.PROTEINS);
        if (treatment.insulin > 0) {
            result.bolus = new EBolus(treatment.insulin, treatment.insulin, treatment.timestamp, treatment.uuid);
        } else {
            result.bolus = new EBolus(0.0001,0.0001, treatment.timestamp, treatment.uuid); // fake insulin record
        }
        return result;
    }

}
