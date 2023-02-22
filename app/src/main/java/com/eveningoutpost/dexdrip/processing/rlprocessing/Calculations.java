package com.eveningoutpost.dexdrip.processing.rlprocessing;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class Calculations {
    /**
     * Using data from the database, calculate the ratios.
     * @return
     */
    public static ModelApi.Ratios calculateRatios() {
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
        ModelApi.RLInput input = getRLInput();
        return ModelApi.getInstance().inferInsulin(input);
    }

    /**
     * Gets all the data needed for the RL model from other classes.
     */
    public static ModelApi.RLInput getRLInput() {
        // TODO get the data from the database.

        List<Treatments> treatments = Treatments.latest(2016); // TODO: get the number of treatments using timestamps.
        List<BgReading> bgReadings = BgReading.latest(2016); // TODO: get the number of bg readings using timestamps.

        ModelApi.RLInput input = new ModelApi.RLInput(treatments, bgReadings);

        return input;
    }
}
