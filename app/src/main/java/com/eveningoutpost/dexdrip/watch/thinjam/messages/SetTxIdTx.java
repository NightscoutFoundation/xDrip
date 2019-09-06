package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;

import java.io.UnsupportedEncodingException;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SET_TXID;

// jamorham

public class SetTxIdTx extends BaseTx {

    private static final int SET_BIT_MMOL = 0;
    private static final int SET_BIT_24_HOUR_CLOCK = 1;
    private static final int SET_BIT_RUN_COLLECTOR = 2;

    public SetTxIdTx(final String txId, final String mac) {

        // TODO needs parameter checking for length etc
        try {

            init(OPCODE_SET_TXID, 18);
            if (txId == null || txId.length() < 6) {
                // if we have no txid then write zeros
                data.put(JoH.tolerantHexStringToByteArray("00 00 00 00 00 00"), 0, 6);
            } else {
                byte[] txIdBytes = txId.getBytes("UTF-8");
                data.put(txIdBytes, 0, 6);
            }

            byte[] macBytes = JoH.tolerantHexStringToByteArray(mac);
            if (macBytes == null || macBytes.length < 6) {
                // if mac bytes invalid then write zeros
                data.put(JoH.tolerantHexStringToByteArray("00 00 00 00 00 00"), 0, 6);
            } else {
                data.put(reverseBytes(macBytes), 0, 6);

            }
            // TODO get low threshold and high threshold from preferences/alarms
            data.putShort((short) 71); // low alert mgdl
            data.putShort((short) 216); // high alert mgdl
            data.putShort(constructBitfield());
        } catch (UnsupportedEncodingException e) {
            // broken
        }
    }


    private short constructBitfield() {
        short bits = 0;
        bits |= (Unitized.usingMgDl() ? 0 : 1); // simplified shift of SET_BIT_MMOL
        bits |= (Pref.getBooleanDefaultFalse("bluejay_option_24_hour_clock") ? 1 : 0) << SET_BIT_24_HOUR_CLOCK;
        bits |= (Pref.getBooleanDefaultFalse("bluejay_collector_enabled") ? 1 : 0) << SET_BIT_RUN_COLLECTOR;
        return bits;
    }

}
