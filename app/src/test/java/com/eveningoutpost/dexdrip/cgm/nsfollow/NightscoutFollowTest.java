package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.eveningoutpost.dexdrip.utils.framework.RetrofitService.UNRELIABLE_INTEGER_FACTORY;

import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class NightscoutFollowTest {

    @Test
    public void parseJSON() {
        String json1 = "{ \"_id\":\"5f3ae87305b00f042915a948\", \"device\":\"xDrip-DexcomG5 G5 Native::BlueJay\", \"date\":1597696056075, \"dateString\":\"2020-08-17T20:27:36.075Z\", \"sgv\":244, \"delta\":-5.01, \"direction\":\"FortyFiveDown\", \"type\":\"sgv\", \"filtered\":0, \"unfiltered\":0, \"rssi\":100, \"noise\":1, \"sysTime\":\"2020-08-17T20:27:36.075Z\", \"utcOffset\":120 }";
        String jsonStr1 = "{ \"_id\":\"5f3ae87305b00f042915a948\", \"device\":\"xDrip-DexcomG5 G5 Native::BlueJay\", \"date\":1597696056075, \"dateString\":\"2020-08-17T20:27:36.075Z\", \"sgv\":244, \"delta\":-5.01, \"direction\":\"FortyFiveDown\", \"type\":\"sgv\", \"filtered\":0, \"unfiltered\":0, \"rssi\":100, \"noise\":\"1\", \"sysTime\":\"2020-08-17T20:27:36.075Z\", \"utcOffset\":120 }";
        String jsonNull = "{ \"_id\":\"5f399b2d8287432672f2856f\", \"device\":\"bubble\", \"date\":1597610794217, \"dateString\":\"2020-08-16T20:46:34.217Z\", \"sgv\":291, \"delta\":900000, \"direction\":\"SingleUp\", \"type\":\"sgv\", \"filtered\":291000, \"unfiltered\":291000, \"rssi\":100, \"noise\":null, \"sysTime\":\"2020-08-16T20:46:34.217Z\", \"utcOffset\":60 }";
        String jsonNullStr = "{ \"_id\":\"5f399b2d8287432672f2856f\", \"device\":\"bubble\", \"date\":1597610794217, \"dateString\":\"2020-08-16T20:46:34.217Z\", \"sgv\":291, \"delta\":900000, \"direction\":\"SingleUp\", \"type\":\"sgv\", \"filtered\":291000, \"unfiltered\":291000, \"rssi\":100, \"noise\":\"null\", \"sysTime\":\"2020-08-16T20:46:34.217Z\", \"utcOffset\":60 }";
        String jsonNoNoise = "{ \"_id\":\"5f399b2d8287432672f2856f\", \"device\":\"bubble\", \"date\":1597610794217, \"dateString\":\"2020-08-16T20:46:34.217Z\", \"sgv\":291, \"delta\":900000, \"direction\":\"SingleUp\", \"type\":\"sgv\", \"filtered\":291000, \"unfiltered\":291000, \"rssi\":100, \"sysTime\":\"2020-08-16T20:46:34.217Z\", \"utcOffset\":60 }";
        final Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(UNRELIABLE_INTEGER_FACTORY)
                .create();
        Entry entry = gson.fromJson(json1, Entry.class);
        Assert.assertThat(String.format("Noise value is: %d%n", entry.noise), entry.noise, is(equalTo(1)));
        entry = gson.fromJson(jsonStr1, Entry.class);
        Assert.assertThat(String.format("Noise value is: %d%n", entry.noise), entry.noise, is(equalTo(1)));
        entry = gson.fromJson(jsonNull, Entry.class);
        Assert.assertThat(String.format("Noise value is: %d%n", entry.noise), entry.noise, is(equalTo(0)));
        entry = gson.fromJson(jsonNullStr, Entry.class);
        Assert.assertThat(String.format("Noise value is: %d%n", entry.noise), entry.noise, is(equalTo(0)));
        entry = gson.fromJson(jsonNoNoise, Entry.class);
        Assert.assertThat(String.format("Noise value is: %d%n", entry.noise), entry.noise, is(equalTo(0)));
//        System.out.format("Noise value is: %d%n", entry.noise);
    }
}
