package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.NoArgsConstructor;

/**
 * Created by jamorham on 25/11/2016.
 *
 * Alternate mechanism for reading data using the transmitter's internal algorithm.
 *
 * initial packet structure cribbed from Loopkit
 */

@NoArgsConstructor
public class GlucoseRxMessage extends BaseGlucoseRxMessage {

    private final static String TAG = G5CollectionService.TAG; // meh

    public static final byte opcode = 0x31;


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

                UserError.Log.d(TAG, "GlucoseRX: seq:" + sequence + " ts:" + timestamp + " sg:" + glucose + " do:" + glucoseIsDisplayOnly + " ss:" + status + " sr:" + status_raw + " st:" + CalibrationState.parse(state) + " tr:" + getTrend());

            }
        } else {
            UserError.Log.d(TAG, "GlucoseRxMessage packet length received wrong: " + packet.length);
        }

    }


}
