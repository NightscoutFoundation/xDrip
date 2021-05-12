package com.eveningoutpost.dexdrip.cgm.medtrum;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Const {

    public static final int DEVICE_TYPE_A6 = 165;
    public static final int BLUETOOTH_MTU = 20;
    static final List<Integer> SUPPORTED_DEVICES = new ArrayList<>();

    static final UUID CGM_SERVICE = UUID.fromString("669A9002-0008-968F-E311-6050405558B3");
    static final UUID CGM_CHARACTERISTIC_NOTIFY = UUID.fromString("669A9140-0008-968F-E311-6050405558B3");
    static final UUID CGM_CHARACTERISTIC_INDICATE = UUID.fromString("669A9101-0008-968F-E311-6050405558B3");
    static final int MANUFACTURER_ID = 18305;
    static final long CIPHER_BIT_FLIP = 0x50274781;
    public static final int DEX_RAW_SCALE = 1000;


    public static final int OPCODE_AUTH_REQST = 0x05;
    public static final int OPCODE_AUTH_REPLY = 0x05;
    public static final int OPCODE_STAT_REQST = 0x41;
    public static final int OPCODE_STAT_REPLY = 0x41;

    public static final int OPCODE_READ_REQST = 0x44;
    public static final int OPCODE_READ_REPLY = 0x81;

    public static final int OPCODE_TIME_REQST = 0x57;
    public static final int OPCODE_TIME_REPLY = 0x57;

    public static final int OPCODE_BACK_REQST = 0x42;
    public static final int OPCODE_BACK_REPLY = 0x42;

    public static final int OPCODE_UALM_REQST = 0x54;
    public static final int OPCODE_UALM_REPLY = 0x8a;

    public static final int OPCODE_CONN_REQST = 0x0a;
    public static final int OPCODE_CONN_REPLY = 0x0a;

    public static final int OPCODE_CALI_REQST = 0x40;
    public static final int OPCODE_CALI_REPLY = 0x40;


    static {
        SUPPORTED_DEVICES.add(DEVICE_TYPE_A6);
    }

}
