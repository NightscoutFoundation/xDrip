package com.eveningoutpost.dexdrip.insulin;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;

public class MultipleInsulins {

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("multiple_insulin_types");
    }

    public static boolean useBasalActivity() {
        return Pref.getBooleanDefaultFalse("multiple_insulin_use_basal_activity");
    }

}
