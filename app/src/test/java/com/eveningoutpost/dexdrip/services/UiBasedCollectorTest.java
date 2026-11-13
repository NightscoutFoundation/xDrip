package com.eveningoutpost.dexdrip.services;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import org.junit.Before;
import org.junit.Test;

import lombok.val;

public class UiBasedCollectorTest extends RobolectricTestWithConfig {

    @Before
    @Override
    public void setUp() {
        super.setUp();
        PersistentStore.setLong("UI_BASED_STORE_LAST_VALUE", 0);
        PersistentStore.setLong("UI_BASED_STORE_LAST_REPEAT", 0);
    }

    @Test
    public void isValidMmolTest() {

        assertWithMessage("good 1").that(UiBasedCollector.isValidMmol("5.6")).isTrue();
        assertWithMessage("good 2").that(UiBasedCollector.isValidMmol("5.55")).isTrue();
        assertWithMessage("good 3").that(UiBasedCollector.isValidMmol("12.34")).isTrue();

        assertWithMessage("bad 1").that(UiBasedCollector.isValidMmol("555")).isFalse();
        assertWithMessage("bad 2").that(UiBasedCollector.isValidMmol("abc")).isFalse();
        assertWithMessage("bad 3").that(UiBasedCollector.isValidMmol("abc 12.34")).isFalse();
        assertWithMessage("bad 4").that(UiBasedCollector.isValidMmol("abc12.34")).isFalse();
        assertWithMessage("bad 5").that(UiBasedCollector.isValidMmol("12.34abc")).isFalse();
        assertWithMessage("bad 6").that(UiBasedCollector.isValidMmol("5..55")).isFalse();
        assertWithMessage("bad 7").that(UiBasedCollector.isValidMmol("5.")).isFalse();
        assertWithMessage("bad 8").that(UiBasedCollector.isValidMmol(".5")).isFalse();
        assertWithMessage("bad 9").that(UiBasedCollector.isValidMmol("5")).isFalse();


    }

    @Test
    public void filterStringTest() {
        val i = new UiBasedCollector();
        val spec1 = "non spec";
        val spec2 = "≤5.123";
        val spec2p = "5.123";
        val spec3 = "≥100.123";
        val spec3p = "100.123";
        val spec4 = "\u0038\u002c\u0039\u00a0\u006d\u006d\u006f\u006c\u2060\u002f\u2060\u006c\u0020";
        val spec4p = "8,9";
        val spec5 = "\u0038\u002c\u0039\u00a0\u006d\u006d\u006f\u006c\u2060\u002f\u2060\u2191\u2b06\u006c\u0020";
        val spec5p = "8,9";
        assertWithMessage("null test pass through 1").that(i.filterString(spec1)).isEqualTo(spec1);
        assertWithMessage("null test pass through 2").that(i.filterString(spec2)).isEqualTo(spec2);
        assertWithMessage("null test pass through 3").that(i.filterString(spec3)).isEqualTo(spec3);
        i.lastPackage = "hello world";
        assertWithMessage("non 1").that(i.filterString(spec1)).isEqualTo(spec1);
        assertWithMessage("gte 1").that(i.filterString(spec2)).isEqualTo(spec2p);
        assertWithMessage("lte 1").that(i.filterString(spec3)).isEqualTo(spec3p);
        assertWithMessage("gc1 1").that(i.filterString(spec4)).isEqualTo(spec4p);
        assertWithMessage("gc1 2").that(i.filterString(spec5)).isEqualTo(spec5p);
    }

    @Test
    public void parseIoBOmnipodTest() {
        val i = new UiBasedCollector();

        val valid = "Automated Mode (IOB: 5.1 U)";
        val invalid = "Foobar";

        assertWithMessage("valid Omnipod IoB message").that(i.parseIoB(valid)).isEqualTo(5.1);
        assertWithMessage("invalid IoB message").that(i.parseIoB(invalid)).isNull();
    }

