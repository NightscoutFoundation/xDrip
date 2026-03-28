package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

/**
 * A single record from the Nightscout /api/v1/devicestatus.json endpoint.
 * Fields match both v1 (bare array) and v3 (wrapped) response shapes.
 *
 * @author Asbjørn Aarrestad
 */
public class DeviceStatus {

    @Expose
    public long date;

    @Expose
    public Integer uploaderBattery;

    @Expose
    public Boolean isCharging;

    @Expose
    public Pump pump;

    public static class Pump {

        @Expose
        public Double reservoir;

        @Expose
        public Extended extended;
    }

    public static class Extended {

        @Expose
        public Double BaseBasalRate;
    }
}
