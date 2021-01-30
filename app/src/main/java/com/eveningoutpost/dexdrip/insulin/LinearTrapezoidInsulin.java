package com.eveningoutpost.dexdrip.insulin;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class LinearTrapezoidInsulin extends Insulin {
    /// curvedata and all timestamps are defined in minutes
    private long onset;  // when does profile activity starts
    private long t1;     // when does activity reaches max
    private long t2;     // when does activity leaves max
    private long t3;     // when does activity ends

    private double max;

    public LinearTrapezoidInsulin(String n, String dn, ArrayList<String> ppn, String c, JsonObject curveData) {
        super(n, dn, ppn, c, curveData);

        onset = curveData.get("onset").getAsLong();
        if (curveData.get("peak").getAsString().contains("-")) {
            t1 = Integer.parseInt(curveData.get("peak").getAsString().split("-")[0]);
            t2 = Integer.parseInt(curveData.get("peak").getAsString().split("-")[1]);
        } else {
            t1 = Integer.parseInt(curveData.get("peak").getAsString());
            t2 = t1;
        }
        t3 = curveData.get("duration").getAsLong();

        max = 2.0 / (t2 - t1 + t3 - onset);
        maxEffect = t3;
    }

    public double calculateIOB(double t) {

		if ((0 <= t) && (t < onset))
			return 1.0;
		else if ((onset <= t) && (t < t1))
			return 1.0 - 0.5 * (t - onset)* (t - onset) * max / (t1 - onset);
        else if ((t1 <= t) && (t < t2))
            return 1.0 + 0.5 * max * (t1 - onset) - max * (t - onset);
        else if ((t2 <= t) && (t < t3))
            return 0.5 * (t3 - t) * (t3 - t) * max / (t3 - t2);
        else return 0;
    }

    public double calculateActivity(double t) {

        if ((0 <= t) && (t < onset))
            return 0.0;
        else if ((onset <= t) && (t < t1))
            return concentration * (t - onset) * max / (t1 - onset);
        else if ((t1 <= t) && (t < t2))
            return concentration * max;
        else if ((t2 <= t) && (t < t3))
            return concentration * (t - t3) * max / (t3 - t2);
        else return 0;
    }
}