    @Test
    public void parseIoBMiniMedTest() {
        val i = new UiBasedCollector();

        // Basic variations
        assertWithMessage("one decimal place").that(i.parseIoB("0.8 U")).isEqualTo(0.8);
        assertWithMessage("two decimal places").that(i.parseIoB("3.65 IE")).isEqualTo(3.65);
        assertWithMessage("three decimal places").that(i.parseIoB("7.975 J")).isEqualTo(7.975);
        assertWithMessage("more insulin").that(i.parseIoB("37.4 e")).isEqualTo(37.4);

        // Test all locales
        assertWithMessage("locales en,el,ja,pt,pt-rBR").that(i.parseIoB("1.234 U")).isEqualTo(1.234);
        assertWithMessage("locales es,fi,fr,it,lt,lv,mk,ro").that(i.parseIoB("1,234 U")).isEqualTo(1.234);
        assertWithMessage("locales da,hu,nl,sl,sv").that(i.parseIoB("1,234 E")).isEqualTo(1.234);
        assertWithMessage("locales cs,hr,sr").that(i.parseIoB("1,234 J")).isEqualTo(1.234);
        assertWithMessage("locales nb,nn").that(i.parseIoB("1,234 e")).isEqualTo(1.234);
        assertWithMessage("locales et,tr").that(i.parseIoB("1,234 Ü")).isEqualTo(1.234);
        assertWithMessage("locales ko,zh").that(i.parseIoB("1.234U")).isEqualTo(1.234);
        assertWithMessage("locale de").that(i.parseIoB("1,234 IE")).isEqualTo(1.234);
        assertWithMessage("locale pl").that(i.parseIoB("1,234 j")).isEqualTo(1.234);
        assertWithMessage("locale sk").that(i.parseIoB("1,234 j.")).isEqualTo(1.234);
        assertWithMessage("locale ru").that(i.parseIoB("1,234 Ед.")).isEqualTo(1.234);
        assertWithMessage("locale uk").that(i.parseIoB("1,234 Од.")).isEqualTo(1.234);
        assertWithMessage("locale bg").that(i.parseIoB("1,234 единици")).isEqualTo(1.234);
        assertWithMessage("locale iw").that(i.parseIoB("1.234 \u05D9\u05D7'")).isEqualTo(1.234);  // 1.234 יח'
        assertWithMessage("locale ar 1").that(i.parseIoB("1.234 \u0648")).isEqualTo(1.234);  // 1.234 و
        assertWithMessage("locale ar 2").that(i.parseIoB("\u0661\u066B\u0662\u0663\u0664 \u0648")).isEqualTo(1.234);  // ١٫٢٣٤ و

        // Some invalid variants
        assertWithMessage("missing unit").that(i.parseIoB("1.234")).isNull();
        assertWithMessage("unexpected prefix").that(i.parseIoB("Active Insulin 1.234 U")).isNull();
        assertWithMessage("invalid unit").that(i.parseIoB("1.234 X")).isNull();
        assertWithMessage("no accidental wildcard").that(i.parseIoB("1,234 jx")).isNull();
        assertWithMessage("missing space").that(i.parseIoB("1,234IE")).isNull();
        assertWithMessage("integer value").that(i.parseIoB("5")).isNull();
    }

