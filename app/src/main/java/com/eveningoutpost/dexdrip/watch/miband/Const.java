package com.eveningoutpost.dexdrip.watch.miband;

import java.util.UUID;

public class Const {

    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString(String.format(BASE_UUID, "180D"));
    public static final UUID UUID_SERVICE_FIRMWARE_SERVICE = UUID.fromString("00001530-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_FIRMWARE = UUID.fromString("00001531-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_DATA = UUID.fromString("00001532-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_3_CONFIGURATION  = UUID.fromString("00000003-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC4 = UUID.fromString("00000004-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_5_ACTIVITY_DATA = UUID.fromString("00000005-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_6_BATTERY_INFO = UUID.fromString("00000006-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_7_REALTIME_STEPS = UUID.fromString("00000007-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_8_USER_SETTINGS = UUID.fromString("00000008-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_AUTH = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_DEVICEEVENT = UUID.fromString("00000010-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_AUDIO = UUID.fromString("00000012-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_AUDIODATA = UUID.fromString("00000013-0000-3512-2118-0009af100700");
    //Main service
    public static final UUID UUID_SERVICE_FEE0 = UUID.fromString(String.format(BASE_UUID, "FEE0"));

    //Device information, read
    public static final UUID UUID_CHAR_DEVICE_INFO = UUID.fromString(String.format(BASE_UUID, "FF01"));

    //Device name, read and write
    //public static final UUID UUID_CHAR_DEVICE_NAME = UUID.fromString(String.format(BASE_UUID, "FF02"));

    //General notification, read and notify
    public static final UUID UUID_CHAR_NOTIFICATION = UUID.fromString(String.format(BASE_UUID, "FF03"));

    //User information, read and write
    public static final UUID UUID_CHAR_USER_INFO = UUID.fromString(String.format(BASE_UUID, "FF04"));

    //Control, such as vibration, etc, write
    public static final UUID UUID_CHAR_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "FF05"));

    //Real-time steps Notifications, read and notify
    public static final UUID UUID_CHAR_REALTIME_STEPS = UUID.fromString(String.format(BASE_UUID, "FF06"));

    //Activity information, read and indicate
    public static final UUID UUID_CHAR_ACTIVITY_DATA = UUID.fromString(String.format(BASE_UUID, "FF07"));

    //Firmware information, write without response
    public static final UUID UUID_CHAR_FIRMWARE_DATA = UUID.fromString(String.format(BASE_UUID, "FF08"));

    //LE params, read and write
    public static final UUID UUID_CHAR_LE_PARAMS = UUID.fromString(String.format(BASE_UUID, "FF09"));

    //Date time, read and write
    public static final UUID UUID_CHAR_DATA_TIME = UUID.fromString(String.format(BASE_UUID, "FF0A"));

    //Statistics, read and write
    public static final UUID UUID_CHAR_STATISTICS = UUID.fromString(String.format(BASE_UUID, "FF0B"));

    //Battery, read and notify
    public static final UUID UUID_CHAR_BATTERY = UUID.fromString(String.format(BASE_UUID, "FF0C"));

    //Self-test, read and write
    public static final UUID UUID_CHAR_TEST = UUID.fromString(String.format(BASE_UUID, "FF0D"));

    //Sensor data, read and notify
    public static final UUID UUID_CHAR_SENSOR_DATA = UUID.fromString(String.format(BASE_UUID, "FF0E"));

    //Pairing, read and write
    public static final UUID UUID_CHAR_PAIR = UUID.fromString(String.format(BASE_UUID, "FF0F"));

    public static final UUID UUID_DESCRIPTOR_UPDATE_NOTIFICATION = UUID.fromString(String.format(BASE_UUID, "2902"));

    public static final UUID UUID_SERVICE_VIBRATE = UUID.fromString(String.format(BASE_UUID, "1802"));

    //EXTRAS (FROM : https://github.com/Freeyourgadget/Gadgetbridge/blob/master/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/miband/MiBandService.java)
    public static final UUID UUID_SERVICE_GENERIC_ACCESS = UUID.fromString(String.format(BASE_UUID, "1800"));
    public static final UUID UUID_SERVICE_GENERIC_ATTRIBUTE = UUID.fromString(String.format(BASE_UUID, "1801"));

    public static final UUID UUID_CHAR_ALERT_CATEGORY_ID = UUID.fromString(String.format(BASE_UUID, "2A43"));
    public static final UUID UUID_CHAR_ALERT_CATEGORY_ID_BIT_MASK = UUID.fromString(String.format(BASE_UUID, "2A42"));
    public static final UUID UUID_CHAR_ALERT_LEVEL = UUID.fromString(String.format(BASE_UUID, "2A06"));
    public static final UUID UUID_CHAR_ALERT_NOTIFICATION_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "2A44"));
    public static final UUID UUID_CHAR_ALERT_STATUS = UUID.fromString(String.format(BASE_UUID, "2A3F"));


    public static final UUID UUID_CHAR_BLOOD_PRESSURE_FEATURE = UUID.fromString(String.format(BASE_UUID, "2A49"));
    public static final UUID UUID_CHAR_BLOOD_PRESSURE_MEASUREMENT = UUID.fromString(String.format(BASE_UUID, "2A35"));

    public static final UUID UUID_CHAR_BODY_SENSOR_LOCATION = UUID.fromString(String.format(BASE_UUID, "2A38"));
    public static final UUID UUID_CHAR_CURRENT_TIME = UUID.fromString(String.format(BASE_UUID, "2A2B"));

    public static final UUID UUID_CHAR_DATE_TIME = UUID.fromString(String.format(BASE_UUID, "2A08"));
    public static final UUID UUID_CHAR_DAY_DATE_TIME = UUID.fromString(String.format(BASE_UUID, "2A0A"));
    public static final UUID UUID_CHAR_DAY_OF_WEEK = UUID.fromString(String.format(BASE_UUID, "2A09"));

    public static final UUID UUID_CHAR_DEVICE_NAME = UUID.fromString(String.format(BASE_UUID, "2A00"));
    public static final UUID UUID_CHAR_APPEARANCE = UUID.fromString(String.format(BASE_UUID, "2A01"));

    public static final UUID UUID_CHAR_DST_OFFSET = UUID.fromString(String.format(BASE_UUID, "2A0D"));
    public static final UUID UUID_CHAR_EXACT_TIME_256 = UUID.fromString(String.format(BASE_UUID, "2A0C"));
    public static final UUID UUID_CHAR_FIRMWARE_REVISION_STRING = UUID.fromString(String.format(BASE_UUID, "2A26"));
    public static final UUID UUID_CHAR_HARDWARE_REVISION_STRING = UUID.fromString(String.format(BASE_UUID, "2A27"));

    public static final UUID UUID_CHAR_HEART_RATE_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "2A39"));
    public static final UUID UUID_CHAR_HEART_RATE_MEASUREMENT = UUID.fromString(String.format(BASE_UUID, "2A37"));

