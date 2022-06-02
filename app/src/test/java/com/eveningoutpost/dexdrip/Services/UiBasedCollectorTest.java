package com.eveningoutpost.dexdrip.Services;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

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
}