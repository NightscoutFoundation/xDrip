package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

import java.util.Locale;

public class BasalProfileEntry {
    @Expose
    public String time; // 01:00 etc.
    @Expose
    public int timeAsSeconds;
    @Expose
    public double value;

    public BasalProfileEntry(int timeAsSeconds, double value) {
        this.timeAsSeconds = timeAsSeconds;
        this.value = value;
        this.time = String.format((Locale) null, "%02d:00", timeAsSeconds / 3600);
    }
}
