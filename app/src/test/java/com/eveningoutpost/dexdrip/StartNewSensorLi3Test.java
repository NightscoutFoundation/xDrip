package com.eveningoutpost.dexdrip;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import com.eveningoutpost.dexdrip.cgm.li3.Li3Activity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for {@link StartNewSensor} with the Li3 collector: a Li3 sensor is started by scanning it on the Li3 screen,
 * so the start sensor screen sends the user there instead of starting a sensor by date and time.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class StartNewSensorLi3Test extends RobolectricTestWithConfig {

    private DexCollectionType sourceBefore;

    @Before
    public void setUp() {
        xdrip.setContextAlways(RuntimeEnvironment.application);
        sourceBefore = DexCollectionType.getDexCollectionType();
        Sensor.stopSensor();
    }

    @After
    public void tearDown() {
        DexCollectionType.setDexCollectionType(sourceBefore);
    }

    // ================================================== Start sensor =================================================

    /** With Li3 and no sensor, the start sensor screen opens the Li3 screen and closes itself. */
    @Test
    public void onCreate_li3NoSensor_opensLi3Screen() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);

        // :: Act
        StartNewSensor screen = Robolectric.buildActivity(StartNewSensor.class).create().get();

        // :: Verify
        Intent opened = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(Li3Activity.class.getName());
        assertThat(screen.isFinishing()).isTrue();
        assertThat(Sensor.isActive()).isFalse();
    }

    /** With Li3 and a sensor already running, the screen goes to stopping it, as for every other collector. */
    @Test
    public void onCreate_li3SensorActive_opensStopSensor() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
        Sensor.create(JoH.tsl());

        // :: Act
        Robolectric.buildActivity(StartNewSensor.class).create();

        // :: Verify
        Intent opened = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(StopSensor.class.getName());
    }

    /** Another collector still gets the start sensor screen itself. */
    @Test
    public void onCreate_gluProNoSensor_staysOpen() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.GluPro);

        // :: Act
        StartNewSensor screen = Robolectric.buildActivity(StartNewSensor.class).create().get();

        // :: Verify
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedActivity()).isNull();
        assertThat(screen.isFinishing()).isFalse();
    }
}
