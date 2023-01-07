package com.eveningoutpost.dexdrip.nfc;

import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.glucosemeter.glucomen.GlucoMen;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.insulin.opennov.Options;

import lombok.val;

/**
 * JamOrHam
 * NFC control
 * enable reader mode for the tags we are interested in
 */

public class NFControl {

    private static final String TAG = "NFControl";

    private static int getReaderFlags() {   // if 0 then nothing enabled
        int flags = 0;
        if (Options.isEnabled()) {
            flags |= NfcAdapter.FLAG_READER_NFC_A
                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                    | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

        }
        if (NFCReaderX.useNFC()) {
            flags |= NfcAdapter.FLAG_READER_NFC_V
                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                    | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;
        }

        if (GlucoMen.isEnabled()) {
                flags |= NfcAdapter.FLAG_READER_NFC_V
                        | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                        | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;
        }

        return flags;
    }

    private static Bundle getOptionsBundle() {
        if (NFCReaderX.useNFC()) {
            final Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000);
            return options;
        } else {
            return null;
        }
    }

    public static void initNFC(final Activity context, final boolean disable) {
        UserError.Log.d(TAG, "InitNFC start");
        val mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        val flags = disable ? 0 : getReaderFlags();
        if (flags != 0) {

            try {
                if (mNfcAdapter == null) {
                    JoH.static_toast_long(gs(R.string.phone_has_no_nfc_reader));
                    return;

                } else if (!mNfcAdapter.isEnabled()) {
                    if (JoH.quietratelimit("nfc-not-enabled-toast", 300)) {
                        JoH.static_toast_long(gs(R.string.nfc_is_not_enabled));
                    }
                    return;
                }
            } catch (NullPointerException e) {
                JoH.static_toast_long(gs(R.string.phone_nfc_is_having_problems));
                return;
            }

            try {
                mNfcAdapter.isEnabled();
            } catch (NullPointerException e) {
                return;
            }
            // some superstitious code here
            try {
                mNfcAdapter.isEnabled();
            } catch (NullPointerException e) {
                return;
            }

            UserError.Log.d(TAG, "Enabling reader mode with flags: " + flags);
            mNfcAdapter.enableReaderMode(context, new TagMultiplexer(context), flags, getOptionsBundle());
        } else {
            if (mNfcAdapter != null) {
                UserError.Log.d(TAG, "Disabling reader mode");
                mNfcAdapter.disableReaderMode(context);
            }
        }
    }

}
