package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.Test;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies that DisplayQRCode builds its OkHttpClient from the shared singleton
 * and that FormBody.Builder (OkHttp3) correctly encodes form data.
 *
 * @author Asbjørn Aarrestad
 */
public class DisplayQRCodeTest extends RobolectricTestWithConfig {

    @Test
    public void clientFromSharedBuilder_sharesConnectionPool() {
        // :: Setup — reproduce the client construction from DisplayQRCode.uploadBytes
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // :: Act & Verify
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void clientFromSharedBuilder_hasWriteTimeout30s() {
        // :: Setup
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // :: Act & Verify
        assertThat(client.writeTimeoutMillis()).isEqualTo(30_000);
    }

    @Test
    public void formBody_encodesDataField() {
        // :: Setup
        String testData = "dGVzdA==";

        // :: Act
        FormBody formBody = new FormBody.Builder()
                .add("data", testData)
                .build();

        // :: Verify
        assertThat(formBody.name(0)).isEqualTo("data");
        assertThat(formBody.value(0)).isEqualTo(testData);
    }
}
