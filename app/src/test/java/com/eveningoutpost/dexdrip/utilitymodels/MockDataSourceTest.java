package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * Created by jamorham on 10/10/2017.
 */

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.services.TransmitterRawData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MockDataSourceTest extends RobolectricTestWithConfig {

    static final int RAW_LOWER_BOUND = 50000;
    static final int RAW_UPPER_BOUND = 150000;


    private static void log(String msg) {
        System.out.println(msg);
    }

    @Test
    public void test_getFakeWifiData() {
        final String str = MockDataSource.getFakeWifiData();
        log("Mock Data: " + str);

        assertThat("Data not null", str != null, is(true));
        assertThat("Marker found", str.contains("RelativeTime"), is(true));

        final Gson gson = new GsonBuilder().create();
        final TransmitterRawData trd = gson.fromJson(str, TransmitterRawData.class);
        log(trd.toTableString());

        assertThat("Sane Raw", trd.getRawValue() < RAW_UPPER_BOUND && trd.getRawValue() > RAW_LOWER_BOUND, is(true));
        assertThat("Sane Filtered", trd.getFilteredValue() < RAW_UPPER_BOUND && trd.getFilteredValue() > RAW_LOWER_BOUND, is(true));

    }
}