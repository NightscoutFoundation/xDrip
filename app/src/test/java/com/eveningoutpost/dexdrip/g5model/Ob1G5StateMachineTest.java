package com.eveningoutpost.dexdrip.g5model;

import android.bluetooth.BluetoothGattCharacteristic;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Ob1G5StateMachine#handleAuthenticationThrowable}.
 *
 * @author Asbjørn Aarrestad
 */
public class Ob1G5StateMachineTest extends RobolectricTestWithConfig {

    private Ob1G5CollectionService parent;

    @Before
    public void setUpParent() {
        parent = mock(Ob1G5CollectionService.class);
    }

    // ================ handleAuthenticationThrowable — BleCannotSetCharacteristicNotificationException ================

    @Test
    public void handleAuthThrowable_notificationException_incrementsErrors() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        BleCannotSetCharacteristicNotificationException exception =
                new BleCannotSetCharacteristicNotificationException(
                        mock(BluetoothGattCharacteristic.class));

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).incrementErrors();
    }

    @Test
    public void handleAuthThrowable_notificationException_triesGattRefresh() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        BleCannotSetCharacteristicNotificationException exception =
                new BleCannotSetCharacteristicNotificationException(
                        mock(BluetoothGattCharacteristic.class));

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).tryGattRefresh();
    }

    @Test
    public void handleAuthThrowable_notificationException_changesStateToScan() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        BleCannotSetCharacteristicNotificationException exception =
                new BleCannotSetCharacteristicNotificationException(
                        mock(BluetoothGattCharacteristic.class));

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).changeState(Ob1G5CollectionService.STATE.SCAN);
    }

    // ======================== handleAuthenticationThrowable — BleGattCharacteristicException =========================

    @Test
    public void handleAuthThrowable_gattCharacteristicException_changesStateToScan() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        BleGattCharacteristicException exception = mock(BleGattCharacteristicException.class);

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).incrementErrors();
        verify(parent).tryGattRefresh();
        verify(parent).changeState(Ob1G5CollectionService.STATE.SCAN);
    }

    // =================== handleAuthenticationThrowable — BleDisconnectedException in CLOSED state ====================

    @Test
    public void handleAuthThrowable_disconnectInClosedState_callsConnectionStateChange() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CLOSED);
        BleDisconnectedException exception = new BleDisconnectedException("test-mac", 19);

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).connectionStateChange(Ob1G5StateMachine.CLOSED_OK_TEXT);
    }

    @Test
    public void handleAuthThrowable_disconnectInClosedState_doesNotIncrementErrors() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CLOSED);
        BleDisconnectedException exception = new BleDisconnectedException("test-mac", 19);

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent, never()).incrementErrors();
    }

    // =============================== handleAuthenticationThrowable — generic throwable ===============================

    @Test
    public void handleAuthThrowable_genericException_incrementsErrors() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        RuntimeException exception = new RuntimeException("test error");

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).incrementErrors();
    }

    @Test
    public void handleAuthThrowable_genericException_doesNotTryGattRefreshOrChangeState() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        RuntimeException exception = new RuntimeException("test error");

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent, never()).tryGattRefresh();
        verify(parent, never()).changeState(Ob1G5CollectionService.STATE.SCAN);
    }

    // ========================= handleAuthenticationThrowable — Part A: preScanFailureMarker ==========================

    @Test
    public void handleAuthThrowable_notificationException_setsPreScanFailureMarker() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        BleCannotSetCharacteristicNotificationException exception =
                new BleCannotSetCharacteristicNotificationException(
                        mock(BluetoothGattCharacteristic.class));

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).setPreScanFailureMarker();
    }

    @Test
    public void handleAuthThrowable_gattCharException_setsPreScanFailureMarker() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        BleGattCharacteristicException exception = mock(BleGattCharacteristicException.class);

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).setPreScanFailureMarker();
    }

    // ==================== handleAuthenticationThrowable — Part C: BT restart on repeated failures ====================

    @Test
    public void handleAuthThrowable_notificationException_withHighErrorCount_requestsBtRestart() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        when(parent.getErrorCount()).thenReturn(4);
        BleCannotSetCharacteristicNotificationException exception =
                new BleCannotSetCharacteristicNotificationException(
                        mock(BluetoothGattCharacteristic.class));

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent).requestBluetoothRestart();
    }

    @Test
    public void handleAuthThrowable_notificationException_withLowErrorCount_doesNotRequestBtRestart() {
        // :: Setup
        when(parent.getState()).thenReturn(Ob1G5CollectionService.STATE.CHECK_AUTH);
        when(parent.getErrorCount()).thenReturn(2);
        BleCannotSetCharacteristicNotificationException exception =
                new BleCannotSetCharacteristicNotificationException(
                        mock(BluetoothGattCharacteristic.class));

        // :: Act
        Ob1G5StateMachine.handleAuthenticationThrowable(exception, parent);

        // :: Verify
        verify(parent, never()).requestBluetoothRestart();
    }
}
