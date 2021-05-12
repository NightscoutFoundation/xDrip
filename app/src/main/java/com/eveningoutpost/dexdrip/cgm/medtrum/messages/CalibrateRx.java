package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

// jamorham

import lombok.Getter;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_CALI_REPLY;

public class CalibrateRx extends BaseMessage {

    @Getter
    boolean valid = false;
    int replyStatus = -1;
    int shortValue2 = -1;
    int opcode = -1;
    int status = -1;
    @Getter
    int errorCode = -1;


    public CalibrateRx(byte[] packet) {
        if (packet == null || packet.length < 7) return;
        wrap(packet);

        status = getUnsignedByte();
        opcode = getUnsignedByte();
        replyStatus = getUnsignedShort(); // typically 0x0000 = received
        errorCode = getUnsignedByte(); // typically 0x00 or 0x01 = good, other = bad
        shortValue2 = getUnsignedShort(); // typically 0x0000

        if (status == 0x82 && opcode == OPCODE_CALI_REPLY) {
            valid = true;
        }
    }

    public boolean isOk() {
        return valid && errorCode < 2;
    }
}
