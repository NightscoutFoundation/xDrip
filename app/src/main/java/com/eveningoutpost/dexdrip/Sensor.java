package com.eveningoutpost.dexdrip;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.UtilityModels.SensorSendQueue;

import java.util.UUID;

/**
 * Created by stephenblack on 10/29/14.
 */

@Table(name = "Sensors", id = BaseColumns._ID)
public class Sensor extends Model {

//    @Expose
    @Column(name = "started_at", index = true)
    public double started_at;

//    @Expose
    @Column(name = "stopped_at")
    public double stopped_at;

//    @Expose
    @Column(name = "latest_battery_level")
    public int latest_battery_level;

//    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

    public static Sensor create(double started_at) {
        Sensor sensor = new Sensor();
        sensor.started_at = started_at;
        sensor.uuid = UUID.randomUUID().toString();
        sensor.save();
        SensorSendQueue.addToQueue(sensor);
        Log.w("SENSOR MODEL:", sensor.toString());
        return sensor;
    }

    public static Sensor currentSensor() {
        Sensor sensor = new Select()
                .from(Sensor.class)
                .where("started_at != 0")
                .where("stopped_at = 0")
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return sensor;
    }

    public static boolean isActive() {
        Sensor sensor = new Select()
                .from(Sensor.class)
                .where("started_at != 0")
                .where("stopped_at = 0")
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        if(sensor == null) {
            return false;
        } else {
            return true;
        }
    }
}

