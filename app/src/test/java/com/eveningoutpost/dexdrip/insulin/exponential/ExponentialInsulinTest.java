package com.eveningoutpost.dexdrip.insulin.exponential;

import com.eveningoutpost.dexdrip.insulin.*;
import com.google.gson.*;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ExponentialInsulinTest {

    @Test
    public void testIobCalc() {
        double testUserDefinedDia = 4.0;
        double testPeak = 30.0;
        double amount = 10.0;

        ExponentialInsulin exp = new ExponentialInsulin(testPeak, testUserDefinedDia * 60);
        // check directly after bolus
        long time = 0;
        assertEquals(10.0, exp.calculateIOB(time) * amount, 0.01);
        // check after 1 hour
        time = 60;
        assertEquals(3.82, exp.calculateIOB(time) * amount, 0.01);
        // check after 2 hour
        time = 2 * 60;
        assertEquals(0.67, exp.calculateIOB(time) * amount, 0.01);
        // check after 3 hour
        time = 3 * 60;
        assertEquals(0.06, exp.calculateIOB(time) * amount, 0.01);
        // check after dia
        time = 4 * 60;
        assertEquals(0.0, exp.calculateIOB(time) * amount, 0.01);
    }

    @Test
    public void comparisonTest() {
        double testUserDefinedDia = 6.0;
        double testPeak = 75.0;
        double amount = 5.0;

        JsonObject curve = new JsonObject();
        curve.add("onset", new JsonPrimitive(String.format("%d", 15)));
        curve.add("duration", new JsonPrimitive(String.format("%.0f", testUserDefinedDia * 60)));
        curve.add("peak", new JsonPrimitive(String.format("%.0f", testPeak)));
        ArrayList<String> ppn = new ArrayList<>();
        ppn.add("1100558736");
        LinearTrapezoidInsulin lin = new LinearTrapezoidInsulin("Lin", "lin", ppn, "U100", curve);

        ExponentialInsulin exp = new ExponentialInsulin(testPeak, testUserDefinedDia * 60);

        double maxDiff = 0.0;
        double diffIntegral = 0.0;
        double linIntegral = 0.0;
        double expIntegral = 0.0;
        long maxDiffT = 0;
        for (long t = 0; t <= testUserDefinedDia * 60; t++) {
            expIntegral += exp.calculateActivity(t);
            linIntegral += lin.calculateActivity(t);
            double activityDiff = exp.calculateActivity(t) - lin.calculateActivity(t);
            double diff = amount * (exp.calculateIOB(t) - lin.calculateIOB(t));
            diffIntegral += activityDiff;
            if (Math.abs(diff) > Math.abs(maxDiff)) {
                maxDiff = diff;
                maxDiffT = t;
            }
        }

        assertEquals(1.0, expIntegral, 0.01);
        assertEquals(-0.65, linIntegral, 0.01);
        assertEquals(0.7, Math.abs(maxDiff), 0.02);
        assertEquals(128, maxDiffT);
        assertEquals(1.65, diffIntegral, 0.02);
    }
}
