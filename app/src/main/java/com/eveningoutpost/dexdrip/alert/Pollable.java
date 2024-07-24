package com.eveningoutpost.dexdrip.alert;

/**
 * JamOrHam
 *
 * Alert polling interface
 */

public interface Pollable {

    enum When {
        Reading,
        Hour,
        ScreenOn,
        ChargeChange,
    }

    // main polling method
    PollResult poll(When event);

    // grouping identifier
    String group();

    // relative sorting position
    int sortPos();

}
