package com.eveningoutpost.dexdrip.insulin.exponential;

import com.eveningoutpost.dexdrip.insulin.ExponentialInsulin;
import org.junit.Test;

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
}
