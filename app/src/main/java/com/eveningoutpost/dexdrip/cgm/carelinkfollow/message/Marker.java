package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

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
    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date timestamp = null;
    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date displayTime = null;
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
    public MarkerData data;

    public Date getDate(){
        if(displayTime != null)
            return displayTime;
        else if(timestamp != null)
            return timestamp;
        else if(dateTime != null)
            return dateTime;
        else
            return null;
    }

    public Float getInsulinAmount(){
        if(deliveredExtendedAmount != null && deliveredFastAmount != null)
            return deliveredExtendedAmount + deliveredFastAmount;
        else if(data.dataValues != null && data.dataValues.deliveredFastAmount != null)
            return data.dataValues.deliveredFastAmount;
        else if(data != null && data.dataValues != null && data.dataValues.insulinUnits != null)
            try {
                return Float.parseFloat(data.dataValues.insulinUnits);
            } catch (Exception ex){
                return null;
            }
        else
            return null;
    }

    public Double getCarbAmount(){
        if(amount != null)
            return amount;
        else if(data.dataValues.amount != null)
            return data.dataValues.amount;
        else
            return null;
    }

    public Double getBloodGlucose()
    {
        if(value != null)
            return value;
        else if(data.dataValues.unitValue != null)
            return data.dataValues.unitValue;
        else
            return null;
    }

}