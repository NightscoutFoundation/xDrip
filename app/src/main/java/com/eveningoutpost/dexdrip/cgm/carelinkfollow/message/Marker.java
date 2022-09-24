package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink Marker data
 */
public class Marker {

    public static final String MARKER_TYPE_MEAL = "MEAL";
    public static final String MARKER_TYPE_CALIBRATION = "CALIBRATION";
    public static final String MARKER_TYPE_BG_READING = "BG_READING";
    public static final String MARKER_TYPE_BG = "BG";
    public static final String MARKER_TYPE_INSULIN = "INSULIN";
    public static final String MARKER_TYPE_AUTO_BASAL = "AUTO_BASAL_DELIVERY";
    public static final String MARKER_TYPE_AUTO_MODE_STATUS = "AUTO_MODE_STATUS";

    public boolean isBloodGlucose() {
        if (type == null)
            return false;
        else
            return (type.equals(MARKER_TYPE_BG_READING) || type.equals(MARKER_TYPE_CALIBRATION) || type.equals(MARKER_TYPE_BG));
    }

    public String type;
    public int index;
    public Double value;
    public String kind;
    public int version;
    public Date dateTime;
    public Integer relativeOffset;
    public Boolean calibrationSuccess;
    public Double amount;
    public Float programmedExtendedAmount;
    public String activationType;
    public Float deliveredExtendedAmount;
    public Float programmedFastAmount;
    public Integer programmedDuration;
    public Float deliveredFastAmount;
    public Integer effectiveDuration;
    public Boolean completed;
    public String bolusType;
    public Boolean autoModeOn;
    public Float bolusAmount;

}