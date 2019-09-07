package com.eveningoutpost.dexdrip.utils.bt;

import com.polidea.rxandroidble2.RxBleConnection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// jamorham

@RequiredArgsConstructor
public abstract class ReplyProcessor {

    @Getter
    private Object tag;

    protected byte[] mOutbound;
    @Getter
    private final RxBleConnection connection;

    public abstract void process(byte[] bytes);

    public ReplyProcessor setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    public ReplyProcessor setOutbound(final byte[] tag) {
        this.mOutbound = tag;
        return this;
    }

}
