package lwld.glucose.profile.packet;

import java.util.HashSet;
import java.util.stream.Collectors;

import no.nordicsemi.android.ble.common.profile.cgm.CGMTypes;

/**
 * JamOrHam
 * <p>
 * CGM status converter
 */

public enum Status {

    SESSION_STOPPED,
    DEVICE_BATTERY_LOW,
    SENSOR_TYPE_INCORRECT_FOR_DEVICE,
    SENSOR_MALFUNCTION,
    DEVICE_SPECIFIC_ALERT,
    GENERAL_DEVICE_FAULT,
    TIME_SYNC_REQUIRED,
    CALIBRATION_NOT_ALLOWED,
    CALIBRATION_RECOMMENDED,
    CALIBRATION_REQUIRED,
    CALIBRATION_ERROR,
    SENSOR_TEMPERATURE_TOO_HIGH,
    SENSOR_TEMPERATURE_TOO_LOW,
    SENSOR_RESULT_LOWER_THEN_PATIENT_LOW_LEVEL,
    SENSOR_RESULT_HIGHER_THEN_PATIENT_HIGH_LEVEL,
    SENSOR_RESULT_LOWER_THEN_HYPO_LEVEL,
    SENSOR_RESULT_HIGHER_THEN_HYPER_LEVEL,
    SENSOR_RATE_OF_DECREASE_EXCEEDED,
    SENSOR_RATE_OF_INCREASE_EXCEEDED,
    SENSOR_RESULT_LOWER_THEN_DEVICE_CAN_PROCESS,
    SENSOR_RESULT_HIGHER_THEN_DEVICE_CAN_PROCESS;

    public static HashSet<Status> fromCGMStatus(CGMTypes.CGMStatus status) {
        if (status == null) return null;
        HashSet<Status> result = new HashSet<>();
        if (status.sessionStopped) result.add(SESSION_STOPPED);
        if (status.deviceBatteryLow) result.add(DEVICE_BATTERY_LOW);
        if (status.sensorTypeIncorrectForDevice) result.add(SENSOR_TYPE_INCORRECT_FOR_DEVICE);
        if (status.sensorMalfunction) result.add(SENSOR_MALFUNCTION);
        if (status.deviceSpecificAlert) result.add(DEVICE_SPECIFIC_ALERT);
        if (status.generalDeviceFault) result.add(GENERAL_DEVICE_FAULT);
        if (status.timeSyncRequired) result.add(TIME_SYNC_REQUIRED);
        if (status.calibrationNotAllowed) result.add(CALIBRATION_NOT_ALLOWED);
        if (status.calibrationRecommended) result.add(CALIBRATION_RECOMMENDED);
        if (status.calibrationRequired) result.add(CALIBRATION_REQUIRED);
        if (status.sensorTemperatureTooHigh) result.add(SENSOR_TEMPERATURE_TOO_HIGH);
        if (status.sensorTemperatureTooLow) result.add(SENSOR_TEMPERATURE_TOO_LOW);
        if (status.sensorResultLowerThenPatientLowLevel)
            result.add(SENSOR_RESULT_LOWER_THEN_PATIENT_LOW_LEVEL);
        if (status.sensorResultHigherThenPatientHighLevel)
            result.add(SENSOR_RESULT_HIGHER_THEN_PATIENT_HIGH_LEVEL);
        if (status.sensorResultLowerThenHypoLevel) result.add(SENSOR_RESULT_LOWER_THEN_HYPO_LEVEL);
        if (status.sensorResultHigherThenHyperLevel)
            result.add(SENSOR_RESULT_HIGHER_THEN_HYPER_LEVEL);
        if (status.sensorRateOfDecreaseExceeded) result.add(SENSOR_RATE_OF_DECREASE_EXCEEDED);
        if (status.sensorRateOfIncreaseExceeded) result.add(SENSOR_RATE_OF_INCREASE_EXCEEDED);
        if (status.sensorResultLowerThenDeviceCanProcess)
            result.add(SENSOR_RESULT_LOWER_THEN_DEVICE_CAN_PROCESS);
        if (status.sensorResultHigherThenDeviceCanProcess)
            result.add(SENSOR_RESULT_HIGHER_THEN_DEVICE_CAN_PROCESS);
        return result;
    }

    public static String getStatusString(HashSet<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "";
        }
        return statuses.stream()
                .map(status -> status.name().replace('_', ' ').toLowerCase())
                .collect(Collectors.joining(", "));
    }

}
