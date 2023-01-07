package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import java.util.UUID;

public class Const {

    public static final byte OPCODE_VERSION_REQUEST = 0x01;
    public static final byte OPCODE_DEFINE_WINDOW = 0x02;
    public static final byte OPCODE_FILL_WINDOW = 0x03;
    public static final byte OPCODE_BULK_UP_REQUEST = 0x04;

    public static final byte OPCODE_GET_STATUS_1 = 0x21;
    public static final byte OPCODE_GET_STATUS_2 = 0x22;
    public static final byte OPCODE_GET_STATUS_3 = 0x23;

    public static final byte OPCODE_GET_TXID = 0x24;
    public static final byte OPCODE_BACKFILL_REQ = 0x25;
    public static final byte OPCODE_INBOUND_GLUCOSE = 0x26;

    public static final byte OPCODE_PUSH_RX = 0x30;

    public static final byte OPCODE_AUTH_REQ = 0x40;
    public static final byte OPCODE_IDENTIFY = 0x41;
    public static final byte OPCODE_SHOW_QRCODE = 0x44;

    public static final byte OPCODE_SET_TIME = 0x49;
    public static final byte OPCODE_SET_TXID = 0x50;
    public static final byte OPCODE_RESET_ALL = 0x51;
    public static final byte OPCODE_RESET_PERSIST = 0x52;
    public static final byte OPCODE_SWITCH_OFF = 0x53;
    public static final byte OPCODE_EASY_AUTH = 0x56;
    public static final byte OPCODE_SET_GAMMA = 0x5C;

    public static final int OPCODE_BULK_R_XFER_0 =  0x84;

    public static final byte OPCODE_INVALID = (byte) 0xFF;

    public static final byte OPCODE_STOP_NOTFY = 0x60;
    public static final byte OPCODE_NOTIFY_MSG = 0x61;
    public static final byte OPCODE_INCOMING_CALL = 0x62;

    public static final byte OPCODE_DISCONNECT = 0x70;

    public static final byte ERROR_OK = 0x00;
    public static final byte ERROR_INVALID = 0x01;
    public static final byte ERROR_INVALID_LENGTH = 0x02;
    public static final byte ERROR_MISC = 0x03;
    public static final byte ERROR_INVALID_OPCODE = 0x04;
    public static final byte ERROR_OUT_OF_RANGE = 0x05;

    public static final byte DEFINE_WINDOW_QUIET = (byte)0x80;

    // Relates to opcodes also > 80
    public static final byte ERROR_OK_WITH_PARAMETER = (byte) 0x80;

    public static final byte PUSH_OPCODE_CHARGE = 1;
    public static final byte PUSH_OPCODE_B1_LONG = 2;
    public static final byte PUSH_OPCODE_BACKFILL = 3;
    public static final byte PUSH_OPCODE_CHOICE = 4;
    public static final byte PUSH_OPCODE_ASSET_REQ = 5;
    public static final byte PUSH_OPCODE_BULK_ERROR = 6;
    public static final byte PUSH_OPCODE_C_INFO = 7;
    public static final byte PUSH_OPCODE_I_INFO = 8;
    public static final byte PUSH_OPCODE_T_INFO = 9;
    public static final byte PUSH_OPCODE_H_INFO = 10;
    public static final byte PUSH_OPCODE_B_INFO = 11;


    public static final String[] errorText = {
            "OK",
            "INVALID",
            "INVALID LENGTH",
            "INVALID MISC",
            "INVALID OPCODE",
            "OUT OF RANGE",
            "RESEND FROM",
            "BUSY"
    };

    public static final int FEATURE_TJ_DISP_A = 1;
    public static final int FEATURE_TJ_DISP_B = 2;
    public static final int FEATURE_TJ_DISP_C = 3;
    public static final int FEATURE_TJ_AUDIO_I = 14;
    public static final int FEATURE_TJ_AUDIO_O = 15;

    public static final String THINJAM_SERVICE_STRING = "4a616d21-722a-4ce8-a2a2-a2b0b7da2cd0";
    public static final String THINJAM_HUNT_SERVICE_STRING = "4a616d21-0000-1000-8000-00805f9b34fb";
    public static final String THINJAM_HUNT_MASK_STRING = "ffffffff-0000-0000-0000-000000000000";

    public static final UUID THINJAM_SERVICE = UUID.fromString(THINJAM_SERVICE_STRING);

    public static final UUID THINJAM_WRITE = UUID.fromString("4a616d21-722a-4ce8-a2a2-a2b0b7da2cd7");
    public static final UUID THINJAM_BULK = UUID.fromString("4a616d21-722a-4ce8-a2a2-a2b0b7da2cd8");
    public static final UUID THINJAM_TEST = UUID.fromString("4a616d21-722a-4ce8-a2a2-a2b0b7da2cd1");
    public static final UUID THINJAM_OTA = UUID.fromString("4a616d21-722a-4ce8-a2a2-a2b0b7da2cd9");

    public static final String THINJAM_NOTIFY_TYPE_CANCEL = "THINJAM_NOTIFY_TYPE_CANCEL";
    public static final String THINJAM_NOTIFY_TYPE_CALL = "THINJAM_NOTIFY_TYPE_CALL";
    public static final String THINJAM_NOTIFY_TYPE_TEXT_MESSAGE = "THINJAM_NOTIFY_TYPE_TEXT_MESSAGE";
    public static final String THINJAM_NOTIFY_TYPE_HIGH_ALERT = "THINJAM_NOTIFY_TYPE_HIGH_ALERT";
    public static final String THINJAM_NOTIFY_TYPE_LOW_ALERT = "THINJAM_NOTIFY_TYPE_LOW_ALERT";
    public static final String THINJAM_NOTIFY_TYPE_OTHER_ALERT = "THINJAM_NOTIFY_TYPE_OTHER_ALERT";
    public static final String THINJAM_NOTIFY_TYPE_TEXTBOX1 = "THINJAM_NOTIFY_TYPE_TEXTBOX1";
    public static final String THINJAM_NOTIFY_TYPE_TEXTBOX2 = "THINJAM_NOTIFY_TYPE_TEXTBOX2";
    public static final String THINJAM_NOTIFY_TYPE_DIALOG = "THINJAM_NOTIFY_TYPE_DIALOG";

}
