package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.google.gson.annotations.Expose;

import org.junit.Test;

import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Characterization tests for {@link BaseMessage#getBody()}.
 *
 * Pins the request body serialization and content-type produced on okhttp 3.12.13,
 * so the okhttp4 migration can be proven behavior-preserving.
 *
 * @author Asbjørn Aarrestad
 */
public class BaseMessageTest extends RobolectricTestWithConfig {

    /**
     * Minimal concrete message so the abstract base can be exercised. Fields use {@code @Expose}
     * because {@code JoH.defaultGsonInstance()} is configured with
     * {@code excludeFieldsWithoutExposeAnnotation()} — without it, Gson emits {@code {}}.
     */
    private static class SampleMessage extends BaseMessage {
        @Expose
        final String name = "abc";
        @Expose
        final int value = 7;
    }

    @Test
    public void getBody_serializesJsonWithCharsetContentType() throws Exception {
        // :: Setup
        final SampleMessage message = new SampleMessage();

        // :: Act
        final RequestBody body = message.getBody();
        final Buffer buffer = new Buffer();
        body.writeTo(buffer);
        final String content = buffer.readUtf8();

        // :: Verify
        assertThat(content).isEqualTo("{\"name\":\"abc\",\"value\":7}");
        assertThat(body.contentType()).isNotNull();
        assertThat(body.contentType().toString()).isEqualTo("application/json; charset=utf-8");
    }
}
