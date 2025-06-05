package com.eveningoutpost.dexdrip.models;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.SensorSendQueue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Setter;
import lombok.val;

/**
 * Created by Emma Black on 10/29/14.
 */

@Table(name = "Sensors", id = BaseColumns._ID)
public class Sensor extends Model {
    private final static String TAG = Sensor.class.getSimpleName();

    @Expose
    @Column(name = "started_at", index = true)
    public long started_at;

    @Expose
    @Column(name = "stopped_at")
    public long stopped_at;

    @Expose
    //latest minimal battery level
    @Column(name = "latest_battery_level")
    public int latest_battery_level;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

  @Expose
  @Column(name = "sensor_location")
  public String sensor_location;


  @Setter
  private volatile static long unitTestStopTime;

    public synchronized static Sensor create(long starting_at) {
        stopSensor(true); // stop any existing sensor session clamping to the last reading
        return create(starting_at, UUID.randomUUID().toString());
    }

    public synchronized static Sensor create(long starting_at, String uuid) {//KS
        Sensor sensor = new Sensor();
        val lastSensor = lastStopped(); // get the last sensor we stopped
        // find the time it was stopped or 0 if there is no previous sensor
        long lastStoppedTime = lastSensor != null ? lastSensor.stopped_at : 0;
        sensor.started_at = Math.max(lastStoppedTime + 1, starting_at);
        if (sensor.started_at > tsl()) {
            UserError.Log.wtf(TAG, "Sensor create() called with future timestamp, this cannot be right. " + starting_at + " " + lastStoppedTime + " " + sensor.started_at);
        }
        sensor.uuid = uuid;
        sensor.save();
        SensorSendQueue.addToQueue(sensor);
        Log.d("SENSOR MODEL:", sensor.toString());
        return sensor;
    }

    public static Sensor createDefaultIfMissing() {
        final Sensor sensor = currentSensor();
        if (sensor == null) {
            Sensor.create(tsl());
            UserError.Log.ueh(TAG, "Created new default sensor");
        }
        return currentSensor();
    }

    public synchronized static void stopSensor() {
        stopSensor(false); // by default don't clamp to last reading
    }


