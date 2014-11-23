package com.eveningoutpost.dexdrip;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by stephenblack on 11/7/14.
 */
public interface ComparisonInterface {

    @POST("/api/v1/users/{user_uuid}/comparison/new")
    void createComparison(@Path("user_uuid") String user_uuid, @Body Comparison comparison, Callback callback);
}