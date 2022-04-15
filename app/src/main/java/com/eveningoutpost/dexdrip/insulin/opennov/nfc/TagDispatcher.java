package com.eveningoutpost.dexdrip.insulin.opennov.nfc;

import static com.eveningoutpost.dexdrip.insulin.opennov.Options.playSounds;

import android.nfc.NfcAdapter;
import android.nfc.Tag;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.insulin.opennov.OpenNov;
import com.eveningoutpost.dexdrip.insulin.opennov.data.ICompleted;
import com.eveningoutpost.dexdrip.insulin.opennov.data.SaveCompleted;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;

import lombok.val;

/**
 * JamOrHam
 * OpenNov tag dispatching
 */

public class TagDispatcher implements NfcAdapter.ReaderCallback {

    private static final String TAG = "OpenNov";
    private static final String openNovRateLimit = "opennov-successful-run";

    private final ICompleted dataSaver = new SaveCompleted();

    private static class Singleton {
        private static final TagDispatcher INSTANCE = new TagDispatcher();
    }

    public static TagDispatcher getInstance() {
        return TagDispatcher.Singleton.INSTANCE;
    }

    @Override
    public synchronized void onTagDiscovered(final Tag tag) {
        if (JoH.ratelimit(openNovRateLimit, 4)) {
            val openNov = new OpenNov();
            if (!openNov.processTag(tag, dataSaver)) {
                UserError.Log.d(TAG, "Failed to read pen!");
                if (playSounds()) {
                    BackgroundQueue.post(() -> JoH.playResourceAudio(R.raw.bt_meter_disconnect));
                }
                JoH.clearRatelimit(openNovRateLimit);
                JoH.static_toast_short("Failed to read pen!");
            } else {
                JoH.static_toast_short("Pen read okay");
            }
            dataSaver.prunePrimingDoses();
        }
    }
}
