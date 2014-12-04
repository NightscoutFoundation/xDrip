package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.Date;

/**
 * Created by stephenblack on 11/29/14.
 */

@Table(name = "Notifications", id = BaseColumns._ID)
public class UserNotification extends Model {
    public int snoozeMinutes = 20;

    @Column(name = "timestamp", index = true)
    public double timestamp;

    @Column(name = "message")
    public String message;

    @Column(name = "bg_alert")
    public boolean bg_alert;

    @Column(name = "cleared", index = true)
    public boolean cleared;

    public static UserNotification lastBgAlert() {
        return new Select()
                .from(UserNotification.class)
                .where("bg_alert = ?", true)
                .where("cleared = ?", false)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static UserNotification create(String message, String type) {
        UserNotification userNotification = new UserNotification();
        userNotification.timestamp = new Date().getTime();
        userNotification.message = message;
        userNotification.cleared = false;
        if (type == "bg_alert") {
            userNotification.bg_alert = true;
        }
        userNotification.save();
        return userNotification;

    }
}
