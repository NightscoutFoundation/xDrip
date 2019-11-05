package com.eveningoutpost.dexdrip.watch.miband.message;

import java.nio.ByteBuffer;

public class OperationCodes {
    public static final byte[] OPCODE_AUTH_REQ = {0x01, 0x00};
    public static final byte[] OPCODE_AUTH_REQ2 = {0x02, 0x00}; //Get random localKey from band
    public static final byte[] OPCODE_AUTH_REQ3 = {0x03, 0x00};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE1 = {0x10 , 0x01, 0x01}; //user confirmed authorisation by pushing button on band
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE2 = {0x10, 0x02, 0x01};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE_SUCCESS = {0x10, 0x03, 0x01};
    public static final byte[] OPCODE_AUTH_NOTIFY_RESPONSE_ERROR = {0x10, 0x03, 0x04};

    public static byte ENDPOINT_DISPLAY_ITEMS = 0x0a;

    public static byte DISPLAY_ITEM_BIT_CLOCK = 0x01;
    public static byte DISPLAY_ITEM_BIT_STEPS = 0x02;
    public static byte DISPLAY_ITEM_BIT_DISTANCE = 0x04;
    public static byte DISPLAY_ITEM_BIT_CALORIES= 0x08;
    public static byte DISPLAY_ITEM_BIT_HEART_RATE = 0x10;
    public static byte DISPLAY_ITEM_BIT_BATTERY = 0x20;

    public static int SCREEN_CHANGE_BYTE = 1;


    public static byte ENDPOINT_DISPLAY = 0x06;
    public static final byte[] DATEFORMAT_TIME_24_HOURS = new byte[] {ENDPOINT_DISPLAY, 0x02, 0x0, 0x1 };


    public static boolean isCommandEqual(byte[] command, byte[] buffer){
       return ByteBuffer.wrap(command, 0, command.length).equals(ByteBuffer.wrap(buffer, 0, command.length));
    }
}
