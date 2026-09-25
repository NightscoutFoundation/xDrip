package com.eveningoutpost.dexdrip.g5model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// created by jamorham

public class SessionStartRxMessage extends BaseMessage {
    public static final byte opcode = 0x27;
    final byte length = 17;

    private byte status = (byte) 0xFF;
    private byte info = (byte) 0xFF;
    final String transmitterId;
    int sessionStartTime = 0;
    int requestedStartTime = 0;
    int transitterTime = 0;

    boolean valid = false;

    public SessionStartRxMessage(byte[] packet, String transmitterId) {
        this.transmitterId = transmitterId;
        if (packet.length == length) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode) && checkCRC(packet)) {
                valid = true;
                status = data.get();
                info = data.get();
                requestedStartTime = data.getInt();
                sessionStartTime = data.getInt();
                transitterTime = data.getInt();
            }
        }
    }

    boolean isValid() {
        return valid;
    }

    boolean isOkay() {
        return isValid() && status == 0x00 && (info == 0x01 || info == 0x05 || info == 0x06) && sessionStartTime != INVALID_TIME;
    }

    // beyond hope?
    boolean isFubar() {
        return info == 0x04;
    }

    long getSessionStart() {
        if (isOkay() && sessionStartTime > 0) {
            return DexTimeKeeper.fromDexTime(transmitterId, sessionStartTime);
        } else {
            return 0;
        }
    }

    long getRequestedStart() {
        if (isOkay() && requestedStartTime > 0) {
            return DexTimeKeeper.fromDexTime(transmitterId, requestedStartTime);
        } else {
            return 0;
        }
    }

    long getTransmitterTime() {
        if (isOkay() && transitterTime > 0) {
            return DexTimeKeeper.fromDexTime(transmitterId, transitterTime);
        } else {
            return 0;
        }
    }

    String message() {
        switch (info) {
            case 0x01:
                return "OK";
            case 0x02:
                return "Already started";
            case 0x03:
                return "Invalid";
            case 0x04:
                return "Clock not synchronized or other error"; // probably
            case 0x05:
                return "OK G6"; // probably
            case 0x06:
                return "OK G6 - unsure"; // probably
            default:
                return "Unknown code: " + info;
        }
    }
}

