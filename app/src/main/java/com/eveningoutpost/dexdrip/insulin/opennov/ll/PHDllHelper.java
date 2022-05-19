package com.eveningoutpost.dexdrip.insulin.opennov.ll;

import static com.eveningoutpost.dexdrip.Models.JoH.tolerantHexStringToByteArray;
import static com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage.d;
import static com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage.log;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov link layer helper
 */

@RequiredArgsConstructor
public class PHDllHelper {

    private static final String TAG = "OpenNov";
    private static final byte[] EMPTY_NDEF = tolerantHexStringToByteArray("D0 00 00");

    private final T4Transceiver ts;
    private int sequence = 0;

    public byte[] extractInnerPacket(byte[] res, boolean sendAck) {
        val p = PHDll.parse(res);
        if (p == null) return null;
        if (p.getSeq() != sequence) {
            UserError.Log.e(TAG, "Unexpected sequence " + p.getSeq() + " vs " + sequence);
            return null;
        }
        if (sendAck) {
            ts.writeToLinkLayer(EMPTY_NDEF);
        }
        return p.getInner();
    }

    public int writeInnerPacket(final byte[] inner) {
        if (d) log("inner write: " + HexDump.dumpHexString(inner));
        val outer = PHDll.builder()
                .inner(inner)
                .seq(++sequence)
                .build().encode();
        sequence = (sequence + 1) & 0x0F;
        ts.writeToLinkLayer(outer);
        return inner.length;
    }

}
