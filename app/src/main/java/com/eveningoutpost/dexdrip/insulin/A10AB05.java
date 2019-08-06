package com.eveningoutpost.dexdrip.insulin;

import java.time.Duration;
import java.util.ArrayList;

public class A10AB05 extends Insulin {
    public A10AB05(String n, ArrayList<String> ppn) {
        super(n, ppn);
    }

    public double calculate(double units, Duration duration)
    {
        return -1;
    }

}
