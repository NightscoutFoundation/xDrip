package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.g5model.DexSyncKeeper;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link Ob1G5CollectionService} error tracking and scan strategy.
 *
 * @author Asbjørn Aarrestad
 */
public class Ob1G5CollectionServiceTest extends RobolectricTestWithConfig {

    private Ob1G5CollectionService service;

    @Before
    public void setUpService() throws Exception {
        service = new Ob1G5CollectionService();
        service.clearErrors();
        setStaticField("transmitterIDmatchingMAC", "");
        setStaticField("preScanFailureMarker", false);
        setStaticField("lastConnectFailed", false);
        setStaticField("connectNowFailures", 0);
        setStaticField("scanTimeouts", 0);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = Ob1G5CollectionService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    // ================================= incrementErrors / clearErrors / getErrorCount =================================

    @Test
    public void incrementErrors_increasesCount() {
        // :: Act
        service.incrementErrors();

        // :: Verify
        assertThat(service.getErrorCount()).isEqualTo(1);
    }

    @Test
    public void incrementErrors_calledMultipleTimes_accumulatesCount() {
        // :: Act
        service.incrementErrors();
        service.incrementErrors();
        service.incrementErrors();

        // :: Verify
        assertThat(service.getErrorCount()).isEqualTo(3);
    }

    @Test
    public void clearErrors_resetsCountToZero() {
        // :: Setup
        service.incrementErrors();
        service.incrementErrors();

        // :: Act
        service.clearErrors();

        // :: Verify
        assertThat(service.getErrorCount()).isEqualTo(0);
    }

    // ========================================== useMinimizeScanningStrategy ==========================================

    @Test
    public void useMinimizeScanningStrategy_returnsFalse_whenTransmitterMacIsNull() throws Exception {
        // :: Setup
        setStaticField("minimize_scanning", true);
        setStaticField("transmitterMAC", null);

        // :: Act
        boolean result = service.useMinimizeScanningStrategy();

        // :: Verify
        assertThat(result).isFalse();
    }

    @Test
    public void useMinimizeScanningStrategy_returnsFalse_whenMinimizeScanningDisabled() throws Exception {
        // :: Setup
        setStaticField("minimize_scanning", false);
        setStaticField("transmitterMAC", "AA:BB:CC:DD:EE:FF");

        // :: Act
        boolean result = service.useMinimizeScanningStrategy();

        // :: Verify
        assertThat(result).isFalse();
    }

    @Test
    public void useMinimizeScanningStrategy_returnsTrue_whenAllConditionsMet() throws Exception {
        // :: Setup
        setStaticField("minimize_scanning", true);
        setStaticField("transmitterMAC", "AA:BB:CC:DD:EE:FF");
        setStaticField("transmitterID", "ABCD");
        setStaticField("preScanFailureMarker", false);
        setStaticField("lastConnectFailed", false);
        DexSyncKeeper.store("ABCD");

        // :: Act
        boolean result = service.useMinimizeScanningStrategy();

        // :: Verify
        assertThat(result).isTrue();
    }

    @Test
    public void useMinimizeScanningStrategy_returnsFalse_whenLastConnectFailedAndNoOverride() throws Exception {
        // :: Setup
        setStaticField("minimize_scanning", true);
        setStaticField("transmitterMAC", "AA:BB:CC:DD:EE:FF");
        setStaticField("transmitterID", "ABCD");
        setStaticField("preScanFailureMarker", false);
        setStaticField("lastConnectFailed", true);
        setStaticField("connectNowFailures", 0);
        setStaticField("scanTimeouts", 0);
        DexSyncKeeper.store("ABCD");

        // :: Act
        boolean result = service.useMinimizeScanningStrategy();

        // :: Verify
        assertThat(result).isFalse();
    }
}
