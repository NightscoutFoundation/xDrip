package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.utils.CipherUtils.getRandomKey;

/**
 * Created by joeginley on 3/16/16.
 */
@SuppressWarnings("FieldCanBeLocal")
public class AuthRequestTxMessage extends BaseMessage {
    public final byte opcode = 0x01;
    public byte[] singleUseToken;
    private final byte endByteStd = 0x2;
    private final byte endByteAlt = 0x1;


    public AuthRequestTxMessage(int token_size) {
        this(token_size, false);
    }

    public AuthRequestTxMessage(int token_size, boolean alt) {
        byte[] uuidBytes = getRandomKey();
        final UUID uuid = UUID.nameUUIDFromBytes(uuidBytes);

        try {
            uuidBytes = uuid.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final ByteBuffer bb = ByteBuffer.allocate(token_size);
        bb.put(uuidBytes, 0, token_size);
        singleUseToken = bb.array();

        data = ByteBuffer.allocate(token_size + 2);
        data.put(opcode);
        data.put(singleUseToken);
        data.put(alt ? endByteAlt : endByteStd);

        byteSequence = data.array();
        UserError.Log.d(TAG, "New AuthRequestTxMessage: " + JoH.bytesToHex(byteSequence));
    }
}

