package com.eveningoutpost.dexdrip.tidepool;

// jamorham

import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.gson.annotations.Expose;

import java.util.LinkedList;
import java.util.List;

class EBloodGlucose extends BaseElement {
    private static final String TAG = "BaseElement";
    
    @Expose
    String subType;
    @Expose
    String units;
    @Expose
    int value;

    EBloodGlucose() {
        this.type = "smbg";
        this.units = "mg/dL";
    }

    private static boolean IsBloodGlucoseValid(BloodTest bloodtest) {
        if(bloodtest.mgdl > 0 && bloodtest.mgdl <= 1000) {
            return true;
        }
        UserError.Log.e(TAG, "Ignoring invalid treatment " + bloodtest.toS());
        return false;
    }
    
    static EBloodGlucose fromBloodTest(final BloodTest bloodtest) {
        if(!IsBloodGlucoseValid(bloodtest)) {
            return null;
        }
        final EBloodGlucose bg = new EBloodGlucose();
        bg.populate(bloodtest.timestamp, bloodtest.uuid);

        bg.subType = "manual"; // TODO
        bg.value = (int) bloodtest.mgdl;
        return bg;
    }

    static List<EBloodGlucose> fromBloodTests(final List<BloodTest> bloodTestList) {
        if (bloodTestList == null) return null;
        final List<EBloodGlucose> results = new LinkedList<>();
        for (BloodTest bt : bloodTestList) {
            EBloodGlucose eBloodGlucose = fromBloodTest(bt);
            if(eBloodGlucose != null) {
                results.add(fromBloodTest(bt));
            }
        }
        return results;
    }

}

