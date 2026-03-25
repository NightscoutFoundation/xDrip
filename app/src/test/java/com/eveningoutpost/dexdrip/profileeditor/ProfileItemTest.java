package com.eveningoutpost.dexdrip.profileeditor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * @author Asbjørn Aarrestad
 */
public class ProfileItemTest {

    private TimeZone oldTimeZone;

    @Before
    public void setUp() throws Exception {
        oldTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        getHourMinConvert().setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void tearDown() throws Exception {
        TimeZone.setDefault(oldTimeZone);
        getHourMinConvert().setTimeZone(oldTimeZone);
    }

    private static SimpleDateFormat getHourMinConvert() throws Exception {
        Field field = ProfileItem.class.getDeclaredField("hourMinConvert");
        field.setAccessible(true);
        return (SimpleDateFormat) field.get(null);
    }

    // -- Time period formatting --

    @Test
    public void timePeriod_formatsAsHHMM() {
        // :: Setup
        ProfileItem item = new ProfileItem(90, 150, 10.0, 50.0);

        // :: Verify
        assertThat(item.getTimePeriod()).isEqualTo("01:30 -> 02:30");
    }

    @Test
    public void timePeriod_midnight() {
        // :: Setup
        ProfileItem item = new ProfileItem(0, 1440, 10.0, 50.0);

        // :: Verify
        assertThat(item.getTimeStart()).isEqualTo("00:00");
        assertThat(item.getTimeEnd()).isEqualTo("24:00");
    }

    // -- timeStampToMin: converts epoch millis to minute-of-day --

    @Test
    public void timeStampToMin_midnight_returnsZero() {
        // :: Setup — 2026-01-01 00:00:00 UTC
        long midnightUtc = 1767225600000L;

        // :: Verify
        assertThat(ProfileItem.timeStampToMin(midnightUtc)).isEqualTo(0);
    }

    @Test
    public void timeStampToMin_noon_returns720() {
        // :: Setup — 2026-01-01 12:00:00 UTC
        long noonUtc = 1767225600000L + 12 * 3600 * 1000L;

        // :: Verify
        assertThat(ProfileItem.timeStampToMin(noonUtc)).isEqualTo(720);
    }

    @Test
    public void timeStampToMin_1530_returns930() {
        // :: Setup — 15:30 = 15*60 + 30 = 930 minutes
        long time1530 = 1767225600000L + (15 * 3600 + 30 * 60) * 1000L;

        // :: Verify
        assertThat(ProfileItem.timeStampToMin(time1530)).isEqualTo(930);
    }

    // -- Double overload gives same result --

    @Test
    public void timeStampToMin_doubleAndLong_agree() {
        // :: Setup — 08:00 UTC
        long ts = 1767225600000L + 8 * 3600 * 1000L;

        // :: Verify
        assertThat(ProfileItem.timeStampToMin((double) ts))
                .isEqualTo(ProfileItem.timeStampToMin(ts));
    }

    // -- Equality is value-based (carb_ratio, sensitivity, absorption_rate) --

    @Test
    public void equals_sameValues_areEqual() {
        // :: Setup — different time ranges but same ratio/sensitivity
        ProfileItem a = new ProfileItem(0, 60, 10.0, 50.0);
        ProfileItem b = new ProfileItem(120, 180, 10.0, 50.0);

        // :: Verify
        assertThat(a).isEqualTo(b);
    }

    @Test
    public void equals_differentValues_notEqual() {
        // :: Setup
        ProfileItem a = new ProfileItem(0, 60, 10.0, 50.0);
        ProfileItem b = new ProfileItem(0, 60, 12.0, 50.0);

        // :: Verify
        assertThat(a).isNotEqualTo(b);
    }

    // -- Clone produces equal but distinct object --

    @Test
    public void clone_producesEqualCopy() {
        // :: Setup
        ProfileItem orig = new ProfileItem(60, 120, 8.5, 45.0);

        // :: Act
        ProfileItem copy = orig.clone();

        // :: Verify
        assertThat(copy).isEqualTo(orig);
        assertThat(copy).isNotSameInstanceAs(orig);
    }

    // -- toJson serialization --

    @Test
    public void toJson_containsExposedFields() {
        // :: Setup
        ProfileItem item = new ProfileItem(60, 120, 10.0, 50.0);

        // :: Act
        String json = item.toJson();

        // :: Verify
        assertThat(json).contains("\"start_min\":60");
        assertThat(json).contains("\"end_min\":120");
        assertThat(json).contains("\"carb_ratio\":10.0");
        assertThat(json).contains("\"sensitivity\":50.0");
    }

    // -- Sorting uses start_min --

    @Test
    public void sorting_orderedByStartMin() {
        // :: Setup
        ProfileItem a = new ProfileItem(120, 180, 10.0, 50.0);
        ProfileItem b = new ProfileItem(0, 60, 10.0, 50.0);
        ProfileItem c = new ProfileItem(60, 120, 10.0, 50.0);
        List<ProfileItem> items = Arrays.asList(a, b, c);

        // :: Act
        Collections.sort(items);

        // :: Verify
        assertThat(items.get(0).start_min).isEqualTo(0);
        assertThat(items.get(1).start_min).isEqualTo(60);
        assertThat(items.get(2).start_min).isEqualTo(120);
    }
}
