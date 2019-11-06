package com.eveningoutpost.dexdrip.watch;

import com.eveningoutpost.dexdrip.watch.lefun.LefunPrefBinding;
import com.eveningoutpost.dexdrip.watch.miband.MibandPrefBinding;

public enum PrefBindingFactory {
    MIBAND_INSTANCE,
    LEFUN_INSTANCE;

    public PrefBinding getMiband() {
        return new MibandPrefBinding();
    }
    public PrefBinding getLefun() {
        return new LefunPrefBinding();
    }
}
