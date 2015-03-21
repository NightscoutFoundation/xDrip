package com.eveningoutpost.dexdrip.ShareModels;

import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.QueryMap;

/**
 * Created by stephenblack on 3/16/15.
 */
public interface DexcomShareInterface {
    @POST("/Publisher/ReadPublisherLatestGlucoseValues")
    ShareGlucose[] getShareBg(@QueryMap Map<String, String> options);

    @POST("/General/LoginPublisherAccountByName")
    void getSessionId(@Body ShareAuthenticationBody body, Callback<Response> callback);
    //Since this seems to respond with a string we need a callback that will parse the response body
    //new String(((TypedByteArray) response.getBody()).getBytes());

    @POST("/Publisher/IsRemoteMonitoringSessionActive")
    void checkSessionActive(@QueryMap Map<String, String> options, Callback<Response> callback);
    // needs ?sessionId={YourSessionId}
    // returns true or false

    @POST("/Publisher/PostReceiverEgvRecords")
    void uploadBGRecords(@QueryMap Map<String, String> options, @Body ShareUploadPayload payload, Callback<Response> callback);
    // needs ?sessionId={YourSessionId}
    // body ShareUploadPayload
    // status code
}
