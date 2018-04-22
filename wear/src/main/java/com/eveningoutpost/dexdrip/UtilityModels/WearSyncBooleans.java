package com.eveningoutpost.dexdrip.UtilityModels;

// jamorham

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class WearSyncBooleans {

    @Getter
    private static final List<String> booleansToSync = new ArrayList<>();

    static {
        booleansToSync.add("use_wear_heartrate");
        booleansToSync.add("engineering_mode");
        booleansToSync.add("ob1_g5_use_transmitter_alg");
        booleansToSync.add("ob1_g5_use_insufficiently_calibrated");
        booleansToSync.add("ob1_g5_allow_resetbond");
    }


}
