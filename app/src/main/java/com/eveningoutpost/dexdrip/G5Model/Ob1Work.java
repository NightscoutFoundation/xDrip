package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;

/**
 * Created by jamorham on 12/10/2017.
 */

public class Ob1Work {

    public final TransmitterMessage msg;
    public final String text;
    public final long timestamp;
    public volatile int retry = 0;

    Ob1Work(TransmitterMessage msg, String text) {
        this.msg = msg;
        this.text = text;
        this.timestamp = JoH.tsl();
    }

}
