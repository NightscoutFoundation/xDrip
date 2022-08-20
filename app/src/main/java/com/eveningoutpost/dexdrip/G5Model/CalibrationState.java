package com.eveningoutpost.dexdrip.G5Model;

// created by jamorham

import android.util.SparseArray;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.Services.G5CollectionService.TAG;

public enum CalibrationState {

    // TODO i18n

    Unknown(0x00, "Unknown"),
    Stopped(0x01, "Stopped"),
    WarmingUp(0x02, "Warming Up"),
    ExcessNoise(0x03, "Excess Noise"),
    NeedsFirstCalibration(0x04, "Needs Initial Calibration"),
    NeedsSecondCalibration(0x05, "Needs Second Calibration"),
    Ok(0x06, "OK"),
    NeedsCalibration(0x07, "Needs Calibration"),
    CalibrationConfused1(0x08, "Confused Calibration 1"),
    CalibrationConfused2(0x09, "Confused Calibration 2"),
    NeedsDifferentCalibration(0x0a, "Needs More Calibration"),
    SensorFailed(0x0b, "Sensor Failed"),
    SensorFailed2(0x0c, "Sensor Failed 2"),
    UnusualCalibration(0x0d, "Unusual Calibration"),
    InsufficientCalibration(0x0e, "Insufficient Calibration"),
    Ended(0x0f, "Ended"),
    SensorFailed3(0x10, "Sensor Failed 3"),
    TransmitterProblem(0x11, "Transmitter Problem"),
    Errors(0x12, "Sensor Errors"),
    SensorFailed4(0x13, "Sensor Failed 4"),
    SensorFailed5(0x14, "Sensor Failed 5"),
    SensorFailed6(0x15, "Sensor Failed 6"),
    SensorFailedStart(0x16, "Sensor Failed Start"),
    SensorStarted(0xC1, "Sensor Started"),
    SensorStopped(0xC2, "Sensor Stopped"),
    CalibrationSent(0xC3, "Calibration Sent");

    @Getter
    byte value;
    @Getter
    String text;


    private static final SparseArray<CalibrationState> lookup = new SparseArray<>();
    private static final ImmutableSet<CalibrationState> failed = ImmutableSet.of(SensorFailed, SensorFailed2, SensorFailed3, SensorFailed4, SensorFailed5, SensorFailed6, SensorFailedStart);
    private static final ImmutableSet<CalibrationState> stopped = ImmutableSet.of(Stopped, Ended, SensorFailed, SensorFailed2, SensorFailed3, SensorFailed4, SensorFailed5, SensorFailed6, SensorFailedStart, SensorStopped);
    private static final ImmutableSet<CalibrationState> transitional = ImmutableSet.of(WarmingUp, SensorStarted, SensorStopped, CalibrationSent);


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
        return !stopped.contains(this);
    }

    public boolean sensorFailed() {
        return failed.contains(this);
    }

    public boolean ended() {
        return this == Ended;
    }

    public boolean warmingUp() {
        return this == WarmingUp;
    }

    public boolean transitional() { return transitional.contains(this); }

    public boolean ok() {
        return this == Ok;
    }

    public boolean readyForBackfill() {
        return this != WarmingUp && this != Stopped && this != Unknown && this != NeedsFirstCalibration && this != NeedsSecondCalibration && this != Errors;
    }

    // TODO i18n
    public String getExtendedText() {
        switch (this) {
            case Ok:
                if (DexSessionKeeper.isStarted()) {
                    return getText() + " " + DexSessionKeeper.prettyTime();
                } else {
                    return getText() + " time?";
                }
            case WarmingUp:
                if (DexSessionKeeper.isStarted()) {
                    if (DexSessionKeeper.warmUpTimeValid()) {
                        return getText() + "\n" + DexSessionKeeper.prettyTime() + " left";
                    } else {
                        return getText();
                    }
                } else {
                    return getText();
                }

            default:
                return getText();
        }
    }
}
