package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.cgm.medtrum.Crypt;
import com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum;

import static com.eveningoutpost.dexdrip.models.JoH.tolerantHexStringToByteArray;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_AUTH_REQST;

// jamorham

public class AuthTx extends BaseMessage {

    final byte opcode = OPCODE_AUTH_REQST; // 0x05
    final int length = 10;

    public AuthTx(final long serial) {
        init(opcode, length, true);

        byte[] aBytes = null;
        try {
            aBytes = tolerantHexStringToByteArray(Pref.getString(Medtrum.PREF_AHEX, ""));
        } catch (Exception e) {
            //
        }
        boolean active = aBytes != null && aBytes.length == 4;
        if (Medtrum.getVersion(serial) > 186) {
            active = true;
            aBytes = null;
        }

        data.put(active ? (byte) 0xC9 : (byte) 0x02);
        data.putInt(0);
        if (active && aBytes != null) {
            data.put(aBytes);
        } else {
            data.putInt((int) (active ? Crypt.doubleSchrageSbox(serial) : Crypt.doubleSchrage(serial)));
        }
    }
}
