package com.eveningoutpost.dexdrip.services;

import android.content.Intent;

import com.eveningoutpost.dexdrip.RefusingContext;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Covers {@link BluetoothGlucoseMeter#start_service} when Android refuses the start.
 * <p>
 * This runs from {@code xdrip.onCreate} for anyone with a Bluetooth meter paired, four lines after
 * the sync service, and it is the last unguarded synchronous service start in that block. At target
 * SDK 26 a process started only to deliver a broadcast may not start it, and an unguarded refusal
 * here takes the process down. The meter connects as usual the next time the app comes up itself.
 *
 * @author Asbjørn Aarrestad
 */
public class BluetoothGlucoseMeterStartTest extends RobolectricTestWithConfig {

    private RefusingContext refusing;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        refusing = new RefusingContext(RuntimeEnvironment.application);
        xdrip.setContextAlways(refusing);
    }

    @After
    public void tearDown() {
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    // ============================================ start_service =============================================

    /** A start for a known meter carries the connect address and the refusal stays inside the method. */
    @Test
    public void startServiceAttemptsTheConnectAndSwallowsTheRefusal() {
        // :: Act
        BluetoothGlucoseMeter.start_service("AA:BB:CC:DD:EE:FF");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(1);
        final Intent attempted = refusing.attempted().get(0);
        assertWithMessage("target service")
                .that(RefusingContext.targetClassOf(attempted))
                .isEqualTo(BluetoothGlucoseMeter.class.getName());
        assertWithMessage("service action").that(attempted.getStringExtra("service_action")).isEqualTo("connect");
        assertWithMessage("connect address")
                .that(attempted.getStringExtra("connect_address"))
                .isEqualTo("AA:BB:CC:DD:EE:FF");
    }

    /** With no address to connect to the meter is asked to scan, and a refusal is still swallowed. */
    @Test
    public void startServiceAttemptsAScanWhenNoAddressIsGiven() {
        // :: Act
        BluetoothGlucoseMeter.start_service("");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(1);
        assertWithMessage("service action")
                .that(refusing.attempted().get(0).getStringExtra("service_action"))
                .isEqualTo("scan");
    }

    /** A refused start must not stop the caller from getting to its next statement. */
    @Test
    public void aRefusedStartDoesNotStopTheNextOne() {
        // :: Act
        BluetoothGlucoseMeter.start_service("AA:BB:CC:DD:EE:FF");
        BluetoothGlucoseMeter.start_service("AA:BB:CC:DD:EE:FF");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(2);
    }
}
