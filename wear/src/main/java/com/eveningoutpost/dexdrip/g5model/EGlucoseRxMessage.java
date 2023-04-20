package com.eveningoutpost.dexdrip.g5model;


import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EGlucoseRxMessage extends BaseGlucoseRxMessage {

    private static final String TAG = EGlucoseRxMessage.class.getSimpleName();

    private Integer predicted_glucose; // : UInt16
    public static final byte opcode = 0x4f;

    public EGlucoseRxMessage(byte[] packet) {
        UserError.Log.d(TAG, "EGlucoseRX dbg: " + JoH.bytesToHex(packet));
        if (packet.length >= 14) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode) && checkCRC(packet)) {


                //status_raw = data.get();
                status = TransmitterStatus.getBatteryLevel(data.get()); // ??
                sequence = data.getInt();
                timestamp = data.getInt();


                int glucoseBytes = data.getShort(); // check signed vs unsigned!!
                glucoseIsDisplayOnly = (glucoseBytes & 0xf000) > 0;
                glucose = glucoseBytes & 0xfff;

                state = data.get();
                trend = data.get();

                if (glucose > 13) {
                    unfiltered = glucose * 1000;
                    filtered = glucose * 1000;
                } else {
                    filtered = glucose;
                    unfiltered = glucose;
                }

                predicted_glucose = data.getShort() & 0x03ff; // needs mask??? // remaining bits??

                UserError.Log.d(TAG, "EGlucoseRX: seq:" + sequence + " ts:" + timestamp + " sg:" + glucose + " psg: " + predicted_glucose + " do:" + glucoseIsDisplayOnly + " ss:" + status + " sr:" + status_raw + " st:" + CalibrationState.parse(state) + " tr:" + getTrend());

            }
        } else {
            UserError.Log.d(TAG, "GlucoseRxMessage packet length received wrong: " + packet.length);
        }

    }

    @Override
    public Integer getPredictedGlucose() {
        return predicted_glucose;
    }
}
