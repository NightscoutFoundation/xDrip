package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jamorham on 25/11/2016.
 *
 * Alternate mechanism for reading data using the transmitter's internal algorithm.
 *
 * initial packet structure cribbed from Loopkit
 */

public class GlucoseRxMessage extends TransmitterMessage {

    private final static String TAG = G5CollectionService.TAG; // meh

    public static final byte opcode = 0x31;
    public TransmitterStatus status;
    public int status_raw;
    public int timestamp;
    public int unfiltered;
    public int filtered;
    public int sequence; // : UInt32
    public boolean glucoseIsDisplayOnly; // : Bool
    public int glucose; // : UInt16
    public int state; //: UInt8
    public int trend; // : Int8 127 = invalid

    public GlucoseRxMessage(byte[] packet) {
        UserError.Log.d(TAG, "GlucoseRX dbg: " + JoH.bytesToHex(packet));
        if (packet.length >= 14) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode) && checkCRC(packet)) {

                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                status_raw = data.get(1);
                status = TransmitterStatus.getBatteryLevel(data.get(1));
                sequence = data.getInt(2);
                timestamp = data.getInt(6);


                int glucoseBytes = data.getShort(10); // check signed vs unsigned!!
                glucoseIsDisplayOnly = (glucoseBytes & 0xf000) > 0;
                glucose = glucoseBytes & 0xfff;

                state = data.get(12);
                trend = data.get(13);
                if (glucose > 13) {
                    unfiltered = glucose * 1000;
                    filtered = glucose * 1000;
                } else {
                    filtered = glucose;
                    unfiltered = glucose;
                }

                UserError.Log.d(TAG, "GlucoseRX: seq:" + sequence + " ts:" + timestamp + " sg:" + glucose + " do:" + glucoseIsDisplayOnly + " ss:" + status + " sr:" + status_raw + " st:" + CalibrationState.parse(state) + " tr:" + trend);

            }
        } else {
            UserError.Log.d(TAG, "GlucoseRxMessage packet length received wrong: " + packet.length);
        }

    }

    CalibrationState calibrationState() {
        return CalibrationState.parse(state);
    }

    boolean usable() {
        return calibrationState().usableGlucose();
    }

    boolean OkToCalibrate() {
        return calibrationState().readyForCalibration();
    }

}
