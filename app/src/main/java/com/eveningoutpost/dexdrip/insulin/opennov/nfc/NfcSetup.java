package com.eveningoutpost.dexdrip.insulin.opennov.nfc;

import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;

/**
 * JamOrHam
 * OpenNov nfc setup
 */

public class NfcSetup {

    private static final String TAG = "OpenNov";

    // TODO make this generic and able to handle multiple different types of tags with dispatch to appropriate locations

    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A
                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                    | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

    public static void initNFC(Activity context) {

        UserError.Log.d(TAG, "InitNFC start");
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        try {
            if (mNfcAdapter == null) {
                JoH.static_toast_long(gs(R.string.phone_has_no_nfc_reader));
                return;

            } else if (!mNfcAdapter.isEnabled()) {
                JoH.static_toast_long(gs(R.string.nfc_is_not_enabled));
                return;
            }
        } catch (NullPointerException e) {
            JoH.static_toast_long(gs(R.string.phone_nfc_is_having_problems));
            return;
        }

        NfcManager nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        if (nfcManager != null) {
            mNfcAdapter = nfcManager.getDefaultAdapter();
        }

        if (mNfcAdapter != null) {
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

            mNfcAdapter.enableReaderMode(context, new TagDispatcher(), READER_FLAGS, null);

        }
    }
}
