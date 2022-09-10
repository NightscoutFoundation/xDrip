package com.eveningoutpost.dexdrip.Services;

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
        assertWithMessage("null test pass through 1").that(i.filterString(spec1)).isEqualTo(spec1);
        assertWithMessage("null test pass through 2").that(i.filterString(spec2)).isEqualTo(spec2);
        assertWithMessage("null test pass through 3").that(i.filterString(spec3)).isEqualTo(spec3);
        i.lastPackage = "hello world";
        assertWithMessage("non 1").that(i.filterString(spec1)).isEqualTo(spec1);
        assertWithMessage("gte 1").that(i.filterString(spec2)).isEqualTo(spec2p);
        assertWithMessage("lte 1").that(i.filterString(spec3)).isEqualTo(spec3p);
    }
}