package com.eveningoutpost.dexdrip;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.UUID;

/**
 * Created by stephenblack on 11/6/14.
 */
@Table(name = "Comparisons", id = BaseColumns._ID)
public class Comparison extends Model {
    private final static String TAG = Comparison.class.getSimpleName();

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "sensor_age")
    public double sensor_age;

    @Column(name = "sensor", index = true)
    public Sensor sensor;

    @Expose
    @Column(name = "bg")
    public int bg;

    @Expose
    @Column(name = "raw_value")
    public double raw_value;

    @Expose
    @Column(name = "adjusted_raw_value")
    public double adjusted_raw_value;

    @Expose
    @Column(name = "distance_from_estimate")
    public double distance_from_estimate;

    @Expose
    @Column(name = "estimate_at_time_of_calibration")
    public double estimate_at_time_of_calibration;

    @Expose
    @Column(name = "estimate_five_mins_after_calibration")
    public double estimate_five_mins_after_calibration;

    @Expose
    @Column(name = "estimate_ten_mins_after_calibration")
    public double estimate_ten_mins_after_calibration;

    @Expose
    @Column(name = "estimate_fifteen_mins_after_calibration")
    public double estimate_fifteen_mins_after_calibration;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

    @Expose
    @Column(name = "sensor_uuid", index = true)
    public String sensor_uuid;

    @Expose
    @Column(name = "bg_reading_uuid", index = true)
    public String bg_reading_uuid;

    public static Comparison create(int bg) {
        Comparison comparison = new Comparison();
        Sensor sensor = Sensor.currentSensor();
        BgReading bgReading = BgReading.last();

        comparison.timestamp = new Date().getTime();
        comparison.raw_value = bgReading.raw_data;
        comparison.adjusted_raw_value = bgReading.age_adjusted_raw_value;
        comparison.sensor = sensor;
        comparison.sensor_uuid = sensor.uuid;
        comparison.bg = bg;

        comparison.estimate_at_time_of_calibration = BgReading.estimated_bg(new Date().getTime());
        comparison.estimate_five_mins_after_calibration = BgReading.estimated_bg((new Date().getTime()) + (60 * 5));
        comparison.estimate_ten_mins_after_calibration = BgReading.estimated_bg((new Date().getTime()) + (60 * 10));
        comparison.estimate_fifteen_mins_after_calibration = BgReading.estimated_bg((new Date().getTime()) + (60 * 15));
        double distance = comparison.bg - BgReading.estimated_bg(new Date().getTime());
        comparison.distance_from_estimate = (distance < 0) ? -distance : distance;
        comparison.sensor_age = new Date().getTime() - sensor.started_at;
        comparison.uuid = UUID.randomUUID().toString();
        comparison.bg_reading_uuid = bgReading.uuid;
        comparison.save();
        ComparisonSendQueue.addToQueue(comparison);
        return comparison;
    }
}