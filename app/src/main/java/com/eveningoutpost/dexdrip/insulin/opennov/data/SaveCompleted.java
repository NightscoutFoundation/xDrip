package com.eveningoutpost.dexdrip.insulin.opennov.data;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.Models.JoH.tsl;
import static com.eveningoutpost.dexdrip.insulin.opennov.Options.playSounds;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.insulin.opennov.Message;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.InsulinDose;

import lombok.val;

/**
 * JamOrHam
 * OpenNov save completed data implementation
 */

public class SaveCompleted implements ICompleted {

    private static final String TAG = "OpenNov";
    private static final String MARKER = "xDrip NFC scan";
    private static final String NOTE_PREFIX = "PEN";

    @Override
    public int receiveFinalData(final Message msg) {

        boolean newData = false;
        val doses = msg.getContext().eventReport.doses;
        for (val dose : doses) {
            if (dose.isValid()) {
                if (msSince(dose.absoluteTime) < Constants.WEEK_IN_MS) {
                    val uuid = uuidFromDose(msg, dose);
                    val existing = Treatments.byuuid(uuid);
                    val serial = msg.getContext().specification.getSerial();
                    if (existing == null) {
                        val treatment = Treatments.create(0, dose.units, dose.absoluteTime, uuid);
                        if (treatment != null) {
                            treatment.enteredBy = MARKER + " @ " + JoH.dateTimeText(tsl());
                            treatment.notes = NOTE_PREFIX + " " + serial + "\n" + msg.getContext().model.getModel();
                            treatment.save();
                            newData = true;
                        }
                        UserError.Log.uel(TAG, "New dose logged from pen: " + serial + " " + dose.units + "U @ " + JoH.dateTimeText(dose.absoluteTime));
                        if (playSounds() && JoH.ratelimit("opennov_data_in", 1)) {
                            JoH.playResourceAudio(R.raw.bt_meter_data_in);
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

    private String uuidFromDose(final Message msg, final InsulinDose dose) {
        return msg.getContext().specification.getSerial() + ":" + dose.getHash();
    }

    public static void deleteAll() {
        UserError.Log.d(TAG, "Deleting all pen treatments");
        val list = Treatments.latest(10000);
        if (list != null) {
            for (val item : list) {
                if (item.enteredBy.startsWith(MARKER) && item.notes.startsWith(NOTE_PREFIX)) {
                    UserError.Log.d(TAG, "Deleting item: " + item.uuid);
                    item.delete();
                }
            }
        }
    }

}
