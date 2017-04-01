package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.xdrip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Created by jamorham on 25/11/2016.
 */


public class BatteryInfoRxMessage extends TransmitterMessage {

    private final static String TAG = G5CollectionService.TAG; // meh

    public static final byte opcode = 0x23;

    public int status;
    public int voltagea;
    public int voltageb;
    public int resist;
    public int runtime;
    public int temperature;

    public BatteryInfoRxMessage(byte[] packet) {
        if (packet.length >= 12) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if (data.get() == opcode) {
                status = data.get();
                voltagea = getUnsignedShort(data);
                voltageb = getUnsignedShort(data);
                resist = getUnsignedShort(data);
                runtime = getUnsignedByte(data);
                temperature = data.get(); // not sure if signed or not, but <0c or >127C seems unlikely!
            } else {
                UserError.Log.wtf(TAG, "Invalid opcode for BatteryInfoRxMessage");
            }
        }
    }

    public String toString() {
        return xdrip.getAppContext().getString(R.string.g5_battery_status,
                TransmitterStatus.getBatteryLevel(status).toString(), voltagea, voltageb, resist, runtime, temperature);
    }

}