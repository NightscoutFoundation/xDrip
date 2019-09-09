package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.gson.annotations.Expose;

// jamorham

public class EBolus extends BaseElement {
  
    private static final String TAG = "TidepoolEBolus";
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

    private static boolean IsBolusValid(Treatments treatment) {
        if(treatment.insulin >= 0 && treatment.insulin <= 100) {
            return true;
        }
        UserError.Log.e(TAG, "Ignoring invalid treatment " + treatment.toS());
        return false;
    }
    
    public static EBolus fromTreatment(Treatments treatment) {
        if(!IsBolusValid(treatment)) {
          return null;
        }
        return new EBolus(treatment.insulin, treatment.insulin, treatment.timestamp, treatment.uuid);
    }

}
