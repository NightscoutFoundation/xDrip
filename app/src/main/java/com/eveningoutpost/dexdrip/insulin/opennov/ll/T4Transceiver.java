package com.eveningoutpost.dexdrip.insulin.opennov.ll;


import static com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage.d;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov type 4 transceive helper
 */

@RequiredArgsConstructor
public class T4Transceiver extends MyByteBuffer {

    private static final int MAX_READ_SIZE_PARAMETER = 255;
    private static final long RETRY_PAUSE = 50;
    private static final int MAX_RETRY = 3;
    private static final String TAG = "OpenNov";
    private static final byte[] SA = T4Select.builder().build().aSelect().encode();
    private static final byte[] SC = T4Select.builder().build().ccSelect().encode();
    private static final byte[] SN = T4Select.builder().build().ndefSelect().encode();

    private final IsoDep tag;

    private int mlcMax = -1;
    private int mleMax = -1;

    public T4Reply t4Transceive(byte[] bytes, final T4Reply reply) {
        if (bytes == null) return null;
        try {
            return T4Reply.parse(tag.transceive(bytes), reply);
        } catch (TagLostException e) {
            UserError.Log.d(TAG, "Tag was lost");
            try {
                tag.close();
            } catch (IOException ioException) {
                UserError.Log.e(TAG, "Failure to close lost tag: " + e);
            }
        } catch (IOException e) {
            UserError.Log.e(TAG, "Exception during transceive: ", e);
        }
        return null;
    }

    public T4Reply t4Transceive(byte[] bytes) {
        return t4Transceive(bytes, null);
    }

    public boolean transceiveOkay(final byte[] bytes, final String failureMsg) {
        val result = t4Transceive(bytes);
        val okay = result != null && result.isOkay();
        if (!okay) {
            UserError.Log.d(TAG, failureMsg);
        }
        return okay;
    }

    public void writeToLinkLayer(final byte[] bytes) {
        if (!tag.isConnected()) {
            UserError.Log.d(TAG, "Tag lost (write)");
            return;
        }
        val packets = T4Update.builder().bytes(bytes).build().encodeForMtu(mlcMax);
        for (val p : packets) {
            if (!transceiveOkay(p, "Error during packet write")) {
                break;
            }
        }
    }

    public byte[] readFromLinkLayer() {
        val readLength = T4Read.builder().offset(0).length(2).build().encode();
        for (int i = 0; i < MAX_RETRY; i++) {
            if (!tag.isConnected()) {
                UserError.Log.d(TAG, "Tag lost");
                return null;
            } else {
                tag.setTimeout(3000);
            }
            val lengthResult = t4Transceive(readLength);
            if (lengthResult == null) {
                UserError.Log.d(TAG, "Failed to transceive when reading length");
                return null;
            }
            if (!lengthResult.isOkay()) {
                UserError.Log.d(TAG, "Invalid response when reading length");
                continue;
            }

            val readLen = lengthResult.asInteger();
            UserError.Log.d(TAG, "Reading data of length: " + readLen);

            val read = T4Read.builder().offset(2).length(readLen).build();
            val reads = read.encodeForMtu(Math.min(MAX_READ_SIZE_PARAMETER, mleMax));

            T4Reply reply = null;
            for (val readCmd : reads) {
                for (int retry = 0; retry < MAX_RETRY; retry++) {
                    reply = t4Transceive(readCmd, reply);
                    if (reply == null) {
                        UserError.Log.d(TAG, "Read transceive fully failed");
                        return null;
                    }
                    if (reply.isOkay()) break;
                    JoH.threadSleep(RETRY_PAUSE);
                }
            }

            if (reply != null && reply.isOkay()) {
                val blen = reply.bytes.length;
                if (blen == readLen) {
                    UserError.Log.d(TAG, "Successfully read: " + blen + " bytes");
                    return reply.bytes;
                } else {
                    UserError.Log.e(TAG, "Read length mismatch " + blen + " vs " + readLen);
                }
            }

        } // end for
        return null;
    }

    public boolean readContainerData() {
        val containerSize = 15;
        val reply = t4Transceive(T4Read.builder()
                .offset(0)
                .length(containerSize)
                .build().encode());
        if (reply == null) {
            UserError.Log.d(TAG, "Failed to read container data (null reply)");
            return false;
        }
        if (!reply.isOkay()) {
            UserError.Log.d(TAG, "Failed to read container data (not okay)");
            return false;
        }
        val data = reply.bytes;
        if (d) UserError.Log.d(TAG, "Container data: " + HexDump.dumpHexString(data));

        if (data.length == containerSize) {
            val b = ByteBuffer.wrap(data);
            val cclen = getUnsignedShort(b);
            val mapping = getUnsignedByte(b);
            mleMax = getUnsignedShort(b);
            UserError.Log.d(TAG, "mleMax: " + mleMax);
            mlcMax = getUnsignedShort(b);
            UserError.Log.d(TAG, "mlcMax: " + mlcMax);
            val t = getUnsignedByte(b);
            val l = getUnsignedByte(b);
            val ident = getUnsignedShort(b);
            val nmax = getUnsignedShort(b);
            val rsec = getUnsignedByte(b);
            val wsec = getUnsignedByte(b);
            return true;
        } else {
            UserError.Log.e(TAG, "Read container data fails length check: " + HexDump.dumpHexString(data));
        }
        return false;
    }

    public boolean doNeededSelection() {
        return (transceiveOkay(SA, "Failed to select application")
                && transceiveOkay(SC, "Failed to select container")
                && readContainerData()
                && transceiveOkay(SN, "Failed to select ndef"));
    }
}
