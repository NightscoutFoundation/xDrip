package com.eveningoutpost.dexdrip.processing.rlprocessing;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;

import org.apache.commons.lang3.NotImplementedException;

public class modelapi {
    /**
     * Variables for the loaded model.
     * TODO: Implement loading the model.
     */

    public modelapi() {
        loadModel();
    }

    public static Ratios inferRatios() {
        throw new NotImplementedException("Ratios are not implemented.");
    }

    public static Ratios inferBasal() {
        throw new NotImplementedException("Basal not implemented.");
    }

    public static Ratios inferInsulin() {
        throw new NotImplementedException("Insulin not implemented.");
    }

    /**
     * Wrapper to save the result of any ratio calculated by inferRatios.
     */
    public static class Ratios {
        private Integer carbRatio;
        private Integer insulinSensitivity;
        public Integer getCarbRatio() {
            return carbRatio;
        }

        public void setCarbRatio(Integer carbRatio) {
            this.carbRatio = carbRatio;
        }

        public Integer getInsulinSensitivity() {
            return insulinSensitivity;
        }

        public void setInsulinSensitivity(Integer insulinSensitivity) {
            this.insulinSensitivity = insulinSensitivity;
        }
    }

    public static class rlInput {
        /**
         * This class has to take any type of data used for RL provided
         * from other classes and convert them to a easyly used wrapper.
         */
        public rlInput(Treatments treatments, BgReading bgreadings) {
            // TODO
            throw new NotImplementedException("Input data has to be implemented.");
        }
    }

    /**
     * Loads the model from the file.
     */
    public static void loadModel() {
        // TODO
        throw new NotImplementedException("Model loading has to be implemented.");
    }
}
