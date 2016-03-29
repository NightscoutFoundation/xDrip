package com.eveningoutpost.dexdrip.G5Model;

import android.util.Log;

import com.eveningoutpost.dexdrip.G5Model.TransmitterMessage;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthRequestTxMessage extends TransmitterMessage {
    byte opcode = 0x1;
    public byte[] singleUseToken;
    byte endByte = 0x2;

    public AuthRequestTxMessage() {
        // Create the singleUseToken from a 16 byte array.
        byte[] uuidBytes = new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        UUID uuid = UUID.nameUUIDFromBytes(uuidBytes);

        try {
            uuidBytes = uuid.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put(uuidBytes, 0, 8);
        singleUseToken = bb.array();

        // Create the byteSequence.
        data = ByteBuffer.allocate(10);
        data.put(opcode);
        data.put(singleUseToken);
        data.put(endByte);

        byteSequence = data.array();
    }
}

