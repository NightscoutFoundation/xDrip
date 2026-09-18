package com.eveningoutpost.dexdrip.g5model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import lombok.val;

/**
 * Created by jamorham on 25/11/2016.
 */

public class VersionRequest1RxMessage extends BaseMessage {

    public static final byte opcode = 0x4B;
    public static final byte opcode2 = 0x4A;

    public int status;
    public String firmware_version_string;
    public long build_version;
    public long version_code;
    public int inactive_days;
    public int max_inactive_days;
    public int max_runtime_days;
    public long serial;


    public VersionRequest1RxMessage(byte[] packet) {
        if (packet.length >= 18) {
            // TODO check CRC??
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            val op = data.get();
            status = data.get();
            if (op == opcode) {
                firmware_version_string = dottedStringFromData(data, 4);
                build_version = getUnsignedInt(data);
                inactive_days = getUnsignedShort(data);
                version_code = getUnsignedByte(data);
                max_runtime_days = getUnsignedShort(data);
                max_inactive_days = getUnsignedShort(data);
                // crc
            }
            if (op == opcode2) {
                firmware_version_string = dottedStringFromData(data, 4);
                build_version = getUnsignedInt(data);
                version_code = getUnsignedInt(data);
                serial = longFromData(data, 6);
            }
        }
    }

    public String toString() {
        return String.format(Locale.US, "Status: %s / FW version: %s / Version Code: %d / Build: %d / Inactive: %d / Max Inactive: %d / Max Runtime: %d",
                TransmitterStatus.getBatteryLevel(status).toString(), firmware_version_string, version_code, build_version, inactive_days, max_inactive_days, max_runtime_days);
    }

}