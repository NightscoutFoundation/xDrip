package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Date;
import java.util.List;

public class RecentData {

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

    public int getDeviceBatteryLevel(){
        if(pumpBatteryLevelPercent == 0)
            return  medicalDeviceBatteryLevelPercent;
        else
            return  pumpBatteryLevelPercent;

    }

    //As of new BLE endpoint v11:
    public Long lastConduitUpdateServerDateTime;

    public long lastSensorTS;
    public String medicalDeviceTimeAsString;
    public Date medicalDeviceTimeAsDate;
    public String lastSensorTSAsString;
    public Date lastSensorTSAsDate;
    public String kind;
    public int version;
    public String pumpModelNumber;
    public long currentServerTime;
    public long lastConduitTime;
    public long lastConduitUpdateServerTime;
    public long lastMedicalDeviceDataUpdateServerTime;
    public String firstName;
    public String lastName;
    public String conduitSerialNumber;
    public int conduitBatteryLevel;
    public String conduitBatteryStatus;
    public Boolean conduitInRange;
    public Boolean conduitMedicalDeviceInRange;
    public Boolean conduitSensorInRange;
    public String medicalDeviceFamily;
    public String sensorState;
    public String medicalDeviceSerialNumber;
    public long medicalDeviceTime;
    public String sMedicalDeviceTime;
    public Date dMedicalDeviceTime;
    public int reservoirLevelPercent;
    public int reservoirAmount;
    public float reservoirRemainingUnits;
    public int medicalDeviceBatteryLevelPercent;
    public int sensorDurationHours;
    public int timeToNextCalibHours;
    public String calibStatus;
    public String bgUnits;
    public String timeFormat;
    public long lastSensorTime;
    public String sLastSensorTime;
    public Date dLastSensorTime;
    public boolean medicalDeviceSuspended;
    public String lastSGTrend;
    public SensorGlucose lastSG;
    public Alarm lastAlarm;
    public ActiveInsulin activeInsulin;
    public List<SensorGlucose> sgs;
    public List<Limit> limits;
    public List<Marker> markers;
    public NotificationHistory notificationHistory;
    public TherapyAlgorithmState therapyAlgorithmState;
    public List<PumpBannerState> pumpBannerState;
    public Basal basal;
    public String systemStatusMessage;
    public int averageSG;
    public int belowHypoLimit;
    public int aboveHyperLimit;
    public int timeInRange;
    public Boolean pumpSuspended;
    public int pumpBatteryLevelPercent;
    public Boolean pumpCommunicationState;
    public Boolean gstCommunicationState;
    public int gstBatteryLevel;
    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date lastConduitDateTime;
    public float maxAutoBasalRate;
    public float maxBolusAmount;
    public int sensorDurationMinutes;
    public int timeToNextCalibrationMinutes;
    public String clientTimeZoneName;
    public int sgBelowLimit;
    public float averageSGFloat;
    public Boolean calFreeSensor;
    public Boolean finalCalibration;

}
