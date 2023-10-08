package com.eveningoutpost.dexdrip.models;

import android.content.Context;
import android.provider.BaseColumns;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.Configuration;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import android.database.sqlite.SQLiteDatabase;//KS
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.SensorSendQueue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Emma Black on 10/29/14.
 */

@Table(name = "Sensors", id = BaseColumns._ID)
public class Sensor extends Model {

//    @Expose
    @Column(name = "started_at", index = true)
    public long started_at;

//    @Expose
    @Column(name = "stopped_at")
    public long stopped_at;

//    @Expose
    //latest minimal battery level
    @Column(name = "latest_battery_level")
    public int latest_battery_level;

//    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

//  @Expose
  @Column(name = "sensor_location")
  public String sensor_location;

    public Sensor() {super();}//KS

    public static Sensor create(long started_at) {
        Sensor sensor = new Sensor();
        sensor.started_at = started_at;
        sensor.uuid = UUID.randomUUID().toString();

        sensor.save();
        SensorSendQueue.addToQueue(sensor);
        Log.d("SENSOR MODEL:", sensor.toString());
        return sensor;
    }

    public static Sensor create(long started_at, String uuid) {//KS
        try {
            Sensor sensor = new Sensor();
            sensor.started_at = started_at;
            sensor.uuid = uuid;

            sensor.save();
            SensorSendQueue.addToQueue(sensor);
            Log.d("SENSOR MODEL:", sensor.toString());
            return sensor;
        }
        catch (Exception e)
        {
            Log.d("SENSOR create new error ", uuid);
            return null;
        }
    }

    public static Sensor createDefaultIfMissing() {
        final Sensor sensor = currentSensor();
        if (sensor == null) {
            Sensor.create(JoH.tsl());
        }
        return currentSensor();
    }

    // Used by xDripViewer
    public static void createUpdate(long started_at, long stopped_at,  int latest_battery_level, String uuid) {

        Sensor sensor = getByTimestamp(started_at);
        if (sensor != null) {
            Log.d("SENSOR", "updating an existing sensor");
        } else {
            Log.d("SENSOR", "creating a new sensor");
            sensor = new Sensor();
        }
        sensor.started_at = started_at;
        sensor.stopped_at = stopped_at;
        sensor.latest_battery_level = latest_battery_level;
        sensor.uuid = uuid;
        sensor.save();
    }

    public static void createUpdate(long started_at, long stopped_at,  int latest_battery_level, String sensor_location, String uuid) {//KS

        Sensor sensor = currentSensor();
        if (sensor != null) {
            Log.d("SENSOR", "updating an existing sensor");
        } else {
            Log.d("SENSOR", "creating a new sensor");
            sensor = new Sensor();
        }
        sensor.started_at = started_at;
        sensor.stopped_at = stopped_at;
        sensor.latest_battery_level = latest_battery_level;
        sensor.sensor_location = sensor_location;
        sensor.uuid = uuid;
        sensor.save();
    }

    public static void stopSensor() {
        Sensor sensor = currentSensor();
        if(sensor == null) {
            return;
        }
        sensor.stopped_at = new Date().getTime();
        Log.i("SENSOR", "Sensor stopped at " + sensor.stopped_at);
        sensor.save();
        SensorSendQueue.addToQueue(sensor);

    }

    public String toS() {//KS
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        Log.d("SENSOR", "Sensor toS uuid=" + this.uuid + " started_at=" + this.started_at + " active=" + this.isActive() + " battery=" + this.latest_battery_level + " location=" + this.sensor_location + " stopped_at=" + this.stopped_at);
        return gson.toJson(this);
    }

    public static void DeleteAndInitDb(Context context) {//KS
        Configuration dbConfiguration = new Configuration.Builder(context).create();
        try {
            ActiveAndroid.dispose();
            context.deleteDatabase("DexDrip.db");
            //ActiveAndroid.initialize(dbConfiguration);
            Log.d("wearSENSOR", "DeleteAndInitDb DexDrip.db deleted and initialized.");
        } catch (Exception e) {
            Log.e("wearSENSOR", "DeleteAndInitDb CATCH Error.");
        }
    }

    public static void InitDb(Context context) {//KS
        Configuration dbConfiguration = new Configuration.Builder(context).create();
        try {
            SQLiteDatabase db = Cache.openDatabase();
            if (db != null) {
                Log.d("wearSENSOR", "InitDb DB exists");
            }
            else {
                ActiveAndroid.initialize(dbConfiguration);
                Log.d("wearSENSOR", "InitDb DB does NOT exist. Call ActiveAndroid.initialize()");
            }
        } catch (Exception e) {
            ActiveAndroid.initialize(dbConfiguration);
            Log.d("wearSENSOR", "InitDb CATCH: DB does NOT exist. Call ActiveAndroid.initialize()");
        }
    }

