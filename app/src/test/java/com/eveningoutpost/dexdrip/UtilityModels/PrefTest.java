package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jamorham on 22/01/2018.
 */


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PrefTest {

    @Before
    public void setUp() throws Exception {
        xdrip.checkAppContext(RuntimeEnvironment.application);
    }

    @Test
    public void test_PrefTest() throws Exception {

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