    public static final UUID UUID_CHAR_IEEE_11073_REGULATORY = UUID.fromString(String.format(BASE_UUID, "2A2A"));

    public static final UUID UUID_CHAR_INTERMEDIATE_CUFF_PRESSURE = UUID.fromString(String.format(BASE_UUID, "2A36"));
    public static final UUID UUID_CHAR_INTERMEDIATE_TEMPERATURE = UUID.fromString(String.format(BASE_UUID, "2A1E"));

    public static final UUID UUID_CHAR_LOCAL_TIME_INFORMATION = UUID.fromString(String.format(BASE_UUID, "2A0F"));
    public static final UUID UUID_CHAR_MANUFACTURER_NAME_STRING = UUID.fromString(String.format(BASE_UUID, "2A29"));

    public static final UUID UUID_CHAR_MEASUREMENT_INTERVAL = UUID.fromString(String.format(BASE_UUID, "2A21"));
    public static final UUID UUID_CHAR_MODEL_NUMBER_STRING = UUID.fromString(String.format(BASE_UUID, "2A24"));

    public static final UUID UUID_CHAR_NEW_ALERT = UUID.fromString(String.format(BASE_UUID, "2A46"));

    public static final UUID UUID_CHAR_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = UUID.fromString(String.format(BASE_UUID, "2A04"));
    public static final UUID UUID_CHAR_PERIPHERAL_PRIVACY_FLAG = UUID.fromString(String.format(BASE_UUID, "2A02"));

