package com.eveningoutpost.dexdrip.utilitymodels;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.Sensor;

import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */
@Table(name = "SensorSendQueue", id = BaseColumns._ID)
public class SensorSendQueue extends Model {

    @Column(name = "Sensor", index = true)
    public Sensor sensor;

    @Column(name = "success", index = true)
    public boolean success;


    public static SensorSendQueue nextSensorJob() {
        SensorSendQueue job = new Select()
                .from(SensorSendQueue.class)
                .where("success =", false)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return job;
    }

    public static List<SensorSendQueue> queue() {
        return new Select()
                .from(SensorSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID desc")
                .execute();
    }

    public static void addToQueue(Sensor sensor) {
        SendToFollower(sensor);
        SensorSendQueue sensorSendQueue = new SensorSendQueue();
        sensorSendQueue.sensor = sensor;
        sensorSendQueue.success = false;
        sensorSendQueue.save();
    }
    
    public static void SendToFollower(Sensor sensor) {
       // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        if(Home.get_master()) {
            GcmActivity.syncSensor(sensor, true);
        }
    }
}
