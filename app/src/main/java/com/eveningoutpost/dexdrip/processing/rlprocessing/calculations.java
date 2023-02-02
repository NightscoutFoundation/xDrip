package com.eveningoutpost.dexdrip.processing.rlprocessing;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;

import org.apache.commons.lang3.NotImplementedException;

public class calculations {
    /**
     * Using data from the database, calculate the ratios.
     * @return
     */
    public static modelapi.Ratios calculateRatios() {
        // TODO
        throw new NotImplementedException("Ratios are not implemented.");
    }

    /**
     * Using data from the database, calculate the basal.
     * @return
     */
    public static Double calculateBasal() {
        // TODO
        throw new NotImplementedException("Basal not implemented.");
    }

    /**
     * Using data from the database, calculate the insulin.
     * @return
     */
    public static Double calculateInsulin() {
        // TODO
        throw new NotImplementedException("Insulin not implemented.");
    }

    /**
     * Gets all the data needed for the RL model from other classes.
     */
    public static modelapi.rlInput getRLInput() {
        // TODO
        throw new NotImplementedException("Input data has to be implemented.");
    }
}
