package com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records;

import android.util.Log;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

public class EGVRecord extends GenericTimestampRecord {

    private int bGValue;
    private Constants.TREND_ARROW_VALUES trend;

    public EGVRecord(byte[] packet) {
        // system_time (UInt), display_time (UInt), glucose (UShort), trend_arrow (Byte), crc (UShort))
        super(packet);
        int eGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        bGValue = eGValue & Constants.EGV_VALUE_MASK;
        int trendValue = ByteBuffer.wrap(packet).get(10) & Constants.EGV_TREND_ARROW_MASK;
        trend = Constants.TREND_ARROW_VALUES.values()[trendValue];
        Log.d("ShareTest", "BG: " + bGValue + " TREND: " + trend);
    }

    public EGVRecord(int bGValue,Constants.TREND_ARROW_VALUES trend,Date displayTime, Date systemTime){
        super(displayTime, systemTime);
        this.bGValue=bGValue;
        this.trend=trend;
    }

    public int getBGValue() {
        return bGValue;
    }

    public Constants.TREND_ARROW_VALUES getTrend() {
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
