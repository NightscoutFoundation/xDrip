package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Unit tests for {@link NightscoutCallback} population routing.
 * <p>
 * Verifies that the Consumer populator is called instead of {@link Session#populate(Object)}
 * when provided, and that the legacy path is preserved when no populator is given.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutCallbackTest extends RobolectricTestWithConfig {

    // ===== Consumer populator ====================================================================

    @Test
    public void onResponse_callsPopulator_whenPopulatorProvided() {
        // :: Setup
        AtomicReference<Object> populatorArg = new AtomicReference<>();
        AtomicBoolean populateCalled = new AtomicBoolean(false);
        Session session = new Session() {
            @Override
            public void populate(Object obj) {
                populateCalled.set(true);
            }
        };
        NightscoutCallback<String> callback = new NightscoutCallback<>(
                "test", session, populatorArg::set, null);

        // :: Act
        callback.onResponse(null, Response.success("v3-body"));

        // :: Verify
        assertThat(populatorArg.get()).isEqualTo("v3-body");
        assertThat(populateCalled.get()).isFalse();
    }

    @Test
    public void onResponse_callsSessionPopulate_whenNoPopulator() {
        // :: Setup
        AtomicBoolean populateCalled = new AtomicBoolean(false);
        Session session = new Session() {
            @Override
            public void populate(Object obj) {
                populateCalled.set(true);
            }
        };
        NightscoutCallback<String> callback = new NightscoutCallback<>(
                "test", session, null);

        // :: Act
        callback.onResponse(null, Response.success("body"));

        // :: Verify
        assertThat(populateCalled.get()).isTrue();
    }

    @Test
    public void onResponse_doesNotCallPopulator_whenResponseUnsuccessful() {
        // :: Setup
        AtomicBoolean populatorCalled = new AtomicBoolean(false);
        Session session = new Session();
        NightscoutCallback<String> callback = new NightscoutCallback<>(
                "test", session, body -> populatorCalled.set(true), null);

        // :: Act
        callback.onResponse(null, Response.error(401,
                ResponseBody.create(MediaType.parse("text/plain"), "")));

        // :: Verify
        assertThat(populatorCalled.get()).isFalse();
    }
}
