package com.eveningoutpost.dexdrip.cgm.webfollow;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;

import lombok.val;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * JamOrHam
 * Web follower network abstraction based on config
 */

public class ResponseGetterImpl implements ResponseGetter {

    private static final String TAG = "WebFollower";
    private final OkHttpClient client;
    {
        val builder = new OkHttpClient.Builder();
        if (Cpref.getB("UP")) {
            val proxy = new Proxy(Cpref.getB("HP") ? Proxy.Type.HTTP : Proxy.Type.SOCKS,
                    new InetSocketAddress(Cpref.get("HA"), Integer.parseInt(Cpref.get("HP"))));
            builder.proxy(proxy);
            val proxyUserName = Cpref.get("PU");
            if (!JoH.emptyString(proxyUserName)) {
                UserError.Log.d(TAG, "Using proxy auth");
                val proxyPassword = Cpref.get("PP");
                final Authenticator proxyAuthenticator = (route, response) -> {
                    val credential = Credentials.basic(proxyUserName, proxyPassword);
                    if (response.request().header("Proxy-Authorization") != null) {
                        return null; // Failed to auth
                    }
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };
                builder.proxyAuthenticator(proxyAuthenticator);
            }
        }
        client = builder.build();
    }

    @Override
    public String get(final Config c) {
        val builder = new Request.Builder();
        builder.url(c.url + ((c.query != null) ? "?" + c.query : ""));
        builder.header("User-Agent", c.agent);
        builder.header("version", c.version);
        builder.header("product", c.product);
        if (c.authorization != null) {
            builder.header("Authorization", c.authorization);
        }
        if (c.body != null) {
            builder.post(c.body);
        }
        if (c.contentType != null) {
            builder.header("Content-Type", c.contentType);
        }

        val request = builder.build();
        UserError.Log.d(TAG, "REQUEST URL: " + request.url());
        for (int retry = 0; retry < 3; retry++) {
            try {
                val response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    val res = response.body().string();
                    UserError.Log.d(TAG, "RES: " + res);
                    return res;
                } else {
                    throw new RuntimeException("Got failure response code: " + response.code() + "\n" + (response.body() != null ? response.body().string() : ""));
                }
            } catch (IOException | NullPointerException exception) {
                val msg = "Error: " + exception + " " + c.url;
                UserError.Log.d(TAG, msg);
                if (exception instanceof SocketTimeoutException) {
                    UserError.Log.d(TAG, "Retry due to timeout: " + retry);
                    JoH.threadSleep(5000);
                    continue;
                }
                break;
            }
        }
        return null;
    }
}
