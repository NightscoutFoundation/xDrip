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
        t1 = Integer.parseInt(curveData.get("peak").getAsString().split("-")[0]);
        t2 = Integer.parseInt(curveData.get("peak").getAsString().split("-")[1]);
        t3 = curveData.get("duration").getAsLong();

        max = 2.0/(t2-t1+t3);
    }

    public double calculateIOB(double time)
    {
        if ((0 <= time) && (time < t1))
            return 1.0-time*time*max/(2*t1);
        else if ((t1 <= time) &&  (time < t2))
            return 1.0+0.5*max*t1-max*time;
        else if ((t2 <= time) && (time < t3))
            return 0.5*(time-t3)*(time-t3)*max/(t3-t2);
        else return 0;
    }

    public double calculateActivity(double time)
    {
        if ((0 <= time) && (time < t1))
            return concentration * time*max/t2;
        else if ((t1 <= time) &&  (time < t2))
            return concentration * max;
        else if ((t2 <= time) && (time < t3))
            return concentration * (time-t3)*max/(t3-t2);
        else return 0;
    }


}
