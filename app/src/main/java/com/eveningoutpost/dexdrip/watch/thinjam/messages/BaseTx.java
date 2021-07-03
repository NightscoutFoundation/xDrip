package com.eveningoutpost.dexdrip.watch.thinjam.messages;

// jamorham

import com.eveningoutpost.dexdrip.Models.JoH;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_OK;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.errorText;

public abstract class BaseTx {


    public ByteBuffer data = null;
    private byte[] byteSequence = null;


    public void init(final byte opcode, final int length) {
        final int packet_length = length + 1;
        data = ByteBuffer.allocate(packet_length).order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        //  data.put((byte) packet_length);

    }

    public byte[] getBytes() {
        if (byteSequence == null) {
            byteSequence = data.array();
        }
        return byteSequence;
    }

    public boolean responseOk(final byte[] response) {
        if (response == null || response.length < 2) return false;
        // check status code and opcode
        return response[0] == getBytes()[0] && response[1] == ERROR_OK;
    }

    public String responseText(final byte[] response) {
        int code = response[1] & 0xFF;
        if (code < errorText.length) {
            return errorText[code];
        } else {
            return "Unknown Error Text";
        }
    }

    public static byte[] reverseBytes(byte[] source) {
        byte[] dest = new byte[source.length];
        for (int i = 0; i < source.length; i++) {
            dest[(source.length - i) - 1] = source[i];
        }
        return dest;
    }

    public int getUnsignedByte() {
        return data.get() & 0xff;
    }

    public int getUnsignedShort() {
        return data.getShort() & 0xffff;
    }

    public long getUnsignedInt() {
        return data.getInt() & 0xffffffffL;
    }

    public long getUnsignedInt(int position) {
        return data.getInt(position) & 0xffffffffL;
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

}
