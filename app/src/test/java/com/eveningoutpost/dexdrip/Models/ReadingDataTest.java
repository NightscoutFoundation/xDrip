package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utils.LibreTrendPoint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertWithMessage;

public class ReadingDataTest extends RobolectricTestWithConfig {

    @Test
    public void testClearErrors() {

        ReadingData readingData = new ReadingData();
        readingData.history = new ArrayList<GlucoseData>();
        readingData.history.add(new GlucoseData());
        readingData.history.add(new GlucoseData());
        readingData.history.add(new GlucoseData());
        readingData.history.get(0).sensorTime = 1000;
        readingData.history.get(1).sensorTime = 1015;
        readingData.history.get(2).sensorTime = 1030;


        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }
        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000, 1230, 0x800, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1015, 1230, 0x700, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);

        readingData.ClearErrors(libreTrendPoints);
        assertWithMessage("One object should have been removed").that(readingData.history.size()).isEqualTo(2);

    }

    @Test
    public void testClearTrendErrors() {
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        // Create a list of 10 points.
        for (int i = 0 ; i < 10; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000+i;
        }

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }
        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000, 1230, 0x800, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1005, 1230, 0x700, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);

        readingData.ClearErrors(libreTrendPoints);
        assertWithMessage("6 objects should remain.").that(readingData.trend.size()).isEqualTo(6);
        // object #6 is bad, so last goog object is 5.
        assertWithMessage("span serialize reformed matches").that(readingData.trend.get(4).sensorTime).isEqualTo(1004);
        // Next good object is 1009 (1005-1008 were removed)
        assertWithMessage("span serialize reformed matches").that(readingData.trend.get(5).sensorTime).isEqualTo(1009);
    }

    @Test
    public void testClearTrendTwoErrors() {
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        // Create a list of 10 points.
        for (int i = 0 ; i < 10; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000+i;
        }

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }
        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000, 1230, 0x800, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1003, 1230, 0x700, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1005, 1230, 0x700, GlucoseData.DataSource.FARM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);

        readingData.ClearErrors(libreTrendPoints);
        assertWithMessage("6 objects should remain.").that(readingData.trend.size()).isEqualTo(4);
        // object #4 is bad, so last goog object is 3.
        assertWithMessage("span serialize reformed matches").that(readingData.trend.get(2).sensorTime).isEqualTo(1002);
        // Next good object is 1009 (1005-1008 were removed)
        assertWithMessage("span serialize reformed matches").that(readingData.trend.get(3).sensorTime).isEqualTo(1009);
    }
}