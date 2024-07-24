package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.cgm.medtrum.SensorState;
import com.eveningoutpost.dexdrip.cgm.medtrum.TimeKeeper;
import com.google.gson.annotations.Expose;

import lombok.Getter;

// jamorham

@SuppressWarnings("WeakerAccess")
public class AnnexARx extends BaseMessage {

    final byte[] packet;
    boolean extended = false;

    public final long created = JoH.tsl();

    @Expose
    int spacerByte = -1;

    int status1 = 0;
    @Expose
    int unknownItem = 0;
    int bitFieldArray = 0;
    @Expose
    @Getter
    int batteryPercent = 0;
    @Expose
    public int sensorAge = 0;
    @Expose
    public int referenceCounter = 0;
    @Expose
    @Getter
    public int sensorRaw = 0;
    @Expose
    public int glucoseRate = 0;
    @Expose
    public int calibrationIntercept = 0;
    @Expose
    public int calibrationSlope = 0;
    @Expose
    int sensorState = -1;
    @Expose
    int riseFallType;
    @Expose
    int levelAlertType;
    @Expose
    public boolean sensorGood;
    @Expose
    public boolean sensorFail;
    @Expose
    boolean possibleAlarmState;
    @Expose
    public boolean sensorError;
    @Expose
    public boolean calibrationErrorA;
    @Expose
    public boolean calibrationErrorB;
    @Expose
    public boolean charging;
    @Expose
    public boolean charged;

    @Expose
    int calibationSequence = -1;
    @Expose
    long secondsA;
    @Expose
    long secondsB;

    @Expose
    public long referenceMs = -1;


    public AnnexARx(byte[] packet) {
        this.packet = packet;
        if (packet == null || !validLength()) return;
        wrap(packet);

        extended = extendedLength();

        if (extended) {
            spacerByte = getUnsignedByte();
        }

        status1 = getUnsignedByte();
        bitFieldArray = getUnsignedShort();
        batteryPercent = getUnsignedByte();

        if (extended) {
            // order swapped on extended packet  ¯\_(ツ)_/¯
            referenceCounter = getUnsignedShort();
            sensorAge = getUnsignedShort(); // 2 minute intervals
        } else {
            sensorAge = getUnsignedShort(); // 2 minute intervals
            referenceCounter = getUnsignedShort();
        }

        sensorRaw = getUnsignedShort(); // maybe sensor activation id - or is this raw?

        // first 10-11 bytes above
        if (extended || notificationLength()) {
            // skip 6 bytes
            data.position(data.position() + 6);
        }

        glucoseRate = getUnsignedByte();
        calibrationIntercept = data.get();
        calibrationSlope = getUnsignedShort();

        sensorState = status1 & 0x03;
        unknownItem = status1 >> 2;
        sensorGood = statusBit(3);
        calibrationErrorA = statusBit(4);
        calibrationErrorB = statusBit(5);
        sensorFail = statusBit(6);
        possibleAlarmState = statusBit(7);
        sensorError = statusBit(8);
        charged = statusBit(9);
        charging = statusBit(10); // TODO check possible mismatch if charged but not charging
        riseFallType = bitFieldArray >> 14 & 3; // 1 = rise, 2 = fall
        levelAlertType = bitFieldArray >> 11 & 7; // 1 = high, 2 = low, 5 = warning

        if (extended) {
            secondsA = getUnsignedInt();
            secondsB = getUnsignedInt();
            referenceMs = (secondsA - secondsB) * 1000; // TODO check this is definitely correct order
            calibationSequence = getUnsignedShort();
        }

    }

    // TODO handle GLUCOSE RATE

    public SensorState getState() {
        switch (sensorState) {
            case 0:
                return SensorState.NotConnected;
            case 1:
                return SensorState.WarmingUp1;
            case 2:
                return SensorState.WarmingUp2;
            case 3:
                if (calibrationSlope == 0) {
                    return SensorState.NotCalibrated;
                } else {
                    return SensorState.Ok;
                }
            default:
                return SensorState.ErrorUnknown;

        }
    }

    boolean statusBit(final int n) {
        return isBitSet(bitFieldArray, n);
    }

    boolean validLength() {
        return packet.length == 20 || packet.length == 14 || packet.length == 31 || packet.length == 33;
    }

    boolean extendedLength() {
        return packet.length == 31 || packet.length == 33;
    }

    boolean notificationLength() {
        return packet.length == 20;
    }

    public boolean processForTimeKeeper(long serial) {
        if (referenceMs > 0) {
            final long time = JoH.tsl() - referenceMs;
            UserError.Log.d(TAG, "ReferenceMS equates to: " + JoH.dateTimeText(time));
            TimeKeeper.setTime(serial, time);
            return true;
        } else {
            UserError.Log.d(TAG, "ReferenceMS invalid at: " + referenceMs);
        }
        return false;
    }

    public long getSensorAgeInMs() {
        return sensorAge * 120000;
    }

    public String getNiceSensorAge() {
        return JoH.niceTimeScalar(getSensorAgeInMs(), 1);
    }

    public int getSensorRawEmulateDex() {
        return getSensorRawEmulateDex(sensorRaw);
    }

    public double calculatedGlucose() {
        if (calibrationSlope == 0 || sensorRaw == 0 || getState() != SensorState.Ok) return -1;
        return performCalculation(sensorRaw, calibrationSlope, calibrationIntercept);
    }

    public boolean isStateOkForBackFill() {
        return getState() == SensorState.Ok && getSensorAgeInMs() > Constants.MINUTE_IN_MS * 150;
    }

    public boolean recent() {
        return JoH.msSince(created) < (Constants.MINUTE_IN_MS * 3);
    }

    @Override
    public String toS() {
        return super.toS() + " Sensor State: " + getState();
    }

}
