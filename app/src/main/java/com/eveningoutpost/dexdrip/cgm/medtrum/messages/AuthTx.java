package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.cgm.medtrum.Crypt;
import com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum;

import static com.eveningoutpost.dexdrip.Models.JoH.tolerantHexStringToByteArray;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_AUTH_REQST;

// jamorham

public class AuthTx extends BaseMessage {

    final byte opcode = OPCODE_AUTH_REQST; // 0x05
    final int length = 10;

    public AuthTx(long serial) {
        init(opcode, length, true);

        byte[] aBytes = null;
        try {
            aBytes = tolerantHexStringToByteArray(Pref.getString(Medtrum.PREF_AHEX, ""));
        } catch (Exception e) {
            //
        }
        final boolean active = aBytes != null && aBytes.length == 4;
        data.put(active ? (byte) 0xC9 : (byte) 0x02);
        data.putInt(0);
        if (active) {
            data.put(aBytes);
        } else {
            data.putInt((int) Crypt.doubleSchrage(serial));
        }
    }
}
