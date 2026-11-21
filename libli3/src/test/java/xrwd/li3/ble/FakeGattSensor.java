package xrwd.li3.ble;

import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;

import org.robolectric.shadows.ShadowBluetoothGatt;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

import xrwd.li3.pairing.FakeSensor;

/**
 * Puts a {@link FakeSensor} behind Robolectric's Bluetooth device at one address. Each connect gets a gatt with the
 * Li3 service; writes reach the fake sensor and its notifies come back as characteristic changes. Robolectric
 * calls the gatt callback on the calling thread, so every callback is posted to the main looper, where the Android
 * stack would deliver it.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
final class FakeGattSensor {

    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final List<UUID> CHARACTERISTICS = Arrays.asList(Gatt.SEC_CERT_DATA, Gatt.SEC_CHALLENGE_DATA,
            Gatt.SEC_COMMAND_RESPONSE, Gatt.PATCH_CONTROL, Gatt.EVENT_LOG, Gatt.FACTORY_DATA, Gatt.PATCH_STATUS,
            Gatt.GLUCOSE_DATA, Gatt.HISTORIC_DATA, Gatt.CLINICAL_DATA);

    private final FakeSensor sensor;
    private final Handler main = new Handler(Looper.getMainLooper());

    private volatile BluetoothGatt gatt;
    private volatile Relay relay;
    private volatile int connections;
    /** The first thing that went wrong inside the fake sensor, shown when a test times out. */
    volatile Throwable failure;
    /** A characteristic whose writes the sensor never answers; null for none. */
    volatile UUID unansweredWrites;
    /** A characteristic whose notifications the sensor never confirms enabling; null for none. */
    volatile UUID unconfirmedNotifications;
    /** How many writes and notification requests the sensor has left unanswered so far. */
    volatile int unanswered;
    /** Every frame the phone wrote to patch control, as sent, oldest first. */
    final List<byte[]> patchControlWrites = new CopyOnWriteArrayList<>();

    FakeGattSensor(String address, FakeSensor sensor) {
        this.sensor = sensor;
        shadowOf(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address))
                .setGattConnectionInterceptor(this::onNewGatt);
    }

    /** @return how many times the phone connected */
    int connections() {
        return connections;
    }

    /** Sends a notify on a data-plane characteristic of the current connection. */
    void notifyData(UUID characteristic, byte[] value) {
        BluetoothGatt current = gatt;
        Relay callback = relay;
        BluetoothGattCharacteristic target = current.getService(Gatt.SERVICE).getCharacteristic(characteristic);
        deliver(current, () -> {
            target.setValue(value);
            callback.delegate.onCharacteristicChanged(current, target);
        });
    }

    /** Drops the current connection from the sensor's side, with a GATT status such as 8 for a supervision timeout. */
    void drop(int status) {
        BluetoothGatt current = gatt;
        Relay callback = relay;
        deliver(current, () -> callback.delegate.onConnectionStateChange(current, status,
                BluetoothProfile.STATE_DISCONNECTED));
    }

    private void onNewGatt(BluetoothGatt newGatt) {
        ShadowBluetoothGatt shadow = shadowOf(newGatt);
        BluetoothGattService service = new BluetoothGattService(Gatt.SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for (UUID uuid : CHARACTERISTICS) {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid,
                    BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            characteristic.addDescriptor(new BluetoothGattDescriptor(CCCD, BluetoothGattDescriptor.PERMISSION_WRITE));
            service.addCharacteristic(characteristic);
            shadow.allowCharacteristicNotification(characteristic);
        }
        shadow.addDiscoverableService(service);
        Relay newRelay = new Relay(shadow.getGattCallback());
        shadow.setGattCallback(newRelay);
        gatt = newGatt;
        relay = newRelay;
        connections++;
        newGatt.connect();
    }

    /**
     * Posts a callback to the main looper, and drops it if the gatt is closed by the time it runs, as Android does once
     * the app has unregistered.
     */
    private void deliver(BluetoothGatt target, Runnable callback) {
        main.post(() -> {
            if (!shadowOf(target).isClosed()) {
                callback.run();
            }
        });
    }

    /** Posts each callback to the main looper and plays the sensor's side of every write. */
    private final class Relay extends BluetoothGattCallback {

        final BluetoothGattCallback delegate;

        Relay(BluetoothGattCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            deliver(gatt, () -> delegate.onConnectionStateChange(gatt, status, newState));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            deliver(gatt, () -> delegate.onServicesDiscovered(gatt, status));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getCharacteristic().getUuid().equals(unconfirmedNotifications)) {
                unanswered++;
                return;
            }
            deliver(gatt, () -> delegate.onDescriptorWrite(gatt, descriptor, status));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (characteristic.getUuid().equals(unansweredWrites)) {
                unanswered++;
                return;
            }
            byte[] value = characteristic.getValue().clone();
            if (characteristic.getUuid().equals(Gatt.PATCH_CONTROL)) {
                patchControlWrites.add(value);
            }
            deliver(gatt, () -> delegate.onCharacteristicWrite(gatt, characteristic, status));
            if (!Gatt.HANDSHAKE_NOTIFYING.contains(characteristic.getUuid())) {
                return; // the fake sensor plays only the handshake
            }
            try {
                sensor.write(characteristic.getUuid(), value);
            } catch (Throwable e) {
                if (failure == null) {
                    failure = e;
                }
            }
            BluetoothGattService service = gatt.getService(Gatt.SERVICE);
            for (UUID uuid : Gatt.HANDSHAKE_NOTIFYING) {
                BluetoothGattCharacteristic target = service.getCharacteristic(uuid);
                for (byte[] notify : sensor.takeNotifies(uuid)) {
                    deliver(gatt, () -> {
                        target.setValue(notify);
                        delegate.onCharacteristicChanged(gatt, target);
                    });
                }
            }
        }
    }
}
