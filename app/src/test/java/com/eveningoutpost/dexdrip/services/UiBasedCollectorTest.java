package com.eveningoutpost.dexdrip.services;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import lombok.val;

public class UiBasedCollectorTest {

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
}