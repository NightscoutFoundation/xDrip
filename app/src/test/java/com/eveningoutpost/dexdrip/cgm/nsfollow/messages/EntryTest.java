package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link Entry}
 *
 * @author Asbjørn Aarrestad - 2019.06 - asbjorn@aarrestad.com
 */
public class EntryTest {

    @Before
    public void initLocale() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void getTimeStamp_notSet() {
        // :: Setup
        Entry entry = new Entry();

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(-1);
    }

    // ===== DateString parsing ====================================================================

    @Test
    public void getTimeStamp_dateStringParser_Invalid() {
        // :: Setup
        Entry entry = new Entry();
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(-1);
    }


    @Test
    public void getTimeStamp_dateStringParser_NoMillis_Iso() {
        // :: Setup
        Entry entry = new Entry();
        entry.dateString = "2019-06-29T19:52:01Z";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561837921000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 19:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_dateStringParser_Iso() {
        // :: Setup
        Entry entry = new Entry();
        entry.dateString = "2019-06-29T19:52:01.123Z";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561837921000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 19:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_dateStringParser_DifferentTime_Iso() {
        // :: Setup
        Entry entry = new Entry();
        entry.dateString = "2019-06-29T19:52:01.456Z";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561837921000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 19:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_dateStringParser_Iso2() {
        // :: Setup
        Entry entry = new Entry();
        entry.dateString = "2019-06-29T19:52:01+0200";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561830721000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 17:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_dateStringParser_Iso3() {
        // :: Setup
        Entry entry = new Entry();
        entry.dateString = "2019-06-29T19:52+0200";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561830720000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 17:52:00 UTC 2019");
    }

    // ===== sysTime parsing =======================================================================


    @Test
    public void getTimeStamp_sysTimeParser_Invalid() {
        // :: Setup
        Entry entry = new Entry();
        entry.sysTime = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(-1);
    }


    @Test
    public void getTimeStamp_sysTimeParser_NoMillis_Iso() {
        // :: Setup
        Entry entry = new Entry();
        entry.sysTime = "2019-06-29T19:52:01Z";
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561837921000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 19:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_sysTimeParser_Iso() {
        // :: Setup
        Entry entry = new Entry();
        entry.sysTime = "2019-06-29T19:52:01.123Z";
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561837921000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 19:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_sysTimeParser_DifferentTime_Iso() {
        // :: Setup
        Entry entry = new Entry();
        entry.sysTime = "2019-06-29T19:52:01.456Z";
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561837921000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 19:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_sysTimeParser_Iso2() {
        // :: Setup
        Entry entry = new Entry();
        entry.sysTime = "2019-06-29T19:52:01+0200";
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561830721000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 17:52:01 UTC 2019");
    }

    @Test
    public void getTimeStamp_sysTimeParser_Iso3() {
        // :: Setup
        Entry entry = new Entry();
        entry.sysTime = "2019-06-29T19:52+0200";
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(1561830720000L);
        assertThat(new Date(timeStamp).toString()).isEqualTo("Sat Jun 29 17:52:00 UTC 2019");
    }

    // ===== Date is set ===========================================================================

    @Test
    public void getTimestamp_date_low_aka_invalid() {
        // :: Setup
        Entry entry = new Entry();
        entry.date = 10000;

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(-1);
    }

    @Test
    public void getTimestamp_date_valid() {
        // :: Setup
        Entry entry = new Entry();
        entry.date = 2000000;
        entry.sysTime = "XXX";
        entry.dateString = "XXX";

        // :: Act
        long timeStamp = entry.getTimeStamp();

        // :: Verify
        assertThat(timeStamp).isEqualTo(2000000);
    }
}