    public static boolean TableExists(String table) {//KS
        try {
            SQLiteDatabase db = Cache.openDatabase();
            if (db != null) {
                db.rawQuery("SELECT * FROM " + table, null);
                Log.d("wearSENSOR", "TableExists table does NOT exist:" + table);
                return true;
            }
            else {
                Log.d("wearSENSOR", "TableExists Cache.openDatabase() failed.");
                return false;
            }
        } catch (Exception e) {
            Log.d("wearSENSOR", "TableExists CATCH error table:" + table);
            return false;
        }
    }

    public static Sensor currentSensor() {
        try {//KS
            Sensor sensor = new Select()
                    .from(Sensor.class)
                    .where("started_at != 0")
                    .where("stopped_at = 0")
                    .orderBy("_ID desc")
                    .limit(1)
                    .executeSingle();
            return sensor;
        }
        catch (Exception e) {
            return null;
        }
    }

    public static boolean isActive() {
        try {//KS
            Sensor sensor = new Select()
                    .from(Sensor.class)
                    .where("started_at != 0")
                    .where("stopped_at = 0")
                    .orderBy("_ID desc")
                    .limit(1)
                    .executeSingle();
            if (sensor == null) {
                return false;
            } else {
                return true;
            }
        }
        catch (Exception e) {
            return false;
        }
     }

    public static Sensor getByTimestamp(double started_at) {
        try {//KS
            Sensor sensor = new Select()
                    .from(Sensor.class)
                    .where("started_at = ?", started_at)
                    .executeSingle();
            return sensor;
        }
        catch (Exception e) {
            return null;
        }

    }

    public static Sensor getByUuid(String xDrip_sensor_uuid) {
        if(xDrip_sensor_uuid == null) {
            Log.d("wearSENSOR", "getByUuid xDrip_sensor_uuid is null");
            return null;
        }
        Log.d("wearSENSOR", "getByUuid xDrip_sensor_uuid is " + xDrip_sensor_uuid);

        //if (TableExists("Sensor")) {//com.eveningoutpost.dexdrip.Models.Sensor
            try {//KS
                Sensor sensor = new Select()
                        .from(Sensor.class)
                        .where("uuid = ?", xDrip_sensor_uuid)
                        .executeSingle();
                return sensor;
            } catch (Exception e) {
                Log.d("wearSENSOR", "getByUuid CATCH Select error xDrip_sensor_uuid is " + xDrip_sensor_uuid);
                return null;
            }
        //}
        //else return null;

    }

    public static void updateBatteryLevel(int sensorBatteryLevel, boolean from_sync) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null)
        {
            Log.d("Sensor","Cant sync battery level from master as sensor data is null");
            return;
        }
        updateBatteryLevel(sensor, sensorBatteryLevel, from_sync);
    }

    public static void updateBatteryLevel(Sensor sensor, int sensorBatteryLevel) {
        updateBatteryLevel(sensor, sensorBatteryLevel, false);
    }

    public static void updateBatteryLevel(Sensor sensor, int sensorBatteryLevel, boolean from_sync) {
        if(sensorBatteryLevel < 120) {
            // This must be a wrong battery level. Some transmitter send those every couple of readings
            // even if the battery is ok.
            return;
        }
        int startBatteryLevel = sensor.latest_battery_level;
        if(sensor.latest_battery_level == 0) {
            sensor.latest_battery_level = sensorBatteryLevel;
        } else {
            sensor.latest_battery_level = Math.min(sensor.latest_battery_level, sensorBatteryLevel);
        }
        if(startBatteryLevel == sensor.latest_battery_level) {
            // no need to update anything if nothing has changed.
            return;
        }
        sensor.save();
        SensorSendQueue.addToQueue(sensor);
        //KS if ((!from_sync) && (Home.get_master())) { GcmActivity.sendSensorBattery(sensor.latest_battery_level); }
    }

    public static void updateSensorLocation(String sensor_location) {
        Sensor sensor = currentSensor();
        if (sensor == null) {
            Log.e("SENSOR MODEL:", "updateSensorLocation called but sensor is null");
            return;
        }
        sensor.sensor_location = sensor_location;
        sensor.save();
    }
}

