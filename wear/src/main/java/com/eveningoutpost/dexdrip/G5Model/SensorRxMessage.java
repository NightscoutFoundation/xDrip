package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jcostik1 on 3/26/16.
 */
public class SensorRxMessage extends TransmitterMessage {
    byte opcode = 0x2f;
    public TransmitterStatus status;
    public int timestamp;
    public int unfiltered;
    public int filtered;
    private final static String TAG = G5CollectionService.TAG; // meh

    public SensorRxMessage(byte[] packet) {
        UserError.Log.d(TAG, "SensorRX dbg: " + JoH.bytesToHex(packet));
        if (packet.length >= 14) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                status = TransmitterStatus.getBatteryLevel(data.get(1));
                timestamp = data.getInt(2);

                unfiltered = data.getInt(6);
                filtered = data.getInt(10);
            }
        }
    }
}
