package com.eveningoutpost.dexdrip.utils;

// jamorham

public class LogSlider {

    // logarithmic slider with positions start - end representing values start - end, calculate value at selected position
    public static double calc(int sliderStart, int sliderEnd, double valueStart, double valueEnd, int position) {
        valueStart = Math.log(Math.max(1, valueStart));
        valueEnd = Math.log(Math.max(1, valueEnd));
        return Math.exp(valueStart + (valueEnd - valueStart) / (sliderEnd - sliderStart) * (position - sliderStart));
    }
}
