package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthRequestTxMessage extends TransmitterMessage {
    int opcode = 0x1;
    public byte[] singleUseToken;
    int endByte = 0x2;

    byte[] byteSequence;

    public AuthRequestTxMessage() {
        byte[] uuidBytes = new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        UUID newUUID = UUID.nameUUIDFromBytes(uuidBytes);
        ByteBuffer bb = ByteBuffer.allocate(16).putLong(newUUID.getLeastSignificantBits()).putLong(newUUID.getMostSignificantBits());
        singleUseToken = bb.array();

        // Create the byteSequence.
        data = ByteBuffer.wrap(new byte[18]);
        data.put((byte)opcode);
        data.put(singleUseToken);
        data.put((byte)endByte);

        byteSequence = data.array();
        //Log.i("ByteSequence", Arrays.toString(byteSequence));
    }
}
