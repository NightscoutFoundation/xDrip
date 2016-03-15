package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by jcostik1 on 3/15/16.
 */
public abstract class TransmitterRxMessage {
    public ByteBuffer data;
    TransmitterRxMessage(){};
    TransmitterRxMessage(ByteBuffer data) {
        data = data;
    }
}
