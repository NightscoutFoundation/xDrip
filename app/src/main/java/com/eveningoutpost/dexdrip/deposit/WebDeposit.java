package com.eveningoutpost.dexdrip.deposit;


import android.os.Build;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import android.support.annotation.RequiresApi;
import lombok.val;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import static com.eveningoutpost.dexdrip.UtilityModels.OkHttpWrapper.enableTls12OnPreLollipop;
import static com.eveningoutpost.dexdrip.deposit.ReadingsToJson.getJsonForStartEnd;

/**
 * jamorham
 *
 * Web Deposit
 *
 * Upload chunks of data to a web service. Currently used for diagnostics
 */

@RequiresApi(api = Build.VERSION_CODES.N)
public class WebDeposit {

    protected static final String TAG = WebDeposit.class.getSimpleName();
    private static final boolean D = false;

    private static volatile Retrofit retrofit;
    private static final String WEB_DEPOSIT_POSITION = "WEB_DEPOSIT_POSITION";
    private static final String WEB_DEPOSIT_SERIAL = "web_deposit_serial";
    private static final String WEB_DEPOSIT_URL = "web_deposit_url";

    private static PowerManager.WakeLock wl;

    public interface IWebDeposit {
        @Headers({
                "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME,
                "X-Deposit-Client-Name: " + BuildConfig.APPLICATION_ID,
                "X-Deposit-Client-Version: " + BuildConfig.VERSION_NAME,
        })

        @POST("/create/{sn}")
        Call<DepositReply1> upload(@Header("Authorization") String token, @Path("sn") String id, @Body RequestBody body);
    }


    static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            if (D) {
                httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            final OkHttpClient client = enableTls12OnPreLollipop(new OkHttpClient.Builder())
                    .addInterceptor(httpLoggingInterceptor)
                    .addInterceptor(new InfoInterceptor(TAG))
                    .readTimeout(600, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    //          .addInterceptor(new GzipRequestInterceptor())
                    .build();

            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(getUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }


    static String getUrl() {
        return Pref.getString(WEB_DEPOSIT_URL, "Invalid");
    }

    static String getSerialInfo() {
        return Pref.getString(WEB_DEPOSIT_SERIAL, "Invalid");
    }

    static long getNextTime() {
        return Math.max(JoH.tsl() - Constants.MONTH_IN_MS * 4, Pref.getLong(WEB_DEPOSIT_POSITION, 0));
    }

    static void setNextTime(final long time) {
        Pref.setLong(WEB_DEPOSIT_POSITION, time);
    }


    static void doUpload(final long start, final long end, final F successCallback, final F failCallback, final F statusCallBack) {
        UserError.Log.d(TAG, "doUpload called");

        statusCallBack.apply("Getting data");
        val data = getJsonForStartEnd(start, end);

        if (data.length() < 100) {
            failCallback.apply("Not enough data to deposit");
            return;
        }

        retrofit = null; // Always remake retrofit so we can load new base url
        val service = getRetrofitInstance().create(IWebDeposit.class);
        val body = RequestBody.create(MediaType.parse("application/json"), data.toString().getBytes(StandardCharsets.UTF_8));
        val call = service.upload("no auth token", getSerialInfo(), body);

        statusCallBack.apply("Uploading data, records: " + data.length());
        call.enqueue(new DepositCallback<DepositReply1>(TAG, successCallback)
                .setOnFailure(failCallback)
                .setOnStatus(statusCallBack));
    }

}
