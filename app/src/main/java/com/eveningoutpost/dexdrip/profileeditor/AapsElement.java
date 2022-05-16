package com.eveningoutpost.dexdrip.profileeditor;

import com.google.gson.annotations.Expose;

import lombok.Data;

/**
 * JamOrHam
 * represent an AAPS time/value element
 */

@Data
public class AapsElement implements Comparable<AapsElement> {

    @Expose
    String time;
    @Expose
    int timeAsSeconds;
    @Expose
    double value;

    @Override
    public int compareTo(final AapsElement o) {
        final Integer myTimeAsSeconds = timeAsSeconds;
        final Integer oTimeAsSeconds = o.timeAsSeconds;
        return myTimeAsSeconds.compareTo(oTimeAsSeconds);
    }
}
