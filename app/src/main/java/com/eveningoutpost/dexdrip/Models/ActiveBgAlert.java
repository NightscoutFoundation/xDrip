package com.eveningoutpost.dexdrip.Models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;

import java.util.Date;

/**
 * Created by stephenblack on 1/14/15.
 */
public class ActiveBgAlert extends Model {
    @Column(name = "alert_uuid")
    public String alert_uuid;

    @Column(name = "is_snoozed")
    public boolean is_snoozed;

    @Column(name = "last_alerted_at")
    public double last_alerted_at;

    @Column(name = "next_alert_at")
    public double next_alert_at;

    public boolean ready_to_alarm() {
        if(new Date().getTime() > next_alert_at) {
            return true;
        }
        return false;
    }

    public void snooze() {
        
        
    }
}

