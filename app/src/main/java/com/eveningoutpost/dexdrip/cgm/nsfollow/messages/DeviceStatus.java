package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

/**
 * A single record from the Nightscout /api/v3/devicestatus endpoint.
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
