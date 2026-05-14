package com.eveningoutpost.dexdrip.models;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * @author Asbjørn Aarrestad
 */
public class GlucoseDataTest {

    // -- mg/dL formatting: integer string --

    @Test
    public void glucose_mgdl_returnsWholeNumber() {
        // :: Verify
        assertThat(GlucoseData.glucose(120, false)).isEqualTo("120");
        assertThat(GlucoseData.glucose(80, false)).isEqualTo("80");
    }

    // -- mmol/L formatting: one decimal place --

    @Test
    public void glucose_mmol_returnsOneDecimal() {
        // :: Act — 120 mg/dL ~ 6.7 mmol/L
        double value = Double.parseDouble(GlucoseData.glucose(120, true));

        // :: Verify
        assertWithMessage("120 mg/dL in mmol/L")
                .that(value).isWithin(0.1).of(6.7);
    }

    @Test
    public void glucose_mmol_lowValue() {
        // :: Act — 54 mg/dL ~ 3.0 mmol/L
        double value = Double.parseDouble(GlucoseData.glucose(54, true));

        // :: Verify
        assertWithMessage("54 mg/dL in mmol/L")
                .that(value).isWithin(0.1).of(3.0);
    }

    @Test
    public void glucose_mmol_highValue() {
        // :: Act — 300 mg/dL ~ 16.7 mmol/L
        double value = Double.parseDouble(GlucoseData.glucose(300, true));

        // :: Verify
        assertWithMessage("300 mg/dL in mmol/L")
                .that(value).isWithin(0.1).of(16.7);
    }

    // -- Instance method uses glucoseLevel field --

    @Test
    public void instanceGlucose_usesGlucoseLevel() {
        // :: Setup
        GlucoseData gd = new GlucoseData();
        gd.glucoseLevel = 180;

        // :: Verify
        assertThat(gd.glucose(false)).isEqualTo("180");
    }

    // -- Chronological ordering via compareTo --

    @Test
    public void compareTo_ordersChronologically() {
        // :: Setup
        GlucoseData early = new GlucoseData(100, 1000L);
        GlucoseData late = new GlucoseData(100, 2000L);

        // :: Verify
        assertThat(early.compareTo(late)).isLessThan(0);
        assertThat(late.compareTo(early)).isGreaterThan(0);
    }

    @Test
    public void compareTo_sameTime_returnsZero() {
        // :: Setup
        GlucoseData a = new GlucoseData(100, 1000L);
        GlucoseData b = new GlucoseData(200, 1000L);

        // :: Verify
        assertThat(a.compareTo(b)).isEqualTo(0);
    }

    @Test
    public void sorting_producesChronologicalOrder() {
        // :: Setup
        GlucoseData a = new GlucoseData(100, 3000L);
        GlucoseData b = new GlucoseData(100, 1000L);
        GlucoseData c = new GlucoseData(100, 2000L);
        List<GlucoseData> list = Arrays.asList(a, b, c);

        // :: Act
        Collections.sort(list);

        // :: Verify
        assertThat(list.get(0).realDate).isEqualTo(1000L);
        assertThat(list.get(1).realDate).isEqualTo(2000L);
        assertThat(list.get(2).realDate).isEqualTo(3000L);
    }

    // -- Constructor sets fields --

    @Test
    public void constructor_setsRawGlucoseAndTimestamp() {
        // :: Act
        GlucoseData gd = new GlucoseData(250, 5000L);

        // :: Verify
        assertThat(gd.glucoseLevelRaw).isEqualTo(250);
        assertThat(gd.realDate).isEqualTo(5000L);
    }

    // -- Default glucoseLevel is -1 (not set) --

    @Test
    public void defaultGlucoseLevel_isMinusOne() {
        // :: Verify
        assertThat(new GlucoseData().glucoseLevel).isEqualTo(-1);
    }
}
