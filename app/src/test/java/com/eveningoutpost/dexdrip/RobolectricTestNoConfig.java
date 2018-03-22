package com.eveningoutpost.dexdrip;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Abstract config class for tests that can run with "NoConfig", as opposed to "WithConfig" in
 * {@link RobolectricTestWithConfig}.
 *
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.03
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public abstract class RobolectricTestNoConfig {
}
