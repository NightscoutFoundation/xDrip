package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import lombok.val;

/**
 * Created by jamorham on 25/11/2016.
 */


public class BatteryInfoRxMessage extends BaseMessage {

    private final static String TAG = G5CollectionService.TAG; // meh

    public static final byte opcode = 0x23;
    public static final byte opcode2 = 0x22;

    public int status;
    public int voltagea;
    public int voltageb;
    public int resist;
    public int runtime;
    public int temperature;
    public static volatile boolean battery0VException = false; // True when voltage A and Voltage B are both 0.

    public BatteryInfoRxMessage(byte[] packet) {
        if (packet.length >= 10) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            val oc = data.get();
            if (oc == opcode || oc == opcode2) {
                status = data.get();
                voltagea = getUnsignedShort(data);
                voltageb = getUnsignedShort(data);
                if (packet.length != 10) {
                    resist = getUnsignedShort(data);
                }
                runtime = getUnsignedByte(data);
                if (packet.length == 10) {
                    runtime = -1; // this byte isn't runtime on rev2
                }
                temperature = data.get(); // not sure if signed or not, but <0c or >127C seems unlikely!

                // Exception for 0-V battery
                if (voltagea < 1 && voltageb <1) {
                    battery0VException = true;
                } else {
                    battery0VException = false;
                }
            } else {
                UserError.Log.wtf(TAG, "Invalid opcode for BatteryInfoRxMessage");
            }
        } else {
            UserError.Log.wtf(TAG, "Invalid length for BatteryInfoMessage: " + packet.length);
        }
    }

    public String toString() {
        return String.format(Locale.US, "Status: %s / VoltageA: %d / VoltageB: %d / Resistance: %d / Run Time: %d / Temperature: %d",
                TransmitterStatus.getBatteryLevel(status).toString(), voltagea, voltageb, resist, runtime, temperature);
    }

}