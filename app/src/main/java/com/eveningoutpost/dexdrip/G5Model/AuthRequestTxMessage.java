package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.utils.CipherUtils.getRandomBytes;
import static com.eveningoutpost.dexdrip.utils.CipherUtils.getRandomKey;

/**
 * Created by joeginley on 3/16/16.
 */

/**
 * This message is transmitted either
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

    /**
     * We are expected (presumably although I think this may be somewhat of a misunderstanding
     * of the process and I'm not sure if it has ever really worked but let's try and figure out
     * shall we. We are expected to create tokenSize random bytes preceded by an opcode and
     * followed by a flag. And since little endian only affects byte order there's no need for a
     * ByteBuffer (or two),
     * Note that this method is sneaky and steals two random bytes in order to save some cycles
     * and some memory.
     *
     * @param tokenSize
     * @param alt
     */
    public AuthRequestTxMessage(int tokenSize, boolean alt) {
        byteSequence = getRandomBytes(tokenSize+2);
        singleUseToken = Arrays.copyOfRange(byteSequence,1,tokenSize+1);
        byteSequence[0] = opcode;
        byteSequence[tokenSize+1] = getEndByte(alt);

        UserError.Log.d(TAG, "New AuthRequestTxMessage: " + JoH.bytesToHex(byteSequence));
    }

    public byte getEndByte(boolean alt) {
        return alt ? endByteAlt : endByteStd;
    }
}

