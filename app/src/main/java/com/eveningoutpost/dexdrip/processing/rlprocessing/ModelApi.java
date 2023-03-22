package com.eveningoutpost.dexdrip.processing.rlprocessing;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

import org.apache.commons.lang3.NotImplementedException;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

public class ModelApi {
    private static final String TAG = "RLApi"; // Tag string to identify the class in the logs.
    private static ModelApi instance; // Only instance of the class.
    private Interpreter interpreter; // Model interpreter. Used to feed data to the model and get the result.

    // Path to tflite model
    private final String rl_model_path = PersistentStore.getString(Constants.RL_MODEL_FILE_PATH);
    // The .tflite file is copied to this path to be used by the interpreter.
    private final String tflite_inputstream_path = xdrip.getAppContext().getFilesDir().getAbsolutePath() + "/model.tflite";

    private ModelApi() {}

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
        if (interpreter == null) {
            Log.e(TAG, "Model not loaded.");
            loadModel();
        }

        throw new NotImplementedException("Ratios are not implemented.");
    }

    /**
     * Using historical BG data, calculate the basal.
     * @return Needed basal.
     */
    public Double inferBasal(RLInput input) {
        if (interpreter == null) {
            Log.e(TAG, "Model not loaded.");
            loadModel();
        }

        throw new NotImplementedException("Basal not implemented.");
    }

    /**
     * Using historical BG data, calculate the insulin.
     * @return Needed insulin.
     */
    public float inferInsulin(RLInput input) {
        if (interpreter == null) {
            Log.e(TAG, "Model not loaded.");
            loadModel();
        }

        // float[] input_data = new float[1]; // Test fixed input
        // input_data[0] = 100.0f;
        float[][] output_data = new float[1][1];
        try {
            Log.d(TAG, "Output: " + Arrays.deepToString(output_data));
            interpreter.run(input.getInputData(), output_data);
        }
        catch (Exception e) {
            Log.e(TAG, "Error running the model:" + e.getMessage());
            toast("RL model running failed.");
        }
        return output_data[0][0];
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
        float[] bg_data; // All bg values in the last X hours.
        float[] carbs_data; // All carbs values in the last X hours.
        float[] insulin_data; // All insulin values in the last X hours.

        public RLInput(List<Treatments> treatments, List<BgReading> bgreadings) {
            // For now the model only uses the lastest BG value.
            bg_data = new float[1];
            bg_data[0] = (float) bgreadings.get(0).calculated_value;

            // TODO format the data dependant on timestamps
        }

        public float[] getInputData() {
            // TODO use all the data posible
            // Right now it only uses the last BG value.

            // input: [bg_in_float]
            return bg_data;
        }
    }

    /**
     * Loads the model from the file.
     */
    private void loadModel() {
        // We will use the ByteBuffer to create the interpreter.
        FileInputStream inputStream = null;
        try { inputStream = new FileInputStream(tflite_inputstream_path); }
        catch (FileNotFoundException e) {
            Log.e(TAG, "Error loading the model:" + e.getMessage());
            toast("RL model not loaded.");
        }

        // Check if file exists
        if (inputStream != null) {
            try {
                FileChannel fileChannel = inputStream.getChannel();
                MappedByteBuffer myMappedBuffer =  fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

                this.interpreter = new Interpreter(myMappedBuffer);
                Log.i(TAG, "Model loaded successfully.");
                toast("RL model loaded successfully.");
            }
            catch (Exception e) {
                Log.e(TAG, "Error loading the model:" + e.getMessage());
                toast("RL model not loaded.");
            }
        }
    }

    public void importModel(Uri uri) {
        try {
            InputStream stream = xdrip.getAppContext().getContentResolver().openInputStream(uri);

            OutputStream output = new BufferedOutputStream(new FileOutputStream(tflite_inputstream_path));
            copyFile(stream, output);

            loadModel();
        }
        catch (Exception e) {
            Log.e(TAG, "Error loading the model:" + e.getMessage());
        }
    }

    /**
     * Copies a file from an InputStream to an OutputStream.
     * @param in InputStream to read from.
     * @param out OutputStream to write to.
     * @throws IOException
     */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    // TODO move this to a UI class
    private void toast(String message) {
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(xdrip.getAppContext(), message, duration);
        toast.show();
    }
}
