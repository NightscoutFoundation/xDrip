package com.eveningoutpost.dexdrip.utilitymodels;

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
    String EXTRA_SENSOR_STARTED_AT = "com.eveningoutpost.dexdrip.Extras.SensorStartedAt";
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
    String ACTION_NEW_FOOD = "info.nightscout.client.NEW_FOOD";
    String ACTION_NEW_DEVICESTATUS = "info.nightscout.client.NEW_DEVICESTATUS";
    String ACTION_CHANGED_TREATMENT = "info.nightscout.client.CHANGED_TREATMENT";
    String ACTION_REMOVED_TREATMENT = "info.nightscout.client.REMOVED_TREATMENT";
    String ACTION_NEW_PROFILE = "info.nightscout.client.NEW_PROFILE";
    String ACTION_NEW_SGV = "info.nightscout.client.NEW_SGV";
    String ACTION_NS_BRIDGE = "info.nightscout.client.NS_BRIDGE";



    // Listen on
    String ACTION_DATABASE = "info.nightscout.client.DBACCESS";
    String LIBRE_ALARM_TO_XDRIP_PLUS = "com.eveningoutpost.dexdrip.FROM_LIBRE_ALARM";
    String XDRIP_PLUS_NS_EMULATOR = "com.eveningoutpost.dexdrip.NS_EMULATOR";
    String BLUEJAY_THINJAM_API = "com.eveningoutpost.dexdrip.THINJAM_API";
    String BLUEJAY_THINJAM_EMIT = "com.eveningoutpost.dexdrip.THINJAM_EMIT";
    // Local Broadcasts
    String HOME_STATUS_ACTION = "com.eveningoutpost.dexdrip.HOME_STATUS_ACTION";
    
    // Send to external decoder
    String XDRIP_PLUS_LIBRE_DATA = "com.eveningoutpost.dexdrip.LIBRE_DATA";
    String LIBRE_DATA_BUFFER = "com.eveningoutpost.dexdrip.Extras.DATA_BUFFER";
    String LIBRE_PATCH_UID_BUFFER = "com.eveningoutpost.dexdrip.Extras.LIBRE_PATCH_UID_BUFFER";
    String LIBRE_PATCH_INFO_BUFFER = "com.eveningoutpost.dexdrip.Extras.LIBRE_PATCH_INFO_BUFFER";

    String LIBRE_DATA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.TIMESTAMP";
    String LIBRE_SN = "com.eveningoutpost.dexdrip.Extras.LIBRE_SN";
    String LIBRE_RAW_ID = "com.eveningoutpost.dexdrip.Extras.LIBRE_RAW_ID";

    String LIBRE2_BG = "com.librelink.app.ThirdPartyIntegration.GLUCOSE_READING";
    String LIBRE2_ACTIVATION = "com.librelink.app.ThirdPartyIntegration.SENSOR_ACTIVATE";
    String LIBRE2_SCAN = "com.librelink.app.ThirdPartyIntegration.SENSOR_SCAN";
    String LIBRE2_CONNECTION = "com.librelink.app.ThirdPartyIntegration.CONNECTION_STATE";


    // oop 2 
    String XDRIP_DECODE_FARM_RESULT = "com.eveningoutpost.dexdrip.OOP2_DECODE_FARM_RESULT";
    String XDRIP_DECODE_BLE_RESULT = "com.eveningoutpost.dexdrip.OOP2_DECODE_BLE_RESULT";
    String XDRIP_BLUETOOTH_ENABLE_RESULT = "com.eveningoutpost.dexdrip.OOP2_BLUETOOTH_ENABLE_RESULT";
    String XDRIP_PLUS_LIBRE_BLE_DATA = "com.eveningoutpost.dexdrip.LIBRE_BLE_DATA";
    String XDRIP_PLUS_BLUETOOTH_ENABLE = "com.eveningoutpost.dexdrip.BLUETOOTH_ENABLE";
    String DECODED_BUFFER = "DecodedBuffer";
    String PATCH_UID = "PatchUid";
    String PATCH_INFO = "PatchInfo";
    String ENABLE_TIME = "EnableTime";
    String CONNECTION_INDEX = "ConnectionIndex";
    String BT_UNLOCK_BUFFER_COUNT = "BtUnlockBufferCount";
    String BT_UNLOCK_BUFFER = "BtUnlockBuffer";
    String NFC_UNLOCK_BUFFER = "NfcUnlockBuffer";
    String DEVICE_NAME = "DeviceName";
    String DEVICE_MAC_ADDRESS = "MacAddress";
    String BT_UNLOCK_BUFFER_ARRAY = "BtUnlockBufferArray";
    String TREND_BG = "TrendBg";
    String HISTORIC_BG = "HistoricBg";
    String OOP2_VERSION_NAME = "OOP2_VERSION";
    
    String TAG_ID = "TagId";
    String ROW_ID = "RowId";

    String PREFERENCE_INTENT = "com.eveningoutpost.dexdrip.utils.PROGRESS";
}
