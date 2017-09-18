package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Various constants
 */
public class Constants {
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;

    public static final double DEXCOM_MAX_RAW = 1000; // raw values above this will be treated as error

    public static final long SECOND_IN_MS = 1000;
    public static final long MINUTE_IN_MS = 60000;
    public static final long HOUR_IN_MS = 3600000;
    public static final long DAY_IN_MS = 86400000;
    public static final long WEEK_IN_MS = DAY_IN_MS * 7;
    public static final long MONTH_IN_MS = DAY_IN_MS * 30;


    public static final double LIBRE_MULTIPLIER = 117.64705; // to match (raw/8.5)*1000


    /* Notification IDs */

    static final int NIGHTSCOUT_ERROR_NOTIFICATION_ID = 2001;


    public static final int WIFI_COLLECTION_SERVICE_ID = 1001;
    public static final int DEX_COLLECTION_SERVICE_RETRY_ID = 1002;
    public static final int DEX_COLLECTION_SERVICE_FAILOVER_ID = 1003;


}
