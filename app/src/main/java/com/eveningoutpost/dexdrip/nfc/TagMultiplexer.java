package com.eveningoutpost.dexdrip.nfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import com.eveningoutpost.dexdrip.glucosemeter.glucomen.GlucoMen;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.GlucoMenNfc;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.insulin.opennov.nfc.TagDispatcher;

import java.util.concurrent.Semaphore;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * Multiplex different types of tag to their handler
 */


@RequiredArgsConstructor
public class TagMultiplexer implements NfcAdapter.ReaderCallback {

    private static final String TAG = "NFCmulti";
    private final Activity activity;
    private final Semaphore nfcLock = new Semaphore(1);

    @Override
    public void onTagDiscovered(final Tag tag) {
        try {
            nfcLock.acquire();
            try {
                val tlist = tag.getTechList();
                val tech = tlist[0];
                UserError.Log.d(TAG, "tech:" + tech);

                switch (tech) {
                    case "android.nfc.tech.IsoDep":
                        TagDispatcher.getInstance().onTagDiscovered(tag);
                        break;

                    case "android.nfc.tech.NfcV":
                        if (GlucoMen.wantThis(tag)) {
                             new GlucoMenNfc(tag).scan();
                        } else {
                            NFCReaderX.doTheScan(activity, tag, false);
                        }
                        break;

                    default:
                        UserError.Log.e(TAG, "Unhandled NFC technology type: " + tech);
                        break;
                }
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Caught exception handling NFC: " + e);
            } finally {
                nfcLock.release();
            }
        } catch (InterruptedException e) {
            UserError.Log.wtf(TAG, "Interrupted thread while processing tag");
        }

    }
}
