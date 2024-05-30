package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Emma Black on 11/6/14.
 */

@Table(name = "TransmitterData", id = BaseColumns._ID)
public class TransmitterData extends Model {
    private final static String TAG = TransmitterData.class.getSimpleName();

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    // TODO these should be int or long surely
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

    public static synchronized TransmitterData create(byte[] buffer, int len, Long timestamp) {
        if (len < 6) {
            return null;
        }
        final TransmitterData transmitterData = new TransmitterData();
        try {
            if ((buffer[0] == 0x11 || buffer[0] == 0x15) && buffer[1] == 0x00) {
                //this is a dexbridge packet.  Process accordingly.
                Log.i(TAG, "create Processing a Dexbridge packet");
                final ByteBuffer txData = ByteBuffer.allocate(len);
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
                for (int i = 0; i < len; ++i) {
                    data_string.append((char) buffer[i]);
                }
                final String[] data = data_string.toString().split("\\s+");

                if (data.length > 1) {
                    transmitterData.sensor_battery_level = Integer.parseInt(data[1]);
                    if (data.length > 2) {
                        try {
                            Pref.setInt("bridge_battery", Integer.parseInt(data[2]));
                            if (Home.get_master()) {
                                GcmActivity.sendBridgeBattery(Pref.getInt("bridge_battery", -1));
                            }
                            CheckBridgeBattery.checkBridgeBattery();
                        } catch (Exception e) {
                            Log.e(TAG, "Got exception processing classic wixel or limitter battery value: " + e.toString());
                        }
                        if (data.length > 3) {
                            if ((DexCollectionType.getDexCollectionType() == DexCollectionType.LimiTTer)
                                    && (!Pref.getBooleanDefaultFalse("use_transmiter_pl_bluetooth"))) {
                                try {
                                    // reported sensor age in minutes
                                    final Integer sensorAge = Integer.parseInt(data[3]);
                                    if ((sensorAge > 0) && (sensorAge < 200000))
                                        Pref.setInt("nfc_sensor_age", sensorAge);
                                } catch (Exception e) {
                                    Log.e(TAG, "Got exception processing field 4 in classic limitter protocol: " + e);
                                }
                            }
                        }
                    }
                }
                transmitterData.raw_data = Integer.parseInt(data[0]);
                transmitterData.filtered_data = Integer.parseInt(data[0]);
                // TODO process does_have_filtered_here with extended protocol
                transmitterData.timestamp = timestamp;
            }

            //Stop allowing readings that are older than the last one - or duplicate data, its bad! (from savek-cc)
            final TransmitterData lastTransmitterData = TransmitterData.last();
            if (lastTransmitterData != null && lastTransmitterData.timestamp >= transmitterData.timestamp) {
                Log.e(TAG, "Rejecting TransmitterData constraint: last: " + JoH.dateTimeText(lastTransmitterData.timestamp) + " >= this: " + JoH.dateTimeText(transmitterData.timestamp));
                return null;
            }
            if (lastTransmitterData != null && lastTransmitterData.raw_data == transmitterData.raw_data && Math.abs(lastTransmitterData.timestamp - transmitterData.timestamp) < (Constants.MINUTE_IN_MS * 2)) {
                Log.e(TAG, "Rejecting identical TransmitterData constraint: last: " + JoH.dateTimeText(lastTransmitterData.timestamp) + " due to 2 minute rule this: " + JoH.dateTimeText(transmitterData.timestamp));
                return null;
            }
            final Calibration lastCalibration = Calibration.lastValid();
            if (lastCalibration != null && lastCalibration.timestamp > transmitterData.timestamp) {
                Log.e(TAG, "Rejecting historical TransmitterData constraint: calib: " + JoH.dateTimeText(lastCalibration.timestamp) + " > this: " + JoH.dateTimeText(transmitterData.timestamp));
                return null;
            }

            transmitterData.uuid = UUID.randomUUID().toString();
            transmitterData.save();
            return transmitterData;
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing fields in protocol: " + e + " " + HexDump.dumpHexString(buffer));
        }
        return null;
    }

    public static synchronized TransmitterData create(int raw_data, int filtered_data, int sensor_battery_level, long timestamp) {
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == raw_data && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (Constants.MINUTE_IN_MS * 2)) { //Stop allowing duplicate data, its bad!
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
        if (lastTransmitterData != null && lastTransmitterData.raw_data == raw_data && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (Constants.MINUTE_IN_MS * 2)) { //Stop allowing duplicate data, its bad!
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

    public static List<TransmitterData> last(int count) {
        return new Select()
                .from(TransmitterData.class)
                .orderBy("_ID desc")
                .limit(count)
                .execute();
    }

    public static TransmitterData lastByTimestamp() {
        return new Select()
                .from(TransmitterData.class)
                .orderBy("timestamp desc")
                .executeSingle();
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
    
    public static TransmitterData byid(long id) {
        return new Select()
                .from(TransmitterData.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static void updateTransmitterBatteryFromSync(final int battery_level) {
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

    private static double roundRaw(TransmitterData td) {
        return JoH.roundDouble(td.raw_data,3);
    }
    private static double roundFiltered(TransmitterData td) {
        return JoH.roundDouble(td.filtered_data,3);
    }

    public static boolean unchangedRaw() {
        final List<TransmitterData> items = last(3);
        if (items != null && items.size() == 3) {
            return (roundRaw(items.get(0)) == roundRaw(items.get(1))
                    && roundRaw(items.get(0)) == roundRaw(items.get(2))
                    && roundFiltered(items.get(0)) == roundFiltered(items.get(1))
                    && roundFiltered(items.get(0)) == roundFiltered(items.get(2)));
        }
        return false;
    }

}
