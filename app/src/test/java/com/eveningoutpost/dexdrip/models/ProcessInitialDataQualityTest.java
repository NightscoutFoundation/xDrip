package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


/**
 * Created by jamorham on 01/10/2017.
 */

// TODO this doesn't test whether SensorSanity exclusions are properly processed
public class ProcessInitialDataQualityTest extends RobolectricTestWithConfig {

    // if we have a record which is on an exact millisecond boundary and test it and it passes the test
    // 1ms later it will fail the test resulting in the assertion sometimes incorrectly labelling as a
    // mismatched result because the clock can tick over 1ms between the time we first tested and when we
    // compare the results of that test. To avoid this (even on slow systems) we add in a grace period
    private static final long COMPUTATION_GRACE_TIME = Constants.SECOND_IN_MS;

    private static void log(String msg) {
        System.out.println(msg);
    }

    @Test
    public void testGetInitialDataQuality() {
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

                final long ms_since = (JoH.msSince(bgReadingList.get(bgReadingList.size() - 1).timestamp));
                if ((ms_since > Constants.STALE_CALIBRATION_CUT_OFF + COMPUTATION_GRACE_TIME) || (i < 3)) {
                    assertThat("Stale data should fail: i:" + i + " tm:" + ms_since, test.pass, is(false));
                }
                if ((ms_since <= Constants.STALE_CALIBRATION_CUT_OFF) && (bgReadingList.size() >= 3)) {
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