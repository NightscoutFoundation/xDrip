package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;
import java.util.List;

public class PatientData {

    public static final String DEVICE_FAMILY_GUARDIAN = "GUARDIAN";
    public static final String DEVICE_FAMILY_NGP = "NGP";

    //sensorState
    public static final String SENSOR_STATE_CALIBRATION_REQUIRED = "CALIBRATION_REQUIRED";
    public static final String SENSOR_STATE_NO_DATA_FROM_PUMP = "NO_DATA_FROM_PUMP";
    public static final String SENSOR_STATE_WAIT_TO_CALIBRATE = "WAIT_TO_CALIBRATE";
    public static final String SENSOR_STATE_DO_NOT_CALIBRATE = "DO_NOT_CALIBRATE";
    public static final String SENSOR_STATE_CALIBRATING = "CALIBRATING";
    public static final String SENSOR_STATE_NO_ERROR_MESSAGE = "NO_ERROR_MESSAGE";
    public static final String SENSOR_STATE_WARM_UP = "WARM_UP";
    public static final String SENSOR_STATE_CHANGE_SENSOR = "CHANGE_SENSOR";
    public static final String SENSOR_STATE_NORMAL = "NORMAL";
    public static final String SENSOR_STATE_UNKNOWN = "UNKNOWN";

    //systemStatusMessage
    public static final String SYSTEM_STATUS_BLUETOOTH_OFF = "BLUETOOTH_OFF";
    public static final String SYSTEM_STATUS_UPDATING = "UPDATING";
    public static final String SYSTEM_STATUS_RECONNECTING_TO_PUMP = "RECONNECTING_TO_PUMP";
    public static final String SYSTEM_STATUS_PUMP_PAIRING_LOST = "PUMP_PAIRING_LOST";
    public static final String SYSTEM_STATUS_LOST_PUMP_SIGNAL = "LOST_PUMP_SIGNAL";
    public static final String SYSTEM_STATUS_PUMP_NOT_PAIRED = "PUMP_NOT_PAIRED";
    public static final String SYSTEM_STATUS_SENSOR_OFF = "SENSOR_OFF";
    public static final String SYSTEM_STATUS_NO_ERROR_MESSAGE = "NO_ERROR_MESSAGE";
    public static final String SYSTEM_STATUS_CALIBRATION_REQUIRED = "CALIBRATION_REQUIRED";

    //autoModeShieldState
    public static final String AUTOMODE_SHIELD_AUTO_BASAL = "AUTO_BASAL";
    public static final String AUTOMODE_SHIELD_FEATURE_OFF = "FEATURE_OFF";

    //autoModeReadinessState
    public static final String AUTOMODE_READINESS_NO_ACTION_REQUIRED = "NO_ACTION_REQUIRED";

    //plgmLgsState
    public static final String LGS_STATE_FEATURE_OFF = "FEATURE_OFF";
    public static final String LGS_STATE_MONITORING = "MONITORING";


    public String getDeviceFamily() {
        return medicalDeviceFamily;
    }

    public boolean isGM() {
        return getDeviceFamily().equals(DEVICE_FAMILY_GUARDIAN);
    }

    public boolean isNGP() {
        return getDeviceFamily().equals(DEVICE_FAMILY_NGP);
    }

    public String clientTimeZoneName;   // new
    public String lastName;
    public String firstName;
    public String appModelType;         // new
    public String appModelNumber;       // new
    public long currentServerTime;
    public String conduitSerialNumber;
    public int conduitBatteryLevel;
    public String conduitBatteryStatus;
    public String lastConduitDateTime;  // Changed from Date and "2024-12-22T15:17:51.616+01:00" to String and "2025-01-16T23:47:22"
    public long lastConduitUpdateServerDateTime;  // new
    public String medicalDeviceFamily;
    public MedicalDeviceInformation medicalDeviceInformation;  // new
    public long medicalDeviceTime;
    public long lastMedicalDeviceDataUpdateServerTime;
    public CgmInfo cgmInfo;
    public Boolean calFreeSensor;
    public String calibStatus;
    public String calibrationIconId;  // new
    public int timeToNextEarlyCalibrationMinutes;  // new
    public int timeToNextCalibrationMinutes;
    public int timeToNextCalibrationRecommendedMinutes;  // new
    public int timeToNextCalibHours;
    public Boolean finalCalibration;  // new
    public int sensorDurationMinutes;
    public int sensorDurationHours;
    public String transmitterPairedTime;  // new
    public long systemStatusTimeRemaining;  // new
    public int gstBatteryLevel;
    public List<PumpBannerState> pumpBannerState;
    public TherapyAlgorithmState therapyAlgorithmState;
    public int reservoirLevelPercent;
    public int reservoirAmount;
    public Boolean pumpSuspended;  // new
    public int pumpBatteryLevelPercent;  // new
    public float reservoirRemainingUnits;
    public Boolean conduitInRange;
    public Boolean conduitMedicalDeviceInRange;
    public Boolean conduitSensorInRange;
    public String systemStatusMessage;
    public String sensorState;
    public Boolean gstCommunicationState;
    public Boolean pumpCommunicationState;
    public String timeFormat;
    public String bgUnits;
    public float maxAutoBasalRate;
    public float maxBolusAmount;
    public int sgBelowLimit;
    public Boolean approvedForTreatment;  // new
    public Alarm lastAlarm;
    public ActiveInsulin activeInsulin;
    public Basal basal;
    public long lastSensorTime;
    public SensorGlucose lastSG;
    public String lastSGTrend;
    public List<Limit> limits;
    public int belowHypoLimit;
    public int aboveHyperLimit;
    public int timeInRange;
    public float averageSGFloat;
    public int averageSG;
    public List<Marker> markers;
    public List<SensorGlucose> sgs;
    public NotificationHistory notificationHistory;
    public String sensorLifeText;
    public String sensorLifeIcon;


    // Used previously but no longer provided
    public String medicalDeviceTimeAsString;
    public Date medicalDeviceTimeAsDate;
    public String lastSensorTSAsString;
    public Date lastSensorTSAsDate;
    public String sMedicalDeviceTime;
    public Date dMedicalDeviceTime;
    public int medicalDeviceBatteryLevelPercent;
    public String sLastSensorTime;
    public Date dLastSensorTime;

}
