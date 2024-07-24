package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

public class PumpBannerState {

    public static final String STATE_DUAL_BOLUS = "DUAL_BOLUS";
    public static final String STATE_SQUARE_BOLUS = "SQUARE_BOLUS";
    public static final String STATE_LOAD_RESERVOIR = "LOAD_RESERVOIR";
    public static final String STATE_SUSPENDED_ON_LOW = "SUSPENDED_ON_LOW";
    public static final String STATE_SUSPENDED_BEFORE_LOW = "SUSPENDED_BEFORE_LOW";
    public static final String STATE_DELIVERY_SUSPEND = "DELIVERY_SUSPEND";
    public static final String STATE_BG_REQUIRED = "BG_REQUIRED";
    public static final String STATE_PROCESSING_BG = "PROCESSING_BG";
    public static final String STATE_WAIT_TO_ENTER_BG = "WAIT_TO_ENTER_BG";
    public static final String STATE_TEMP_TARGET = "TEMP_TARGET";
    public static final String STATE_TEMP_BASAL = "TEMP_BASAL";
    public static final String STATE_NO_DELIVERY = "NO_DELIVERY";
    public static final String STATE_CALIBRATION_REQUIRED = "CALIBRATION_REQUIRED";

    public String type;
    public int timeRemaining;

}
