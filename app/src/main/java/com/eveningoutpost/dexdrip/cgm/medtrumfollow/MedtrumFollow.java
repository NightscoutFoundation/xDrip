package com.eveningoutpost.dexdrip.cgm.medtrumfollow;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

interface MedtrumFollow {

    String APP_TAG = "v=1.2.70(112);n=eyfo;p=android";

    @Headers({
            "AppTag: " + APP_TAG,
            "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME
    })
    @FormUrlEncoded
    @POST("mobile/ajax/login")
    Call<JsonObject> login(@Header("DevInfo") String deviceInfo,
                           @Field("user_type") String userType,
                           @Field("user_name") String username,
                           @Field("password") String password,
                           @Field("deviceToken") String deviceToken,
                           @Field("platform") String platform,
                           @Field("apptype") String appType,
                           @Field("push_platform") String pushPlatform,
                           @Field("app_version") String appVersion,
                           @Field("device_name") String deviceName,
                           @Field("bundleID") String bundleId);

    @Headers({
            "AppTag: " + APP_TAG,
            "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME
    })
    @GET("mobile/ajax/monitor?flag=monitor_list")
    Call<JsonObject> getMonitor(@Header("DevInfo") String deviceInfo,
                                @Header("Cookie") String cookie);

    @Headers({
            "AppTag: " + APP_TAG,
            "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME
    })
    @GET("mobile/ajax/download")
    Call<JsonObject> getGraph(@Header("DevInfo") String deviceInfo,
                              @Header("Cookie") String cookie,
                              @Query("flag") String flag,
                              @Query("st") String start,
                              @Query("et") String end,
                              @Query("user_name") String username);
}

