package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Various constants
 */
public class Constants {
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;

    public static final double DEXCOM_MAX_RAW = 1000; // raw values above this will be treated as error

    public static final long HOUR_IN_MS = 3600000;
    public static final long DAY_IN_MS = 86400000;
    public static final long WEEK_IN_MS = DAY_IN_MS * 7;
    public static final long MONTH_IN_MS = DAY_IN_MS * 30;


}
