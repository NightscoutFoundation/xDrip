package com.eveningoutpost.dexdrip.watch.thinjam.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.eveningoutpost.dexdrip.models.UserError;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

public class BitmapTools {

    private static final String TAG = "BlueJayBitmap";
    private static final int MAX_BYTES = 50000;

    @RequiredArgsConstructor
    public enum TJ_BitmapType {
        Mono(2),
        RGB565(1);

        @Getter
        final int value;

    }

    @RequiredArgsConstructor
    public static class Wrapper {
        public final TJ_BitmapType type;
        public final int width;
        public final int height;
        public final byte[] body;

        public String toS() {
            return type + " width: " + width + " height: " + height + " body length: " + ((body != null) ? body.length : "null");
        }
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
        try {
            val options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            final Bitmap bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, options);

            val buffer = ByteBuffer.allocate(bitmap.getByteCount());
            val width = bitmap.getWidth();
            val height = bitmap.getHeight();
            bitmap.copyPixelsToBuffer(buffer);
            bitmap.recycle();
            return new Wrapper(TJ_BitmapType.RGB565, width, height, byteSwapRGB565(buffer.array()));
        } catch (Exception e) {
            UserError.Log.e(TAG, "loadPNGBytesToRGB565: exception " + e);
            return null;
        }
    }

    public static Wrapper convertWrappedToMono(final Wrapper image) {
        if (image != null && image.body != null) {
            if (image.type == TJ_BitmapType.RGB565) {
                return new Wrapper(TJ_BitmapType.Mono, image.width, image.height, packRGB565bytesToMono(image.body));
            } else {
                UserError.Log.wtf(TAG, "convertWrappedToMono: unsupported image type in conversion: " + image.type);
            }
        }
        return null;
    }

    public static byte[] packRGB565bytesToMono(final byte[] input) {
        if (input == null) return null;
        val output = new byte[input.length / 16 + (((input.length / 2) % 8 == 0) ? 0 : 1)];
        int bitPosition = 0;
        for (int i = 0; i < input.length; i = i + 2) {
            final int col = input[i] << 8 | input[i + 1];
            if (col != 0) { // any colour so long as its black
                output[bitPosition / 8] |= 1 << (bitPosition % 8);
            }
            bitPosition++;
        }
        return output;
    }

    public static byte[] unpackMonoBytesToRGB565(final byte[] input) {
        if (input == null) return null;
        val output = new byte[input.length * 16];
        int pos = 0;
        for (byte anInput : input) {
            for (int bit = 0; bit < 8; bit++) {
                byte col = 0;
                if ((anInput & (1 << bit)) != 0) {
                    col = 1;
                }
                output[pos++] = col;
                output[pos++] = col;
            }
        }
        return output;
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
