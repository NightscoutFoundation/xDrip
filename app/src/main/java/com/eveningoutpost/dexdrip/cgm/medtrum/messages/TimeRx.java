package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

// jamorham

import lombok.Getter;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_TIME_REPLY;

public class TimeRx extends BaseMessage {

    @Getter
    boolean valid = false;
    int shortValue = -1;
    int opcode = -1;
    int status = -1;

    public TimeRx(byte[] packet) {
        if (packet == null || packet.length < 4) return;
        wrap(packet);

        status = getUnsignedByte();
        opcode = getUnsignedByte();
        shortValue = getUnsignedShort(); // typically 0x0000

        if (status == 0x8d && opcode == OPCODE_TIME_REPLY) {
            valid = true;
        }
    }
}
