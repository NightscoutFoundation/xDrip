package com.eveningoutpost.dexdrip.Models.usererror;


import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.google.gson.annotations.Expose;

import java.util.Date;

@Table(name = "UserErrors", id = BaseColumns._ID)
public class UserError extends Model {

    @Expose
    @Column(name = "shortError")
    private String shortError; // Short error message to be displayed on table

    @Expose
    @Column(name = "message")
    private String message; // Additional text when error is expanded

    @Expose
    @Column(name = "severity", index = true)
    private int severity; // int between 1 and 3, 3 being most severe

    // 5 = internal lower level user events
    // 6 = higher granularity user events

    @Expose
    @Column(name = "timestamp", index = true)
    private long timestamp; // Time the error was raised

    UserError() {}

    UserError(UserEvent eventType, String shortMsg, String longMessage) {
        this(eventType.getLevel(),shortMsg,longMessage);
    }

    UserError(int severity, String shortError, String message) {
        this.severity = severity;
        this.shortError = shortError;
        this.message = message;
        this.timestamp = new Date().getTime();
    }

    public long getTimestamp(){
        return timestamp;
    }
    public UserEvent getEventSeverity() {
        return UserEvent.forSeverity(severity);
    }
    public int getSeverity() {
        return severity;
    }

    public String getShortError() {
        return shortError;
    }
    void setShortError(String error) {
        this.shortError = error;
    }
    public String getMessage() {
        return message;
    }
    void setMessage(String msg) {
        this.message = msg;
    }

    // TODO use something more reasonable?
    public String bestTime() {
        final long since = JoH.msSince(timestamp);
        if (since < Constants.DAY_IN_MS) {
            return JoH.hourMinuteString(timestamp);
        } else {
            return JoH.dateTimeText(timestamp);
        }
    }
}