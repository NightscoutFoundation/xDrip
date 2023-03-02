package com.eveningoutpost.dexdrip.processing.rlprocessing;

import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;

import org.tensorflow.lite.Interpreter;


import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.util.List;
import java.util.Random;

public class ModelApi {
    private static final String TAG = "RL model:"; // Tag string to identify the class in the logs.
    private static ModelApi instance; // Only instance of the class.
    private Interpreter interpreter; // Model interpreter. Used to feed data to the model and get the result.

    private ModelApi() {
        String model_location = ""; // TODO change to saved location in shared preferences.

        try {
            this.loadModel(model_location);
        } catch (Exception e) {
            Log.e(TAG, "Error loading the model:" + e.getMessage()); // TODO Show an error message to the user.
        }
    }

    /**
     * Singleton pattern.
     * @return The only instance of the class.
     */
    public static ModelApi getInstance() {
        if (instance == null) {
            instance = new ModelApi();
        }
        return instance;
    }

    /**
     * Using historical BG data, calculate the ratios.
     * @return Wrapper class containing insulin/carb ratios.
     */
    public Ratios inferRatios(RLInput input) {
        throw new NotImplementedException("Ratios are not implemented.");
    }

    /**
     * Using historical BG data, calculate the basal.
     * @return Needed basal.
     */
    public Double inferBasal(RLInput input) {
        throw new NotImplementedException("Basal not implemented.");
    }

    /**
     * Using historical BG data, calculate the insulin.
     * @return Needed insulin.
     */
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

    /**
     * This class formats the data obtained from the historical data
     * from other classes and convert them to a easily used wrapper.
     */
    public static class RLInput {

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
