package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Various constants
 */
public class Constants {
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;


    public static final long SECOND_IN_MS = 1000;
    public static final long MINUTE_IN_MS = 60000;
    public static final long HOUR_IN_MS = 3600000;
    public static final long DAY_IN_MS = 86400000;
    public static final long WEEK_IN_MS = DAY_IN_MS * 7;
    public static final long MONTH_IN_MS = DAY_IN_MS * 30;

    public static final double LIBRE_MULTIPLIER = 117.64705; // to match (raw/8.5)*1000

    /* Configuration parameters */

    public static final long STALE_CALIBRATION_CUT_OFF = Constants.MINUTE_IN_MS * 21;

    /* Notification IDs */
    public static final int FINAL_VISIBILITY_ID = 785877617;

    public static final int WIFI_COLLECTION_SERVICE_ID = 1001;
    public static final int DEX_COLLECTION_SERVICE_RETRY_ID = 1002;
    public static final int DEX_COLLECTION_SERVICE_FAILOVER_ID = 1003;

    public static final int SYNC_QUEUE_RETRY_ID = 1004;

    static final int NIGHTSCOUT_ERROR_NOTIFICATION_ID = 2001;

    // increments from this start number
    public static final int INCOMPATIBLE_BASE_ID = 5000;
    public static final int COMPATIBLE_BASE_ID = 6000;
}
