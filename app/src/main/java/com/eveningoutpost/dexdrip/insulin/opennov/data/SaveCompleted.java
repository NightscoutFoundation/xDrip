package com.eveningoutpost.dexdrip.insulin.opennov.data;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.Models.JoH.tsl;
import static com.eveningoutpost.dexdrip.insulin.opennov.Options.playSounds;
import static com.eveningoutpost.dexdrip.insulin.opennov.Options.removePrimingDoses;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.InsulinInjection;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.insulin.MultipleInsulins;
import com.eveningoutpost.dexdrip.insulin.opennov.Machine;
import com.eveningoutpost.dexdrip.insulin.opennov.Message;
import com.eveningoutpost.dexdrip.insulin.opennov.Options;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.InsulinDose;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.val;

/**
 * JamOrHam
 * OpenNov save completed data implementation
 */

public class SaveCompleted implements ICompleted {

    private static final String TAG = "OpenNov";
    private static final String MARKER = "xDrip NFC scan";
    private static final String PRIMING = "Priming";
    private static final String NOTE_PREFIX = "PEN";

    private final List<Treatments> cache = new LinkedList<>();
    private Pens pens;

    @Override
    public int receiveFinalData(final Message msg) {
        final boolean trackPens = MultipleInsulins.isEnabled();
        if (trackPens) {
            pens = Pens.load();
        }
        Boolean knownPen = null;
        boolean newData = false;
        val doses = msg.getContext().eventReport.doses;
        for (val dose : doses) {
            if (dose.isValid()) {
                if (msSince(dose.absoluteTime) < Constants.WEEK_IN_MS) {
                    val uuid = uuidFromDose(msg, dose);
                    val existing = Treatments.byuuid(uuid);
                    val serial = msg.getContext().specification.getSerial();

                    if (trackPens) {
                        if (knownPen == null) {
                            knownPen = pens.hasPenWithSerial(serial);
                        }
                        if (!knownPen) {
                            UserError.Log.d(TAG, "Don't know type of pen for serial: " + serial);
                            newData = true; // to force reload again
                            if (JoH.ratelimit("choose-insulin-pen", 10)) {
                                Home.startHomeWithExtra(xdrip.getAppContext(), Home.CHOOSE_INSULIN_PEN, serial);
                            }
                            break; // exit loop
                        }
                    }

                    if (existing == null) {
                        List<InsulinInjection> insulinType = null;
                        if (trackPens) {
                            insulinType = Treatments.convertLegacyDoseToInjectionListByName(pens.getPenTypeBySerial(serial), dose.units);
                        }
                        val treatment = Treatments.create(0, dose.units, insulinType, dose.absoluteTime, uuid);
                        if (treatment != null) {
                            treatment.enteredBy = MARKER + " @ " + JoH.dateTimeText(tsl());
                            treatment.notes = NOTE_PREFIX + " " + serial + "\n" + msg.getContext().model.getModel(); // must be same for each dose for a specific pen
                            treatment.save();
                            cache.add(treatment);
                            newData = true;
                        }
                        UserError.Log.uel(TAG, "New dose logged from pen: " + serial + " " + dose.units + "U " + (trackPens ? pens.getPenTypeBySerial(serial) : "") + " @ " + JoH.dateTimeText(dose.absoluteTime));
                        if (playSounds() && JoH.ratelimitmilli("opennov_data_in", 400)) {
                            BackgroundQueue.post(() -> JoH.playResourceAudio(R.raw.bt_meter_data_in));
                        }
                    } else {
                        UserError.Log.d(TAG, "Existing dose: " + uuid);
                    }
                } else {
                    UserError.Log.d(TAG, "Discarding too old dose: " + dose.toJson());
                }
            } else {
                UserError.Log.d(TAG, "Discarding invalid dose: " + dose.toJson());
            }
        }
        if (newData) {
            Home.staticRefreshBGChartsOnIdle();
        }
        return newData ? doses.size() : 0;
    }

    // find a dose immediately after this dose within parameters which indicates this dose is priming, check for back reference
    static boolean isPrimingDose(final List<Treatments> cache, final Treatments dose, final double doseThreshold, final long timeThreshold) {
        if (dose.insulin > doseThreshold) {
            return false;
        }
        val primeTime = dose.timestamp;
        val primeMax = primeTime + timeThreshold;
        val primeMin = primeTime - timeThreshold;

        // find earlier prime that could make this not a prime
        for (val nextDose : cache) {
            if ((nextDose.timestamp < primeTime)
                    && nextDose.timestamp > primeMin
                    && !nextDose.uuid.equals(dose.uuid)
                    && nextDose.insulin == 0.0d) {
                return false;
            }
        }

        // find real dose that could make this a prime
        for (val nextDose : cache) {
            if ((nextDose.timestamp > primeTime)
                    && nextDose.timestamp < primeMax
                    && !nextDose.uuid.equals(dose.uuid)
                    && nextDose.notes != null
                    && nextDose.notes.equals(dose.notes)
                    && nextDose.insulin > 0.0d) {
                return true;
            }
            // can optimize as list is sorted
            if (nextDose.timestamp > primeMax) break;
        }
        return false;
    }

    @Override
    public int prunePrimingDoses() {
        int removed = 0;
        if (removePrimingDoses()) {
            val timeThreshold = (long) (Constants.MINUTE_IN_MS * Options.primingMinutes());
            val doseThreshold = Options.primingUnits();

            // sort timestamps ascending
            Collections.sort(cache, (o1, o2) -> Long.valueOf(o1.timestamp).compareTo(Long.valueOf(o2.timestamp)));

            for (val dose : cache) {
                if (isPrimingDose(cache, dose, doseThreshold, timeThreshold)) {
                    dose.notes = PRIMING + " " + dose.insulin + "U" + "\n" + dose.notes;
                    dose.insulin = 0;
                    dose.save();
                    UserError.Log.d(TAG, "Removed priming dose @ " + JoH.dateTimeText(dose.timestamp) + " " + dose.notes);
                }
            }
        } else {
            UserError.Log.d(TAG, "Not removing priming doses due to config");
        }
        cache.clear();
        return removed;
    }

    private String uuidFromDose(final Message msg, final InsulinDose dose) {
        return msg.getContext().specification.getSerial() + ":" + dose.getHash();
    }

    public static void deleteAll() {
        UserError.Log.d(TAG, "Deleting all pen treatments");
        val list = Treatments.latest(10000);
        if (list != null) {
            for (val item : list) {
                if (item.enteredBy.startsWith(MARKER)
                        && (item.notes.startsWith(NOTE_PREFIX) || item.notes.startsWith(PRIMING))) {
                    UserError.Log.d(TAG, "Deleting item: " + item.uuid);
                    item.delete();
                }
            }
        }
        val plist = Pens.load().pens;
        for (val p : plist) {
            Machine.deleteSuccessInfo(p.serial);
        }
        new Pens().save();
    }

}
