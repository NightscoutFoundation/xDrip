package com.eveningoutpost.dexdrip.watch.thinjam.assets;

import com.eveningoutpost.dexdrip.models.UserError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.val;

// jamorham

public class AssetPackage {

    private static final String TAG = AssetPackage.class.getSimpleName();
    private static final long BLUEJAY_ASSET_FILE_MAGIC = 0xFF749E03611DBF63L;
    private static final int BLUEJAY_ASSET_FILE_VERSION = 2;

    public static byte[] parse(final byte[] bytes, final int requestedAsset) {

        try {
            if (bytes == null) {
                UserError.Log.d(TAG, "Null bytes received");
                return null;
            }
            if (bytes.length < 100) {
                UserError.Log.d(TAG, "Package below minimum size");
                return null;
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            val magic = buffer.getLong();
            if (magic != BLUEJAY_ASSET_FILE_MAGIC) {
                UserError.Log.e(TAG, String.format("Asset magic doesn't match: %x vs %x", magic, BLUEJAY_ASSET_FILE_MAGIC));
                return null;
            }

            val asset_file_version = buffer.getInt();
            if (asset_file_version != BLUEJAY_ASSET_FILE_VERSION) {
                UserError.Log.e(TAG, "Unsupported Asset file version: " + asset_file_version);
                return null;
            }

            val assetId = buffer.getInt();

            if (assetId != requestedAsset) {
                UserError.Log.e(TAG, "Asset id doesn't match: " + assetId + " vs " + requestedAsset);
                return null;
            }
            val assetVersion = buffer.getInt();

            val outer_signature_length = buffer.getInt();
            if (outer_signature_length < 10 || outer_signature_length > 512) {
                UserError.Log.e(TAG, "Signature length out of range: " + outer_signature_length);
                return null;
            }

            val final_bytes_length = buffer.getInt();
            UserError.Log.d(TAG, "final byte length: " + final_bytes_length);

            if (final_bytes_length < 10 || final_bytes_length > 65535) {
                UserError.Log.e(TAG, "Payload bytesize out of range: " + final_bytes_length);
                return null;
            }

            val signature_bytes = new byte[outer_signature_length];
            buffer.get(signature_bytes, 0, signature_bytes.length);

            val final_bytes = new byte[final_bytes_length];
            buffer.get(final_bytes, 0, final_bytes_length);

            if (DigitalSignature.firstCheck(final_bytes, signature_bytes)) {
                UserError.Log.d(TAG, "Early stage outer signature check passed");
                return final_bytes;
            } else {
                UserError.Log.e(TAG, "Signature invalid");
                return null;
            }

        } catch (Exception e) {
            UserError.Log.e(TAG, "Asset download error: " + e);
            return null;
        }

    }
}
