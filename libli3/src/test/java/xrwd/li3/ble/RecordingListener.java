package xrwd.li3.ble;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import xrwd.li3.Li3SensorState;
import xrwd.li3.data.HistoricalPage;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;

/**
 * Keeps the stages, errors, statuses, readings and history the collector would get. Safe to read from the test while
 * the worker and the main thread write to it.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
class RecordingListener implements Li3Ble.Listener {

    final List<String> stages = new CopyOnWriteArrayList<>();
    final List<String> errors = new CopyOnWriteArrayList<>();
    final List<PatchStatus> statuses = new CopyOnWriteArrayList<>();
    final List<RealtimeReading> readings = new CopyOnWriteArrayList<>();
    final List<HistoricalPage.Entry> history = new CopyOnWriteArrayList<>();
    volatile int bluetoothOff;

    @Override
    public void onStage(String stage) {
        stages.add(stage);
    }

    @Override
    public void onPaired(Li3SensorState state) {
    }

    @Override
    public void onReading(RealtimeReading reading) {
        readings.add(reading);
    }

    @Override
    public void onStatus(PatchStatus status) {
        statuses.add(status);
    }

    @Override
    public void onHistory(List<HistoricalPage.Entry> entries) {
        history.addAll(entries);
    }

    @Override
    public void onError(String message) {
        errors.add(message);
    }

    @Override
    public void onBluetoothOff() {
        bluetoothOff++;
    }
}
