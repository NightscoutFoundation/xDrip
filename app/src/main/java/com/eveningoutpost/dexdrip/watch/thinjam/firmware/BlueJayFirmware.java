package com.eveningoutpost.dexdrip.watch.thinjam.firmware;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.CipherUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.val;

// jamorham

public class BlueJayFirmware {

    private static final String TAG = "BlueJayFirmware";

    private static final long BLUEJAY_OTA_FILE_MAGIC = 0xFFFFFFFF811FB00BL;
    private static final int BLUEJAY_OTA_FILE_VERSION = 16;
    private static final byte[] BLUEJAY_OTA_FILE_KEY = JoH.hexStringToByteArray("476262f6431c280316cc06198710d4df");

    public static byte[] parse(final byte[] bytes) {

        try {
            if (bytes == null) return null;
            if (bytes.length < 1024) return null;

            val buffer = ByteBuffer.wrap(bytes);

            val magic = buffer.getLong();
            if (magic != BLUEJAY_OTA_FILE_MAGIC) {
                UserError.Log.e(TAG, String.format("Firmware magic doesn't match: %x vs %x", magic, BLUEJAY_OTA_FILE_MAGIC));
                return null;
            }

            val ota_file_version = buffer.getInt();
            if (ota_file_version != BLUEJAY_OTA_FILE_VERSION) {
                UserError.Log.e(TAG, "Unsupported OTA file version: " + ota_file_version);
                return null;
            }

            val hardwareType = buffer.getInt();
            UserError.Log.d(TAG, "Hardware type: " + hardwareType);
            val version = buffer.getInt();
            UserError.Log.d(TAG, "Version: " + version);
            val type = buffer.getInt();
            UserError.Log.d(TAG, "Type: " + type);
            val ota_byte_length = buffer.getInt();
            UserError.Log.d(TAG, "ota byte length: " + ota_byte_length);
            val final_bytes_length = buffer.getInt();
            UserError.Log.d(TAG, "final byte length: " + final_bytes_length);
            val outer_signature_length = buffer.getInt();

            if (outer_signature_length < 10 || outer_signature_length > 512) {
                UserError.Log.e(TAG, "Signature length out of range: " + outer_signature_length);
                return null;
            }

            val identity = buffer.getLong();
            UserError.Log.d(TAG, String.format("Identity: %x", identity));

            val signature_bytes = new byte[outer_signature_length];
            buffer.get(signature_bytes, 0, signature_bytes.length);

            if (final_bytes_length < 1000 || final_bytes_length > 1024 * 1024 * 16) {
                UserError.Log.e(TAG, "Payload bytesize out of range: " + final_bytes_length);
                return null;
            }
            val final_bytes = new byte[final_bytes_length];
            buffer.get(final_bytes, 0, final_bytes_length);

            if (DigitalSignature.firstCheck(final_bytes, signature_bytes)) {
                UserError.Log.d(TAG, "Early stage outer signature check passed");
            } else {
                UserError.Log.e(TAG, "Signature invalid");
                return null;
            }

            val firmware = CipherUtils.decryptBytes(final_bytes, BLUEJAY_OTA_FILE_KEY);
            if (firmware == null || firmware.length != ota_byte_length) {
                UserError.Log.e(TAG, "Failed to decode firmware file: " + (firmware == null ? "null" : firmware.length + " vs " + ota_byte_length));
                return null;
            }

            return firmware;

        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception processing: " + e);
        }
        return null;
    }

    public static List<byte[]> split(final byte[] input) {
        if (input == null || input.length == 0) return null;
        val step = 20;
        val list = new ArrayList<byte[]>();
        for (int i = 0; i < input.length; i = i + step) {
            list.add(Arrays.copyOfRange(input, i, Math.min(i + step, input.length)));
        }
        return list;
    }
}
