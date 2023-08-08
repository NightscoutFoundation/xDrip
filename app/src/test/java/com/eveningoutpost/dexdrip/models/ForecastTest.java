package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.google.common.truth.Truth;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the forecast code.
 * <p>
 * Many of these tests also work as examples of usage of the Forecast function.
 * <p>
 * Created by Asbjorn Aarrestad on 7th January 2018.
 */
public class ForecastTest extends RobolectricTestWithConfig {

    @Test
    public void polyTrendLine_SimpleForecast() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(1);
        trendLine.setValues(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();

        double prediction = trendLine.predict(10);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(10);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test
    public void polyTrendLine_SimpleForecast_SecondDegree() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);
        trendLine.setValues(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(10);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(10);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test
    public void polyTrendLine_AdvancedForecast_SecondDegree() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);
        // Data set as y=x^2
        trendLine.setValues(new double[]{1, 4, 9, 16}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();

        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(25);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setValuesWithDifferentLengthXandY() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);

        // :: Act
        trendLine.setValues(new double[]{1, 2, 3}, new double[]{1, 2});
    }

    @Test
    public void toPrimitiveFromList_nullInput() {
        // :: Act
        double[] result = Forecast.OLSTrendLine.toPrimitiveFromList(null);

        // :: Verify
        Truth.assertThat(result).isNull();
    }

    @Test
    public void toPrimitiveFromList_emptyInput() {
        // :: Act
        double[] result = Forecast.OLSTrendLine.toPrimitiveFromList(Collections.emptyList());

        // :: Verify
        Truth.assertThat(result).isEmpty();
    }

    @Test
    public void toPrimitiveFromList_ListWithElements() {
        // :: Setup
        List<Double> values = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            values.add((double) i);
        }

        // :: Act
        double[] result = Forecast.OLSTrendLine.toPrimitiveFromList(values);

        // :: Verify
        Truth.assertThat(result).hasLength(5);
        Truth.assertThat(result)
                .usingTolerance(0.001)
                .containsExactly(new double[]{1, 2, 3, 4, 5})
                .inOrder();
    }

    @Test
    public void expTrendLine_simpleTest() {
        // :: Setup
        Forecast.ExpTrendLine trendLine = new Forecast.ExpTrendLine();
        // Data set as y=2^x
        trendLine.setValues(new double[]{2, 4, 8, 16}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(32);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test
    public void powerTrendLine_simpleTest() {
        // :: Setup
        Forecast.PowerTrendLine trendLine = new Forecast.PowerTrendLine();
        // Data set as y=2x
        trendLine.setValues(new double[]{2, 4, 6, 8}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(10);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }


    @Test
    public void logTrendLine_simpleTest() {
        // :: Setup
        Forecast.LogTrendLine trendLine = new Forecast.LogTrendLine();
        // Data set first large increase, then less and less increasing values.
        trendLine.setValues(new double[]{1, 16, 26, 31}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(36.41);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0.5);
    }
}
