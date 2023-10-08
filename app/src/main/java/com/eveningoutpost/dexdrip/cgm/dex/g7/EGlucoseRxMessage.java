package com.eveningoutpost.dexdrip.cgm.dex.g7;

import com.eveningoutpost.dexdrip.g5model.CalibrationState;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;
import lombok.val;

public class EGlucoseRxMessage extends BaseMessage {

    private static final String TAG = EGlucoseRxMessage.class.getSimpleName();
    public static final byte opcode = 0x4e;

    private Integer predicted_glucose;

    public int status_raw;
    public long clock;
    public long timestamp;
    public int unfiltered;
    public int filtered;
    public int sequence;
    public boolean glucoseIsDisplayOnly;
    public int glucose;
    public int state;
    public int trend;
    public int age;

    @Getter
    public boolean valid;

    /**
     * JamOrHam
     *
     * @param packet
     */

    public EGlucoseRxMessage(final byte[] packet) {
        UserError.Log.d(TAG, "EGlucoseRX dbg: " + JoH.bytesToHex(packet));
        if (packet.length >= 19) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode)) {

                status_raw = data.get();
                clock = getUnsignedInt(data);
                sequence = getUnsignedShort(data);
                val bogus = getUnsignedShort(data);

                age = getUnsignedShort(data);

                timestamp = JoH.tsl() - (age * Constants.SECOND_IN_MS); // TODO assumes processed now

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

                valid = true; // TODO do better here!

                UserError.Log.d(TAG, toString());

            }
        } else {
            UserError.Log.d(TAG, "GlucoseRxMessage packet length received wrong: " + packet.length);
        }

    }

    public Integer getPredictedGlucose() {
        return predicted_glucose;
    }

    @Override
    public String toString() {
        return "EGlucoseRX: seq:" + sequence + " ts:" + JoH.dateTimeText(timestamp) + " sg:" + glucose + " psg: " + predicted_glucose + " do:" + glucoseIsDisplayOnly + " ss:" + status_raw + " sr:" + status_raw + " st:" + calibrationState() + " tr:" + getTrend();

    }

    public Double getTrend() {
        return trend != 127 ? ((double) trend) / 10d : Double.NaN;
    }

    CalibrationState calibrationState() {
        return CalibrationState.parse(state);
    }

    public boolean usable() {
        return calibrationState().usableGlucose();
    }
}



