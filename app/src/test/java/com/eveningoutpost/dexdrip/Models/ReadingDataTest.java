package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utils.LibreTrendPoint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
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
        while (libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }
        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000, 1230, 800, GlucoseData.DataSource.FRAM);
        libreTrendPoints.set((int) libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1015, 1230, 0x700, GlucoseData.DataSource.FRAM);
        libreTrendPoints.set((int) libreTrendPoint.getSensorTime(), libreTrendPoint);

        readingData.ClearErrors(libreTrendPoints);
        assertWithMessage("One object should have been removed").that(readingData.history.size()).isEqualTo(2);
        assertThat(readingData.history.get(0).sensorTime).isEqualTo(1000);
        assertThat(readingData.history.get(1).sensorTime).isEqualTo(1030);
    }

    @Test
    public void testClearTrendErrors() {
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }

        // Create a list of 10 points.
        for (int i = 0 ; i < 10; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000+i;
            libreTrendPoints.get((int)readingData.trend.get(i).sensorTime).rawSensorValue = 220;
        }

        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000, 1230, 800, GlucoseData.DataSource.FRAM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1005, 1230, 0x700, GlucoseData.DataSource.FRAM);
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

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }
        // Create a list of 10 points.
        for (int i = 0 ; i < 10; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000+i;
            libreTrendPoints.get((int)readingData.trend.get(i).sensorTime).rawSensorValue = 220;
        }

        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000, 1230, 800, GlucoseData.DataSource.FRAM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1003, 1230, 0x700, GlucoseData.DataSource.FRAM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        libreTrendPoint = new LibreTrendPoint(1005, 1230, 0x700, GlucoseData.DataSource.FRAM);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);

        readingData.ClearErrors(libreTrendPoints);
        assertWithMessage("4 objects should remain.").that(readingData.trend.size()).isEqualTo(4);
        // object #4 is bad, so last goog object is 3.
        assertWithMessage("span serialize reformed matches").that(readingData.trend.get(2).sensorTime).isEqualTo(1002);
        // Next good object is 1009 (1005-1008 were removed)
        assertWithMessage("span serialize reformed matches").that(readingData.trend.get(3).sensorTime).isEqualTo(1009);
    }

    @Test
    public void testClearTrendErrorOnOtherPoint() {
        // A very simple case, we only have one reading containing sparsed data.
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }

        int [] times = {0, 2, 4, 6, 7, 12, 15};
        int [] data = {200, 210, 222, 230, 240, 250, 260};
        for(int i = 0 ; i < times.length; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000 - times[i];
            readingData.trend.get(i).glucoseLevelRaw = data[i];

            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        // Add a point with an error at 995. It should cause points '2', '4'
        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(995, 0, 0x700, GlucoseData.DataSource.BLE);
        libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        readingData.ClearErrors(libreTrendPoints);
        assertWithMessage("5 objects should remain.").that(readingData.trend.size()).isEqualTo(5);
        assertThat(readingData.trend.get(0).sensorTime).isEqualTo(1000);
        assertThat(readingData.trend.get(1).sensorTime).isEqualTo(994);
        assertThat(readingData.trend.get(2).sensorTime).isEqualTo(993);
        assertThat(readingData.trend.get(3).sensorTime).isEqualTo(988);
        assertThat(readingData.trend.get(4).sensorTime).isEqualTo(985);
    }

    @Test
    public void testCalculateSmoothDataImproved() {
        // A very simple case, we only have one reading containing sparsed data, no errors
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }


        int [] times = {0, 2, 4, 6, 7, 12, 15};
        int [] data = {200, 210, 222, 230, 240, 250, 260};
        for(int i = 0 ; i < times.length; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000 - times[i];
            readingData.trend.get(i).glucoseLevelRaw = data[i];

            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        readingData.calculateSmoothDataImproved(libreTrendPoints, false);
        assertWithMessage("First 4 values should be used").that(readingData.trend.get(0).glucoseLevelRawSmoothed).isEqualTo((200+210+222+230) / 4);
    }

    @Test
    public void testCalculateSmoothDataImprovedOneError() {
        // Using sparse data.
        // The first value contains an error.
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }

        int [] times = {0, 2, 4, 6, 7, 12, 15};
        int [] data = {200, 210, 222, 230, 240, 250, 260};
        for(int i = 0 ; i < times.length; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000 - times[i];
            readingData.trend.get(i).glucoseLevelRaw = data[i];

            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        // The first value has an error
        libreTrendPoints.get(1000).rawSensorValue = 0;

        readingData.calculateSmoothDataImproved(libreTrendPoints, false);
        assertWithMessage("3 values should be used").that(readingData.trend.get(0).glucoseLevelRawSmoothed).isEqualTo((210+222+230) / 3);
        assertWithMessage("4 values should be used").that(readingData.trend.get(1).glucoseLevelRawSmoothed).isEqualTo((210 + 222 + 230 + 240) / 4);
        assertWithMessage("3 values should be used").that(readingData.trend.get(2).glucoseLevelRawSmoothed).isEqualTo((222 + 230 + 240) / 3);
        assertWithMessage("3 values should be used").that(readingData.trend.get(3).glucoseLevelRawSmoothed).isEqualTo((230 + 240 + 250) / 3);
        assertWithMessage("2 values should be used").that(readingData.trend.get(4).glucoseLevelRawSmoothed).isEqualTo(( 240 + 250) / 2);
        assertWithMessage("2 values should be used").that(readingData.trend.get(5).glucoseLevelRawSmoothed).isEqualTo(( 250 + 260) / 2);
        assertWithMessage("1 value should be used").that(readingData.trend.get(6).glucoseLevelRawSmoothed).isEqualTo(( 260) / 1);
    }

    @Test
    public void testCalculateSmoothDataAllValues() {
        // Having a normal reading data. All data exists (due to further readings).
        // No errors.
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }

        int [] times = {0, 2, 4, 6, 7, 12, 15};
        int [] data = {200, 220, 240, 260, 270, 320, 350};
        for(int i = 0 ; i < times.length; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000 - times[i];
            readingData.trend.get(i).glucoseLevelRaw = data[i];

            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        // for LibreTrendPoint add data to fill the gap.
        times = new int[] {1, 3, 5, 8, 9, 10, 11, 13, 14, 16};
        data = new int[]  {210, 230, 250, 280, 290, 300, 310, 330, 340, 360};

        for(int i = 0 ; i < times.length; i++) {
            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        for(int i =0 ; i < 20 ; i++)
        System.err.println(1000-i + " "  +libreTrendPoints.get(1000 - i ).rawSensorValue);

        readingData.calculateSmoothDataImproved(libreTrendPoints, false);
        assertWithMessage("First 5 values should be used").that(readingData.trend.get(0).glucoseLevelRawSmoothed).isEqualTo((200 + 210 + 220 +230 +240) / 5);
        assertWithMessage("First 5 values should be used (point 2)").that(readingData.trend.get(1).glucoseLevelRawSmoothed).isEqualTo((220 +230 +240 + 250 +260) / 5);
        assertWithMessage("First 5 values should be used (point 4)").that(readingData.trend.get(2).glucoseLevelRawSmoothed).isEqualTo((240 + 250 + 260 +270 +280) / 5);
        assertWithMessage("First 5 values should be used (point 6)").that(readingData.trend.get(3).glucoseLevelRawSmoothed).isEqualTo((260 + 270 + 280 +290 +300) / 5);
        assertWithMessage("First 5 values should be used (point 7)").that(readingData.trend.get(4).glucoseLevelRawSmoothed).isEqualTo((270 + 280 + 290 +300 +310) / 5);
        assertWithMessage("First 5 values should be used (point 12)").that(readingData.trend.get(5).glucoseLevelRawSmoothed).isEqualTo((320 + 330 + 340 +350 +360) / 5);
        assertWithMessage("First 5 values should be used (point 15)").that(readingData.trend.get(6).glucoseLevelRawSmoothed).isEqualTo((350 + 360) / 2);
    }


    @Test
    public void testCalculateSmoothDataAllValuesOneError() {
        // Having a normal reading data. All data exists (due to further readings).
        // No errors.
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }

        int [] times = {0, 2, 4, 6, 7, 12, 15};
        int [] data = {200, 220, 240, 260, 270, 320, 350};
        for(int i = 0 ; i < times.length; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000 - times[i];
            readingData.trend.get(i).glucoseLevelRaw = data[i];

            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        // for LibreTrendPoint add data to fill the gap.
        times = new int[] {1, 3, 5, 8, 9, 10, 11, 13, 14, 16};
        data = new int[]  {210, 230, 250, 280, 290, 300, 310, 330, 340, 360};

        for(int i = 0 ; i < times.length; i++) {
            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }

        // The Sixth value has an error
        libreTrendPoints.get(992).rawSensorValue = 0;

        for(int i =0 ; i < 20 ; i++)
            System.err.println(1000-i + " "  +libreTrendPoints.get(1000 - i ).rawSensorValue);

        readingData.calculateSmoothDataImproved(libreTrendPoints, false);
        // Here is how things should look like
        // reading   0       1       2       3   4                   5           6
        // point   1000 999 998 997 996 995 994 993 992 991 990 989 988 987 986 985 984 983
        // raw     200  210 220 230 240 250 260 270 280 290 300 310 320 330 340 350 360 370
        // isError  f    f   f   f   f   t   t   t   t   f   f   f   f   f   f   f   f   f

        assertWithMessage("First 5 values should be used").that(readingData.trend.get(0).glucoseLevelRawSmoothed).isEqualTo((200 + 210 + 220 +230 +240) / 5);
        assertWithMessage("First 3 values should be used (point 2)").that(readingData.trend.get(1).glucoseLevelRawSmoothed).isEqualTo((220 +230 +240  ) / 3);
        assertWithMessage("First value used then 2 at the end (point 4)").that(readingData.trend.get(2).glucoseLevelRawSmoothed).isEqualTo((240 + 290 + 300 ) / 3);
        assertWithMessage("3 first points ignored, then 4 used values should be used (point 6)").that(readingData.trend.get(3).glucoseLevelRawSmoothed).isEqualTo(( 290 + 300 + 310 + 320) / 4);
        assertWithMessage("2 first points ignored, then 5 used values should be used (point 7)").that(readingData.trend.get(4).glucoseLevelRawSmoothed).isEqualTo((290 + 300 + 310 + 320 + 330) / 5);
        assertWithMessage("First 5 values should be used (point 12)").that(readingData.trend.get(5).glucoseLevelRawSmoothed).isEqualTo((320 + 330 + 340 +350 +360) / 5);
        assertWithMessage("First 5 values should be used (point 15)").that(readingData.trend.get(6).glucoseLevelRawSmoothed).isEqualTo((350 + 360) / 2);
    }

    @Test
    public void testCalculateSmoothDataAllValuesTwoErrors() {
        // Having a normal reading data. All data exists (due to further readings).
        // No errors.
        ReadingData readingData = new ReadingData();
        readingData.trend = new ArrayList<GlucoseData>();

        List<LibreTrendPoint> libreTrendPoints = new ArrayList<LibreTrendPoint>(16 * 24 * 60);
        while(libreTrendPoints.size() < 16 * 24 * 60) {
            libreTrendPoints.add(libreTrendPoints.size(), new LibreTrendPoint());
        }

        int [] times = {0, 2, 4, 6, 7, 12, 15};
        int [] data = {200, 220, 240, 260, 270, 320, 350};
        for(int i = 0 ; i < times.length; i++) {
            readingData.trend.add(new GlucoseData());
            readingData.trend.get(i).sensorTime = 1000 - times[i];
            readingData.trend.get(i).glucoseLevelRaw = data[i];

            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }
        // for LibreTrendPoint add data to fill the gap.
        times = new int[] {1, 3, 5, 8, 9, 10, 11, 13, 14, 16};
        data = new int[]  {210, 230, 250, 280, 290, 300, 310, 330, 340, 360};

        for(int i = 0 ; i < times.length; i++) {
            LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 - times[i], data[i], 0x700, GlucoseData.DataSource.BLE);
            libreTrendPoints.set((int)libreTrendPoint.getSensorTime(), libreTrendPoint);
        }

        // Two errors from 992 to 998
        libreTrendPoints.get(992).rawSensorValue = 0;
        libreTrendPoints.get(995).rawSensorValue = 0;

        for(int i =0 ; i < 20 ; i++)
            System.err.println(1000-i + " "  +libreTrendPoints.get(1000 - i ).rawSensorValue);

        readingData.calculateSmoothDataImproved(libreTrendPoints, false);
        // Here is how things should look like
        // reading   0       1       2       3   4                   5           6
        // point   1000 999 998 997 996 995 994 993 992 991 990 989 988 987 986 985 984 983
        // raw     200  210 220 230 240 250 260 270 280 290 300 310 320 330 340 350 360 370
        // isError  f    f   t   t   t   t   t   t   t   f   f   f   f   f   f   f   f   f

        // Make sure that one reading data was removed, due to no valid data
        assertWithMessage("One object was removed").that(readingData.trend.size()).isEqualTo(6);

        assertWithMessage("First 2 values should be used").that(readingData.trend.get(0).glucoseLevelRawSmoothed).isEqualTo((200 + 210 ) / 2);
        assertWithMessage("5 values should be ingnored then 2 used (point 4)").that(readingData.trend.get(1).glucoseLevelRawSmoothed).isEqualTo(( 290 + 300 ) / 2);
        assertWithMessage("3 first points ignored, then 4 used values should be used (point 6)").that(readingData.trend.get(2).glucoseLevelRawSmoothed).isEqualTo(( 290 + 300 + 310 + 320) / 4);
        assertWithMessage("2 first points ignored, then 5 used values should be used (point 7)").that(readingData.trend.get(3).glucoseLevelRawSmoothed).isEqualTo((290 + 300 + 310 + 320 + 330) / 5);
        assertWithMessage("First 5 values should be used (point 12)").that(readingData.trend.get(4).glucoseLevelRawSmoothed).isEqualTo((320 + 330 + 340 +350 +360) / 5);
        assertWithMessage("First 5 values should be used (point 15)").that(readingData.trend.get(5).glucoseLevelRawSmoothed).isEqualTo((350 + 360) / 2);
    }




    // A test for libreTrendPoint.isError
    @Test
    public void testlibreTrendPointIsError() {
        LibreTrendPoint libreTrendPoint = new LibreTrendPoint(1000 , 0, 0x700, GlucoseData.DataSource.BLE);
        assertWithMessage("rawsensorvalue").that(libreTrendPoint.isError()).isTrue();

        libreTrendPoint = new LibreTrendPoint(1000 , 100, 0x700, GlucoseData.DataSource.BLE);
        assertWithMessage("rawsensorvalue").that(libreTrendPoint.isError()).isFalse();

        libreTrendPoint = new LibreTrendPoint(1000 , 0, 0x700, GlucoseData.DataSource.FRAM);
        assertWithMessage("rawsensorvalue").that(libreTrendPoint.isError()).isTrue();

        libreTrendPoint = new LibreTrendPoint(1000 , 100, 0x700, GlucoseData.DataSource.FRAM);
        assertWithMessage("rawsensorvalue").that(libreTrendPoint.isError()).isTrue();

        libreTrendPoint = new LibreTrendPoint(1000 , 100, 800, GlucoseData.DataSource.FRAM);
        assertWithMessage("rawsensorvalue").that(libreTrendPoint.isError()).isFalse();

        libreTrendPoint = new LibreTrendPoint(1000 , 100, 0, GlucoseData.DataSource.FRAM);
        assertWithMessage("rawsensorvalue").that(libreTrendPoint.isError()).isFalse();

        libreTrendPoint = new LibreTrendPoint();
        assertWithMessage("Empty point has no error.").that(libreTrendPoint.isError()).isFalse();
    }
}

