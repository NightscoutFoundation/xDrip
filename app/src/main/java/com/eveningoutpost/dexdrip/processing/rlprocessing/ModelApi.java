package com.eveningoutpost.dexdrip.processing.rlprocessing;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;

import org.tensorflow.lite.Interpreter;


import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.util.List;
import java.util.Random;

public class ModelApi {
    /**
     * Variables for the loaded model.
     * TODO: Implement loading the model.
     */
    private static ModelApi instance;
    private Interpreter interpreter; // Model interpreter. Used to feed data to the model and get the result.

    private ModelApi() {
        String model_location = ""; // TODO change to saved location in shared preferences.
        this.loadModel(model_location); // TODO use a try catch block to catch errors.
    }

    public static ModelApi getInstance() {
        if (instance == null) {
            instance = new ModelApi();
        }
        return instance;
    }

    public Ratios inferRatios(RLInput input) {
        throw new NotImplementedException("Ratios are not implemented.");
    }

    public Double inferBasal(RLInput input) {
        throw new NotImplementedException("Basal not implemented.");
    }

    public Double inferInsulin(RLInput input) {
        // TODO format the input data to the model.
        // TODO feed the data to the model.
        // TODO get the result from the model.
        // TODO format the result to the app.
        return (double) new Random().nextInt(10);
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

    public static class RLInput {
        /**
         * This class has to take any type of data used for RL provided
         * from other classes and convert them to a easyly used wrapper.
         */
        public RLInput(List<Treatments> treatments, List<BgReading> bgreadings) {
            // TODO format the data to be used by the model.
        }
    }

    /**
     * Loads the model from the file.
     */
    private void loadModel(String file_of_tensorflowlite_model) {
        File modelFile = new File(file_of_tensorflowlite_model);
        this.interpreter = new Interpreter(modelFile);
    }
}
