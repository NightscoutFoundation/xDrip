package com.eveningoutpost.dexdrip.insulin.shared;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.PenData;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.util.List;

/**
 * jamorham
 *
 * Process PenData and create Treatments
 */

public class ProcessPenData {

    private static final String TAG = "ProcessPenData";

    public static synchronized void process() {

        // get data sets
        final List<List<PenData>> classifyResults = PrimeDetection.classify();
        final List<PenData> doses = classifyResults.get(1);
        final List<PenData> rewinds = classifyResults.get(2);

        final List<Treatments> treatments = Treatments.latestForGraph(50000, JoH.tsl() - Constants.WEEK_IN_MS, JoH.tsl());
        UserError.Log.d(TAG, "Existing treatments size: " + treatments.size());

        boolean newData = false;

        if (doses.size() > 0 || rewinds.size() > 0) {

            for (final PenData pd : rewinds) {
                if (!Treatments.matchUUID(treatments, pd.uuid)) {
                    UserError.Log.d(TAG, "New rewind: " + pd.brief());
                    // create rewind treatment entry thing
                    // TODO format string
                    Treatments.create_note("Pen cartridge change: rewound: " + pd.units + "U on " + pd.penName(), pd.timestamp, 0, pd.uuid);
                    newData = true;
                }
            }

            for (final PenData pd : doses) {
                if (!Treatments.matchUUID(treatments, pd.uuid)) {
                    UserError.Log.d(TAG, "New Dose: " + pd.brief());
                    Treatments.create(0, pd.units, pd.timestamp, pd.uuid);
                    newData = true;
                }
            }

            if (newData) {
                Home.staticRefreshBGChartsOnIdle();
            }
        } else {
            UserError.Log.d(TAG, "No results to process");
        }

        // TODO show temporal prime a button
        // TODO adjust bitfields etc on pen data to indicate processed?

    }

}
