package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jamorham on 22/01/2018.
 */

public class PrefTest extends RobolectricTestWithConfig {

    @Test
    public void test_PrefTest() {

        // test getStringToInt

        assertThat("empty defaults", Pref.getStringToInt("text-integer", 300), is(300));

        Pref.setString("text-integer", "123");
        assertThat("set correctly", Pref.getStringToInt("text-integer", 300), is(123));

        Pref.setString("text-integer", "ouch");
        assertThat("bad string defaults", Pref.getStringToInt("text-integer", 300), is(300));

        Pref.setString("text-integer", "");
        assertThat("empty string defaults", Pref.getStringToInt("text-integer", 300), is(300));

    }

}
