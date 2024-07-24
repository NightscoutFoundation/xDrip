package com.eveningoutpost.dexdrip.services;
import java.util.Date;

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.annotations.Expose;
import com.mongodb.BasicDBObject;
/**
 * Created by John Costik on 6/7/14.
 */
public class TransmitterRawData {


    @Expose
    private long _id;

    @Expose
    public String Id;
    @Expose
    public int TransmissionId;
    @Expose
    public String TransmitterId;
    @Expose
    public int RawValue;
    @Expose
    public int FilteredValue;
    @Expose
    public int BatteryLife;
    @Expose
    public int ReceivedSignalStrength;
    @Expose
    public long CaptureDateTime;
    @Expose
    public int Uploaded;
    @Expose
    public int UploadAttempts;
    @Expose
    public int UploaderBatteryLife;
    // When sending set this value to the relative time...
    // The time between the capture and now...
    @Expose
    public long RelativeTime;
    @Expose
    public String GeoLocation;

    public int getTransmissionId() {
        return TransmissionId;
    }

    public void setTransmissionId(int transmissionId) {
        TransmissionId = transmissionId;
    }

    public int getUploaded() {
        return Uploaded;
    }

    public void setUploaded(int uploaded) {
        Uploaded = uploaded;
    }

    public int getUploadAttempts() {
        return UploadAttempts;
    }

    public void setUploadAttempts(int uploadAttempts) {
        UploadAttempts = uploadAttempts;
    }

    public int getUploaderBatteryLife() {
        return UploaderBatteryLife;
    }

    public void setUploaderBatteryLife(int uploaderBatteryLife) {
        UploaderBatteryLife = uploaderBatteryLife;
    }

    public int getBatteryLife() {
        return BatteryLife;
    }

    public void setBatteryLife(int batteryLife) {
        BatteryLife = batteryLife;
    }

    public int getReceivedSignalStrength() {
        return ReceivedSignalStrength;
    }

    public void setReceivedSignalStrength(int receivedSignalStrength) {
        ReceivedSignalStrength = receivedSignalStrength;
    }

    public String getTransmitterId() {
        return TransmitterId;
    }

    public void setTransmitterId(String transmitterId) {
        TransmitterId = transmitterId;
    }

    public int getRawValue() {
        return RawValue;
    }

    public void setRawValue(int rawValue) {
        RawValue = rawValue;
    }

    public int getFilteredValue() {
        return FilteredValue;
    }

    public void setFilteredValue(int filteredValue) {
        FilteredValue = filteredValue;
    }

    public long getCaptureDateTime() {
        return CaptureDateTime;
    }

    public void setCaptureDateTime(long captureDateTime) {
        CaptureDateTime = captureDateTime;
    }

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }

    private Long getRelativeTime() {
        return RelativeTime;
    }


    @Override
    public String toString() {
      return JoH.defaultGsonInstance().toJson(this);
    }
/*
    public TransmitterRawData(String id, String raw, String filter, String battery, String rssi, int uploaderBattery){
        RawValue = Integer.parseInt(raw);
        FilteredValue = Integer.parseInt(filter);
        TransmitterId = id;
        BatteryLife = Integer.parseInt(battery);
        ReceivedSignalStrength = Integer.parseInt(rssi);
        CaptureDateTime = new Date().getTime();
        UploaderBatteryLife = uploaderBattery;

        Uploaded = 0;
        UploadAttempts = 1;
    }

    public TransmitterRawData(byte[] buffer, int len, Context context){

        StringBuilder toParse = new StringBuilder();
        for (int i = 0; i < len; ++i) {
            toParse.append((char) buffer[i]);
        }
        String[] parsed = toParse.toString().split("\\s+");

        RawValue = Integer.parseInt(parsed[1]);
        FilteredValue = Integer.parseInt(parsed[2]);
        TransmitterId = parsed[0];
        BatteryLife = Integer.parseInt(parsed[3]);
        ReceivedSignalStrength = Integer.parseInt(parsed[4]);
        TransmissionId = Integer.parseInt(parsed[5]);
        CaptureDateTime = new Date().getTime();

        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        UploaderBatteryLife = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Uploaded = 0;
        UploadAttempts = 1;
    }
*/
    public String toTableString()
    {
        String displayDt = new Date(getCaptureDateTime()).toLocaleString() + System.getProperty("line.separator");
        String transmitterId = "Transmitter Id: " + getTransmitterId() + System.getProperty("line.separator");
        String transmissionId = "Transmission Id: " + getTransmissionId() + System.getProperty("line.separator");
        String rawVal = "Raw Value: " + getRawValue() + System.getProperty("line.separator");
        String filterVal = "Filtered Value: " + getFilteredValue() + System.getProperty("line.separator");
        String batteryVal = "Transmitter Battery: " + getBatteryLife() + " " + System.getProperty("line.separator");
        String signalVal = "RSSI: " + getReceivedSignalStrength() + " " + System.getProperty("line.separator");
        String uploadDeviceBatteryVal = "Uploader Battery: " + getUploaderBatteryLife() + " " + System.getProperty("line.separator");
        String uploaded = "Uploaded: " + getUploaded() + " " + System.getProperty("line.separator");
        String RelativeTime = "relateive time (seconds): " + getRelativeTime() / 1000 + " "+ System.getProperty("line.separator");

        return displayDt + transmitterId + transmissionId + rawVal + filterVal + batteryVal + signalVal + uploadDeviceBatteryVal + uploaded + RelativeTime;
    }

    public BasicDBObject toDbObj(String DebugInfo) {
    	BasicDBObject doc = new BasicDBObject("TransmissionId", TransmissionId).
    			append("TransmitterId", TransmitterId).
    			append("RawValue", RawValue).
    			append("FilteredValue", FilteredValue).
    			append("BatteryLife", BatteryLife).
    			append("ReceivedSignalStrength", ReceivedSignalStrength).
    			append("CaptureDateTime", CaptureDateTime).
    			append("UploaderBatteryLife", UploaderBatteryLife).
    			append("DebugInfo", DebugInfo);
    	return doc;
    }

    public TransmitterRawData(BasicDBObject src) {
    	TransmissionId = src.getInt("TransmissionId");
    	TransmitterId  = src.getString("TransmitterId");
    	RawValue       = src.getInt("RawValue");
    	FilteredValue  = src.getInt("FilteredValue");
    	BatteryLife    = src.getInt("BatteryLife");
    	ReceivedSignalStrength = src.getInt("ReceivedSignalStrength");
    	CaptureDateTime = src.getLong("CaptureDateTime");
    	UploaderBatteryLife = src.getInt("UploaderBatteryLife");
    }


}
