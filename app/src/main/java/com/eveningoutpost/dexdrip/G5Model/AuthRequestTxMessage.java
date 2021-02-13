package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import java.nio.ByteBuffer;

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
        init(opcode, token_size + 2);
        final byte[] randomBytes = getRandomKey();
        final ByteBuffer bb = ByteBuffer.allocate(token_size);
        bb.put(randomBytes, 0, token_size);
        singleUseToken = bb.array();
        data.put(singleUseToken);
        data.put(alt ? endByteAlt : endByteStd);
        byteSequence = data.array();
        UserError.Log.d(TAG, "New AuthRequestTxMessage: " + JoH.bytesToHex(byteSequence));
    }
}

