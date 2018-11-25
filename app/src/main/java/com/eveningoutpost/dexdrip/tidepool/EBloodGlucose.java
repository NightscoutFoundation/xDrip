package com.eveningoutpost.dexdrip.tidepool;

// jamorham

import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.google.gson.annotations.Expose;

import java.util.LinkedList;
import java.util.List;

class EBloodGlucose extends BaseElement {

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


    static EBloodGlucose fromBloodTest(final BloodTest bloodtest) {
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
            results.add(fromBloodTest(bt));
        }
        return results;
    }

}

