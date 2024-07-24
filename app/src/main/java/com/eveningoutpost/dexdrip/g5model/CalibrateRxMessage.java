package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.R;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.eveningoutpost.dexdrip.xdrip.gs;

// created by jamorham

public class CalibrateRxMessage extends BaseMessage {

    public static final int opcode = 0x35;
    private static final int length = 5;

    private byte info = (byte) 0xff;
    private byte result = (byte) 0xff;

    CalibrateRxMessage(byte[] packet) {

        if (packet.length == length) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode) && checkCRC(packet)) {
                info = data.get();
                result = data.get();
            }
        }
    }

    boolean accepted() {
        return result == 0x00 || result == 0x06 || result == 0x0D;
    }

    boolean wantsCalibration() {
        return result == 0x06;
    }

    String message() {
        // TODO i18n
        switch (result) {

            case (byte) 0x00:
                return "OK";
            case (byte) 0x01:
                return "Code 1";
            case (byte) 0x06:
                return "Second calibration needed";
            case (byte) 0x08:
                return "Rejected";
            case (byte) 0x0B:
                return gs(R.string.sensor_stopped);
            case (byte) 0x0D:
                return "Duplicate";
            case (byte) 0x0E:
                return "Not ready to calibrate";
            case (byte) 0xFF:
                return "Unable to decode";
            default:
                return "Unknown code:" + result;
        }
    }
}
