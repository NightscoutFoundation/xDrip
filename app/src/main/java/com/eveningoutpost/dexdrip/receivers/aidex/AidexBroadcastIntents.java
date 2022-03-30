package com.eveningoutpost.dexdrip.receivers.aidex;

/**
 * created by Andy 4/3/2022
 */
public interface AidexBroadcastIntents {
    // DON'T CHANGE THIS - START

    /**
     * Permission receiving application will need to use if not running in old model
      */
    String RECEIVER_PERMISSION = "com.microtechmd.cgms.aidex.permissions.RECEIVE_BG_ESTIMATE";

    /**
     * Aidex Action: New Bg Estimate
     */
    String ACTION_NEW_BG_ESTIMATE = "com.microtechmd.cgms.aidex.action.BgEstimate";

    /**
     * Aidex Action: Calibration
     */
    String ACTION_CALIBRATION = "com.microtechmd.cgms.aidex.action.Calibration";

    /**
     * Aidex Action: New Sensor
     */
    String ACTION_SENSOR_NEW = "com.microtechmd.cgms.aidex.action.SensorNew";

    /**
     * Aidex Action: Restart Sensor
     */
    String ACTION_SENSOR_RESTART = "com.microtechmd.cgms.aidex.action.SensorRestart";

    /**
     * Aidex Action: Stop Sensor
     */
    String ACTION_SENSOR_STOP = "com.microtechmd.cgms.aidex.action.SensorStop";

    /**
     * Aidex Action: Notification
     */
    String ACTION_NOTIFICATION = "com.microtechmd.cgms.aidex.action.Notification";


    // DATA
    /**
     * BG Type: Can be either mmol/l or mg/dl
     */
    String AIDEX_BG_TYPE = "com.microtechmd.cgms.aidex.BgType";

    /**
     * BG Value: Its float (so it can be 10.0 or 181.0)
     */
    String AIDEX_BG_VALUE = "com.microtechmd.cgms.aidex.BgValue";

    /**
     * BG Slope Name: following values are valid:
     * "DoubleUp", "SingleUp", "FortyFiveUp", "Flat", "FortyFiveDown", "SingleDown",
     * "DoubleDown", "NotComputable", "RateOutOfRange"
     */
    String AIDEX_BG_SLOPE_NAME = "com.microtechmd.cgms.aidex.BgSlopeName";

    /**
     * Timestamp as Epoch (System.currentTimeMillis()) in miliseconds
     */
    String AIDEX_TIMESTAMP = "com.microtechmd.cgms.aidex.Time";  // epoch in ms

    /**
     * Transmitter Id (String)
     */
    String AIDEX_TRANSMITTER_SN = "com.microtechmd.cgms.aidex.TransmitterSerialNumber";

    /**
     * Sensor Id (String)
     */
    String AIDEX_SENSOR_ID = "com.microtechmd.cgms.aidex.SensorId";

    /**
     * Message Type (SENSOR_ERROR, CALIBRATION_REQUESTED, BATTERY_LOW,
     *     BATTERY_EMPTY, SENSOR_EXPIRED, OTHER)
      */
    String AIDEX_MESSAGE_TYPE = "com.microtechmd.cgms.aidex.MessageType";

    /**
     * Message Value
     */
    String AIDEX_MESSAGE_VALUE = "com.microtechmd.cgms.aidex.MessageValue";

    String UNIT_MMOL_L = "mmol/l";
    String UNIT_MG_DL = "mg/dl";

    // DON'T CHANGE THIS - END

    // You can add your own data here

}
