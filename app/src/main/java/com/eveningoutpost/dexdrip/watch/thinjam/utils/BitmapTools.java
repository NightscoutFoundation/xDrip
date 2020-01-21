package com.eveningoutpost.dexdrip.watch.thinjam.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.eveningoutpost.dexdrip.Models.UserError;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import lombok.RequiredArgsConstructor;
import lombok.val;

public class BitmapTools {

    private static final String TAG = "BlueJayBitmap";
    private static final int MAX_BYTES = 50000;

    enum TJ_BitmapType {
        Mono,
        RGB565
    }

    @RequiredArgsConstructor
    public static class Wrapper {
        public final TJ_BitmapType type;
        public final int width;
        public final int height;
        public final byte[] body;
    }

    public static byte[] loadFileToByteArray(final String path) {
        try (final FileInputStream fileInputStream = new FileInputStream(path)) {
            final int length = fileInputStream.available();
            if (length > MAX_BYTES) {
                return null;
            }
            final byte[] buffer = new byte[length];
            readAllToBuffer(fileInputStream, buffer);
            return buffer;
        } catch (FileNotFoundException e) {
            UserError.Log.e(TAG, "Could not find: " + path);
        } catch (IOException e) {
            UserError.Log.e(TAG, "IO Error: " + e);
        }
        return null;
    }

    private static int readAllToBuffer(final InputStream stream, final byte[] buffer) throws IOException {
        int readBytes = 0;
        int thisRead;
        while ((thisRead = stream.read(buffer, readBytes, buffer.length - readBytes)) > 0) {
            readBytes += thisRead;
            if (readBytes > MAX_BYTES) break;
        }
        return readBytes;
    }

    public static Wrapper loadPNGBytesToRGB565(final byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length == 0) return null;
        val options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, options);

        // TODO validate bitmap and return null if invalid? exception handling?

        val buffer = ByteBuffer.allocate(bitmap.getByteCount());
        val width = bitmap.getWidth();
        val height = bitmap.getHeight();
        bitmap.copyPixelsToBuffer(buffer);
        bitmap.recycle();
        return new Wrapper(TJ_BitmapType.RGB565, width, height, byteSwapRGB565(buffer.array()));
    }

    private static byte[] byteSwapRGB565(final byte[] input) {
        if (input == null || ((input.length & 1) == 1)) return input;
        for (int i = 0; i < input.length; i += 2) {
            final byte temp = input[i];
            input[i] = input[i + 1];
            input[i + 1] = temp;
        }
        return input;
    }

}
