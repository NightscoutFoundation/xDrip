package com.eveningoutpost.dexdrip.Services;

import com.google.gson.annotations.Expose;
import com.mongodb.BasicDBObject;

// This class contains data that is received for all house coverage.
// The must fields are the raw data read from the sensor, and timestamp (and relative time).
public class LibreWifiData {
    
    @Expose
    String BlockBytes; // The base 64 encoded FARM
    
    @Expose
    Long CaptureDateTime;
    
    @Expose
    public long RelativeTime;
    
    @Expose
    int ChecksumOk ;
    
    @Expose
    String DebugInfo;
    
    @Expose
    int TomatoBatteryLife;
    
    @Expose
    int UploaderBatteryLife;
    
    @Expose
    int Uploaded;
    
    @Expose
    int HwVersion;
    
    @Expose
    int FwVersion;
    
    @Expose
    String SensorId;
    
    public Long getCaptureDateTime() {
        return CaptureDateTime;
    }
    
    public LibreWifiData(BasicDBObject src) {

        BlockBytes = src.getString("BlockBytes");
        CaptureDateTime = src.getLong("CaptureDateTime");
        ChecksumOk = src.getInt("ChecksumOk");
        DebugInfo = src.getString("DebugInfo");
        TomatoBatteryLife = src.getInt("TomatoBatteryLife");
        UploaderBatteryLife = src.getInt("UploaderBatteryLife");
        Uploaded = src.getInt("Uploaded");
        HwVersion = src.getInt("HwVersion");
        FwVersion = src.getInt("FwVersion");
        SensorId = src.getString("SensorId");
    }
    
    
    @Override
    public String toString() {
        return "LibreWifiData [BlockBytes=" + BlockBytes + ", CaptureDateTime=" + CaptureDateTime + ", RelativeTime="
                + RelativeTime + ", ChecksumOk=" + ChecksumOk + ", DebugInfo=" + DebugInfo + ", TomatoBatteryLife="
                + TomatoBatteryLife + ", UploaderBatteryLife=" + UploaderBatteryLife + ", Uploaded=" + Uploaded
                + ", HwVersion=" + HwVersion + ", FwVersion=" + FwVersion + ", SensorId=" + SensorId + "]";
    }
    
    
}
