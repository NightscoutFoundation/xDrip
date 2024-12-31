package com.eveningoutpost.dexdrip.cloud.jamcm;

import static com.eveningoutpost.dexdrip.GcmActivity.myIdentity;
import static com.eveningoutpost.dexdrip.Home.get_master;
import static com.eveningoutpost.dexdrip.cloud.jamcm.JamCm.getId;
import static com.eveningoutpost.dexdrip.cloud.jamcm.Upstream.getLegacyServerAddress;
import static com.eveningoutpost.dexdrip.models.JoH.isOldVersion;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.xdrip.getAppContext;

import android.util.Log;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import java.io.IOException;

import lombok.val;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * JamOrHam
 * <p>
 * GCM workarounds
 */
public class Legacy {

    private static final String TAG = "JamCm";
    private static final String LAST_SUCCESS = TAG + "_LAST_SUCCESS";
    private static final String PROTOCOL = "https://";
    private static final String ENDPOINT = "legacy";

    private static void getRequestWithParams(String url, String param1, String param2, String param3, String param4, String param5) {
        if (param1 == null || param2 == null || param3 == null || param4 == null || param5 == null) {
            Log.e(TAG, "At least one parameter null");
            return;
        }
        try {
            val client = new OkHttpClient();
            val urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("param1", param1);
            urlBuilder.addQueryParameter("param2", param2);
            urlBuilder.addQueryParameter("param3", param3);
            urlBuilder.addQueryParameter("param4", param4);
            urlBuilder.addQueryParameter("param5", param5);
            val finalUrl = urlBuilder.build().toString();

            val request = new Request.Builder()
                    .url(finalUrl)
                    .addHeader("User-Agent", BuildConfig.APPLICATION_ID + "-" + BuildConfig.VERSION_NAME)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    // e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String myResponse = response.body().string();
                        Log.d(TAG, myResponse);
                        PersistentStore.setLong(LAST_SUCCESS, tsl());
                    } else {
                        Log.d(TAG, "Request not successful");
                    }
                }
            });
        } catch (Exception e) {
            UserError.Log.w(TAG, "Error: " + e);

        }
    }

    public static void migration(String value) {
        if (JoH.ratelimit("legacy-general", 60)) {
            if (JoH.msSince(PersistentStore.getLong(LAST_SUCCESS)) > Constants.HOUR_IN_MS * 12) {
                getRequestWithParams(PROTOCOL + getLegacyServerAddress() + "/" + ENDPOINT, value, getId(), myIdentity(), get_master() + "", isOldVersion(getAppContext()) + "");
            }
        }
    }
}
