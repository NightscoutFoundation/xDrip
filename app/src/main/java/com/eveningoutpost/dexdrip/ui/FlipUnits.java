package com.eveningoutpost.dexdrip.ui;

import static com.eveningoutpost.dexdrip.utils.Preferences.handleUnitsChange;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Created by Navid200 on 25/10/2024.
 *
 * handleUnitsChange runs only when units are changed.  But, even if we don't change units, we may need to trigger that method.
 * Triggers handleUnitsChange to correct values that are in mg/dL even though the selected unit is mmol/L.
 */
public class FlipUnits {

    public static void triggerUnitsChange() {

        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl"); // Identify which unit is chosen
        if (!domgdl) { // Only if the selected unit is mmol/L
            handleUnitsChange(null, "mmol", null);
            Home.staticRefreshBGCharts();
        }
    }
}

