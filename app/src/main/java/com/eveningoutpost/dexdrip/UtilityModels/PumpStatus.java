package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.Models.JoH;

/**
 * Created by jamorham on 18/08/2017.
 */

public class PumpStatus {

    private static final String PUMP_RESERVOIR = "pump-reservoir";
    private static final String PUMP_RESERVOIR_TIME = "pump-reservoir-time";

    public static void setReservoir(double reservoir) {
        if (reservoir < 0) reservoir = 0;
        PersistentStore.setDouble(PUMP_RESERVOIR, reservoir);
        PersistentStore.setLong(PUMP_RESERVOIR_TIME, JoH.tsl());
    }

    public static double getReservoir() {
        final long ts = PersistentStore.getLong(PUMP_RESERVOIR_TIME);
        if ((ts > 1503081681000L) && (JoH.msSince(ts) < Constants.MINUTE_IN_MS * 30)) {
            return PersistentStore.getDouble(PUMP_RESERVOIR);
        } else {
            return -1;
        }
    }

    public static String getReservoirString() {
        final double reservoir = getReservoir();
        if (reservoir > -1) {
            //return "\u231B" + " " + JoH.qs(reservoir, 1) + "U";
            return "\u262F" + " " + JoH.qs(reservoir, 1) + "U";
        } else {
            return "";
        }
    }
}
