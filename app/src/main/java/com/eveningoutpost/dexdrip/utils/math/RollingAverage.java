package com.eveningoutpost.dexdrip.utils.math;

import lombok.Getter;

/**
 * jamorham
 *
 * Stream based rolling average calculator
 *
 * Will reset above 2^31 operations
 */


public class RollingAverage {

    @Getter
    private final int size;
    @Getter
    private final double peak;
    private final int peak_rounded;
    private final double[] values;

    private int position;

    public RollingAverage(final int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Size must be 2 or more!");
        }
        this.size = size;
        this.peak = peak();
        this.peak_rounded = (int) (peak + 0.5d);
        this.values = new double[size];
    }

    // add a value, return average
    public synchronized double put(final double value) {
        values[position % size] = value;
        position++;
        return average();
    }

    // get current average
    public double average() {
        if (position == 0) return 0; // no data yet
        double sum = 0;
        final int top = Math.min(position, size);
        // take average of populated entries
        for (int i = 0; i < top; i++) {
            sum += values[i];
        }
        return sum / top;
    }

    // get the peak position
    private double peak() {
        return (size - 1) / 2d;
    }

    // have we reached this yet? round down
    public boolean reachedPeak() {
        return position > peak_rounded;
    }

    // show string representation
    public String toS() {
        // any changes to the formatting will need to be reflected in unit testing
        final StringBuilder sb = new StringBuilder("Rolling Average: Size: " + Math.min(position, size) + "/" + size + " Average: " + average() + "    ");
        for (int i = 0; i < size; i++) {
            sb.append(i);
            sb.append(":");
            sb.append(values[i]);
            sb.append(" ");
        }
        return sb.toString();
    }

}
