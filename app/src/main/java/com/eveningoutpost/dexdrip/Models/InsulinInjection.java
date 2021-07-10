package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.insulin.Insulin;
import com.eveningoutpost.dexdrip.insulin.InsulinManager;
import com.google.gson.annotations.Expose;

import lombok.Getter;

public class InsulinInjection {
    private Insulin profile;

    @Expose
    @Getter
    private double units;

    @Expose
    @Getter
    private String insulin;

    public InsulinInjection(final Insulin p, final double u) {
        profile = p;
        units = u;
        insulin = (p != null) ? p.getName() : "unknown";
    }


    public Insulin getProfile() {
        // populate on demand
        if (profile == null) {
            profile = InsulinManager.getProfile(insulin);
        }
        return profile;
    }

    // This is just a rough way to decide if it is a basal insulin without user needing to set it
    // question as to whether this should be here or call to encapsulated method in Insulin
    public boolean isBasal() {
        return getProfile().getMaxEffect() > 1000;
    }

}
