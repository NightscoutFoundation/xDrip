package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link Treatments}
 *
 * @author Asbjørn Aarrestad - 2019.07 - asbjorn@aarrestad.com
 */
public class TreatmentsTest extends RobolectricTestWithConfig {

    @Test
    public void createAndReadTreatment() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(55, 2, time);

        // :: Read
        Treatments lastTreatment = Treatments.last();

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(55.0);
        assertThat(lastTreatment.insulin).isEqualTo(2.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith(Treatments.XDRIP_TAG);
    }


    @Test
    public void createManyReadLast() {
        // :: Setup
        long time = Instant.now().getEpochSecond();
        Treatments.create(1, 8, time);
        Treatments.create(2, 7, time);
        Treatments.create(3, 6, time);
        Treatments.create(4, 5, time);
        Treatments.create(5, 4, time);
        Treatments.create(6, 3, time);
        Treatments.create(7, 2, time);
        Treatments.create(8, 1, time);

        // :: Act
        Treatments lastTreatment = Treatments.last();

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(8.0);
        assertThat(lastTreatment.insulin).isEqualTo(1.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith(Treatments.XDRIP_TAG);
    }

    @Test
    public void getLastestNoneXdrip_noneEntered() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(1, 8, time);
        Treatments.create(2, 7, time);
        Treatments.create(3, 6, time);
        Treatments.create(4, 5, time);
        Treatments.create(5, 4, time);
        Treatments.create(6, 3, time);
        Treatments.create(7, 2, time);
        Treatments.create(8, 1, time);

        // :: Read
        Treatments lastTreatment = Treatments.lastNotFromXdrip();

        // :: Verify
        assertThat(lastTreatment).isNull();
    }

    @Test
    public void getLastestNoneXdrip_oneEntered() {
        // :: Create
        long time = Instant.now().getEpochSecond();
        Treatments.create(1, 8, time);
        Treatments.create(2, 7, time);
        Treatments.create(3, 6, time);
        Treatments.create(4, 5, time);
        Treatments.create(5, 4, time);
        Treatments.create(6, 3, time);

        Treatments notFromXdrip = Treatments.create(7, 2, time);
        notFromXdrip.enteredBy = "SomeOtherSource";
        notFromXdrip.save();

        Treatments.create(8, 1, time);

        // :: Read
        Treatments lastTreatment = Treatments.lastNotFromXdrip();

        // :: Verify
        assertThat(lastTreatment.carbs).isEqualTo(7.0);
        assertThat(lastTreatment.insulin).isEqualTo(2.0);
        assertThat(lastTreatment.timestamp).isEqualTo(time);
        assertThat(lastTreatment.enteredBy).startsWith("SomeOtherSource");
    }
}
