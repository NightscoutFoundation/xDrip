package com.eveningoutpost.dexdrip.g5model;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;
import lombok.val;


public class EGlucoseRxMessage2 extends BaseGlucoseRxMessage {

    private static final String TAG = EGlucoseRxMessage2.class.getSimpleName();
    public static final byte opcode = 0x4e;

    private Integer predicted_glucose;

    public int age;
    public int info;

    @Getter
    public boolean valid;

    /**
     * JamOrHam
     *
     * @param packet
     */

    public EGlucoseRxMessage2(final byte[] packet) {
        UserError.Log.d(TAG, "EGlucoseRX2 dbg: " + JoH.bytesToHex(packet));
        if (packet.length >= 19) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode)) {

                status_raw = data.get();
                timestamp = (int) getUnsignedInt(data);
                sequence = getUnsignedShort(data);
                val bogus = getUnsignedShort(data);

                age = getUnsignedShort(data);

                //timestamp = JoH.tsl() - (age * Constants.SECOND_IN_MS); // TODO assumes processed now

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
                if (predicted_glucose == 0x3FF) {
                    predicted_glucose = -1; // invalid
                }

                info = getUnsignedByte(data);

                valid = true; // TODO

                UserError.Log.d(TAG, toString());

            }
        } else {
            UserError.Log.d(TAG, "GlucoseRxMessage packet length received wrong: " + packet.length);
        }

    }

    public Integer getPredictedGlucose() {
        return predicted_glucose;
    }

    public Long getRealSessionStartTime() {
        if (calibrationState().sensorStarted() && timestamp > 0) {
            return tsl() - (timestamp * Constants.SECOND_IN_MS);
        }
        return null;
    }

    public String getRealSessionStartTimeString() {
        val t = getRealSessionStartTime();
        if (t != null) {
            return JoH.dateTimeText(t);
        } else {
            return "N/A";
        }
    }

    @Override
    public String toString() {
        return "EGlucoseRX2: seq:" + sequence + " ts:" + JoH.niceTimeScalar(timestamp * Constants.SECOND_IN_MS, 2) + " sg:" + glucose + " psg: " + predicted_glucose + " do:" + glucoseIsDisplayOnly + " sr:" + status_raw + " st:" + calibrationState() + " info:" + info + " tr:" + getTrend() + " age:" + age + " Start: " + getRealSessionStartTimeString();

    }

}

