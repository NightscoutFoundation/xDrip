package com.eveningoutpost.dexdrip.watch.miband.message;

import java.nio.ByteBuffer;

public class OperationCodes {
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 1: sending a "secret" key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_KEY = 0x01;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 2: requesting a random authentication key from the band.
     * This is byte 0, followed by {@link #AUTH_BYTE}.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_REQUEST_RANDOM_AUTH_NUMBER = 0x02;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 3: sending the encrypted random authentication key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the encrypted random authentication key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_ENCRYPTED_AUTH_NUMBER = 0x03;

    /**
     * Received in response to any authentication requests (byte 0 in the byte[] value.
     */
    public static final byte AUTH_RESPONSE = 0x10;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x01 means success.
     */
    public static final byte AUTH_SUCCESS = 0x01;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x04 means failure.
     */
    public static final byte AUTH_FAIL = 0x04;
    public static final byte AUTH_MIBAND4_FAIL = 0x51;
    public static final byte AUTH_MIBAND4_CODE_FAIL = 0x08;

    public static final byte AUTH_MIBAND4_CRYPT_FLAG = (byte)0x80;


    /**
     * In some logs it's 0x08...
     */
    public static final byte AUTH_BYTE = 0x00;

    public static final byte[] OPCODE_AUTH_REQ = {AUTH_SEND_KEY, AUTH_BYTE};
    public static final byte[] OPCODE_AUTH_REQ2 = {AUTH_REQUEST_RANDOM_AUTH_NUMBER, AUTH_BYTE}; //Get random localKey from band
    public static final byte[] OPCODE_AUTH_REQ3 = {AUTH_SEND_ENCRYPTED_AUTH_NUMBER, AUTH_BYTE};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE1 = {AUTH_RESPONSE , AUTH_SEND_KEY, AUTH_SUCCESS}; //user confirmed authorisation by pushing button on band
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE2 = {AUTH_RESPONSE, AUTH_REQUEST_RANDOM_AUTH_NUMBER, AUTH_SUCCESS};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE_SUCCESS = {AUTH_RESPONSE, AUTH_SEND_ENCRYPTED_AUTH_NUMBER, AUTH_SUCCESS};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE_ERROR = {AUTH_RESPONSE, AUTH_SEND_ENCRYPTED_AUTH_NUMBER, AUTH_FAIL};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE_ERROR_MIBAND4 = {AUTH_RESPONSE, AUTH_SEND_KEY, AUTH_MIBAND4_FAIL};

    public static byte ENDPOINT_DISPLAY_ITEMS = 0x0a;

    public static byte DISPLAY_ITEM_BIT_CLOCK = 0x01;
    public static byte DISPLAY_ITEM_BIT_STEPS = 0x02;
    public static byte DISPLAY_ITEM_BIT_DISTANCE = 0x04;
    public static byte DISPLAY_ITEM_BIT_CALORIES= 0x08;
    public static byte DISPLAY_ITEM_BIT_HEART_RATE = 0x10;
    public static byte DISPLAY_ITEM_BIT_BATTERY = 0x20;

    public static int SCREEN_CHANGE_BYTE = 1;


    public static byte ENDPOINT_DISPLAY = 0x06;

    public static final byte[] COMMAND_CHANGE_SCREENS_MIBAND2 = new byte[]{ENDPOINT_DISPLAY_ITEMS, DISPLAY_ITEM_BIT_CLOCK, 0x00, 0x00, 0x1, 0x02, 0x03, 0x04, 0x05};
    public static final byte[] COMMAND_CHANGE_SCREENS_MIDAND3_4 = new byte[]{ENDPOINT_DISPLAY_ITEMS, DISPLAY_ITEM_BIT_CLOCK, 0x30, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] DATEFORMAT_DATE_TIME = new byte[] {ENDPOINT_DISPLAY, 0x0a, 0x0, 0x03 };
    public static final byte[] DATEFORMAT_TIME = new byte[] {ENDPOINT_DISPLAY, 0x0a, 0x0, 0x0 };
    public static final byte[] DATEFORMAT_TIME_12_HOURS = new byte[] {ENDPOINT_DISPLAY, 0x02, 0x0, 0x0 };
    public static final byte[] DATEFORMAT_TIME_24_HOURS = new byte[] {ENDPOINT_DISPLAY, 0x02, 0x0, 0x1 };
    public static final byte[] DATEFORMAT_DATE_MM_DD_YYYY = new byte[]{ENDPOINT_DISPLAY, 30, 0x00, 'M', 'M', '/', 'd', 'd', '/', 'y', 'y', 'y', 'y'};
    public static final byte[] COMMAND_ENBALE_VISIBILITY = new byte[]{ENDPOINT_DISPLAY, 0x01, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_VISIBILITY = new byte[]{ENDPOINT_DISPLAY, 0x01, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST = new byte[]{ENDPOINT_DISPLAY, 0x05, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST = new byte[]{ENDPOINT_DISPLAY, 0x05, 0x00, 0x00};
    public static final byte[] COMMAND_SCHEDULE_DISPLAY_ON_LIFT_WRIST = new byte[]{ENDPOINT_DISPLAY, 0x05, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_GOAL_NOTIFICATION = new byte[]{ENDPOINT_DISPLAY, 0x06, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_GOAL_NOTIFICATION = new byte[]{ENDPOINT_DISPLAY, 0x06, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_ROTATE_WRIST_TO_SWITCH_INFO = new byte[]{ENDPOINT_DISPLAY, 0x0d, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_ROTATE_WRIST_TO_SWITCH_INFO = new byte[]{ENDPOINT_DISPLAY, 0x0d, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_DISPLAY_CALLER = new byte[]{ENDPOINT_DISPLAY, 0x10, 0x00, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_DISPLAY_CALLER = new byte[]{ENDPOINT_DISPLAY, 0x10, 0x00, 0x00, 0x00};
    public static final byte[] DISPLAY_YYY = new byte[] {ENDPOINT_DISPLAY, 0x10, 0x0, 0x1, 0x1 };
    public static final byte[] COMMAND_DISTANCE_UNIT_METRIC = new byte[] { ENDPOINT_DISPLAY, 0x03, 0x00, 0x00 };
    public static final byte[] COMMAND_DISTANCE_UNIT_IMPERIAL = new byte[] { ENDPOINT_DISPLAY, 0x03, 0x00, 0x01 };
    public static final byte[] COMMAND_SET_LANGUAGE_NEW_TEMPLATE = new byte[]{ENDPOINT_DISPLAY, 0x17, 0x00, 0, 0, 0, 0, 0};
    public static final byte[] COMMAND_FACTORY_RESET = new byte[]{ENDPOINT_DISPLAY, 0x0b, 0x00, 0x01};
    public static final byte[] COMMAND_ENABLE_DISCONNECT_NOTIFCATION = new byte[]{ENDPOINT_DISPLAY, 0x0c, 0x00, 0x01};//, 0, 0, 0, 0};
    public static final byte[] COMMAND_DISABLE_DISCONNECT_NOTIFCATION = new byte[]{ENDPOINT_DISPLAY, 0x0c, 0x00, 0x00};//, 0, 0, 0, 0};
    public static final byte[] COMMAND_ACK_FIND_PHONE_IN_PROGRESS = new byte[]{ENDPOINT_DISPLAY, 0x14, 0x00, 0x00};

    public static final byte[] COMMAND_DISABLE_CALL = new byte[]{0x00,(byte) 0xc0,0x00,0x03,0x03,0x00,0x00,0x00,0x00};


    public static final byte RESPONSE = 0x10;

    public static final byte SUCCESS = 0x01;

    public static final byte TIMER_RUNNING = 0x21;
    public static final byte LOW_BATTERY_ERROR = 0x22;
    public static final byte ON_CALL = 0x22;

    public static final byte COMMAND_FIRMWARE_INIT = 0x01; // to UUID_CHARACTERISTIC_FIRMWARE, followed by fw file size in bytes
    public static final byte COMMAND_FIRMWARE_START_DATA = 0x03; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_UPDATE_SYNC = 0x00; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_CHECKSUM = 0x04; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_REBOOT = 0x05; // to UUID_CHARACTERISTIC_FIRMWARE

    public static final byte[] RESPONSE_FINISH_INIT_SUCCESS = new byte[] {RESPONSE, COMMAND_FIRMWARE_INIT, SUCCESS };
    public static final byte[] RESPONSE_FIRMWARE_DATA_SUCCESS = new byte[] {RESPONSE, COMMAND_FIRMWARE_START_DATA, SUCCESS };

    private static final int CONN_INTERVAL_MIN = 240;
    private static final int CONN_INTERVAL_MAX = 240;
    private static final int CONN_SLAVE_LATENCY = 0x0;
    private static final int CONN_SUPERVISION_TIMEOUT = 500;

    public static final byte[] COMMAND_NIGHT_MODE_OFF = new byte[]{0x1a, 0x00};
    public static final byte[] COMMAND_NIGHT_MODE_SUNSET = new byte[]{0x1a, 0x02};
    public static final byte[] COMMAND_NIGHT_MODE_SCHEDULED = new byte[]{0x1a, 0x01, 0x10, 0x00, 0x07, 0x00};

    public static final byte[] SET_CONNECTION_PARAM = {
            (byte) (CONN_INTERVAL_MIN & 0x00FF), // gets LSB of 2 byte value
            (byte) ((CONN_INTERVAL_MIN & 0xFF00) >> 8), // gets MSB of 2 byte value
            (byte) (CONN_INTERVAL_MAX & 0x00FF),
            (byte) ((CONN_INTERVAL_MAX & 0xFF00) >> 8),
            (byte) (CONN_SLAVE_LATENCY & 0x00FF),
            (byte) ((CONN_SLAVE_LATENCY & 0xFF00) >> 8),
            (byte) (CONN_SUPERVISION_TIMEOUT & 0x00FF),
            (byte) ((CONN_SUPERVISION_TIMEOUT & 0xFF00) >> 8)
    };

    public static boolean isCommandEqual(byte[] command, byte[] buffer){
       return ByteBuffer.wrap(command, 0, command.length).equals(ByteBuffer.wrap(buffer, 0, command.length));
    }
}
