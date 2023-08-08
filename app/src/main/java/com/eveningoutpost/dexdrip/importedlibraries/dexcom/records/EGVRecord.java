package com.eveningoutpost.dexdrip.importedlibraries.dexcom.records;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.Dex_Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class EGVRecord extends GenericTimestampRecord {

    private int bGValue;
    private int noise;
    private Dex_Constants.TREND_ARROW_VALUES trend;

    public EGVRecord(byte[] packet) {
        // system_time (UInt), display_time (UInt), glucose (UShort), trend_arrow (Byte), crc (UShort))
        super(packet);
        bGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8) & Dex_Constants.EGV_VALUE_MASK;
        byte trendAndNoise = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).get(10);
        int trendValue = trendAndNoise & Dex_Constants.EGV_TREND_ARROW_MASK;
        byte noiseValue = (byte) ((trendAndNoise & Dex_Constants.EGV_NOISE_MASK) >> 4);
        trend = Dex_Constants.TREND_ARROW_VALUES.values()[trendValue];
        noise = noiseValue;
    }

    public EGVRecord(int bGValue, Dex_Constants.TREND_ARROW_VALUES trend, Date displayTime, Date systemTime){
        super(displayTime, systemTime);
        this.bGValue=bGValue;
        this.trend=trend;
    }

    public String noiseValue() { return String.valueOf(noise); }
    public int getBGValue() {
        return bGValue;
    }

    public Dex_Constants.TREND_ARROW_VALUES getTrend() {
        return trend;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("sgv", getBGValue());
            obj.put("date", getDisplayTimeSeconds());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
