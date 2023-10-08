package com.eveningoutpost.dexdrip.profileeditor;

import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.clearRates;
import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.getActiveRate;
import static com.eveningoutpost.dexdrip.profileeditor.BasalProfile.getActiveRateName;
import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.getRateByMinuteOfDay;
import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.getRateByTimeStamp;
import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.minutesPerSegment;
import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.populateRateAsNeeded;
import static com.eveningoutpost.dexdrip.profileeditor.BasalRepository.rates;
import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class BasalRepositoryTest extends RobolectricTestWithConfig {

    private static final String ACTIVE_RATE_NAME = "1";
    private static final long SPEC_TIMESTAMP = 1650988561000L; // 15:xx UTC 16:xx BST 17:xx CEST
    private TimeZone oldTimeZone;

    @Before
    public void setup() {
        oldTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final List<Double> segments = new LinkedList<>();
        for (int i = 1; i < 25; i++) {
            segments.add(i / 10d);
        }
        BasalProfile.save(ACTIVE_RATE_NAME, segments);
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(oldTimeZone);
    }

    @Test
    public void getActiveRateNameTest() {
        assertWithMessage("rate name test").that(getActiveRateName()).isEqualTo(ACTIVE_RATE_NAME);
    }

    @Test
    public void populateRateAsNeededTest() {
        clearRates();
        assertWithMessage("rates empty after clear").that(rates.size()).isEqualTo(0);
        populateRateAsNeeded();
        assertWithMessage("rates full after populate").that(rates.size()).isEqualTo(24);
        populateRateAsNeeded();
        assertWithMessage("rates still good size after populate").that(rates.size()).isEqualTo(24);
    }

    @Test
    public void minutesPerSegmentTest() {
        clearRates();
        assertWithMessage("no rates full day").that(minutesPerSegment()).isEqualTo(1440);
        populateRateAsNeeded();
        assertWithMessage("24 rates 60 minutes").that(minutesPerSegment()).isEqualTo(60);
    }

    @Test
    public void getRateByMinuteOfDayTest() {
        clearRates();
        assertWithMessage("no rates zero rate").that(getRateByMinuteOfDay(60)).isEqualTo(0d);
        populateRateAsNeeded();
        assertWithMessage("24 rates out of bounds zero rate 1").that(getRateByMinuteOfDay(1440)).isEqualTo(0d);
        assertWithMessage("24 rates out of bounds zero rate 2").that(getRateByMinuteOfDay(-1)).isEqualTo(0d);
        for (int i = 0; i < 1440; i++) {
            assertWithMessage("rate for minute " + i).that(getRateByMinuteOfDay(i)).isEqualTo(JoH.roundDouble((((i / 60) / 10d) + 0.1d), 2));
        }
    }

    @Test
    public void getRateByTimeStampTest() {
        clearRates();
        assertWithMessage("no rates zero rate").that(getRateByTimeStamp(SPEC_TIMESTAMP)).isEqualTo(0d);
        populateRateAsNeeded();
        assertWithMessage("rates timezone rate 16:00").that(getRateByTimeStamp(SPEC_TIMESTAMP)).isEqualTo(1.6d);
    }

    @Test
    public void getActiveRateTest() {
        clearRates();
        assertWithMessage("rates timezone rate 16:00").that(getActiveRate(SPEC_TIMESTAMP)).isEqualTo(1.6d);
    }
}