    public synchronized static void stopSensor(final boolean clampToLastReading) {
        final Sensor sensor = currentSensor();
        if (sensor == null) {
            return;
        }

        // stop now or use specified unit testing stop time
        sensor.stopped_at = (unitTestStopTime == 0) ? tsl() : unitTestStopTime;

        if (clampToLastReading) {
            val lastReading = BgReading.last(true);
            if (lastReading != null) {
                // if we have a last reading then set the stop time to that last reading so long as it
                // was actually from this sensor
                sensor.stopped_at = Math.max(sensor.started_at, lastReading.timestamp + 1);
            }
        }

        UserError.Log.ueh("SENSOR", "Sensor stopped at value: " + JoH.dateTimeText(sensor.stopped_at));
        sensor.save();
        if (currentSensor() != null) {
            UserError.Log.wtf(TAG, "Failed to update sensor stop in database");
        }
        SensorSendQueue.addToQueue(sensor);
        JoH.clearCache();

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

    public static Sensor lastStopped() {
        Sensor sensor = new Select()
                .from(Sensor.class)
                .where("started_at != 0")
                .where("stopped_at != 0")
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return sensor;
    }

    public static boolean stoppedRecently() {
        final Sensor last = lastStopped();
        return last != null && last.stopped_at < tsl() && (JoH.msSince(last.stopped_at) < (Constants.HOUR_IN_MS * 2));
    }

    public static Sensor currentSensor() {
        Sensor sensor = new Select()
                .from(Sensor.class)
                .where("started_at != 0")
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();

        if (sensor != null) {
            if (sensor.stopped_at != 0) {
                // last sensor is stopped
                return null;
            }
        }
        return sensor;
    }

    public static boolean isActive() {
        return currentSensor() != null;
    }

    public static Sensor getByTimestamp(long started_at) {
        return new Select()
                .from(Sensor.class)
                .where("started_at = ?", started_at)
                .executeSingle();
    }

    public static Sensor restartSensor(String sensorUuid) {
        Sensor sensor = getByUuid(sensorUuid);

        sensor.stopped_at = 0;
        UserError.Log.ueh("SENSOR", "Sensor restarted");
        sensor.save();

        Sensor currentSensor = currentSensor();
        if (currentSensor != null) {
            UserError.Log.wtf(TAG, "Failed to update sensor restart in database");
        }
        SensorSendQueue.addToQueue(currentSensor);
        JoH.clearCache();

        return currentSensor;
    }

    public static Sensor getByUuid(String xDrip_sensor_uuid) {
        if(xDrip_sensor_uuid == null) {
            Log.e("SENSOR", "xDrip_sensor_uuid is null");
            return null;
        }
        Log.d("SENSOR", "xDrip_sensor_uuid is " + xDrip_sensor_uuid);

        return new Select()
                .from(Sensor.class)
                .where("uuid = ?", xDrip_sensor_uuid)
                .executeSingle();
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
        if (sensorBatteryLevel < 120) {
            // This must be a wrong battery level. Some transmitter send those every couple of readings
            // even if the battery is ok.
            return;
        }
        int startBatteryLevel = sensor.latest_battery_level;
        //  if(sensor.latest_battery_level == 0) {
        // allow sensor battery level to go up and down
        sensor.latest_battery_level = sensorBatteryLevel;
        //  } else {
        //     sensor.latest_battery_level = Math.min(sensor.latest_battery_level, sensorBatteryLevel);
        // }
        if (startBatteryLevel == sensor.latest_battery_level) {
            // no need to update anything if nothing has changed.
            return;
        }
        sensor.save();
        SensorSendQueue.addToQueue(sensor);
        if ((!from_sync) && (Home.get_master())) {
            GcmActivity.sendSensorBattery(sensor.latest_battery_level);
        }
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
    
    public static void upsertFromMaster(Sensor jsonSensor) {
        if (jsonSensor == null) {
            Log.wtf(TAG,"Got null sensor from json");
            return;
        }
        try {
            Sensor existingSensor = getByUuid(jsonSensor.uuid);
            if (existingSensor == null) {
                Log.d(TAG, "saving new sensor record.");
                jsonSensor.save();
            } else {
                Log.d(TAG, "updating existing sensor record.");
                existingSensor.started_at = jsonSensor.started_at;
                existingSensor.stopped_at = jsonSensor.stopped_at;
                existingSensor.latest_battery_level = jsonSensor.latest_battery_level;
                existingSensor.sensor_location = jsonSensor.sensor_location;
                existingSensor.save();
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not save Sensor: " + e.toString());
        }
    }

    public static void deleteAll() {
        // will fail if bg readings not deleted first
            SensorSendQueue.deleteAll();
            new Delete()
                    .from(Sensor.class)
                    .execute();
    }

    public String toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("started_at", started_at);
            jsonObject.put("stopped_at", stopped_at);
            jsonObject.put("latest_battery_level", latest_battery_level);
            jsonObject.put("uuid", uuid);
            jsonObject.put("sensor_location", sensor_location);
            return jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG,"Got JSONException handeling sensor", e);
            return "";
        }
    }

    @Override
    public String toString() {
        return "SENSOR: " + toJSON() + " started: " + JoH.dateTimeText(started_at) + ((stopped_at != 0) ? " stopped: " + JoH.dateTimeText(stopped_at) : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { // Check for reference equality
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) { // Check for null and class type
            return false;
        }
        Sensor sensor = (Sensor) obj; // Typecast

        return started_at == sensor.started_at
             //   && stopped_at == sensor.stopped_at // Note: not checking stopped_at as this maybe transient
                && latest_battery_level == sensor.latest_battery_level
                && Objects.equals(uuid, sensor.uuid)
                && Objects.equals(sensor_location, sensor.sensor_location);

    }

    @Override
    public int hashCode() {
        return Objects.hash(started_at, stopped_at, latest_battery_level, uuid, sensor_location);
    }
    
    public static Sensor fromJSON(String json) {
        if (json.length()==0) {
            Log.d(TAG,"Empty json received in Sensor fromJson");
            return null;
        }
        try {
            Log.d(TAG, "Processing incoming json: " + json);
           return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json,Sensor.class);
        } catch (Exception e) {
            Log.d(TAG, "Got exception parsing Sensor json: " + e.toString());
            Home.toaststaticnext("Error on Sensor sync.");
            return null;
        }
    }


    public static void shutdownAllSensors() {
        final List<Sensor> l = new Select().from(Sensor.class).execute();
        for (final Sensor s : l) {
            s.stopped_at = s.started_at;
            s.save();
            System.out.println(s.toJSON());
        }
    }
}

