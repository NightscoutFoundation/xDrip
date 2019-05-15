package com.eveningoutpost.dexdrip.cgm.sharefollow;

import java.util.List;
import java.util.Map;

import retrofit.http.Headers;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.USER_AGENT;

// jamorham

public interface DexcomShareInterface {

    // logging in and getting session id
    @POST("General/LoginPublisherAccountByName")
    @Headers({USER_AGENT, "Content-Type: application/json", "Accept: application/json"})
    Call<String> getSessionId(@Body ShareAuthenticationBody body);


    // getting recent data
    @POST("Publisher/ReadPublisherLatestGlucoseValues")
    @Headers({USER_AGENT, "Content-Length: 0", "Content-Type: application/json", "Accept: application/json"})
    Call<List<ShareGlucoseRecord>> getGlucoseRecords(@QueryMap Map<String, String> options);

}

