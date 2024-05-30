package com.eveningoutpost.dexdrip.utilitymodels;

import android.annotation.SuppressLint;
import android.os.Build;

import com.eveningoutpost.dexdrip.models.UserError;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

public class OkHttpWrapper {

    /*
     * Aggressive server cipher suite restrictions mean we may run out of compatible ciphers
     * unless we can enable TLS 1.2 on older android devices.
     */

    @SuppressLint("ObsoleteSdkInt")
    public static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));

                final ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .build();
                final List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                client.connectionSpecs(specs);
            } catch (Exception e) {
                UserError.Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", e);
            }
        }

        return client;
    }

}
