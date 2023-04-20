package com.eveningoutpost.dexdrip.cgm.webfollow;

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.Disabled;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.setDexCollectionType;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * JamOrHam
 * Community script template loader
 */

public class Template {

    private static final String TAG = "WebFollow" + Template.class.getSimpleName();
    private static final String SCHEME = "https://";
    private static final String PREFIX = "community-script";
    private static final String SUFFIX = "net";
    private static final String DOT = ".";

    private static byte[] getData() {
        val url = getUrl();
        if (url == null) return null;
        val client = new OkHttpClient();
        val builder = new Request.Builder().url(getUrl());
        val request = builder.build();
        UserError.Log.d(TAG, "T REQUEST URL: " + request.url());
        try {
            val response = client.newCall(request).execute();
            if (response.code() == 410) {
                UserError.Log.d(TAG, "Shutdown requested");
                setDexCollectionType(Disabled);
            }
            if (response.isSuccessful()) {
                return response.body().bytes();
            } else {
                throw new RuntimeException("Got script failure response code: " + response.code() + "\n" + (response.body() != null ? response.body().string() : ""));
            }
        } catch (IOException e) {
            UserError.Log.e(TAG, "Exception getting template: " + e);
            e.printStackTrace();
        }
        return null;
    }

    public static MContext get() {
        try {
            val bytes = getData();
            if (bytes == null) return null;
            val index = Bytes.indexOf(bytes, new byte[4]);
            val b1 = JoH.splitBytes(bytes, 0, index);
            val b2 = JoH.splitBytes(bytes, index + 4, (bytes.length - b1.length) - 8);
            val ok = Verify.verify(b1, b2);
            if (ok) {
                return MContext.fromJson(new String(b1, StandardCharsets.UTF_8));
            } else {
                UserError.Log.e(TAG, "Invalid verification");
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception in get() " + e);
        }
        return null;
    }

    private static String getUrl() {
        val ck = Cpref.get("CK");
        if (JoH.emptyString(ck)) return null;
        if (!ck.contains(".")) {
            return SCHEME + PREFIX + DOT + ck + DOT + SUFFIX;
        }
        return ck;
    }
}
