package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Created by jamorham on 25/11/2016.
 *
 * Message contains a general status flag, two voltage measurements,
 * an <B>optional</B> resistance measurement, a runtime measurement
 * and finally a temperature, ending in a two byte typical CRC16
 *
 * Remember that all of these are little Endian, signed or unsigned
 * is completely uninteresting in this particular context (or for that
 * matter in general)
 *
 * Value        |  bits  | Datatype
 * ----------------------------------
 *  opCode      |    8   | byte
 *  status      |    8   | byte
 *  voltageA    |   16   | short
 *  voltageB    |   16   | short
 *  '''''''''''''''''''''''''''''''''
 *  optional: if packet length is 12
 *  this is included
 *  _________________________________
 *  resistance  |   16   | short
 *  _________________________________
 *  end optional
 *  '''''''''''''''''''''''''''''''''
 *  runtime     |    8   | byte
 *  temperature |    8   | byte
 *  crcRecord   |   16   | [byte]
 *
 *  So to reiterate. There are two variants of this packet using the same opCode.
 *
 *  One is 10 bytes, it does **NOT** include the resistance
 *  One is 12 byte, it **DOES** include the resistance
 *
 *  NOTE: This should (not) be replicated in the wear variant since we really shouldn't
 *  replicate code in that way.
 *
 *
 *
 */


public class BatteryInfoRxMessage extends BaseMessage {

    private static final String TAG = G5CollectionService.TAG; // meh
    private static final PacketValidator validator = new PacketValidator();

    public static final byte opcode = 0x23;

    public int status;
    public int voltagea;
    public int voltageb;
    public int resist = 0;
    public int runtime;
    public int temperature;

    /**
     * There is no reason for us to do anything other than
     * assert this packet fulfills our prerequisites which
     * mean a better solution is an Ensure/Assert that throws
     * an illegalArguemntException.
     * A common counter argument to using Exceptions it at there
     * is a cost to generating Exceptions. Feel free to write a test
     * that shows a significant performance win. The cost of the
     * Exception occurs when unwinding the stack trace.
     *
     * BatteryInfoRxMessage receives a packet PREIDENTIFIED as an
     * array of bytes where byte[0] equals the required opcode.
     * However BatteryInfoRxMessage has no particular reason to
     * trust that any array of bytes conforms to it's expected
     * format. Normally this would be done with Predicates
     * but at least breaking the validation out of the constructor
     * is better than nothing.
     *
     *
     * @param packet
     */
    public BatteryInfoRxMessage(byte[] packet) {
        if (!validator.test(packet)) {
            return;
        }
        // allocate, write data to buffer, set order and rewind
        this.data = ByteBuffer.allocate(packet.length-1) ;
        this.data.put(packet,1,packet.length-1)
            .order(ByteOrder.LITTLE_ENDIAN)
            .rewind();

        this.status = data.get();
        this.voltagea  = (data.getShort() & 0x0000FFFF);
        this.voltageb  = (data.getShort() & 0x0000FFFF);
        if (packet.length == 12) {
            this.resist = (data.getShort() & 0x0000FFFF);
        }
        this.runtime = (data.get() & 0x00FF);
        this.temperature = data.get();

    }

    /**
     * Surprisingly enough toString is one of those methods you s
     * @return a formatted String in a fixed Locale describing the current status.
     */
    @Override
    public String toString() {
        return String.format(Locale.US, "Status: %s / VoltageA: %d / VoltageB: %d / Resistance: %d / Run Time: %d / Temperature: %d",
                TransmitterStatus.getBatteryLevel(status).toString(), voltagea, voltageb, resist, runtime, temperature);
    }

    /**
     * Predicate replacement for brevity in constructur.
     */
    static class PacketValidator {

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
                        UserError.Log.e(TAG, "Unknown BattteryInfoRxMessage packet opcode: 0x" + Integer.toHexString(bytes[0]) + " and length was " + bytes.length + ", expected is 10 or 12");
                }
            } else {
                UserError.Log.e(TAG, "a null byte array sent to BatteryInfoRxMessage ");
            }
            return false;
        }
    }

}