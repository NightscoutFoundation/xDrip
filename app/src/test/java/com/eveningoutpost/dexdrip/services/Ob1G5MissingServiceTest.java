package com.eveningoutpost.dexdrip.services;

import android.bluetooth.BluetoothGattService;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.g5model.BluetoothServices;
import com.eveningoutpost.dexdrip.models.JoH;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;

import io.reactivex.disposables.Disposables;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CHECK_AUTH;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CONNECT_NOW;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** A discovery without the CGM service reconnects once, right away. No real BLE hardware. */
public class Ob1G5MissingServiceTest extends RobolectricTestWithConfig {
    private Ob1G5CollectionService collector;

    @Before
    public void prepareCollector() throws Exception {
        Field field = Ob1G5CollectionService.class.getDeclaredField("transmitterID");
        field.setAccessible(true);
        field.set(null, "1234"); // synthetic pairing code
        JoH.clearRatelimit("ob1-missing-service-reconnect");
        collector = spy(new Ob1G5CollectionService());
        doNothing().when(collector).changeState(any(Ob1G5CollectionService.STATE.class));
        doNothing().when(collector).changeState(any(Ob1G5CollectionService.STATE.class), anyInt());
    }

    private void discovered(BluetoothGattService... services) throws Exception {
        Method method = Ob1G5CollectionService.class.getDeclaredMethod("onServicesDiscovered", RxBleDeviceServices.class);
        method.setAccessible(true);
        method.invoke(collector, new RxBleDeviceServices(Arrays.asList(services)));
    }

    @Test
    public void emptyDiscoveryReconnectsOnceRightAway() throws Exception {
        assertTrue(JoH.ratelimit("g5-to-discover", 1));

        discovered();

        verify(collector).changeState(CONNECT_NOW, 200);
        // the reconnect must be allowed to go on to discovery even within a second of the first connection
        assertTrue(JoH.ratelimit("g5-to-discover", 1));
    }

    @Test
    public void secondEmptyDiscoveryInTheSameWakeUpDoesNotReconnectAgain() throws Exception {
        discovered();
        discovered();

        verify(collector, times(1)).changeState(CONNECT_NOW, 200);
    }

    @Test
    public void legacyTransmitterDoesNotReconnectOrConsumeTheG7Limit() throws Exception {
        Field field = Ob1G5CollectionService.class.getDeclaredField("transmitterID");
        field.setAccessible(true);
        field.set(null, "ABC123");

        discovered();

        verify(collector, never()).changeState(any(Ob1G5CollectionService.STATE.class), anyInt());
        assertTrue(JoH.ratelimit("ob1-missing-service-reconnect", 60));
    }

    @Test
    public void disposesDiscoveryConnectionAndStateObserverBeforeSchedulingReconnect() throws Exception {
        Subscription discovery = new Subscription(Disposables.empty());
        Subscription connection = new Subscription(Disposables.empty());
        Subscription observer = new Subscription(Disposables.empty());
        for (String name : new String[]{"discoverSubscription", "connectionSubscription", "stateSubscription"}) {
            Field field = Ob1G5CollectionService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(collector, name.equals("discoverSubscription") ? discovery
                    : name.equals("connectionSubscription") ? connection : observer);
        }
        doAnswer(invocation -> {
            assertTrue(discovery.isDisposed());
            assertTrue(connection.isDisposed());
            assertTrue(observer.isDisposed());
            return null;
        }).when(collector).changeState(CONNECT_NOW, 200);

        discovered();

        verify(collector).changeState(CONNECT_NOW, 200);
    }

    @Test
    public void nonEmptyDiscoveryMissingTheCgmServiceAlsoReconnects() throws Exception {
        discovered(new BluetoothGattService(java.util.UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"),
                BluetoothGattService.SERVICE_TYPE_PRIMARY));

        verify(collector).changeState(CONNECT_NOW, 200);
    }

    @Test
    public void discoveryWithTheCgmServiceDoesNotReconnect() throws Exception {
        discovered(new BluetoothGattService(BluetoothServices.CGMService, BluetoothGattService.SERVICE_TYPE_PRIMARY));

        verify(collector).changeState(CHECK_AUTH);
        verify(collector, never()).changeState(any(Ob1G5CollectionService.STATE.class), anyInt());
    }
}
