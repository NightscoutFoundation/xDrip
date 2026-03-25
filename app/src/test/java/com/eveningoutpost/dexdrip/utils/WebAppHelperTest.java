package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class WebAppHelperTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void client_usesSharedConnectionPool() throws Exception {
        // :: Setup
        WebAppHelper helper = new WebAppHelper(null);
        Field clientField = WebAppHelper.class.getDeclaredField("client");
        clientField.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) clientField.get(helper);

        // :: Verify
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void client_hasWriteTimeout30s() throws Exception {
        // :: Setup
        WebAppHelper helper = new WebAppHelper(null);
        Field clientField = WebAppHelper.class.getDeclaredField("client");
        clientField.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) clientField.get(helper);

        // :: Verify
        assertThat(client.writeTimeoutMillis()).isEqualTo(30_000);
    }

    @Test
    public void doInBackground_sendsCorrectHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("hello"));
        WebAppHelper helper = new WebAppHelper(null);

        // :: Act
        helper.doInBackground(server.url("/test").toString());
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Mozilla/5.0 (jamorham)");
        assertThat(recorded.getHeader("Connection")).isEqualTo("close");
    }

    @Test
    public void doInBackground_successfulResponse_returnsBodyLength() throws Exception {
        // :: Setup
        byte[] expected = new byte[]{0x01, 0x02, 0x03};
        server.enqueue(new MockResponse().setBody(new okio.Buffer().write(expected)));
        WebAppHelper helper = new WebAppHelper(null);

        // :: Act
        Integer result = helper.doInBackground(server.url("/test").toString());

        // :: Verify
        assertThat(result).isEqualTo(3);
    }

    @Test
    public void doInBackground_failedResponse_returnsZero() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500));
        WebAppHelper helper = new WebAppHelper(null);

        // :: Act
        Integer result = helper.doInBackground(server.url("/test").toString());

        // :: Verify
        assertThat(result).isEqualTo(0);
    }
}
