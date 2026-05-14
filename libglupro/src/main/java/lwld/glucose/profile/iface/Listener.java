package lwld.glucose.profile.iface;

import android.bluetooth.BluetoothDevice;

import java.util.List;
import java.util.Map;

/**
 * JamOrHam
 * <p>
 * Listener interface
 */

public interface Listener {
    void onConnected(BluetoothDevice device);

    void onDisconnected(BluetoothDevice device, int reason);

    void onReconnecting(int attempt);

    void onData(Map<DataKey, String> data);

    void onScan(List<Device> scannedDevices);

    void onState(State state);

    void onError(String message);
}
