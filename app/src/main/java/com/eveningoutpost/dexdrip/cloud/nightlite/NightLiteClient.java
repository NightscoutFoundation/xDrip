package com.eveningoutpost.dexdrip.cloud.nightlite;

import static com.eveningoutpost.dexdrip.cloud.nightlite.NightLiteEntry.setEnabled;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;

import android.annotation.SuppressLint;
import android.provider.Settings;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.val;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

// JamOrHam

public class NightLiteClient {

    private static final String TAG = "NightLiteClient";

    private static final OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
            .addInterceptor(new GzipRequestInterceptor())
            .build();

    public static void doUpload() {
        if (!NightLiteEntry.isEnabled()) {
            return;
        }
        if (DexCollectionType.getDexCollectionType() != DexCollectionType.NSFollow) {
            Inevitable.task("NightLiteUpload", 2000, NightLiteClient::doUploadReal);
        } else {
            UserError.Log.e(TAG, "NightLiteUpload skipped as cannot be used with NSFollow collector");
        }
    }

    private static void doUploadReal() {
        if (JoH.pratelimit(TAG, 60 * 4)) {

            val bytes = NightLite.getForHours(12).toByteArray();

            if (bytes.length == 0) {
                UserError.Log.e(TAG, "No data to upload");
                return;
            }

            val apiString = NightLiteEntry.getApi();
            if (apiString.isEmpty()) {
                UserError.Log.e(TAG, "No nightlite url set - disabling");
                setEnabled(false);
                return;
            }

            List<String> pathElements;
            URL url;
            try {
                url = new URL(apiString);
                String path = url.getPath();
                pathElements = new ArrayList<>(Arrays.asList(path.split("/")));
                pathElements.removeIf(String::isEmpty);
                if (pathElements.isEmpty()) {
                    UserError.Log.e(TAG, "Invalid nightlite url: " + apiString);
                    return;
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Error parsing URL: " + e);
                return;
            }

            val query = (ActiveBgAlert.getOnly() != null
                    || pratelimit("nightlite-aged", 3600 * 6)) ? "pager=1" : "";

            try {
                val urlString = url.getProtocol() + "://" + url.getHost() + "/upload?" + query;
                val apiKey = pathElements.get(0);
                UserError.Log.d(TAG, "url string: " + urlString);
                val body = RequestBody.create(MediaType.parse("application/octet-stream"), bytes);

                val request = new Request.Builder()
                        .url(urlString)
                        .post(body)
                        .addHeader("X-API-Key", apiKey)
                        .addHeader("X-Drip", BuildConfig.APPLICATION_ID + "-" + BuildConfig.VERSION_NAME)
                        .addHeader("X-Upload-Id", uploadId())
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        val responseBody = response.body() != null ? response.body().string() : "";
                        UserError.Log.e(TAG, "Unexpected code " + response + " " + responseBody);
                        handleResponse(responseBody);
                    } else {
                        UserError.Log.d(TAG, "Upload successful: " + bytes.length);
                        for (val header : response.headers().names()) {
                            UserError.Log.d(TAG, "Header: " + header + ": " + response.header(header));
                        }
                        try {
                            val responseBody = response.body() != null ? response.body().string() : "";
                            UserError.Log.d(TAG, "Response: " + responseBody);
                            handleResponse(responseBody);
                        } catch (Exception e) {
                            //
                        }
                    }
                } catch (IOException e) {
                    UserError.Log.e(TAG, "Error uploading: " + e);
                }
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Error: " + e);
            }
        } else {
            UserError.Log.d(TAG, "Rate limit exceeded");
        }
    }

    private static void handleResponse(String response) {
        if (response == null) {
            return;
        }
        try {
            val jsonObject = new org.json.JSONObject(response);
            if (jsonObject.has("notification")) {
                val notification = jsonObject.getJSONObject("notification");

                val title = notification.optString("title", "");
                val content = notification.optString("content", "");
                val notificationId = notification.optInt("notificationId", 3487);
                val sound = notification.optBoolean("sound", false);
                val vibrate = notification.optBoolean("vibrate", false);
                val onetime = notification.optBoolean("onetime", false);
                UserError.Log.ueh(TAG, "Notification: " + title + " " + content);
                showNotification(title, content, null, notificationId, sound, vibrate, onetime);
            }

            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error");
                val message = error.optString("message", "");
                UserError.Log.wtf(TAG, "Error: " + message);
            }

            if (jsonObject.has("cease")) {
                val reason = jsonObject.optString("cease", "unknown");
                UserError.Log.wtf(TAG, "Server requested cease, reason: " + reason);
                setEnabled(false);
            }

        } catch (Exception e) {
            UserError.Log.e(TAG, "Error parsing response JSON: " + e);
        }
    }

    @SuppressLint("HardwareIds")
    private static String uploadId() {
        try {
            return Settings.Secure.getString(xdrip.getAppContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        } catch (Exception e) {
            return "none";
        }
    }
}