    // standard 5 minute apart readings are all accepted
    @Test
    public void deDupeTest1() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        for (int i = 0; i < 50; i++) {
            val ts = start + (Constants.SECOND_IN_MS * 300 * i);
            val mgdl = i + 100;
            val result = ui.handleNewValue(ts, mgdl);
            assertWithMessage("deDupeTest1 ts: " + ts + " mgdl: " + mgdl).that(result).isTrue();
        }
    }

    // differing 1 minute apart readings are all accepted
    @Test
    public void deDupeTest2() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        for (int i = 0; i < 50; i++) {
            val ts = start + (Constants.SECOND_IN_MS * 60 * i);
            val mgdl = i + 100;
            val result = ui.handleNewValue(ts, mgdl);
            assertWithMessage("deDupeTest2 ts: " + ts + " mgdl: " + mgdl).that(result).isTrue();
        }
    }

    // differing readings 5 seconds apart are rejected
    @Test
    public void deDupeTest3() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        assertWithMessage("deDupeTest3 A ts: ")
                .that(ui.handleNewValue(start + (Constants.SECOND_IN_MS * 5 * 0), 100)).isTrue();
        assertWithMessage("deDupeTest3 B ts: ")
                .that(ui.handleNewValue(start + (Constants.SECOND_IN_MS * 5 * 1), 101)).isFalse();
    }

    // differing readings 15 seconds apart are accepted
    @Test
    public void deDupeTest4() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        assertWithMessage("deDupeTest4 A ts: ")
                .that(ui.handleNewValue(start + (Constants.SECOND_IN_MS * 15 * 0), 100)).isTrue();
        assertWithMessage("deDupeTest4 B ts: ")
                .that(ui.handleNewValue(start + (Constants.SECOND_IN_MS * 15 * 1), 101)).isTrue();
    }

    // same readings 2 minutes apart are rejected
    @Test
    public void deDupeTest5() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        assertWithMessage("deDupeTest5 A ts: ")
                .that(ui.handleNewValue(start + (Constants.MINUTE_IN_MS * 2 * 0), 100)).isTrue();
        assertWithMessage("deDupeTest5 B ts: ")
                .that(ui.handleNewValue(start + (Constants.MINUTE_IN_MS * 2 * 1), 100)).isFalse();
    }

    // same readings 4 minutes apart are rejected
    @Test
    public void deDupeTest6() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        assertWithMessage("deDupeTest5 A ts: ")
                .that(ui.handleNewValue(start + (Constants.MINUTE_IN_MS * 4 * 0), 100)).isTrue();
        assertWithMessage("deDupeTest5 B ts: ")
                .that(ui.handleNewValue(start + (Constants.MINUTE_IN_MS * 4 * 1), 100)).isFalse();
    }


    // same readings 5 minutes apart are allowed
    @Test
    public void deDupeTest7() {
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();
        assertWithMessage("deDupeTest5 A ts: ")
                .that(ui.handleNewValue(start + (Constants.MINUTE_IN_MS * 5 * 0), 100)).isTrue();
        assertWithMessage("deDupeTest5 B ts: ")
                .that(ui.handleNewValue(start + (Constants.MINUTE_IN_MS * 5 * 1), 100)).isTrue();
    }

    /**
     * Characterization: different values 5 minutes apart should always be accepted.
     * Exercises the normal case where the sensor provides fresh, unique readings
     * every 5 minutes.
     */
    @Test
    public void deDupeTest_differentValues5minApart_accepted() {
        // :: Setup
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();

        // :: Act & Verify
        assertWithMessage("first reading accepted")
                .that(ui.handleNewValue(start, 100)).isTrue();
        assertWithMessage("different value 5 min later accepted")
                .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 5, 110)).isTrue();
        assertWithMessage("different value 10 min later accepted")
                .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 10, 120)).isTrue();
    }

    /**
     * Characterization: same value 10 minutes apart should be accepted.
     * When the sensor genuinely reads the same value after a full update cycle,
     * it should be recorded.
     */
    @Test
    public void deDupeTest_sameValue10minApart_accepted() {
        // :: Setup
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();

        // :: Act & Verify
        assertWithMessage("first reading accepted")
                .that(ui.handleNewValue(start, 100)).isTrue();
        assertWithMessage("same value 10 min later accepted")
                .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 10, 100)).isTrue();
    }

    /**
     * Characterization: alternating values at 5-min intervals all accepted.
     * e.g. 100, 110, 100 — each differs from its predecessor.
     */
    @Test
    public void deDupeTest_alternatingValues_allAccepted() {
        // :: Setup
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        val start = JoH.tsl();

        // :: Act & Verify
        assertWithMessage("reading 1 accepted")
                .that(ui.handleNewValue(start, 100)).isTrue();
        assertWithMessage("different reading 2 accepted")
                .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 5, 110)).isTrue();
        assertWithMessage("back to original reading 3 accepted")
                .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 10, 100)).isTrue();
    }

    /**
     * Characterization: jam detection rejects after Medtronic threshold (9 repeats).
     * Readings spaced at 10-min intervals to pass dedup but increment jam counter.
     */
    @Test
    public void deDupeTest_jamDetection_rejectsExcessiveRepeats() {
        // :: Setup
        BgReading.deleteALL();
        val ui = new UiBasedCollector();
        ui.lastPackage = "com.medtronic.diabetes.guardian";
        val start = JoH.tsl();

        // :: Act - insert same value 10 times at 10-min intervals
        for (int i = 0; i <= 9; i++) {
            assertWithMessage("reading " + i + " accepted")
                    .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 10 * i, 100)).isTrue();
        }

        // :: Verify - 11th same value rejected by jam detection
        assertWithMessage("11th identical value rejected by jam detection")
                .that(ui.handleNewValue(start + Constants.MINUTE_IN_MS * 10 * 10, 100)).isFalse();
    }

}
