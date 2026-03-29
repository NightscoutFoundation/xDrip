package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

/**
 * A single record from the Nightscout /api/v1/devicestatus.json endpoint.
 * Fields match both v1 (bare array) and v3 (wrapped) response shapes.
 * Battery is read from either the flat {@code uploaderBattery} field (legacy
 * MongoDB-direct uploads) or the nested {@code uploader.battery} field (modern
 * REST uploads from xDrip and other uploaders).
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
    public Uploader uploader;

    @Expose
    public Pump pump;

    public static class Uploader {

        @Expose
        public Integer battery;
    }

    public static class Pump {

        @Expose
        public Double reservoir;

        @Expose
        public Battery battery;

        @Expose
        public Extended extended;

        public static class Battery {

            @Expose
            public Integer percent;
        }
    }

    public static class Extended {

        @Expose
        public Double BaseBasalRate;
    }
}
