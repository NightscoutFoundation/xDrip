package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.junit.Test;

import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;

public class TreatmentsSyncTest extends RobolectricTestWithConfig {

    public class TreatmentsCompat {
        @Expose
        public long timestamp;
        @Expose
        public String eventType;
        @Expose
        public String enteredBy;
        @Expose
        public String notes;
        @Expose
        public String uuid;
        @Expose
        public double carbs;
        @Expose
        public double insulin;
        @Expose
        public String created_at;
    }

    // if this test fails then compatibility with previous xDrip versions is likely broken
    @Test
    public void syncCompatibilityTest() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(55, 2, time);

        // :: Read
        Treatments lastTreatment = Treatments.last();
        lastTreatment.notes = "Hello World";

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(55.0);
        assertThat(lastTreatment.insulin).isEqualTo(2.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith(Treatments.XDRIP_TAG);

        final String json = lastTreatment.toJSON();


        //System.out.println(json);

        assertThat(json).isNotEmpty();
        assertThat(json.length()).isLessThan(256);
        final TreatmentsCompat compat = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, TreatmentsCompat.class);
        assertThat(compat.timestamp).isEqualTo(lastTreatment.timestamp);
        assertThat(compat.enteredBy).isEqualTo(lastTreatment.enteredBy);
        assertThat(compat.notes).isEqualTo(lastTreatment.notes);
        assertThat(compat.uuid).isEqualTo(lastTreatment.uuid);
        assertThat(compat.carbs).isEqualTo(lastTreatment.carbs);
        assertThat(compat.insulin).isEqualTo(lastTreatment.insulin);


    }

}
