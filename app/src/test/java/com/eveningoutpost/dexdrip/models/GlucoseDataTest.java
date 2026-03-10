package com.eveningoutpost.dexdrip.models;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class GlucoseDataTest {

    // -- mg/dL formatting: integer string --

    @Test
    public void glucose_mgdl_returnsWholeNumber() {
        assertThat(GlucoseData.glucose(120, false)).isEqualTo("120");
        assertThat(GlucoseData.glucose(80, false)).isEqualTo("80");
    }

    // -- mmol/L formatting: one decimal place --

    @Test
    public void glucose_mmol_returnsOneDecimal() {
        // 120 mg/dL ~ 6.7 mmol/L
        String result = GlucoseData.glucose(120, true);
        double value = Double.parseDouble(result);
        assertWithMessage("120 mg/dL in mmol/L")
                .that(value).isWithin(0.1).of(6.7);
    }

    @Test
    public void glucose_mmol_lowValue() {
        // 54 mg/dL ~ 3.0 mmol/L
        String result = GlucoseData.glucose(54, true);
        double value = Double.parseDouble(result);
        assertWithMessage("54 mg/dL in mmol/L")
                .that(value).isWithin(0.1).of(3.0);
    }

    @Test
    public void glucose_mmol_highValue() {
        // 300 mg/dL ~ 16.7 mmol/L
        String result = GlucoseData.glucose(300, true);
        double value = Double.parseDouble(result);
        assertWithMessage("300 mg/dL in mmol/L")
                .that(value).isWithin(0.1).of(16.7);
    }

    // -- Instance method uses glucoseLevel field --

    @Test
    public void instanceGlucose_usesGlucoseLevel() {
        GlucoseData gd = new GlucoseData();
        gd.glucoseLevel = 180;
        assertThat(gd.glucose(false)).isEqualTo("180");
    }

    // -- Chronological ordering via compareTo --

    @Test
    public void compareTo_ordersChronologically() {
        GlucoseData early = new GlucoseData(100, 1000L);
        GlucoseData late = new GlucoseData(100, 2000L);
        assertThat(early.compareTo(late)).isLessThan(0);
        assertThat(late.compareTo(early)).isGreaterThan(0);
    }

    @Test
    public void compareTo_sameTime_returnsZero() {
        GlucoseData a = new GlucoseData(100, 1000L);
        GlucoseData b = new GlucoseData(200, 1000L);
        assertThat(a.compareTo(b)).isEqualTo(0);
    }

    @Test
    public void sorting_producesChronologicalOrder() {
        GlucoseData a = new GlucoseData(100, 3000L);
        GlucoseData b = new GlucoseData(100, 1000L);
        GlucoseData c = new GlucoseData(100, 2000L);
        List<GlucoseData> list = Arrays.asList(a, b, c);
        Collections.sort(list);
        assertThat(list.get(0).realDate).isEqualTo(1000L);
        assertThat(list.get(1).realDate).isEqualTo(2000L);
        assertThat(list.get(2).realDate).isEqualTo(3000L);
    }

    // -- Constructor sets fields --

    @Test
    public void constructor_setsRawGlucoseAndTimestamp() {
        GlucoseData gd = new GlucoseData(250, 5000L);
        assertThat(gd.glucoseLevelRaw).isEqualTo(250);
        assertThat(gd.realDate).isEqualTo(5000L);
    }

    // -- Default glucoseLevel is -1 (not set) --

    @Test
    public void defaultGlucoseLevel_isMinusOne() {
        GlucoseData gd = new GlucoseData();
        assertThat(gd.glucoseLevel).isEqualTo(-1);
    }
}
