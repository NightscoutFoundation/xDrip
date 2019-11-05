package com.eveningoutpost.dexdrip.watch.miband.Firmware;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_CHECKSUM;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_INIT;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_START_DATA;

public class FirmwareOperations {
    public enum FirmwareType {
        FIRMWARE((byte) 0),
        FONT((byte) 1),
        RES((byte) 2),
        RES_COMPRESSED((byte) 130),
        GPS((byte) 3),
        GPS_CEP((byte) 4),
        GPS_ALMANAC((byte) 5),
        WATCHFACE((byte) 8),
        FONT_LATIN((byte) 11),
        INVALID(Byte.MIN_VALUE);

        private final byte value;

        FirmwareType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    private static final int packetLength = 20;
    private byte[] fw = new byte[0];
    private FirmwareType firmwareType = FirmwareType.WATCHFACE;

    public FirmwareType getFirmwareType() {
        return firmwareType;
    }

    public byte[] getBytes() {
        return fw;
    }

    public int getPackeLenght() {
        return packetLength;
    }

    public FirmwareOperations(InputStream file) {
        try {
            fw = readAll(file, 1024 * 2048); // 2.0 MB
        } catch (IOException e) {
        }
    }

    public int getSize() {
        return fw.length;
    }

    public static byte[] fromUint24(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
        };
    }

    public static byte[] fromUint32(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
        };
    }

    public static byte[] readAll(InputStream in, long maxLen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, in.available()));
        byte[] buf = new byte[8192];
        int read;
        long totalRead = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            totalRead += read;
            if (totalRead > maxLen) {
                throw new IOException("Too much data to read into memory. Got already " + totalRead);
            }
        }
        return out.toByteArray();
    }

    public byte[] sendFwInfo() {
        int fwSize = getSize();
        byte[] sizeBytes = fromUint24(fwSize);
        byte[] bytes = new byte[10];
        int i = 0;
        bytes[i++] = COMMAND_FIRMWARE_INIT;
        bytes[i++] = getFirmwareType().value;
        bytes[i++] = sizeBytes[0];
        bytes[i++] = sizeBytes[1];
        bytes[i++] = sizeBytes[2];
        bytes[i++] = 0; // TODO: what is that?
        int crc32 = (int) JoH.checksum(fw);
        byte[] crcBytes = fromUint32(crc32);
        bytes[i++] = crcBytes[0];
        bytes[i++] = crcBytes[1];
        bytes[i++] = crcBytes[2];
        bytes[i] = crcBytes[3];
        return bytes;
    }

    public byte[] prepareFirmawareUploadCommand() {
        return new byte[]{COMMAND_FIRMWARE_INIT, (byte) 0xFF};
    }

    public byte[] sendChecksum() {
        return new byte[]{COMMAND_FIRMWARE_CHECKSUM};
    }

    public byte[] getFirmwareStartCommand() {
        return new byte[]{COMMAND_FIRMWARE_START_DATA, (byte) 0x1};
    }

    public UUID getFirmwareCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_FIRMWARE;
    }

    public UUID getFirmwareDataCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_FIRMWARE_DATA;
    }
}