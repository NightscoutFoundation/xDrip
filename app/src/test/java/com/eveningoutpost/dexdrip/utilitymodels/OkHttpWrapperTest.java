package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjorn Aarrestad
 */
public class OkHttpWrapperTest extends RobolectricTestWithConfig {

    @Test
    public void getClient_returnsSameInstance() {
        // :: Setup
        // (no setup needed)

        // :: Act
        OkHttpClient first = OkHttpWrapper.getClient();
        OkHttpClient second = OkHttpWrapper.getClient();

        // :: Verify
        assertThat(first).isSameInstanceAs(second);
    }

    @Test
    public void getClient_newBuilder_createsDifferentClient() {
        // :: Setup
        OkHttpClient shared = OkHttpWrapper.getClient();

        // :: Act
        OkHttpClient custom = shared.newBuilder().build();

        // :: Verify
        assertThat(custom).isNotSameInstanceAs(shared);
        assertThat(custom.connectionPool()).isSameInstanceAs(shared.connectionPool());
    }
}
