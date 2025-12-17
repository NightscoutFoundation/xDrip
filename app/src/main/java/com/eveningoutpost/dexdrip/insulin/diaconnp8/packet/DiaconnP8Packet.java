package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DiaconnP8Packet {
    public static final int MSG_LEN = 20;
    public static final int MSG_LEN_BIG = 182;
    public static final byte SOP = (byte) 0xef;
    public static final byte SOP_BIG = (byte) 0xed;
    public static final byte MSG_TYPE_LOC = 1;
//    public static final byte MSG_SEQ_LOC = 2;
    public static final byte BT_MSG_DATA_LOC = 4;
    public static final byte MSG_PAD = (byte) 0xff;
    public static final byte MSG_CON_END = (byte) 0x00;

    /**
     * CRC
     */
    private static final byte[] crc_table = {
            (byte) 0x00, (byte) 0x25, (byte) 0x4A, (byte) 0x6F, (byte) 0x94, (byte) 0xB1, (byte) 0xDE, (byte) 0xFB,
            (byte) 0x0D, (byte) 0x28, (byte) 0x47, (byte) 0x62, (byte) 0x99, (byte) 0xBC, (byte) 0xD3, (byte) 0xF6,
            (byte) 0x1A, (byte) 0x3F, (byte) 0x50, (byte) 0x75, (byte) 0x8E, (byte) 0xAB, (byte) 0xC4, (byte) 0xE1,
            (byte) 0x17, (byte) 0x32, (byte) 0x5D, (byte) 0x78, (byte) 0x83, (byte) 0xA6, (byte) 0xC9, (byte) 0xEC,
            (byte) 0x34, (byte) 0x11, (byte) 0x7E, (byte) 0x5B, (byte) 0xA0, (byte) 0x85, (byte) 0xEA, (byte) 0xCF,
            (byte) 0x39, (byte) 0x1C, (byte) 0x73, (byte) 0x56, (byte) 0xAD, (byte) 0x88, (byte) 0xE7, (byte) 0xC2,
            (byte) 0x2E, (byte) 0x0B, (byte) 0x64, (byte) 0x41, (byte) 0xBA, (byte) 0x9F, (byte) 0xF0, (byte) 0xD5,
            (byte) 0x23, (byte) 0x06, (byte) 0x69, (byte) 0x4C, (byte) 0xB7, (byte) 0x92, (byte) 0xFD, (byte) 0xD8,
            (byte) 0x68, (byte) 0x4D, (byte) 0x22, (byte) 0x07, (byte) 0xFC, (byte) 0xD9, (byte) 0xB6, (byte) 0x93,
            (byte) 0x65, (byte) 0x40, (byte) 0x2F, (byte) 0x0A, (byte) 0xF1, (byte) 0xD4, (byte) 0xBB, (byte) 0x9E,
            (byte) 0x72, (byte) 0x57, (byte) 0x38, (byte) 0x1D, (byte) 0xE6, (byte) 0xC3, (byte) 0xAC, (byte) 0x89,
            (byte) 0x7F, (byte) 0x5A, (byte) 0x35, (byte) 0x10, (byte) 0xEB, (byte) 0xCE, (byte) 0xA1, (byte) 0x84,
            (byte) 0x5C, (byte) 0x79, (byte) 0x16, (byte) 0x33, (byte) 0xC8, (byte) 0xED, (byte) 0x82, (byte) 0xA7,
            (byte) 0x51, (byte) 0x74, (byte) 0x1B, (byte) 0x3E, (byte) 0xC5, (byte) 0xE0, (byte) 0x8F, (byte) 0xAA,
            (byte) 0x46, (byte) 0x63, (byte) 0x0C, (byte) 0x29, (byte) 0xD2, (byte) 0xF7, (byte) 0x98, (byte) 0xBD,
            (byte) 0x4B, (byte) 0x6E, (byte) 0x01, (byte) 0x24, (byte) 0xDF, (byte) 0xFA, (byte) 0x95, (byte) 0xB0,
            (byte) 0xD0, (byte) 0xF5, (byte) 0x9A, (byte) 0xBF, (byte) 0x44, (byte) 0x61, (byte) 0x0E, (byte) 0x2B,
            (byte) 0xDD, (byte) 0xF8, (byte) 0x97, (byte) 0xB2, (byte) 0x49, (byte) 0x6C, (byte) 0x03, (byte) 0x26,
            (byte) 0xCA, (byte) 0xEF, (byte) 0x80, (byte) 0xA5, (byte) 0x5E, (byte) 0x7B, (byte) 0x14, (byte) 0x31,
            (byte) 0xC7, (byte) 0xE2, (byte) 0x8D, (byte) 0xA8, (byte) 0x53, (byte) 0x76, (byte) 0x19, (byte) 0x3C,
            (byte) 0xE4, (byte) 0xC1, (byte) 0xAE, (byte) 0x8B, (byte) 0x70, (byte) 0x55, (byte) 0x3A, (byte) 0x1F,
            (byte) 0xE9, (byte) 0xCC, (byte) 0xA3, (byte) 0x86, (byte) 0x7D, (byte) 0x58, (byte) 0x37, (byte) 0x12,
            (byte) 0xFE, (byte) 0xDB, (byte) 0xB4, (byte) 0x91, (byte) 0x6A, (byte) 0x4F, (byte) 0x20, (byte) 0x05,
            (byte) 0xF3, (byte) 0xD6, (byte) 0xB9, (byte) 0x9C, (byte) 0x67, (byte) 0x42, (byte) 0x2D, (byte) 0x08,
            (byte) 0xB8, (byte) 0x9D, (byte) 0xF2, (byte) 0xD7, (byte) 0x2C, (byte) 0x09, (byte) 0x66, (byte) 0x43,
            (byte) 0xB5, (byte) 0x90, (byte) 0xFF, (byte) 0xDA, (byte) 0x21, (byte) 0x04, (byte) 0x6B, (byte) 0x4E,
            (byte) 0xA2, (byte) 0x87, (byte) 0xE8, (byte) 0xCD, (byte) 0x36, (byte) 0x13, (byte) 0x7C, (byte) 0x59,
            (byte) 0xAF, (byte) 0x8A, (byte) 0xE5, (byte) 0xC0, (byte) 0x3B, (byte) 0x1E, (byte) 0x71, (byte) 0x54,
            (byte) 0x8C, (byte) 0xA9, (byte) 0xC6, (byte) 0xE3, (byte) 0x18, (byte) 0x3D, (byte) 0x52, (byte) 0x77,
            (byte) 0x81, (byte) 0xA4, (byte) 0xCB, (byte) 0xEE, (byte) 0x15, (byte) 0x30, (byte) 0x5F, (byte) 0x7A,
            (byte) 0x96, (byte) 0xB3, (byte) 0xDC, (byte) 0xF9, (byte) 0x02, (byte) 0x27, (byte) 0x48, (byte) 0x6D,
            (byte) 0x9B, (byte) 0xBE, (byte) 0xD1, (byte) 0xF4, (byte) 0x0F, (byte) 0x2A, (byte) 0x45, (byte) 0x60
    };

    public ByteBuffer prefixEncode(byte msgType, int msgSeq, byte msgConEnd) {
        ByteBuffer buffer = ByteBuffer.allocate(MSG_LEN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(SOP);
        buffer.put(msgType);
        buffer.put((byte) msgSeq);
        buffer.put(msgConEnd);
        return buffer;
    }
    public byte[] suffixEncode(ByteBuffer buffer) {
        int remainSize = MSG_LEN - buffer.position() - 1;
        for (int i = 0; i < remainSize; i++) {
            buffer.put(MSG_PAD);
        }
        byte crc = getCRC(buffer.array(), MSG_LEN - 1);
        buffer.put(crc);
        return buffer.array();
    }
    public static ByteBuffer prefixDecode(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(BT_MSG_DATA_LOC);
        return buffer;
    }

    public static int getType(byte[] bytes) {
        return (bytes[MSG_TYPE_LOC] & 0xC0) >> 6;
    }

    public static int getCmd(byte[] bytes) {
        return bytes[MSG_TYPE_LOC];
    }

    //public static int getSeq(byte[] bytes) { return bytes[MSG_SEQ_LOC]; }

    public static int getByteToInt(ByteBuffer buffer) {
        return buffer.get() & 0xff;
    }

    public static int getShortToInt(ByteBuffer buffer) {
        return buffer.getShort() & 0xffff;
    }

    public static int getIntToInt(ByteBuffer buffer) {
        return buffer.getInt();
    }


    public static byte[] getBytes(ByteBuffer buffer, int limit) {
        ByteBuffer data = ByteBuffer.allocate(MSG_LEN);
        int orgPos = buffer.position();
        int orgLimit = buffer.limit();
        buffer.limit(buffer.position() + limit);
        data.put(buffer);
        buffer.position(orgPos);
        buffer.limit(orgLimit);
        return data.array();
    }
    static byte getCRC(byte[] data, int length) {
        int i = 0;
        byte crc = 0;
        while (length-- != 0) {
            crc = crc_table[(crc ^ data[i]) & 0xFF];
            i++;
        }
        return crc;
    }
    public static int defect(byte[] bytes) {
        int result = 0;
        if (bytes[0] != SOP && bytes[0] != SOP_BIG) {
            // Start Code Check
            result = 98;
        } else if ((bytes[0] == SOP && bytes.length != MSG_LEN) ||
                (bytes[0] == SOP_BIG && bytes.length != MSG_LEN_BIG)) {
            // length check
            result = 97;
        } else if (bytes[bytes.length - 1] != getCRC(bytes, bytes.length - 1)) {
            // CRC check
            result = 99;
        }
        return result;
    }

    public byte[] encode(int msgSeq) {
        return new byte[0];
    }

    public static String toNarrowHex(byte[] packet) {
        StringBuilder sb = new StringBuilder();
        for (final byte b : packet)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static Boolean isSuccInquireResponseResult(int result) {
        boolean isSuccess = false;
        switch (result) {
            case 16:
                isSuccess = true;
                break;
            case 17:
                UserError.Log.e("DiaconnP8Packet","Packet CRC error");
                JoH.static_toast_short(xdrip.gs(R.string.title_diaconnp8_packet_crc_error));
                break;

            case 18:
                UserError.Log.e("DiaconnP8Packet","Parameter error.");
                JoH.static_toast_short(xdrip.gs(R.string.title_diaconnp8_packet_parameter_error));
                break;

            case 19:
                UserError.Log.e("DiaconnP8Packet","Protocol specification error.");
                JoH.static_toast_short(xdrip.gs(R.string.title_diaconnp8_packet_protocol_specification_error));
                break;

            default:
                UserError.Log.e("DiaconnP8Packet","System error.");
                JoH.static_toast_short(xdrip.gs(R.string.title_diaconnp8_packet_system_error));
                break;
        }
        return isSuccess;
    }

    public static Boolean isSuccSettingResponseResult(int result) {
        boolean isSuccess = false;
        switch (result) {
            case 0:
                isSuccess = true;
                break;
            case 1:
                UserError.Log.e("DiaconnP8Packet","Packet CRC error");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_crc_error));
                break;

            case 2:
                UserError.Log.e("DiaconnP8Packet", "Parameter error.");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_parameter_error));
                break;

            case 3:
                UserError.Log.e("DiaconnP8Packet","Protocol specification error.");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_protocol_specification_error));
                break;

            case 4:
                UserError.Log.e("DiaconnP8Packet","Battery Charging, not injectable.");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_battery_charging_error));
                break;

            case 7:
                UserError.Log.e("DiaconnP8Packet", "In the midst of other operations, limited app setup capabilities");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_other_operation_limited_app_setting_error));
                break;

            case 8:
                UserError.Log.e("DiaconnP8Packet","During another bolus injection, injection is restricted");
                JoH.static_toast_short(xdrip.gs(R.string.title_diaconnp8_packet_another_bolus_restricted_error));
                break;

            case 10:
                UserError.Log.e("DiaconnP8Packet", "Canceled due to the opt number did not match.");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_otp_not_match_error));
                break;

            case 11:
                UserError.Log.e("DiaconnP8Packet","Injection is not possible due to low battery.");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_low_battery_error));
                break;

            case 14:
                UserError.Log.e("DiaconnP8Packet","It cannot be injected due to an excess of injection volume today");
                JoH.static_toast_short( xdrip.gs(R.string.title_diaconnp8_packet_excess_today_volume_error));
                break;

            default:
                UserError.Log.e("DiaconnP8Packet","Setup is not possible due to system error.");
                JoH.static_toast_short(xdrip.gs(R.string.title_diaconnp8_packet_system_error));
                break;

        }
        return isSuccess;
    }
}