    public static final UUID UUID_CHAR_RECONNECTION_ADDRESS = UUID.fromString(String.format(BASE_UUID, "2A03"));

    public static final UUID UUID_CHAR_REFERENCE_TIME_INFORMATION = UUID.fromString(String.format(BASE_UUID, "2A14"));

    public static final UUID UUID_CHAR_RINGER_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "2A40"));
    public static final UUID UUID_CHAR_RINGER_SETTING = UUID.fromString(String.format(BASE_UUID, "2A41"));

    public static final UUID UUID_CHAR_SERIAL_NUMBER_STRING = UUID.fromString(String.format(BASE_UUID, "2A25"));

    public static final UUID UUID_CHAR_SERVICE_CHANGED = UUID.fromString(String.format(BASE_UUID, "2A05"));

    public static final UUID UUID_CHAR_SOFTWARE_REVISION_STRING = UUID.fromString(String.format(BASE_UUID, "2A28"));

    public static final UUID UUID_CHAR_SUPPORTED_NEW_ALERT_CATEGORY = UUID.fromString(String.format(BASE_UUID, "2A47"));
    public static final UUID UUID_CHAR_SUPPORTED_UNREAD_ALERT_CATEGORY = UUID.fromString(String.format(BASE_UUID, "2A48"));

    public static final UUID UUID_CHAR_SYSTEM_ID = UUID.fromString(String.format(BASE_UUID, "2A23"));

    public static final UUID UUID_CHAR_TEMPERATURE_MEASUREMENT = UUID.fromString(String.format(BASE_UUID, "2A1C"));
    public static final UUID UUID_CHAR_TEMPERATURE_DEVICETYPE = UUID.fromString(String.format(BASE_UUID, "2A1D"));

    public static final UUID UUID_CHAR_TIME_ACCURACY = UUID.fromString(String.format(BASE_UUID, "2A12"));
    public static final UUID UUID_CHAR_TIME_SOURCE = UUID.fromString(String.format(BASE_UUID, "2A13"));
    public static final UUID UUID_CHAR_TIME_UPDATE_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "2A16"));
    public static final UUID UUID_CHAR_TIME_UPDATE_STATE = UUID.fromString(String.format(BASE_UUID, "2A17"));
    public static final UUID UUID_CHAR_TIME_WITH_DST = UUID.fromString(String.format(BASE_UUID, "2A11"));
    public static final UUID UUID_CHAR_TIME_ZONE = UUID.fromString(String.format(BASE_UUID, "2A0E"));

    public static final UUID UUID_CHAR_TX_POWER_LEVEL = UUID.fromString(String.format(BASE_UUID, "2A07"));
    public static final UUID UUID_CHAR_UNREAD_ALERT_STATUS = UUID.fromString(String.format(BASE_UUID, "2A45"));

    public static final UUID UUID_CHARACTERISTIC_CHUNKEDTRANSFER = UUID.fromString("00000020-0000-3512-2118-0009af100700");

    public static final String MIBAND_NAME_2 = "MI Band 2";
    public static final String MIBAND_NAME_3 = "MI Band 3";
    public static final String MIBAND_NAME_3_1 = "Xiaomi Band 3";
    public static final String MIBAND_NAME_4 = "Mi Smart Band 4";

    public static final int PREFERRED_MTU_SIZE = 247;


    public static final String MIBAND_NOTIFY_TYPE_CANCEL = "NOTIFY_TYPE_CANCEL";
    public static final String MIBAND_NOTIFY_TYPE_CALL = "NOTIFY_TYPE_CALL";
    public static final String MIBAND_NOTIFY_TYPE_MESSAGE = "NOTIFY_TYPE_MESSAGE";
    public static final String MIBAND_NOTIFY_TYPE_ALARM = "NOTIFY_TYPE_ALARM";
}
