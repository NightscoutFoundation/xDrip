package com.eveningoutpost.dexdrip.importedlibraries.dexcom.records;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.Dex_Constants;

import java.util.Date;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class GlucoseDataSet {

    private Date systemTime;
    private Date displayTime;
    private int bGValue;
    private Dex_Constants.TREND_ARROW_VALUES trend;
    private long unfiltered;
    private long filtered;
    private int rssi;

    public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
        // TODO check times match between record
        systemTime = egvRecord.getSystemTime();
        displayTime = egvRecord.getDisplayTime();
        bGValue = egvRecord.getBGValue();
        trend = egvRecord.getTrend();
        unfiltered = sensorRecord.getUnfiltered();
        filtered = sensorRecord.getFiltered();
        rssi = sensorRecord.getRSSI();
    }

    public Date getSystemTime() {
        return systemTime;
    }

    public Date getDisplayTime() {
        return displayTime;
    }

    public int getBGValue() {
        return bGValue;
    }

    public Dex_Constants.TREND_ARROW_VALUES getTrend() {
        return trend;
    }

    public String getTrendSymbol() {
        return trend.Symbol();
    }

    public long getUnfiltered() {
        return unfiltered;
    }

    public long getFiltered() {
        return filtered;
    }

    public int getRssi() {
        return rssi;
    }
}
