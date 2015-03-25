package com.eveningoutpost.dexdrip.Interfaces;

import com.eveningoutpost.dexdrip.Sensor;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by stephenblack on 11/7/14.
 */
public interface SensorInterface {

    @POST("/api/v1/users/{user_uuid}/sensors/new")
    void createSensor(@Path("user_uuid") String user_uuid, @Body Sensor sensor, Callback callback);
}
