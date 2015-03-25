package com.eveningoutpost.dexdrip.Interfaces;

import com.eveningoutpost.dexdrip.Models.BgReading;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by stephenblack on 11/6/14.
 */
public interface BgReadingInterface {

    @POST("/api/v1/users/{user_uuid}/BgReadings/new")
    void createReading(@Path("user_uuid") String user_uuid, @Body BgReading bgReading, Callback callback);

    @PUT("/api/v1/users/{user_uuid}/BgReading/{bgReading_uuid}")
    void updateReading(@Path("user_uuid") String user_uuid, @Path("bgReading_uuid") String uuid, @Body BgReading bgReading, Callback callback);
}
