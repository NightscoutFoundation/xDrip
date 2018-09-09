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
        booleansToSync.add("ob1_g5_use_errored_data");
        booleansToSync.add("ob1_g5_allow_resetbond");
        booleansToSync.add("ob1_g5_fallback_to_xdrip");
        booleansToSync.add("ob1_g5_restart_sensor");
        booleansToSync.add("ob1_g5_preemptive_restart");
        booleansToSync.add("only_ever_use_wear_collector");
        booleansToSync.add("external_blukon_algorithm");
        booleansToSync.add("bluetooth_trust_autoconnect");
        booleansToSync.add("bluetooth_use_scan");
        booleansToSync.add("ob1_minimize_scanning");
        booleansToSync.add("using_g6");
    }


}
