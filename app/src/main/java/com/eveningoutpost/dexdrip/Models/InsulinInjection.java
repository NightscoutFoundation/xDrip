package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.insulin.Insulin;
import com.google.gson.annotations.Expose;

public class InsulinInjection {
    private Insulin profile;

    @Expose
    private double units;

    @Expose
    private String insulin;

    public InsulinInjection(Insulin p, double u)
    {
        profile = p;
        units = u;
        insulin = p.getName();
    }

    public String getInsulin() { return insulin; }

    public double getUnits() {
        return units;
    }

    public Insulin getProfile() {
        return profile;
    }
    public void setProfile(Insulin profile) {
        this.profile = profile;
    }
}
