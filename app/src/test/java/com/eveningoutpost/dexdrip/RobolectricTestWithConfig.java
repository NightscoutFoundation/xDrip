package com.eveningoutpost.dexdrip;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Abstract test config class to handle the different config for Android studio and CI builds.
 * Used for tests requiring MockModel framework to be active.
 *
 * To work for local tests build, uncomment the first @Config line and comment the second.
 *
 * If you commit this file with the wrong config active, the build will not pass CI.
 *
 * @author jamorham on 01/10/2017
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.03
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        packageName = "com.eveningoutpost.dexdrip",
        application = TestingApplication.class
)
public abstract class RobolectricTestWithConfig {

    @Before
    public void setUp() {
        xdrip.checkAppContext(RuntimeEnvironment.application);
    }
}
