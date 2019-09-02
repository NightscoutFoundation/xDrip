package com.eveningoutpost.dexdrip.Models;

import java.util.Date;

/**
 * Created by jamorham on 02/01/16.
 */
public class Iob {
    public long timestamp;
    public Date date;
    public double iob = 0;
    public CobCalc cobCalc;
    public double cob = 0;
    public double rawCarbImpact = 0;
    public double jCarbImpact = 0;
    public double jActivity = 0;
}


