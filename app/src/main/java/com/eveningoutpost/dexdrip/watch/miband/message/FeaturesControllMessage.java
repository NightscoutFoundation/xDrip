package com.eveningoutpost.dexdrip.watch.miband.message;

import android.util.Pair;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.UUID;

public class FeaturesControllMessage extends BaseMessage {
    private static final String TAG = FeaturesControllMessage.class.getSimpleName();

    public static final int FEATURE_CLOCK_FORMAT = 0;
    public static final int FEATURE_SHOW_DATE = 1;
    public static final int FEATURE_DISPLAY_ON_LIFT_WRIST = 2;
    public static final int FEATURE_SWITCH_DISPLAY_ON_LIFT_WRIST = 3;
    public static final int FEATURE_UNITS = 4;
    public static final int FEATURE_ANTI_LOST = 5;
    public static final int FEATURE_GOAL_NOTIFICATION = 6;
    public static final int FEATURE_VISISBILITY = 7;

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    public byte[] getClockFormatCmd(Boolean is24HourFormat) {
        init(4);
        if (is24HourFormat)
            putData(OperationCodes.DATEFORMAT_TIME_24_HOURS);
        else
            putData(OperationCodes.DATEFORMAT_TIME_12_HOURS);
        return getBytes();
    }

    public byte[] getShowDateCmd(Boolean isDateEnabled) {
        init(4);
        if (isDateEnabled)
            putData(OperationCodes.DATEFORMAT_DATE_TIME);
        else
            putData(OperationCodes.DATEFORMAT_TIME);
        return getBytes();
    }

    public byte[] getDisplayOnWristCmd(Boolean isEnabled) {
        init(4);
        if (isEnabled)
            putData(OperationCodes.COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST);
        else
            putData(OperationCodes.COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST);
        return getBytes();
    }

    public byte[] getSwithDisplayOnWristCmd(Boolean isEnabled) {
        init(4);
        if (isEnabled)
            putData(OperationCodes.COMMAND_ENABLE_ROTATE_WRIST_TO_SWITCH_INFO);
        else
            putData(OperationCodes.COMMAND_DISABLE_ROTATE_WRIST_TO_SWITCH_INFO);
        return getBytes();
    }

    public byte[] getAntiLostCmd(Boolean isEnabled) {
        init(4);
        if (isEnabled)
            putData(OperationCodes.COMMAND_ENABLE_DISCONNECT_NOTIFCATION);
        else
            putData(OperationCodes.COMMAND_DISABLE_DISCONNECT_NOTIFCATION);
        return getBytes();
    }

    public byte[] getGoalNotificationCmd(Boolean isEnabled) {
        init(4);
        if (isEnabled)
            putData(OperationCodes.COMMAND_ENABLE_GOAL_NOTIFICATION);
        else
            putData(OperationCodes.COMMAND_DISABLE_GOAL_NOTIFICATION);
        return getBytes();
    }

    public byte[] getUnitsCmd(Boolean isMetric) {
        init(4);
        if (isMetric)
            putData(OperationCodes.COMMAND_DISTANCE_UNIT_METRIC);
        else
            putData(OperationCodes.COMMAND_DISTANCE_UNIT_IMPERIAL);
        return getBytes();
    }

    public byte[] getVisibilityCmd(Boolean isVisible) {
        init(4);
        if (isVisible)
            putData(OperationCodes.COMMAND_ENBALE_VISIBILITY);
        else
            putData(OperationCodes.COMMAND_DISABLE_VISIBILITY);
        return getBytes();
    }

    public byte[] getMessage(Pair<Integer, Boolean> feature){
        byte[] message = new byte[0];
        switch (feature.first){
            case FEATURE_CLOCK_FORMAT:
                message = getClockFormatCmd(feature.second);
                break;
            case FEATURE_SHOW_DATE:
                message = getShowDateCmd(feature.second);
                break;
            case FEATURE_DISPLAY_ON_LIFT_WRIST:
                message = getDisplayOnWristCmd(feature.second);
                break;
            case FEATURE_SWITCH_DISPLAY_ON_LIFT_WRIST:
                message = getSwithDisplayOnWristCmd(feature.second);
                break;
            case FEATURE_UNITS:
                message = getUnitsCmd(feature.second);
                break;
            case FEATURE_ANTI_LOST:
                message = getAntiLostCmd(feature.second);
                break;
            case FEATURE_GOAL_NOTIFICATION:
                message = getGoalNotificationCmd(feature.second);
                break;
            case FEATURE_VISISBILITY:
                message = getVisibilityCmd(feature.second);
                break;
        }
        return message;
    }

}
