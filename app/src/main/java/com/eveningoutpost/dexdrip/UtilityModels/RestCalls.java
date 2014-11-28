package com.eveningoutpost.dexdrip.UtilityModels;

import android.util.Log;

import com.eveningoutpost.dexdrip.Interfaces.BgReadingInterface;
import com.eveningoutpost.dexdrip.Interfaces.CalibrationInterface;
import com.eveningoutpost.dexdrip.Interfaces.SensorInterface;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.Models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 11/6/14.
 */
public class RestCalls {
    private static final String baseUrl = "http://10.0.2.2:3000";


    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .create();



    public static void sendBgReading(final BgSendQueue bgSendQueue) {
        User user = User.currentUser();
        bgReadingInterface().createReading(user.uuid, bgSendQueue.bgReading, new Callback<Gson>() {
                    @Override
                    public void success(Gson gsonResponse, Response response) {
                        bgSendQueue.success = true;
                        bgSendQueue.save();
                        BgReading bgReading = bgSendQueue.bgReading;
                        bgReading.synced = true;
                        bgReading.save();
                    }
                    @Override
                    public void failure(RetrofitError error) {
                        Response response = error.getResponse();
                        Log.w("REST CALL ERROR:", "****************");
                        Log.w("REST CALL STATUS:", "" + response.getStatus());
                        Log.w("REST CALL REASON:", response.getReason());
                    }
                }
        );
    }


    public static void updateBgReading(final BgSendQueue bgSendQueue) {
        User user = User.currentUser();
        bgReadingInterface().updateReading(user.uuid, bgSendQueue.bgReading.uuid, bgSendQueue.bgReading, new Callback<Gson>() {
                    @Override
                    public void success(Gson gsonResponse, Response response) {
                        Log.w("REST CALL Update Success!:", "****************");
                        bgSendQueue.success = true;
                        bgSendQueue.save();
                    }
                    @Override
                    public void failure(RetrofitError error) {
                        Response response = error.getResponse();
                        Log.w("REST CALL ERROR:", "****************");
                        Log.w("REST CALL STATUS:", "" + response.getStatus());
                        Log.w("REST CALL REASON:", response.getReason());
                    }
                }
        );
    }


    public static void sendCalibration(final CalibrationSendQueue calibrationSendQueue) {
        User user = User.currentUser();
        calibrationInterface().createCalibration(user.uuid, calibrationSendQueue.calibration, new Callback<Gson>() {
                    @Override
                    public void success(Gson gsonResponse, Response response) {
                        calibrationSendQueue.success = true;
                        calibrationSendQueue.save();
                        Calibration calibration = calibrationSendQueue.calibration;
                        calibration.save();
                    }
                    @Override
                    public void failure(RetrofitError error) {
                        Response response = error.getResponse();
                        Log.w("REST CALL ERROR:", "****************");
                        Log.w("REST CALL STATUS:", "" + response.getStatus());
                        Log.w("REST CALL REASON:", response.getReason());
                    }
                }
        );
    }


    public static void sendSensor(final SensorSendQueue sensorSendQueue) {
        User user = User.currentUser();
        sensorInterface().createSensor(user.uuid, sensorSendQueue.sensor, new Callback<Gson>() {
                    @Override
                    public void success(Gson gsonResponse, Response response) {
                        sensorSendQueue.success = true;
                        sensorSendQueue.save();
                        Sensor sensor = sensorSendQueue.sensor;
                        sensor.save();
                    }
                    @Override
                    public void failure(RetrofitError error) {
                        Response response = error.getResponse();
                        Log.w("REST CALL ERROR:", "****************");
                        Log.w("REST CALL STATUS:", "" + response.getStatus());
                        Log.w("REST CALL REASON:", response.getReason());
                    }
                }
        );
    }

    public static BgReadingInterface bgReadingInterface() {
        RestAdapter adapter = adapterBuilder().build();
            BgReadingInterface bgReadingInterface =
                adapter.create(BgReadingInterface.class);
        return bgReadingInterface;
    }

    public static SensorInterface sensorInterface() {

        RestAdapter adapter = adapterBuilder().build();
        SensorInterface sensorInterface =
                adapter.create(SensorInterface.class);
        return sensorInterface;
    }

    public static CalibrationInterface calibrationInterface() {
        RestAdapter adapter = adapterBuilder().build();
        CalibrationInterface calibrationInterface =
                adapter.create(CalibrationInterface.class);
        return calibrationInterface;
    }

    public static RestAdapter.Builder adapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setEndpoint(baseUrl)
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(requestInterceptor());
        return adapterBuilder;
    }

    public static RequestInterceptor requestInterceptor(){
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            User currentUser = User.currentUser();
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("email", currentUser.email);
                request.addHeader("token", currentUser.token);
            }
        };
        return requestInterceptor;
    }
}
