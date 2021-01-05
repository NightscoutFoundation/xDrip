package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Created by jamorham on 25/11/2016.
 */


public class BatteryInfoRxMessage extends BaseMessage {

    private final static String TAG = G5CollectionService.TAG; // meh
    private final static PacketValidator validator = new PacketValidator();

    public static final byte opcode = 0x23;

    public int status;
    public int voltagea;
    public int voltageb;
    public int resist = 0;
    public int runtime;
    public int temperature;

    public BatteryInfoRxMessage(byte[] packet) {
        if (!validator.test(packet)) {
            return;
        }
        //there is no reason for us to do anything other than assert
        // this packet fulfills our prerequisites which means
        // a better solution is an Ensure/Assert that throws an illegalArguemntException
        // No the cost of Exceptions isn't the exception, it's the unwinding of the stack trace.
        // byte packetOpcode = packet[0];


        ByteBuffer.allocate(packet.length-1).mark()             //sure we could keep it in there
                .put(packet,1,packet.length-1)      //but why?
                .rewind().order(ByteOrder.LITTLE_ENDIAN);       //BYTES READ TOTAL
                                                                // 10b      12b
                                                                // ---------------
                                                                //  1       1 //opcode
        status = data.get();                                    //  2       2
        voltagea  = (data.getShort() & 0x0000FFFF);             //  4       4
        voltageb  = (data.getShort() & 0x0000FFFF);             //  6       6
        if (packet.length == 12) {
            resist = (data.getShort() & 0x0000FFFF);            //  6       8
        }
        runtime = (data.get() & 0x00FF);                        //  7       9
        temperature = data.get();                               //  8       10
        // and here we are with 2 bytes left.
        // what those contains, only .... someeone probably knows so

    }


    public String toString() {
        return String.format(Locale.US, "Status: %s / VoltageA: %d / VoltageB: %d / Resistance: %d / Run Time: %d / Temperature: %d",
                TransmitterStatus.getBatteryLevel(status).toString(), voltagea, voltageb, resist, runtime, temperature);
    }

    static class PacketValidator implements Predicate<byte[]> {
        @Override
        public boolean test(byte[] bytes) {
            if (bytes != null) {
                switch(bytes.length) {
                    case(0):
                        UserError.Log.wtf(TAG, "0 bytes sent to BatteryInfoRxMessage ");
                        break;
                    case(10):
                    case(12):
                        return bytes[0] == opcode;
                    default:
                        UserError.Log.wtf(TAG, "Malformed BattteryInfoRxMessage packet opcode: 0x" + Integer.toHexString(bytes[0]) + " and length was " + bytes.length + ", expected is 10 or 12");
                }
            } else {
                UserError.Log.wtf(TAG, "a null byte array sent to BatteryInfoRxMessage ");
            }
            return false;
        }
    }

}