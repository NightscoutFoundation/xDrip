package com.eveningoutpost.dexdrip.Models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;

/**
 * Created by stephenblack on 1/14/15.
 */
public class AlertType extends Model {

    @Column(name = "name")
    public String name;

    @Column(name = "active")
    public boolean active;

    @Column(name = "volume")
    public int volume;

    @Column(name = "vibrate")
    public boolean vibrate;

    @Column(name = "light")
    public boolean light;

    @Column(name = "override_silent_mode")
    public boolean override_silent_mode;

    @Column(name = "predictive")
    public boolean predictive;

    @Column(name = "time_until_threshold")
    public double time_until_threshold_crossed;

    @Column(name = "above")
    public boolean above;

    @Column(name = "below")
    public boolean below;

    @Column(name = "threshold")
    public double threshold;

    @Column(name = "low_threshold")
    public double low_threshold;

    @Column(name = "all_day")
    public boolean all_day;

    @Column(name = "start_time")
    public double start_time;

    @Column(name = "end_time")
    public double end_time;

    @Column(name = "minutes_between")
    public int minutes_between;

    @Column(name = "default_snooze")
    public int default_snooze;

    @Column(name = "text")
    public String text;
    
    @Column(name = "uuid", index = true)
    public String uuid;

    public boolean in_time_frame() {
        double time_now = new Date().getTime(); //TODO: Get the actual time of day as a double this WILL NOT WORK without that
        if (all_day) { return true; }

        if(start_time < end_time) {
            if (time_now > start_time && time_now < end_time) {
                return true;
            }
        } else {
            if (time_now < start_time || time_now > end_time) {
                return true;
            }
        }
        return false;
    }

    public boolean beyond_threshold() {
        double bg = 0; //TODO: Get the actual BG value at this point in time obviously it wont work without that
        if (above && bg > threshold) {
            return true;
        } else if (below && bg < threshold) {
            return true;
        }
        return false;
    }

    public boolean trending_to_threshold() {
        if (!predictive) { return false; }
        double bg = 0; //TODO: Get the actual BG value time_until_threshold minutes from now
        if (above && bg > threshold) {
            return true;
        } else if (below && bg < threshold) {
            return true;
        }
        return false;
    }

    public boolean should_alarm() {
        if(in_time_frame() && (beyond_threshold() || trending_to_threshold())) {
            return true;
        } else {
            return false;
        }
    }
}
