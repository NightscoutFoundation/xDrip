package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

public class NightscoutFollowTest {

    @Test
    public void parseJSON() {
        String json = "{ \"_id\":\"5f3ae87305b00f042915a948\", \"device\":\"xDrip-DexcomG5 G5 Native::BlueJay\", \"date\":1597696056075, \"dateString\":\"2020-08-17T20:27:36.075Z\", \"sgv\":244, \"delta\":-5.01, \"direction\":\"FortyFiveDown\", \"type\":\"sgv\", \"filtered\":0, \"unfiltered\":0, \"rssi\":100, \"noise\":1, \"sysTime\":\"2020-08-17T20:27:36.075Z\", \"utcOffset\":120 }";
        String jsonNull = "{ \"_id\":\"5f399b2d8287432672f2856f\", \"device\":\"bubble\", \"date\":1597610794217, \"dateString\":\"2020-08-16T20:46:34.217Z\", \"sgv\":291, \"delta\":900000, \"direction\":\"SingleUp\", \"type\":\"sgv\", \"filtered\":291000, \"unfiltered\":291000, \"rssi\":100, \"noise\":\"null\", \"sysTime\":\"2020-08-16T20:46:34.217Z\", \"utcOffset\":60 }";
        final Gson gson = new GsonBuilder().create();
        final Entry entry = gson.fromJson(json, Entry.class);
        System.out.println(entry.toString());
    }
}
