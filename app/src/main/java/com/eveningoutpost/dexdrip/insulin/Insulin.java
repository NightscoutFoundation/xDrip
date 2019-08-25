package com.eveningoutpost.dexdrip.insulin;

import java.time.Duration;
import java.util.ArrayList;

public abstract class Insulin {
    private final String displayName;
    private final ArrayList<String> pharmacyProductNumber;
    private final String ATCCode;
    private Boolean enabled;

    public Insulin(String n, ArrayList<String> ppn)
    {
        displayName = n;
        pharmacyProductNumber = ppn;
        ATCCode = this.getClass().getSimpleName();
        enabled = true;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getATCCode() {
        return ATCCode;
    }
    public void enable() { enabled = true; }
    public void disable() { enabled = false; }
    public boolean isEnabled() { return enabled; }

    public ArrayList<String> getPharmacyProductNumber() {
        return pharmacyProductNumber;
    }

    public double calculate(double units, Duration duration)
    {
        return -1;
    }
}
