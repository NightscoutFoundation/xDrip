package com.eveningoutpost.dexdrip.importedlibraries.dexcom.records;

import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class SensorRecord extends GenericTimestampRecord {

    private int unfiltered;
    private int filtered;
    private int rssi;
    private int OFFSET_UNFILTERED = 8;
    private int OFFSET_FILTERED = 12;
    private int OFFSET_RSSI = 16;

    public SensorRecord(byte[] packet) {
        super(packet);
        unfiltered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_UNFILTERED);
        filtered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_FILTERED);
        byte [] usRSSI = new byte[]{packet[17],packet[16]};
        rssi = usRSSI[0] << 8 | usRSSI[1];
        //rssi = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(OFFSET_RSSI);
        Log.d("ShareTest", "filtered: " + filtered + " unfiltered: " + unfiltered);
    }

    public long getUnfiltered() {
        return unfiltered;
    }

    public long getFiltered() {
        return filtered;
    }

    public int getRSSI() {
        return rssi;
    }
}
