package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * For integration.
 */
public interface Intents {
    String RECEIVER_PERMISSION = "com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE";

    String ACTION_NEW_BG_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate";
    String EXTRA_SENDER = "com.eveningoutpost.dexdrip.Extras.Sender";
    String EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate";
    String EXTRA_BG_SLOPE = "com.eveningoutpost.dexdrip.Extras.BgSlope";
    String EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName";
    String EXTRA_SENSOR_BATTERY = "com.eveningoutpost.dexdrip.Extras.SensorBattery";
    String EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time";
    String EXTRA_RAW = "com.eveningoutpost.dexdrip.Extras.Raw";
    String EXTRA_NOISE = "com.eveningoutpost.dexdrip.Extras.Noise";
    String EXTRA_NOISE_WARNING = "com.eveningoutpost.dexdrip.Extras.NoiseWarning";
    String EXTRA_NOISE_BLOCK_LEVEL = "com.eveningoutpost.dexdrip.Extras.NoiseBlockLevel";
    String EXTRA_NS_NOISE_LEVEL = "com.eveningoutpost.dexdrip.Extras.NsNoiseLevel";
    String XDRIP_DATA_SOURCE_DESCRIPTION = "com.eveningoutpost.dexdrip.Extras.SourceDesc";
    String XDRIP_DATA_SOURCE_INFO = "com.eveningoutpost.dexdrip.Extras.SourceInfo";
    String XDRIP_VERSION_INFO = "com.eveningoutpost.dexdrip.Extras.VersionInfo";
    String XDRIP_CALIBRATION_INFO = "com.eveningoutpost.dexdrip.Extras.CalibrationInfo";
    String XDRIP_CALIBRATION_PLUGIN = "com.eveningoutpost.dexdrip.Extras.CalibrationPluginInfo";

    String ACTION_REMOTE_CALIBRATION = "com.eveningoutpost.dexdrip.NewCalibration";
    String ACTION_NEW_BG_ESTIMATE_NO_DATA = "com.eveningoutpost.dexdrip.BgEstimateNoData";
    String ACTION_STATUS_UPDATE = "com.eveningoutpost.dexdrip.StatusUpdate";
    String ACTION_SNOOZE = "com.eveningoutpost.dexdrip.Snooze";

    String ACTION_VEHICLE_MODE = "com.eveningoutpost.dexdrip.VehicleMode";
    String EXTRA_VEHICLE_MODE_ENABLED = "com.eveningoutpost.dexdrip.VehicleMode.Enabled";

    String EXTRA_COLLECTOR_NANOSTATUS = "com.eveningoutpost.dexdrip.Extras.Collector.NanoStatus";

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
    String BLUEJAY_THINJAM_API = "com.eveningoutpost.dexdrip.THINJAM_API";
    String BLUEJAY_THINJAM_EMIT = "com.eveningoutpost.dexdrip.THINJAM_EMIT";

    // Local Broadcasts
    String HOME_STATUS_ACTION = "com.eveningoutpost.dexdrip.HOME_STATUS_ACTION";
    
    // Send to LibreXposed
    String XDRIP_PLUS_LIBRE_DATA = "com.eveningoutpost.dexdrip.LIBRE_DATA";
    String LIBRE_DATA_BUFFER = "com.eveningoutpost.dexdrip.Extras.DATA_BUFFER";
    String LIBRE_PATCH_UID_BUFFER = "com.eveningoutpost.dexdrip.Extras.LIBRE_PATCH_UID_BUFFER";
    String LIBRE_PATCH_INFO_BUFFER = "com.eveningoutpost.dexdrip.Extras.LIBRE_PATCH_INFO_BUFFER";

    String LIBRE_DATA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.TIMESTAMP";
    String LIBRE_SN = "com.eveningoutpost.dexdrip.Extras.LIBRE_SN";
    String LIBRE_RAW_ID = "com.eveningoutpost.dexdrip.Extras.LIBRE_RAW_ID";

    String LIBRE2_BG = "com.librelink.app.ThirdPartyIntegration.GLUCOSE_READING";
    String LIBRE2_ACTIVATION = "com.librelink.app.ThirdPartyIntegration.SENSOR_ACTIVATE";



    String PREFERENCE_INTENT = "com.eveningoutpost.dexdrip.utils.PROGRESS";
}
