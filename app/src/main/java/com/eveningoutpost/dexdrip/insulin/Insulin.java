package com.eveningoutpost.dexdrip.insulin;

import java.time.Duration;
import java.util.ArrayList;

public abstract class Insulin {
    private final String displayName;
    private final ArrayList<String> pharmacyProductNumber;

    public Insulin(String n, ArrayList<String> ppn)
    {
        displayName = n;
        pharmacyProductNumber = ppn;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ArrayList<String> getPharmacyProductNumber() {
        return pharmacyProductNumber;
    }

    public double calculate(double units, Duration duration)
    {
        return -1;
    }
}
