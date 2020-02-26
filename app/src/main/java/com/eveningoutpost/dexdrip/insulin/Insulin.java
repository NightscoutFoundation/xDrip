package com.eveningoutpost.dexdrip.insulin;

import android.util.Log;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public abstract class Insulin {
    private static final String TAG = "InsulinManager";
    private final String name;
    private final String displayName;
    private final ArrayList<String> pharmacyProductNumber;
    private Boolean enabled;
    protected double concentration;
    protected long maxEffect;

    public Insulin(String n, String dn, ArrayList<String> ppn, String c, JsonObject curveData) {
        name = n;
        displayName = dn;
        pharmacyProductNumber = ppn;
        maxEffect = 0;
        enabled = true;
        concentration = 1;
        switch (c.toLowerCase()) {
            case "u100":
                concentration = 1;
                break;
            case "u200":
                concentration = 2;
                break;
            case "u300":
                concentration = 3;
                break;
            case "u400":
                concentration = 4;
                break;
            case "u500":
                concentration = 5;
                break;
            default:
                Log.d(TAG, "unknown insulin concentration code " + c);
        }
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getMaxEffect() {
        return maxEffect;
    }

    public ArrayList<String> getPharmacyProductNumber() {
        return pharmacyProductNumber;
    }

    public double calculateIOB(double time) {
        return -1;
    }

    public double calculateActivity(double time) {
        return -1;
    }
}
