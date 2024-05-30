package com.eveningoutpost.dexdrip.services;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.Test;

import lombok.val;

public class UiBasedCollectorTest extends RobolectricTestWithConfig {

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
    public void parseIoBTest() {
        val i = new UiBasedCollector();

        val valid = "Automated Mode (IOB: 5.1 U)";
        val invalid = "Foobar";

        assertWithMessage("valid IoB message").that(i.parseIoB(valid)).isEqualTo(5.1);
        assertWithMessage("invalid IoB message").that(i.parseIoB(invalid)).isNull();
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



}