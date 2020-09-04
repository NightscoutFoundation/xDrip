package com.eveningoutpost.dexdrip.watch.miband.Firmware;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.watch.miband.Const;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence.SequenceState;
import com.eveningoutpost.dexdrip.watch.miband.MiBandService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_CHECKSUM;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_INIT;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_START_DATA;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_UNKNOWN_MIBAND5;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_UPDATE_SYNC;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MAXIMUM;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MINIMUM;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_WRITE_MTU_OVERHEAD;

public class FirmwareOperations {
    protected String TAG = MiBandService.class.getSimpleName();

    private byte[] fw;
    private FirmwareType firmwareType = FirmwareType.WATCHFACE;
    private int mMTU = GATT_MTU_MINIMUM;
    public static int FIRMWARE_SYNC_PACKET = 175;

    protected SequenceState mState;

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

    public FirmwareOperations(byte[] file, SequenceState sequenceState) {
        fw = file;
        mState = sequenceState;
    }

    public void setMTU(int mMTU) {
        this.mMTU = mMTU;
        if (this.mMTU > GATT_MTU_MAXIMUM) this.mMTU = GATT_MTU_MAXIMUM;
        if (this.mMTU < GATT_MTU_MINIMUM) this.mMTU = GATT_MTU_MINIMUM;
    }

    public FirmwareType getFirmwareType() {
        return firmwareType;
    }

    public byte[] getBytes() {
        return fw;
    }

    public int getPackeLenght() {
        return mMTU - GATT_WRITE_MTU_OVERHEAD;
    }

    public String getSequence() {
        return mState.getSequence();
    }

    public void nextSequence() {
        String oldState = mState.getSequence();
        String new_state = mState.next();
        if (MiBandService.d)
            UserError.Log.d(TAG, "Changing firmware state from: " + oldState + " to " + new_state);
    }

    public int getSize() {
        return fw.length;
    }

    public static byte fromUint8(int value) {
        return (byte) (value & 0xff);
    }

    public static byte[] fromUint16(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
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

    public byte[] getFwInfoCommand() {
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

    public byte[] prepareFWUploadInitCommand() {
        return new byte[]{COMMAND_FIRMWARE_INIT, (byte) 0xFF};
    }

    public byte[] getChecksumCommand() {
        return new byte[]{COMMAND_FIRMWARE_CHECKSUM};
    }

    public byte[] getSyncCommand() {
        return new byte[]{COMMAND_FIRMWARE_UPDATE_SYNC};
    }

    public byte[] getFirmwareStartCommand() {
        return new byte[]{COMMAND_FIRMWARE_START_DATA, (byte) 0x1};
    }

    public byte[] getUnknownMiBand5Command() {
        return new byte[]{COMMAND_FIRMWARE_UNKNOWN_MIBAND5};
    }

    public UUID getFirmwareCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_FIRMWARE;
    }

    public UUID getFirmwareDataCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_FIRMWARE_DATA;
    }
}
