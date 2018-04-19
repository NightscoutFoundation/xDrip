package com.eveningoutpost.dexdrip.G5Model;

// created by jamorham

import android.util.SparseArray;

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
    Errors(0x12, "Errors");

    byte value;
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
        return result != null ? result : Unknown;
    }

    public static CalibrationState parse(int state) {
        return parse((byte) state);
    }

    public boolean usableGlucose() {
        return this == Ok
                || this == NeedsCalibration;
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
        return this == SensorFailed;
    }

    public String getText() {
        return text;
    }

}
