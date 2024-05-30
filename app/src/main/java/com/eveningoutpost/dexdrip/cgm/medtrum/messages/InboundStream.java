package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.models.UserError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * jamorham
 *
 * Reassemble packet fragments from stream
 */

public class InboundStream extends BaseMessage {

    private int complete_length = 0;
    private volatile byte[] inboundByteArray;

    public InboundStream() {
        reset();
    }

    public synchronized void push(final byte[] raw_packet) {
        if (raw_packet == null) return;
        if (isComplete()) {
            UserError.Log.e(TAG, "Cannot push as stream is complete");
            return;
        }
        final ByteBuffer packet = ByteBuffer.wrap(raw_packet).order(ByteOrder.LITTLE_ENDIAN);

        // first packet in sequence get length
        if (complete_length == 0 && data.position() == 0) {
            if (packet.remaining() >= 2) {
                complete_length = packet.getShort();
            }
        }

        // byte copy data up to limit
        while (packet.hasRemaining() && data.position() < complete_length && data.position() < data.limit()) {
            data.put(packet.get());
        }

        if (packet.remaining() != 0) {
            UserError.Log.e(TAG, "Excess data in packet fragment: remaining: " + packet.remaining());
        }

    }

    public boolean isComplete() {
        return inboundByteArray != null || (complete_length > 0 && data.position() == complete_length);
    }

    public boolean hasSomeData() {
        return data.position() > 0;
    }

    public void reset() {
        complete_length = 0;
        inboundByteArray = null;
        data = ByteBuffer.allocate(1000);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }


    @Override
    public byte[] getByteSequence() {
        if (isComplete()) {
            // build cache final byte array
            if (inboundByteArray == null) {
                data.rewind();
                inboundByteArray = new byte[complete_length];
                data.get(inboundByteArray, 0, complete_length);
            }
            return inboundByteArray;
        } else {
            return null;
        }
    }

}
