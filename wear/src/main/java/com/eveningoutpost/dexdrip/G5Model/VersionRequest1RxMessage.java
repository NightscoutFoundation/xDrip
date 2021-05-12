package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Created by jamorham on 25/11/2016.
 */

public class VersionRequest1RxMessage extends BaseMessage {

    public static final byte opcode = 0x4B;

    public int status;
    public String firmware_version_string;
    public long build_version;
    public int version_code;
    public int inactive_days;
    public int max_inactive_days;
    public int max_runtime_days;


    public VersionRequest1RxMessage(byte[] packet) {
        if (packet.length >= 18) {
            // TODO check CRC??
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if (data.get() == opcode) {
                status = data.get();
                firmware_version_string = dottedStringFromData(data, 4);
                build_version = getUnsignedInt(data);
                inactive_days = getUnsignedShort(data);
                version_code = getUnsignedByte(data);
                max_runtime_days = getUnsignedShort(data);
                max_inactive_days = getUnsignedShort(data);
                // crc
            }
        }
    }

    public String toString() {
        return String.format(Locale.US, "Status: %s / FW version: %s / Version Code: %d / Build: %d / Inactive: %d / Max Inactive: %d / Max Runtime: %d",
                TransmitterStatus.getBatteryLevel(status).toString(), firmware_version_string, version_code, build_version, inactive_days, max_inactive_days, max_runtime_days);
    }

}