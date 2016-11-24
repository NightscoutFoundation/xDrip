package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

//KS import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by stephenblack on 11/6/14.
 */

@Table(name = "TransmitterData", id = BaseColumns._ID)
public class TransmitterData extends Model {
    private final static String TAG = TransmitterData.class.getSimpleName();

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "raw_data")
    public double raw_data;

    @Expose
    @Column(name = "filtered_data")
    public double filtered_data;

    @Expose
    @Column(name = "sensor_battery_level")
    public int sensor_battery_level;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

    public TransmitterData () {
        super ();
    }

    public static synchronized TransmitterData create(byte[] buffer, int len, Long timestamp) {
        if (len < 6) { return null; }
        TransmitterData transmitterData = new TransmitterData();
        if ((buffer[0] == 0x11 || buffer[0] == 0x15) && buffer[1] == 0x00) {
            //this is a dexbridge packet.  Process accordingly.
            Log.i(TAG, "create Processing a Dexbridge packet");
            ByteBuffer txData = ByteBuffer.allocate(len);
            txData.order(ByteOrder.LITTLE_ENDIAN);
            txData.put(buffer, 0, len);
            transmitterData.raw_data = txData.getInt(2);
            transmitterData.filtered_data = txData.getInt(6);
            //  bitwise and with 0xff (1111....1) to avoid that the byte is treated as signed.
            transmitterData.sensor_battery_level = txData.get(10) & 0xff;
            if (buffer[0] == 0x15) {
                Log.i(TAG, "create Processing a Dexbridge packet includes delay information");
                transmitterData.timestamp = timestamp - txData.getInt(16);
            } else {
                transmitterData.timestamp = timestamp;
            }
            Log.i(TAG, "Created transmitterData record with Raw value of " + transmitterData.raw_data + " and Filtered value of " + transmitterData.filtered_data + " at " + timestamp + " with timestamp " + transmitterData.timestamp);
        } else { //this is NOT a dexbridge packet.  Process accordingly.
            Log.i(TAG, "create Processing a BTWixel or IPWixel packet");
            StringBuilder data_string = new StringBuilder();
            for (int i = 0; i < len; ++i) { data_string.append((char) buffer[i]); }
            final String[] data = data_string.toString().split("\\s+");

            if (data.length > 1) { 
                transmitterData.sensor_battery_level = Integer.parseInt(data[1]);
                /* //KS
                if (data.length > 2) {
                    try {
                        Home.setPreferencesInt("bridge_battery", Integer.parseInt(data[2]));
                        if (Home.get_master()) GcmActivity.sendBridgeBattery(Home.getPreferencesInt("bridge_battery",-1));
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception processing classic wixel or limitter battery value: " + e.toString());
                    }
                }
                */
            }
            transmitterData.raw_data = Integer.parseInt(data[0]);
            transmitterData.filtered_data = Integer.parseInt(data[0]);
            // TODO process does_have_filtered_here with extended protocol
            transmitterData.timestamp = timestamp;
        }

        //Stop allowing readings that are older than the last one - or duplicate data, its bad! (from savek-cc)
        final TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.timestamp >= timestamp) {
            return null;
        }
        if (lastTransmitterData != null && lastTransmitterData.raw_data == transmitterData.raw_data && Math.abs(lastTransmitterData.timestamp - timestamp) < (120000)) {
            return null;
        }
        final Calibration lastCalibration = Calibration.lastValid();
        if (lastCalibration != null && lastCalibration.timestamp > timestamp) {
            return null;
        }

        transmitterData.uuid = UUID.randomUUID().toString();
        transmitterData.save();
        return transmitterData;
    }

    public static synchronized TransmitterData create(int raw_data, int filtered_data, int sensor_battery_level, long timestamp) {
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == raw_data && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (120000)) { //Stop allowing duplicate data, its bad!
            return null;
        }

        TransmitterData transmitterData = new TransmitterData();
        transmitterData.sensor_battery_level = sensor_battery_level;
        transmitterData.raw_data = raw_data;
        transmitterData.filtered_data = filtered_data;
        transmitterData.timestamp = timestamp;
        transmitterData.uuid = UUID.randomUUID().toString();
        transmitterData.save();
        return transmitterData;
    }

    public static synchronized TransmitterData create(int raw_data ,int sensor_battery_level, long timestamp) {
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == raw_data && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (120000)) { //Stop allowing duplicate data, its bad!
            return null;
        }

        TransmitterData transmitterData = new TransmitterData();
        transmitterData.sensor_battery_level = sensor_battery_level;
        transmitterData.raw_data = raw_data ;
        transmitterData.timestamp = timestamp;
        transmitterData.uuid = UUID.randomUUID().toString();
        transmitterData.save();
        return transmitterData;
    }

    public static TransmitterData last() {
        return new Select()
                .from(TransmitterData.class)
                .orderBy("_ID desc")
                .executeSingle();
    }


    public static List<TransmitterData> latestForGraphAsc(int number, long startTime) {//KS
        return latestForGraphAsc(number, startTime, Long.MAX_VALUE);
    }

    public static List<TransmitterData> latestForGraphAsc(int number, long startTime, long endTime) {//KS
        return new Select()
                .from(TransmitterData.class)
                .where("timestamp >= " + Math.max(startTime, 0))
                .where("timestamp <= " + endTime)
                //.where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static TransmitterData getForTimestamp(double timestamp) {//KS
        try {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                TransmitterData bgReading = new Select()
                        .from(TransmitterData.class)
                        .where("timestamp <= ?", (timestamp + (60 * 1000))) // 1 minute padding (should never be that far off, but why not)
                        .orderBy("timestamp desc")
                        .executeSingle();
                if (bgReading != null && Math.abs(bgReading.timestamp - timestamp) < (3 * 60 * 1000)) { //cool, so was it actually within 4 minutes of that bg reading?
                    Log.i(TAG, "getForTimestamp: Found a BG timestamp match");
                    return bgReading;
                }
            }
        } catch (Exception e) {
            Log.e(TAG,"getForTimestamp() Got exception on Select : "+e.toString());
            return null;
        }
        Log.d(TAG, "getForTimestamp: No luck finding a BG timestamp match");
        return null;
    }

    public static TransmitterData findByUuid(String uuid) {//KS
        try {
            return new Select()
                .from(TransmitterData.class)
                .where("uuid = ?", uuid)
                .executeSingle();
        } catch (Exception e) {
            Log.e(TAG,"findByUuid() Got exception on Select : "+e.toString());
            return null;
        }
    }

    public String toS() {//KS
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();

        return gson.toJson(this);
    }

    /*
    //KS public static void updateTransmitterBatteryFromSync(final int battery_level) {
        try {
            TransmitterData td = TransmitterData.last();
            if ((td == null) || (td.raw_data!=0))
            {
                td=TransmitterData.create(0,battery_level,(long)JoH.ts());
                Log.d(TAG,"Created new fake transmitter data record for battery sync");
                if (td==null) return;
            }
            if ((battery_level != td.sensor_battery_level) || ((JoH.ts()-td.timestamp)>(1000*60*60))) {
                td.sensor_battery_level = battery_level;
                td.timestamp = (long)JoH.ts(); // freshen timestamp on this bogus record for system status
                Log.d(TAG,"Saving synced sensor battery, new level: "+battery_level);
                td.save();
            } else {
                Log.d(TAG,"Synced sensor battery level same as existing: "+battery_level);
            }
        } catch (Exception e) {
            Log.e(TAG,"Got exception updating sensor battery from sync: "+e.toString());
        }
    }
    */

}
