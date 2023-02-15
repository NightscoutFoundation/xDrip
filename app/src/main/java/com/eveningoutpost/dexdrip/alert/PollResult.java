package com.eveningoutpost.dexdrip.alert;

/**
 * JamOrHam
 * <p>
 * Data class for returning poll results to caller
 */

public class PollResult {

    boolean remove;
    boolean triggered;

    public void reset() {
        remove = false;
        triggered = false;
    }

}
