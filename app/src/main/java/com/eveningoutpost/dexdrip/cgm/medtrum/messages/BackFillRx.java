package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

// jamorham

import com.eveningoutpost.dexdrip.models.UserError;
import com.google.gson.annotations.Expose;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_BACK_REPLY;

public class BackFillRx extends BaseMessage {

    @Getter
    @Expose
    boolean valid = false;
    int replyStatus = -1;
    int opcode = -1;
    int status = -1;
    @Expose
    public int sequenceStart = -1;
    int bitFieldArray = 0;
    @Expose
    int intercept = 0;
    @Expose
    int slope = 0;
    @Expose
    int recordCount = 0;
    @Expose
    boolean sensorError = false;
    @Expose
    boolean notCalibrated = false;
    @Expose
    boolean sensorFailed = false;
    @Expose
    boolean calibrationError = false;

    List<Integer> raw;


    public BackFillRx(final byte[] packet) {


        if (packet == null || packet.length < 11) return;
        wrap(packet);

        status = getUnsignedByte();
        opcode = getUnsignedByte();
        replyStatus = getUnsignedShort(); // typically 0x0000 = received

        if (status == 0x84 && opcode == OPCODE_BACK_REPLY && replyStatus == 0) {

            sequenceStart = getUnsignedShort();
            bitFieldArray = getUnsignedByte();
            recordCount = getUnsignedByte();
            intercept = data.get();
            slope = getUnsignedShort();

            // bits 0,1 unknown
            calibrationError = statusBit(2) || statusBit(3);
            notCalibrated = statusBit(4);
            sensorFailed = statusBit(5) || statusBit(6);
            sensorError = statusBit(7);

            valid = true; // TODO validate above more?
        }
    }

    private boolean statusBit(final int n) {
        return isBitSet(bitFieldArray, n);
    }

    // if we should process this packet
    public boolean isOk() {
        return (valid && !sensorFailed);
    }

    public double getGlucose(int sensorRaw) {
        if (!valid || sensorFailed || slope == 0 || sensorRaw == 0) return -1;
        return performCalculation(sensorRaw, slope, intercept);

    }

    public synchronized List<Integer> getRawList() {
        if (!valid) return null;
        if (raw == null) {
            try {
                raw = new ArrayList<>();
                for (int i = 0; i < recordCount; i++) {
                    raw.add(getUnsignedShort());
                }
                if (data.remaining() > 0) {
                    UserError.Log.e(TAG, "Excess data received in backfill packet: " + data.remaining() + " remaining");
                }
            } catch (BufferUnderflowException e) {
                UserError.Log.e(TAG, "Insufficient data received in broken backfill packet: " + toS());
                valid = false;
                return null;
            }
        }
        return raw;
    }

    @Override
    public String toS() {
        final StringBuilder raws = new StringBuilder(" Raw: ");
        if (getRawList() != null) {
            for (Integer x : getRawList()) {
                raws.append(" ").append(x);
            }
        } else {
            raws.append("NULL LIST");
        }
        return super.toS() + raws.toString();
    }

}
