package com.eveningoutpost.dexdrip.Models;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/**
 * Created by jamorham on 06/01/16.
 * <p/>
 * lazy helper class for utilities
 */
public class JoH {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String TAG = "jamorham JoH";

    // qs = quick string conversion of double for printing
    public static String qs(double x) {
        return qs(x, 2);
    }

    public static String qs(double x, int digits) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(digits);
        df.setMinimumIntegerDigits(1);
        return df.format(x);
    }

    public static double ts() {
        return new Date().getTime();
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String compressString(String source) {
        try {

            Deflater deflater = new Deflater();
            deflater.setInput(source.getBytes(Charset.forName("UTF-8")));
            deflater.finish();

            byte[] buf = new byte[source.length() + 256];
            int count = deflater.deflate(buf);
            // check count
            deflater.end();
            return Base64.encodeToString(buf, 0, count, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] compressStringToBytes(String string) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(string.length());
            GZIPOutputStream gzipped_data = new GZIPOutputStream(output);
            gzipped_data.write(string.getBytes(Charset.forName("UTF-8")));
            gzipped_data.close();
            byte[] compressed = output.toByteArray();
            output.close();
            return compressed;
        } catch (Exception e) {
            Log.e(TAG, "Exception in compress: " + e.toString());
            return new byte[0];
        }
    }

    public static byte[] compressBytesToBytes(byte[] bytes) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length);
            GZIPOutputStream gzipped_data = new GZIPOutputStream(output);
            gzipped_data.write(bytes);
            gzipped_data.close();
            byte[] compressed = output.toByteArray();
            output.close();
            return compressed;
        } catch (Exception e) {
            Log.e(TAG, "Exception in compress: " + e.toString());
            return new byte[0];
        }
    }

    public static byte[] decompressBytesToBytes(byte[] bytes) {
        try {
            Log.d(TAG, "Decompressing  bytes size: " + bytes.length);
            byte[] buffer = new byte[8192];
            int bytes_read;
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length);
            GZIPInputStream gzipped_data = new GZIPInputStream(input, buffer.length);
            while ((bytes_read = gzipped_data.read(buffer)) != -1) {
                output.write(buffer, 0, bytes_read);
            }
            gzipped_data.close();
            input.close();
            // output.close();
            return output.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Exception in decompress: " + e.toString());
            return new byte[0];
        }
    }


    public static String uncompressString(String input) {
        try {
            byte[] bytes = Base64.decode(input, Base64.NO_WRAP);
            Inflater inflater = new Inflater();
            inflater.setInput(bytes);
            inflater.finished();

            byte[] buf = new byte[10000]; // max packet size because not using stream
            int count = inflater.inflate(buf);
            inflater.end();
            Log.d(TAG, "Inflated bytes: " + count);
            return new String(buf, 0, count, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Got exception uncompressing string");
            return null;
        }

    }
}
