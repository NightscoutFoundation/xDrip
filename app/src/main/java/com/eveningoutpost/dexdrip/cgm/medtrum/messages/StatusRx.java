package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

// jamorham

import com.google.gson.annotations.Expose;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_STAT_REPLY;

public class StatusRx extends BaseMessage {

    @Getter
    boolean valid = false;
    @Expose
    public int opcode = -1;
    @Expose
    int replyCode = -1;
    @Expose
    int resultCode = -1;
    @Expose
    @Getter
    AnnexARx annex = null;

    public StatusRx(final byte[] packet) {
        if (packet == null || packet.length < 6) return;
        wrap(packet);

        replyCode = getUnsignedByte();
        opcode = getUnsignedByte();
        resultCode = getUnsignedShort();

        if (opcode != OPCODE_STAT_REPLY
                || replyCode != 0x83
                || resultCode != 0) {
            // response not valid
            return;
        }

        // Process Annex
        final byte[] remainder = new byte[data.remaining()];
        data.get(remainder);

        if (remainder.length == 14 || remainder.length == 31 || remainder.length == 33) {
            annex = new AnnexARx(remainder);
            valid = true; // TODO some more checks
        }

    }

}
