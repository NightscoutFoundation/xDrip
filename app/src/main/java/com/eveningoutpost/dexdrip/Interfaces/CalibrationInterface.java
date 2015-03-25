package com.eveningoutpost.dexdrip.Interfaces;

import com.eveningoutpost.dexdrip.Models.Calibration;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by stephenblack on 11/7/14.
 */
public interface CalibrationInterface {

    @POST("/api/v1/users/{user_uuid}/calibrations/new")
    void createCalibration(@Path("user_uuid") String user_uuid, @Body Calibration calibration, Callback callback);
}
