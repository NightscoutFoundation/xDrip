package com.eveningoutpost.dexdrip;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Abstract config and setup for tests.
 *
 * Starts ActiveAndroid and initiates xdrip with appContext.
 *
 * @author jamorham on 01/10/2017
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.03
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = BuildConfig.targetSDK,
        packageName = "com.eveningoutpost.dexdrip",
        application = TestingApplication.class
)
public abstract class RobolectricTestWithConfig {

    @Before
    public void setUp() {
        xdrip.checkAppContext(RuntimeEnvironment.application);
    }
}
