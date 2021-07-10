package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
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
    @Expose
    public volatile boolean dontRetry = false;
    public volatile Runnable postWriteCallback;
    public volatile Runnable preWriteCallback;

    Ob1Work(BaseMessage msg, String text) {
        this.msg = msg;
        this.text = text;
        this.timestamp = JoH.tsl();
    }

    public boolean streamable() {
        return streamClasses.contains(msg.getClass());
    }

    public void preWrite() {
        if (preWriteCallback != null) {
            preWriteCallback.run();
        }
    }

    public void postWrite() {
        if (postWriteCallback != null) {
            postWriteCallback.run();
        }
    }

    public Ob1Work setPreWrite(final Runnable callback) {
        this.preWriteCallback = callback;
        return this;
    }

    public Ob1Work setPostWrite(final Runnable callback) {
        this.postWriteCallback = callback;
        return this;
    }

    public Ob1Work setDontRetry() {
        this.dontRetry = true;
        return this;
    }

}
