package com.eveningoutpost.dexdrip;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Created by stephenblack on 11/10/14.
 */
public interface UserInterface {

    @POST("/api/v1/sessions/new")
    void authenticate(@Body User user, Callback callback);

}
