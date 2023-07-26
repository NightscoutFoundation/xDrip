package com.eveningoutpost.dexdrip.glucosemeter.glucomen.data;


import static com.eveningoutpost.dexdrip.glucosemeter.glucomen.GlucoMen.playSounds;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;

import com.eveningoutpost.dexdrip.glucosemeter.glucomen.GlucoMenNfc;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.LowPriorityThread;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * GlucoMen records processor task
 */

@RequiredArgsConstructor
public class ProcessingThread {

    private static final String TAG = ProcessingThread.class.getSimpleName();

    private final GlucoMenNfc p;

    public volatile boolean running = false;
    @Getter
    private volatile boolean enoughGlucose = false;
    @Getter
    private volatile boolean enoughKetones = false;

    private long timeoutTime = 0;


    private void extendTimeout(final long ms) {
        timeoutTime = tsl() + ms;
    }

    private boolean timedOut() {
        return timeoutTime < tsl();
    }

    public void start() {
        synchronized (ProcessingThread.class) {
            enoughGlucose = false;
            enoughKetones = false;
            if (!running) {
                new LowPriorityThread(() -> {
                    running = true;
                    extendTimeout(SECOND_IN_MS * 10);
                    UserError.Log.d(TAG, "Started running!");

                    val glucose = p.getGlucoseBlocks();
                    val ketones = p.getKetoneBlocks();

                    while (!glucose.isEmpty()
                            || !ketones.isEmpty()
                            || !timedOut()) {

                        while (true) {
                            val r = glucose.poll();
                            if (r == null) break;
                            UserError.Log.d(TAG, "Processing glucose block: " + r + " ts: " + JoH.dateTimeText(r.getTimestamp()));
                            extendTimeout(SECOND_IN_MS * 5);
                            // if already seen and not full then terminate processing?
                            if (r.isUsable()) {
                                if (r.mgdl) {
                                    mgdlWarning();
                                } else {
                                    val ts = r.getTimestamp();
                                    BloodTest btresult = null;
                                    if (msSince(ts) < Constants.MONTH_IN_MS) {
                                        synchronized (BloodTest.class) {
                                            btresult = BloodTest.create(ts, r.getMgDlValue(), "Glucomen: " + p.getSerial(), p.getSerial() + ":" + ts + ":" + r.mmolValue);
                                        }
                                        if (btresult != null && playSounds()) {
                                            if (JoH.ratelimitmilli("glucose-meter-in", 300)) {
                                                BackgroundQueue.post(() -> JoH.playResourceAudio(R.raw.bt_meter_data_in));
                                            }
                                        }
                                        if (btresult == null) {
                                            if (!p.getIndexBlock().glucoseMemoryFull()) {
                                                UserError.Log.d(TAG, "Didn't create a new record and meter not full so skipping further requests");
                                                enoughGlucose = true;
                                            }
                                        }
                                    } else {
                                        if (!p.getIndexBlock().glucoseMemoryFull()) {
                                            UserError.Log.d(TAG, "In to older glucose records and meter not full so not asking for more");
                                            enoughGlucose = true;
                                        }
                                    }
                                }
                            }
                        }

                        while (true) {
                            val r = ketones.poll();
                            if (r == null) break;
                            final long now = tsl();
                            UserError.Log.d(TAG, "Processing ketone block: " + r + " ts: " + JoH.dateTimeText(r.getTimestamp()));
                            extendTimeout(SECOND_IN_MS * 5);
                            if (r.isUsableKetone()) {
                                if (r.mgdl) {
                                    mgdlWarning();
                                } else {
                                    val ts = r.getTimestamp();
                                    if (msSince(ts) < Constants.MONTH_IN_MS) {
                                        if (ts < now) {
                                            val msg = "Ketone " + Unitized.unitized_string_static_no_interpretation_short(r.getMgDlValue());
                                            UserError.Log.d(TAG, "Attempting to create note for ketone: " + msg);
                                            synchronized (BloodTest.class) {
                                                val t = Treatments.create_note(msg, ts);
                                                if (t == null || !t.wasCreatedRecently()) {
                                                    if (t != null)
                                                        UserError.Log.d(TAG, "Was created recently! " + t.created_at);
                                                    if (!p.getIndexBlock().ketoneMemoryFull()) {
                                                        UserError.Log.d(TAG, "Didn't create a new ketone record and meter not full so not asking for more");
                                                        enoughKetones = true;
                                                    }
                                                }
                                            }
                                        } else {
                                            UserError.Log.e(TAG, "Ketone record in future: " + JoH.dateTimeText(ts));
                                        }
                                    } else {
                                        if (!p.getIndexBlock().ketoneMemoryFull()) {
                                            UserError.Log.d(TAG, "In to older ketone records and meter not full so not asking for more");
                                            enoughKetones = true;
                                        }
                                    }

                                }
                            }
                        }

                        JoH.threadSleep(300);
                    }

                    running = false;
                    UserError.Log.d(TAG, "Stopped running!");

                }, TAG).start();
            } else {
                UserError.Log.d(TAG, "Already running!");
            }
        }
    }

    void mgdlWarning() {
        if (JoH.quietratelimit("mgdl-warning", 5)) {
            JoH.static_toast_long("Mg/Dl meter type is not supported yet, please join github issue to get support added");
        }
    }

    public void stop() {
        running = false;
    }


}
