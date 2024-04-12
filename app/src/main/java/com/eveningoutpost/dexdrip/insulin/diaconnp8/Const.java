package com.eveningoutpost.dexdrip.insulin.diaconnp8;

import java.util.UUID;

public class Const {

//    public static final double MINIMUM_DOSE = 0.5d;
    public static final int LOG_BLOCK_SIZE = 11;

    public static final UUID DIACONNP8_SERVICE = UUID.fromString("68070001-5d16-401c-99aa-0b300a2d9710");
    public static final UUID OUTGOING_CHAR = UUID.fromString("68070002-5d16-401c-99aa-0b300a2d9710");
    public static final UUID INCOMING_CHAR = UUID.fromString("68070003-5d16-401c-99aa-0b300a2d9710");

    public static final byte SYSTEM_STATUS_RESPONSE_MSG_TYPE = (byte) 0x95;
//    public static final byte INJECTION_SETTING_RESPONSE_MSG_TYPE = (byte) 0x85;
    public static final byte TIME_SETTING_RESPONSE_MSG_TYPE = (byte) 0x8C;
    public static final byte TIME_INQUIRE_RESPONSE_MSG_TYPE = (byte) 0x8C;
    public static final byte LOG_STATUS_INQUIRE_RESPONSE_MSG_TYPE = (byte) 0x96;
    public static final byte BIG_LOG_INQUIRE_RESPONSE_MSG_TYPE = (byte) 0x9D;
    public static final byte INJECTION_EVENT_REPORT_MSG_TYPE = (byte) 0xC6;
    public static final int INCARNATION_INQUIRE_RESPONSE_MSG_TYPE = (byte) 0xA5;

}
