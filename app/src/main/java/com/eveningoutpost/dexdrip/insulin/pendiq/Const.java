package com.eveningoutpost.dexdrip.insulin.pendiq;

import java.util.UUID;

public class Const {

    public static final double MINIMUM_DOSE = 0.5d;

    public static final UUID PENDIQ_SERVICE = UUID.fromString("783b03e-8535-b5a0-7140-a304d2495cb7");
    public static final UUID OUTGOING_CHAR = UUID.fromString("783b03e-8535-b5a0-7140-a304d2495cba");
    public static final UUID INCOMING_CHAR = UUID.fromString("783b03e-8535-b5a0-7140-a304d2495cb8");

    public static final byte COMMAND_PACKET = 0x01;
    public static final byte PROGRESS_PACKET = 0x02;
    public static final byte REPORT_PACKET = 0x03;
    public static final byte RESULT_PACKET = 0x04;

    public static final byte INSULIN_RESULT = 0x01;
    public static final byte STATUS_RESULT = 0x02;
    public static final byte CART_RESULT = 0x03;
    public static final byte UNKNOWN_RESULT = 0x04; // maybe time related
    public static final byte SOUND_RESULT = 0x05;

    static final byte STATUS_CLASSIFIER = 0x01;
    static final byte INSULIN_CLASSIFIER = 0x04;

    public static final byte OK = 0x01;
    public static final byte NOT_OK = 0x02;

    public static final byte START_OF_MESSAGE = 0x02;
    public static final byte END_OF_MESSAGE = 0x03;
    public static final byte LAST_MESSAGE = 0x04;
    public static final byte MORE_TO_FOLLOW = 0x05;
    public static final byte ESCAPE_CHARACTER = 0x06;

    public static final byte CONTROL_SHIFT = 5;

    public static final byte REQUEST_RESULT = 1;
    public static final byte DONT_REQUEST_RESULT = 2;

    public static final byte OPCODE_SET_TIME = 4;
    public static final byte OPCODE_SET_INJECT = 5;
    public static final byte OPCODE_SET_TIME_ALT = 14;
    public static final byte OPCODE_GET_STATUS = 12;
    public static final byte OPCODE_GET_INJECTION_STATUS = 13;
    public static final byte OPCODE_GET_INSULIN_LOG = 15;
    public static final byte OPCODE_GET_FULL_LOG = 16;

}
