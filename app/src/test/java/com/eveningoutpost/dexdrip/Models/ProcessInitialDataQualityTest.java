package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.MockModel;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;


/**
 * Created by jamorham on 01/10/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "../../../../../src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml")
public class ProcessInitialDataQualityTest {

    private static void log(String msg) {
        System.out.println(msg);
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetInitialDataQuality() throws Exception {

        // check we can mock ActiveAndroid which depends on Android framework
        final MockModel m = new MockModel();
        assertThat("ActiveAndroid Mock Model can be created", m != null, is(true));
        assertThat("ActiveAndroid Mock Model can be created", m.intField == 0, is(true));

        // result object store
        ProcessInitialDataQuality.InitialDataQuality test;

        // try with null data set
        test = ProcessInitialDataQuality.getInitialDataQuality(null);

        assertThat("Result object not null", test != null, is(true));
        assertThat("Null input should fail", test.pass, is(false));

        // try with empty data set
        List<BgReading> bgReadingList = new ArrayList<>();
        test = ProcessInitialDataQuality.getInitialDataQuality(bgReadingList);
        assertThat("Result object not null", test != null, is(true));
        assertThat("Empty input should fail", test.pass, is(false));

        // create an assortment of data sets with data spaced out by different frequencies
        for (int frequency = 5; frequency < 21; frequency = frequency + 1) {
            bgReadingList.clear();
            for (int i = 1; i <= 3; i++) {
                // add an element
                bgReadingList.add(getGoodMockBgReading(i * Constants.MINUTE_IN_MS * frequency)); // we add older readings to the end
                test = ProcessInitialDataQuality.getInitialDataQuality(bgReadingList);
                log("Frequency: " + frequency + " Loop " + i + " size:" + bgReadingList.size()
                        + " Newest age: " + JoH.niceTimeScalar(JoH.msSince(bgReadingList.get(0).timestamp))
                        + " Oldest age: " + JoH.niceTimeScalar(JoH.msSince(bgReadingList.get(bgReadingList.size() - 1).timestamp))
                        + " / Mock Advice: " + test.advice + " VERDICT: " + (test.pass ? "PASS" : "NOT PASSED"));

                assertThat("Result object not null", test != null, is(true));
                if (i < 3) assertThat("Empty input should fail", test.pass, is(false));
                assertThat("There should be some advice on loop " + i, test.advice.length() > 0, is(true));


                if ((JoH.msSince(bgReadingList.get(bgReadingList.size() - 1).timestamp) > Constants.STALE_CALIBRATION_CUT_OFF) || (i < 3)) {
                    assertThat("Stale data should fail", test.pass, is(false));
                }
                if ((JoH.msSince(bgReadingList.get(bgReadingList.size() - 1).timestamp) <= Constants.STALE_CALIBRATION_CUT_OFF) && (bgReadingList.size() >= 3)) {
                    assertThat("Good data should pass", test.pass, is(true));
                }

            }
        }

    }

    // Timestamps from this will not be realistic, this could become an issue if other filtering
    // mechanisms failed to prevent duplicates etc. A better mock bg reading creator could be
    // produced but I have left this as flexible as possible at this point.
    private static BgReading getGoodMockBgReading(long ago) {
        final BgReading bg = new BgReading();
        bg.raw_data = 123;
        bg.timestamp = JoH.tsl() - ago; // current timestamp
        bg.noise = "MOCK DATA - DO NOT USE";
        return bg;
    }

}