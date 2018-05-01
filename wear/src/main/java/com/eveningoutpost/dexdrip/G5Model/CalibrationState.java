package com.eveningoutpost.dexdrip.G5Model;

// created by jamorham

import android.util.SparseArray;

import com.eveningoutpost.dexdrip.Models.UserError;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.Services.G5CollectionService.TAG;

public enum CalibrationState {

    // TODO i18n

    Unknown(0x00, "Unknown"),
    Stopped(0x01, "Stopped"),
    WarmingUp(0x02, "Warming Up"),
    NeedsFirstCalibration(0x04, "Needs Initial Calibration"),
    NeedsSecondCalibration(0x05, "Needs Second Calibration"),
    Ok(0x06, "OK"),
    NeedsCalibration(0x07, "Needs Calibration"),
    NeedsDifferentCalibration(0x0a, "Needs More Calibration"),
    SensorFailed(0x0b, "Sensor Failed"),
    SensorFailed2(0x0c, "Sensor Failed 2"),
    UnusualCalibration(0x0d, "Unusual Calibration"),
    InsufficientCalibration(0x0e, "Insufficient Calibration"),
    Ended(0x0f, "Ended"),
    Errors(0x12, "Errors");

    @Getter
    byte value;
    @Getter
    String text;


    private static final SparseArray<CalibrationState> lookup = new SparseArray<>();

    CalibrationState(int value, String text) {
        this.value = (byte) value;
        this.text = text;
    }

    static {
        for (CalibrationState state : values()) {
            lookup.put(state.value, state);
        }
    }

    public static CalibrationState parse(byte state) {
        final CalibrationState result = lookup.get(state);
        if (result == null) UserError.Log.e(TAG, "Unknown calibration state: " + state);
        return result != null ? result : Unknown;
    }

    public static CalibrationState parse(int state) {
        return parse((byte) state);
    }

    public boolean usableGlucose() {
        return this == Ok
                || this == NeedsCalibration;
    }

    public boolean insufficientCalibration() {
        return this == InsufficientCalibration;
    }

    public boolean readyForCalibration() {
        return this == Ok
                || needsCalibration();
    }

    public boolean needsCalibration() {
        return this == NeedsCalibration
                || this == NeedsFirstCalibration
                || this == NeedsSecondCalibration
                || this == NeedsDifferentCalibration;
    }

    public boolean sensorStarted() {
        return readyForCalibration() || this == WarmingUp;
    }

    public boolean sensorFailed() {
        return this == SensorFailed || this == SensorFailed2;
    }

    public boolean readyForBackfill() {
        return this != WarmingUp && this != Stopped && this != Unknown && this != NeedsFirstCalibration && this != NeedsSecondCalibration;
    }
}
