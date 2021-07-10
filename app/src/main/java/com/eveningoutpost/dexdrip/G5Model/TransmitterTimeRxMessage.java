package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

/**
 * Created by joeginley on 3/28/16.
 */
public class TransmitterTimeRxMessage extends BaseMessage {
    public static final byte opcode = 0x25;
    @Getter
    private TransmitterStatus status;
    @Getter
    private int currentTime;
    @Getter
    private int sessionStartTime;

    public TransmitterTimeRxMessage(byte[] packet) {
        if (packet.length >= 10) {
            if (packet[0] == opcode && checkCRC(packet)) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                status = TransmitterStatus.getBatteryLevel(data.get(1));
                currentTime = data.getInt(2);
                sessionStartTime = data.getInt(6);
                // TODO more bytes after this?
            }
        }
    }

    public boolean sessionInProgress() {
        return sessionStartTime != -1 && currentTime != sessionStartTime;
    }

    public long getRealSessionStartTime(long now) {
        return now - ((currentTime - sessionStartTime) * 1000L);
    }

    public long getRealSessionStartTime() {

        if (sessionInProgress()) {
            return getRealSessionStartTime(JoH.tsl());
        } else {
            return -1;
        }
    }

    public long getSessionDuration() {
        if (sessionInProgress()) {
            return JoH.msSince(getRealSessionStartTime());
        } else {
            return -1;
        }
    }

}
