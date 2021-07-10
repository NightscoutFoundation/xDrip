package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.google.gson.annotations.Expose;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_AUTH_REPLY;

// jamorham

public class AuthRx extends BaseMessage {

    @Getter
    boolean valid = false;
    @Expose
    public int opcode = -1;
    @Expose
    int replyCode = -1;
    @Expose
    int resultCode = -1;


    public AuthRx(final byte[] packet) {
        if (packet == null || packet.length < 4) return;
        wrap(packet);

        replyCode = getUnsignedByte();
        opcode = getUnsignedByte();
        resultCode = getUnsignedShort();

        if (opcode != OPCODE_AUTH_REPLY
                //  || replyCode != 0x83
                || resultCode != 0) {
            // response not valid
            return;
        }

        valid = true;
    }
}
