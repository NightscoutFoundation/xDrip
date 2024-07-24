package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.common.collect.ImmutableSet;
import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 12/10/2017.
 */

public class Ob1Work {

    private static final ImmutableSet<Class> streamClasses = ImmutableSet.of(SessionStartTxMessage.class, SessionStopTxMessage.class, CalibrateTxMessage.class);

    @Expose
    public final BaseMessage msg;
    @Expose
    public final String text;
    @Expose
    public final long timestamp;
    public volatile int retry = 0;

    Ob1Work(BaseMessage msg, String text) {
        this.msg = msg;
        this.text = text;
        this.timestamp = JoH.tsl();
    }

    public boolean streamable() {
        return streamClasses.contains(msg.getClass());
    }

}
