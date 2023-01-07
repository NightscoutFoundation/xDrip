package com.eveningoutpost.dexdrip.profileeditor;

import static com.eveningoutpost.dexdrip.Models.JoH.defaultGsonInstance;
import static com.eveningoutpost.dexdrip.profileeditor.BasalProfile.consolidate;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;


public class BasalProfileTest {

    @Test
    public void consolidateTest() {

        final List<Float> ma = new LinkedList<>();

        for (int i = 0; i < 1440; i++) {
            int vi = i / 60;
            ma.add((float) vi);
        }

        assertWithMessage("null means null").that(consolidate(null, 100)).isNull();
        assertWithMessage("max means null").that(defaultGsonInstance().toJson(consolidate(ma, ma.size()))).isEqualTo(defaultGsonInstance().toJson(ma));
        assertWithMessage("48 means 48").that(defaultGsonInstance().toJson(consolidate(ma, 48))).isEqualTo("[0.0,0.0,1.0,1.0,2.0,2.0,3.0,3.0,4.0,4.0,5.0,5.0,6.0,6.0,7.0,7.0,8.0,8.0,9.0,9.0,10.0,10.0,11.0,11.0,12.0,12.0,13.0,13.0,14.0,14.0,15.0,15.0,16.0,16.0,17.0,17.0,18.0,18.0,19.0,19.0,20.0,20.0,21.0,21.0,22.0,22.0,23.0,23.0]");
        assertWithMessage("24 means 24").that(defaultGsonInstance().toJson(consolidate(ma, 24))).isEqualTo("[0.0,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0,11.0,12.0,13.0,14.0,15.0,16.0,17.0,18.0,19.0,20.0,21.0,22.0,23.0]");
        assertWithMessage("12 means 24").that(defaultGsonInstance().toJson(consolidate(ma, 12))).isEqualTo("[0.0,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0,11.0,12.0,13.0,14.0,15.0,16.0,17.0,18.0,19.0,20.0,21.0,22.0,23.0]");

        // mess things up a bit
        ma.set(60, 0f);
        assertWithMessage("24 means 1440").that(consolidate(ma, 24).size()).isEqualTo(1440);
        ma.set(61, 0f);
        assertWithMessage("24 means 720").that(consolidate(ma, 24).size()).isEqualTo(720);
        ma.set(62, 0f);
        assertWithMessage("24 means 480").that(consolidate(ma, 24).size()).isEqualTo(480);
        ma.set(63, 0f);
        assertWithMessage("24 means 360").that(consolidate(ma, 24).size()).isEqualTo(360);

    }
}