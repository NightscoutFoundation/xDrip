package com.eveningoutpost.dexdrip.glucosemeter.glucomen.devices;

import android.nfc.Tag;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.UserError;

import lombok.val;

/**
 * JamOrHam
 * GlucoMen device identifier
 */

public class Identify {

    private static final String TAG = "GlucoMenID";

    private static final byte ISO_15693_ID = (byte) 0xE0;
    private static final byte MANUFACTURER_ST = 0x02;
    private static final byte AERO2K_PRODUCT_CODE = 0x26;

    public static BaseDevice getDevice(final Tag tag) {
        val id = tag.getId();
        UserError.Log.d(TAG, "Tag ID bytes: " + HexDump.dumpHexString(id));
        if (id.length == 8) {
            if (id[7] == ISO_15693_ID
                    && id[6] == MANUFACTURER_ST
                    && id[5] == AERO2K_PRODUCT_CODE) {
                return new Aero2kDevice();
            }
        }
        return new UnknownDevice();
    }
}
