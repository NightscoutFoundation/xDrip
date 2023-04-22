package com.eveningoutpost.dexdrip.g5model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * JamOrHam
 */

public class AdvMatcher extends BaseMessage {

    public byte[] check = new byte[2];
    public byte diu;
    public byte ver;

    public boolean valid;

    public AdvMatcher(final byte[] packet) {
        if (packet != null && packet.length >= 6) {
            if (packet.length >= 19) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
                data.get(check);
                data.get(diu);
                data.get(ver);
                valid = true;
            }
        }
    }
}
