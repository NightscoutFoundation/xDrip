package com.eveningoutpost.dexdrip.watch.thinjam.io;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class GetURLTest extends RobolectricTestWithConfig {

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
    public void getURL_returnsBodyOnSuccess() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("test-response"));

        // :: Act
        String result = GetURL.getURL(server.url("/test").toString());

        // :: Verify
        assertThat(result).isEqualTo("test-response");
    }

    @Test
    public void getURL_sendsCorrectHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));

        // :: Act
        GetURL.getURL(server.url("/test").toString());
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Mozilla/5.0");
        assertThat(recorded.getHeader("Connection")).isEqualTo("close");
    }

    @Test
    public void getURL_returnsNullOnFailure() {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500));

        // :: Act
        String result = GetURL.getURL(server.url("/fail").toString());

        // :: Verify
        assertThat(result).isNull();
    }

    @Test
    public void getURLbytes_returnsBodyBytesOnSuccess() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("byte-data"));

        // :: Act
        byte[] result = GetURL.getURLbytes(server.url("/bytes").toString());

        // :: Verify
        assertThat(result).isEqualTo("byte-data".getBytes());
    }
}
