package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * Various constants
 */
public class Constants {
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;


    public static final long SECOND_IN_MS = 1_000;
    public static final long MINUTE_IN_MS = 60_000;
    public static final long HOUR_IN_MS = 3_600_000;
    public static final long DAY_IN_MS = 86_400_000;
    public static final long WEEK_IN_MS = DAY_IN_MS * 7;
    public static final long MONTH_IN_MS = DAY_IN_MS * 30;
    public static final long YEAR_IN_MS = DAY_IN_MS * 365;

    public static final double LIBRE_MULTIPLIER = 117.64705; // to match (raw/8.5)*1000

    /* Configuration parameters */

    public static final long STALE_CALIBRATION_CUT_OFF = Constants.MINUTE_IN_MS * 21;

    /* Notification IDs */
    public static final int FINAL_VISIBILITY_ID = 785877617;

    public static final int WIFI_COLLECTION_SERVICE_ID = 1001;
    public static final int DEX_COLLECTION_SERVICE_RETRY_ID = 1002;
    public static final int DEX_COLLECTION_SERVICE_FAILOVER_ID = 1003;
    public static final int SYNC_QUEUE_RETRY_ID = 1004;
    public static final int NUMBER_TEXT_TEST_ID = 1005;
    public static final int MISSED_READING_SERVICE_ID = 1006;
    public static final int G5_CALIBRATION_REQUEST = 1007;
    public static final int G5_CALIBRATION_REJECT = 1008;
    public static final int G5_START_REJECT = 1009;
    public static final int G5_SENSOR_ERROR = 1010;
    public static final int G5_SENSOR_FAILED = 1011;
    public static final int G5_SENSOR_STARTED = 1012;
    public static final int G5_SENSOR_RESTARTED = 1013;
    public static final int G6_DEFAULTS_MESSAGE = 1014;
    public static final int MEDTRUM_SERVICE_RETRY_ID = 1015;
    public static final int MEDTRUM_SERVICE_FAILOVER_ID = 1016;
    public static final int DESERT_MASTER_UNREACHABLE = 1017;
    public static final int LEFUN_SERVICE_RETRY_ID = 1018;
    //public static final int LEFUN_SERVICE_FAILOVER_ID = 1019;
    public static final int NSFOLLOW_SERVICE_FAILOVER_ID = 1020;
    public static final int NSFOLLOW_SERVICE_RETRY_ID = 1021;
    public static final int INPEN_SERVICE_FAILOVER_ID = 1022;
    public static final int GET_PHONE_READ_PERMISSION = 1023;
    public static final int SHFOLLOW_SERVICE_FAILOVER_ID = 1024;
    public static final int BLUEJAY_SERVICE_RETRY_ID = 1025;
    public static final int MIBAND_SERVICE_RETRY_ID = 1026;
    public static final int MIBAND_SERVICE_BG_RETRY_ID = 1027;
    public static final int WEBFOLLOW_SERVICE_FAILOVER_ID = 1028;
    public static final int BACKUP_ACTIVITY_ID = 1029;
    public static final int CARELINK_SERVICE_FAILOVER_ID = 1030;

    static final int NIGHTSCOUT_ERROR_NOTIFICATION_ID = 2001;
    public static final int HEALTH_CONNECT_RESPONSE_ID = 2002;
    public static final int SENSORY_EXPIRY_NOTIFICATION_ID = 2003;


    // increments from this start number
    public static final int INCOMPATIBLE_BASE_ID = 5000;
    public static final int COMPATIBLE_BASE_ID = 6000;
    public static final int DEX_BASE_ID = 6400;
    public static final int SETTINGS_INADVISABLE_BASE_ID = 7000;

    //
    public static final int HEART_RATE_JOB_ID = 60920012;
    public static final int APK_DOWNLOAD_JOB_ID = 60920014;

    public static final int LIBRE_1_2_FRAM_SIZE = 344;

    public static final int LIBREPRO_HEADER1_SIZE = 40;
    public static final int LIBREPRO_HEADER2_SIZE = 32;
    public static final int LIBREPRO_HEADER3_SIZE = 104;

}
