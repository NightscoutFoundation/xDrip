package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public abstract class TransmitterMessage {
    public byte[] byteSequence = null;
    public ByteBuffer data = null;

    public TransmitterMessage(){}
    public TransmitterMessage(ByteBuffer d) {
        data = d;
    }
}
