package com.eveningoutpost.dexdrip.Services;

import java.util.Arrays;

import com.google.gson.annotations.Expose;
import com.mongodb.BasicDBObject;

// This class contains data that is received for all house coverage.
// The must fields are the raw data read from the sensor, and timestamp (and relative time).
public class LibreWifiData {
    
    LibreWifiData(){
        
    }
    
    @Expose
    String BlockBytes; // The base 64 encoded FRAM
    
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
    String HwVersion;
    
    @Expose
    String FwVersion;
    
    @Expose
    String SensorId;
    
    @Expose
    String patchUid; // The base 64 encoded patchUid
    
    @Expose
    String patchInfo; // The base 64 encoded patchInfo
    
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
        HwVersion = src.getString("HwVersion");
        FwVersion = src.getString("FwVersion");
        SensorId = src.getString("SensorId");
        patchUid = src.getString("patchUid");
        patchInfo = src.getString("patchInfo");
    }
    
    
    @Override
    public String toString() {
        return "LibreWifiData [BlockBytes=" + BlockBytes + ", CaptureDateTime=" + CaptureDateTime + ", RelativeTime="
                + RelativeTime + ", ChecksumOk=" + ChecksumOk + ", DebugInfo=" + DebugInfo + ", TomatoBatteryLife="
                + TomatoBatteryLife + ", UploaderBatteryLife=" + UploaderBatteryLife + ", Uploaded=" + Uploaded
                + ", HwVersion=" + HwVersion + ", FwVersion=" + FwVersion + ", SensorId=" + SensorId 
                + ", patchUid=" + patchUid + ", patchInfo=" + patchInfo +"]";
    }
    
}

class LibreWifiHeader {
    
    // Version of the reply
    @Expose
    int reply_version;
    
    // Maximum version of protocol supported by this version
    @Expose
    int max_protocol_version;
    
    // Time of last reading from libre (can be no sensor for example
    @Expose
    long last_reading;
    
    // Any debug message that wants to be displayed.
    @Expose
    String debug_message;
    
    @Expose
    String device_type;
    
    // The actual data
    @Expose
    LibreWifiData [] libre_wifi_data;

    @Override
    public String toString() {
        return "LibreWifiHeader [reply_version=" + reply_version + ", max_protocol_version=" + max_protocol_version
                + ", last_reading=" + last_reading + ", debug_message=" + debug_message + ", device_type=" + device_type
                + ", libre_wifi_data=" + Arrays.toString(libre_wifi_data) + "]";
    }

   

}
