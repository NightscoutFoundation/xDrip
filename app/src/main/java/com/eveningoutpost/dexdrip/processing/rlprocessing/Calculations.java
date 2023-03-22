package com.eveningoutpost.dexdrip.processing.rlprocessing;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class Calculations {
    /**
     * Using historical BG data, calculate the ratios.
     * @return Wrapper class containing insulin/carb ratios.
     */
    public static ModelApi.Ratios calculateRatios() {
        // TODO
        throw new NotImplementedException("Ratios are not implemented.");
    }

    /**
     * Using historical BG data, calculate the basal.
     * @return Needed basal.
     */
    public static Double calculateBasal() {
        // TODO
        throw new NotImplementedException("Basal not implemented.");
    }

    /**
     * Using historical BG data, calculate the insulin.
     * @return Needed insulin.
     */
    public static float calculateInsulin() {
        ModelApi.RLInput input = getRLInput();
        return ModelApi.getInstance().inferInsulin(input);
    }

    /**
     * Gets all the data needed for the RL model from other classes.
     */
    private static ModelApi.RLInput getRLInput() {
        // TODO get the data from the database.

        List<Treatments> treatments = Treatments.latest(20); // TODO: get the number of treatments using timestamps.
        List<BgReading> bgReadings = BgReading.latest(256); // TODO: get the number of bg readings using timestamps.

        return new ModelApi.RLInput(treatments, bgReadings);
    }
}
