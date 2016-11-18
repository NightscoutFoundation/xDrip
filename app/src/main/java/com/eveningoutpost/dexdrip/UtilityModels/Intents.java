package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * For integration.
 */
public interface Intents {
    String RECEIVER_PERMISSION = "com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE";

    String ACTION_NEW_BG_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate";
    String EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate";
    String EXTRA_BG_SLOPE = "com.eveningoutpost.dexdrip.Extras.BgSlope";
    String EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName";
    String EXTRA_SENSOR_BATTERY = "com.eveningoutpost.dexdrip.Extras.SensorBattery";
    String EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time";
    String EXTRA_RAW = "com.eveningoutpost.dexdrip.Extras.Raw";


    String ACTION_NEW_BG_ESTIMATE_NO_DATA = "com.eveningoutpost.dexdrip.BgEstimateNoData";

    // From NS Android Client
    // send
    String ACTION_NEW_TREATMENT = "info.nightscout.client.NEW_TREATMENT";
    String ACTION_CHANGED_TREATMENT = "info.nightscout.client.CHANGED_TREATMENT";
    String ACTION_REMOVED_TREATMENT = "info.nightscout.client.REMOVED_TREATMENT";
    String ACTION_NEW_PROFILE = "info.nightscout.client.NEW_PROFILE";
    String ACTION_NEW_SGV = "info.nightscout.client.NEW_SGV";


    // Listen on
    String ACTION_DATABASE = "info.nightscout.client.DBACCESS";
    String LIBRE_ALARM_TO_XDRIP_PLUS = "com.eveningoutpost.dexdrip.FROM_LIBRE_ALARM";
    String XDRIP_PLUS_NS_EMULATOR = "com.eveningoutpost.dexdrip.NS_EMULATOR";

}
