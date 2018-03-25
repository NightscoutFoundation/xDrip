package com.eveningoutpost.dexdrip;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
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


// use this config inside android studio 3
//@Config(constants = BuildConfig.class, manifest = "../../../../app/src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml")

// use this config for CI test hosts
@Config(constants = BuildConfig.class, manifest = "../../../../../src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml")
public abstract class RobolectricTestWithConfig {
}
