package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_OK_WITH_PARAMETER;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_BULK_UP_REQUEST;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_INVALID;

// jamorham

public class BulkUpRequestTx extends BaseTx {

    @Getter
    private int bytesIncluded;

    private boolean noAck = false;
    public BulkUpRequestTx(final int type, final int id, final int length, final byte[] initialData, boolean noAck) {
        if (initialData == null || ((id & 0x80)==0 && initialData.length < length)) {
            return; // fail to initialize if data invalid
        }

        this.noAck = noAck;
        init(OPCODE_BULK_UP_REQUEST, 19);
        // TODO can we squeeze type and id in to a single byte? should we?
        data.put((byte) (type | (noAck ? 0x80 : 0))); // 1 = window
        data.put((byte) id); // window id etc
        data.putShort((short) length);
        bytesIncluded = Math.min(data.remaining(), initialData.length);
        data.put(initialData, 0, bytesIncluded);
    }


    @Override
    public boolean responseOk(final byte[] response) {
        if (response == null || response.length < 2) return false;
        // check status code and opcode
        return response[0] == getBytes()[0] && ((response[1] & ERROR_OK_WITH_PARAMETER) != 0);
    }


    public int getBulkUpOpcode(final byte[] response) {
        if (responseOk(response)) {
            return response[1] & 0xFF;
        } else {
            return OPCODE_INVALID;
        }
    }

    public static int encodeLength(final int sequence, final int length) {
        if (sequence == 0) return length;
        if (length > 256 || length == 0 || sequence > 255) {
            throw new RuntimeException("Length or sequence out of range: " + length + " " + sequence);
        }
        return (sequence << 8 | (length - 1));
    }

}


