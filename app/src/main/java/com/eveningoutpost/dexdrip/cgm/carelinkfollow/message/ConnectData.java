package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.List;

/**
 * CareLink ConnectData message
 */
public class ConnectData {

    public String lastSensorTSAsString;
    public String medicalDeviceTimeAsString;
    public String bgunits;
    public long lastSensorTS;
    public String kind;
    public long version;
    public long currentServerTime;
    public long lastConduitTime;
    public long lastConduitUpdateServerTime;
    public long lastMedicalDeviceDataUpdateServerTime;
    public String firstName;
    public String lastName;
    public String conduitSerialNumber;
    public int conduitBatteryLevel;
    public String conduitBatteryStatus;
    public boolean conduitInRange;
    public boolean conduitMedicalDeviceInRange;
    public boolean conduitSensorInRange;
    public String medicalDeviceFamily;
    public String sensorState;
    public String medicalDeviceSerialNumber;
    public long medicalDeviceTime;
    public String sMedicalDeviceTime;
    public int reservoirLevelPercent;
    public long reservoirAmount;
    public int medicalDeviceBatteryLevelPercent;
    public int sensorDurationHours;
    public int timeToNextCalibHours;
    public String calibStatus;
    public String bgUnits;
    public String timeFormat;
    public int lastSensorTime;
    public String sLastSensorTime;
    public boolean medicalDeviceSuspended;
    public String lastSGTrend;
    public SensorGlucose lastSG;
    public Alarm lastAlarm;
    public ActiveInsulin activeInsulin;
    public List<SensorGlucose> sgs;
    public List<Marker> markers;

    public ConnectData() {

    }

}