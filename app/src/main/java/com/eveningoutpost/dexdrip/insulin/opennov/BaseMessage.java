package com.eveningoutpost.dexdrip.insulin.opennov;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.GsonBuilder;

import java.nio.ByteBuffer;

import lombok.val;

/**
 * JamOrHam
 * OpenNov base message helper
 */

public class BaseMessage extends MyByteBuffer {

    private static final String TAG = "OpenNov";
    public static boolean d = false;


    protected static byte[] getIndexedBytes(final ByteBuffer buffer) {
        val blen = getUnsignedShort(buffer);
        val bytes = new byte[blen];
        buffer.get(bytes, 0, blen);
        return bytes;
    }

    protected static void putIndexedBytes(final ByteBuffer buffer, final byte[] bytes) {
        putUnsignedShort(buffer, bytes != null ? bytes.length : 0);
        if (bytes != null) buffer.put(bytes);
    }

    protected static String getIndexedString(final ByteBuffer buffer) {
        return new String(getIndexedBytes(buffer)).replace("\0", "");
    }

    protected static long getIvalue(final byte[] bytes, final int len) {
        val buffer = ByteBuffer.wrap(bytes);
        switch (len) {
            case 4:
                return getUnsignedInt(buffer);
            case 2:
                return getUnsignedShort(buffer);
            default:
                return -1;
        }
    }

    public String toJson() {
        val gson = new GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    protected static String getTAG() {
        val fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    public static void log(final String msg) {
        if (d) {
            if (xdrip.isRunningTest()) {
                val tag = getTAG();
                System.out.println(tag + ":: " + msg);
            } else {
                UserError.Log.d(TAG, msg);
            }
        }
    }

    protected static void error(final String msg) {
        if (d) {
            System.out.println(getTAG() + ":: " + msg);
        } else {
            UserError.Log.e(TAG, msg);
        }
    }

}
