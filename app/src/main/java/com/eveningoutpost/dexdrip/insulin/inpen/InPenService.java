package com.eveningoutpost.dexdrip.insulin.inpen;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.PenData;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.AdvertRx;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.BatteryRx;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.BondTx;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.KeepAliveTx;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.RecordRx;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.RecordTx;
import com.eveningoutpost.dexdrip.insulin.inpen.messages.TimeRx;
import com.eveningoutpost.dexdrip.insulin.shared.ProcessPenData;
import com.eveningoutpost.dexdrip.utils.bt.Helper;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.utils.math.Converters;
import com.eveningoutpost.dexdrip.utils.time.SlidingWindowConstraint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;
import com.polidea.rxandroidble2.exceptions.BleGattException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

//import rx.schedulers.Schedulers;

import io.reactivex.schedulers.Schedulers;


import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump.dumpHexString;
import static com.eveningoutpost.dexdrip.models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.hourMinuteString;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.quietratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.ratelimit;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.INPEN_SERVICE_FAILOVER_ID;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.AUTHENTICATION;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.BATTERY;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.BONDCONTROL;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.HEXDUMP_INFO_CHARACTERISTICS;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.INFO_CHARACTERISTICS;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.KEEPALIVE;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.PEN_ATTACH_TIME;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.PEN_TIME;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.PRINTABLE_INFO_CHARACTERISTICS;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.RECORD_END;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.RECORD_INDEX;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.RECORD_INDICATE;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.RECORD_REQUEST;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.RECORD_START;
import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.REMAINING_INDEX;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPen.DEFAULT_BOND_UNITS;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPen.STORE_INPEN_ADVERT;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPen.STORE_INPEN_BATTERY;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPen.STORE_INPEN_INFOS;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenEntry.ID_INPEN;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenEntry.isStarted;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.BONDAGE;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.BOND_AUTHORITY;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_AUTH_STATE;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_AUTH_STATE2;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_A_TIME;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_BATTERY;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_IDENTITY;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_INDEX;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_RECORDS;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.GET_TIME;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenService.InPenState.KEEP_ALIVE;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.getCharactersticName;

/** jamorham
 *
 * InPen connection and data transfer service
 */

public class InPenService extends JamBaseBluetoothSequencer {

    private static final boolean D = false;
    private static final int MAX_BACKLOG = 80;
    private final ConcurrentLinkedQueue<byte[]> records = new ConcurrentLinkedQueue<>();

    private static TimeRx currentPenTime = null; // TODO hashmap for multiple pens?
    private static TimeRx currentPenAttachTime = null; // TODO hashmap for multiple pens?
    private int lastIndex = -1; // TODO hashmap for multiple pens? Use storage object for each pen??
    private int gotIndex = -2; // TODO hashmap for multiple pens? Use storage object for each pen??

    private static volatile String lastState = "None";
    private static volatile String lastError = null;
    private static volatile long lastStateUpdated = -1;
    private static volatile long lastReceivedData = -1;
    private static volatile boolean needsAuthentication = false;
    private static int lastBattery = -1;
    private static PenData lastPenData = null;
    private static boolean infosLoaded = false;
    private static boolean gotAll = false;
    private static ConcurrentHashMap<UUID, Object> staticCharacteristics;
    private static PendingIntent serviceFailoverIntent;
    private static long failover_time;

    {
        mState = new InPenState().setLI(I);
        I.backgroundStepDelay = 0;
        I.autoConnect = true;
        I.autoReConnect = true; // TODO control these two from preference?
        I.playSounds = true;
        I.connectTimeoutMinutes = 25;
        I.reconnectConstraint = new SlidingWindowConstraint(30, MINUTE_IN_MS, "max_reconnections");
        //I.resetWhenAlreadyConnected = true;
    }


    static class InPenState extends JamBaseBluetoothSequencer.BaseState {
        static final String PROTOTYPE = "Prototype Test";
        static final String GET_IDENTITY = "Get Identity";
        static final String KEEP_ALIVE = "Keep Alive";
        static final String BOND_AUTHORITY = "Bond Authority";
        static final String GET_AUTH_STATE = "Get Auth";
        static final String GET_AUTH_STATE2 = "Get Post Auth";
        static final String BONDAGE = "Bonding";
        static final String GET_BATTERY = "Get Battery";
        static final String GET_INDEX = "Get Index";
        static final String GET_TIME = "Get Time";
        static final String GET_A_TIME = "Get Attach Time";
        static final String GET_RECORDS = "Get Records";

        {
            sequence.clear();

            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(DISCOVER); // automatically executed
            sequence.add(GET_IDENTITY);
            sequence.add(GET_A_TIME);
            sequence.add(GET_AUTH_STATE);
            sequence.add(KEEP_ALIVE);
            sequence.add(BOND_AUTHORITY);
            sequence.add(BONDAGE);
            // TODO wait for bonding to be bonded
            sequence.add(GET_AUTH_STATE2);
            sequence.add(SLEEP);
            //
            sequence.add(GET_INDEX);
            sequence.add(GET_TIME);
            sequence.add(GET_BATTERY);
            sequence.add(GET_RECORDS); // tends to close connection after this
            sequence.add(SLEEP);
            //
            sequence.add(PROTOTYPE);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);
            //

        }
    }

    @Override
    protected synchronized boolean automata() {
        extendWakeLock(1000);
        if (D) UserError.Log.d(TAG, "Automata called in InPen");
        msg(I.state);

        switch (I.state) {

            case INIT:
                // connect by default
                changeNextState();
                break;
            case GET_IDENTITY:
                getIdentity(null);
                break;
            case GET_BATTERY:
                getBattery();
                break;
            case GET_A_TIME:
                getAttachTime();
                break;
            case GET_TIME:
                getTime();
                break;
            case GET_RECORDS:
                getRecords();
                break;
            case KEEP_ALIVE:
                keepAlive();
                break;
            case BOND_AUTHORITY:
                bondAuthority();
                break;
            case BONDAGE:
                bondAsRequired(true);
                break;
            case GET_AUTH_STATE:
            case GET_AUTH_STATE2:
                getAuthState();
                break;
            case GET_INDEX:
                getIndex();
                break;
            default:
                if (shouldServiceRun()) {
                    if (msSince(lastReceivedData) < MINUTE_IN_MS) {
                        Inevitable.task("inpen-set-failover", 1000, this::setFailOverTimer);
                    }
                    return super.automata();
                } else {
                    UserError.Log.d(TAG, "Service should be shut down so stopping automata");
                }
        }
        return true; // lies
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("inpen service", 60000);
        try {
            InPenEntry.started_at = JoH.tsl();
            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP");
            if (shouldServiceRun()) {

                final String mac = InPen.getMac(); // load from settings class
                if (emptyString(mac)) {
                    // if mac not set then start up a scan and do nothing else
                    new FindNearby().scan();
                } else {

                    setAddress(mac);
                    commonServiceStart();
                    if (intent != null) {
                        final String function = intent.getStringExtra("function");
                        if (function != null) {
                            switch (function) {

                                case "failover":
                                    changeState(CLOSE);
                                    break;
                                case "reset":
                                    JoH.static_toast_long("Searching for Pen");
                                    InPen.setMac("");
                                    InPenEntry.startWithRefresh();
                                    break;
                                case "refresh":
                                    currentPenAttachTime = null;
                                    currentPenTime = null;
                                    changeState(INIT);
                                    break;
                                case "prototype":
                                    //     changeState(PROTOTYPE);
                                    break;
                            }
                        }
                    }
                }
                setFailOverTimer();
                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void commonServiceStart() {
        I.playSounds = false;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        InPenEntry.started_at = -1;
    }


    ///// Methods

    private void getAuthState() {
        I.connection.readCharacteristic(AUTHENTICATION).subscribe(
                readValue -> {
                    UserError.Log.d(TAG, "Authentication result: " + dumpHexString(readValue));
                    authenticationProcessor(readValue);
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read after Authentication status: " + throwable);
                    changeState(CLOSE);
                });
    }

    private void authenticationProcessor(final byte[] value) {
        if (value == null || value.length < 1 || value[0] != 0) { // "U" = unbonded?
            UserError.Log.d(TAG, "authenticationProcessor: not authenticated: " + bytesToHex(value));
            needsAuthentication = true;
            changeNextState(); // not authenticated
        } else {
            UserError.Log.d(TAG, "authenticationProcessor: we are authenticated: " + bytesToHex(value));
            // we are authenticated!
            needsAuthentication = false;
            changeState(GET_INDEX);
        }
    }

    // TODO make sure we don't get stuck on a bum record
    private boolean checkMissingIndex() {
        final long missingIndex = PenData.getMissingIndex(I.address);
        if (missingIndex != -1) {
            UserError.Log.d(TAG, "Index: " + missingIndex + " is missing");
            getRecords((int) missingIndex, (int) missingIndex);
            return true;
        }
        return false;
    }

    private void getIndex() {
        I.connection.readCharacteristic(RECORD_INDEX).subscribe(
                indexValue -> {
                    UserError.Log.d(TAG, "GetIndex result: " + dumpHexString(indexValue));
                    lastReceivedData = JoH.tsl();
                    I.connection.readCharacteristic(REMAINING_INDEX).subscribe(
                            remainingValue -> indexProcessor(indexValue, remainingValue),
                            throwable -> UserError.Log.e(TAG, "Could not read after Remaining status: " + throwable));
                }, throwable -> UserError.Log.e(TAG, "Could not read after Index status: " + throwable));
    }

    private void indexProcessor(final byte[] indexValue, final byte[] remainingValue) {
        lastIndex = Converters.unsignedBytesToInt(indexValue);
        gotAll = lastIndex == gotIndex;
        UserError.Log.d(TAG, "Index value: " + lastIndex);
        UserError.Log.d(TAG, "Remain value: " + Converters.unsignedBytesToInt(remainingValue));
        changeNextState();
    }


    private void getTime() {
        // TODO persist epoch
        if (currentPenTime == null || ratelimit("inpen-get-time", 10000)) {
            I.connection.readCharacteristic(PEN_TIME).subscribe(
                    timeValue -> {
                        UserError.Log.d(TAG, "GetTime result: " + dumpHexString(timeValue));
                        currentPenTime = new TimeRx().fromBytes(timeValue);
                        if (currentPenTime != null) {
                            UserError.Log.d(TAG, "Current pen epoch: " + JoH.dateTimeText(currentPenTime.getPenEpoch()));
                            changeNextState();
                        } else {
                            UserError.Log.e(TAG, "Current pen time invalid");
                        }
                    }, throwable -> UserError.Log.e(TAG, "Could not read after get time status: " + throwable));

        } else {
            UserError.Log.d(TAG, "Skipping get time, already have epoch");
            changeNextState();
        }
    }


    private void getAttachTime() {
        // TODO persist attach epoch
        if (currentPenAttachTime == null || ratelimit("inpen-get-time", 180000)) {
            I.connection.readCharacteristic(PEN_ATTACH_TIME).subscribe(
                    timeValue -> {
                        UserError.Log.d(TAG, "GetAttachTime result: " + dumpHexString(timeValue));
                        currentPenAttachTime = new TimeRx().fromBytes(timeValue);
                        if (currentPenAttachTime != null) {
                            UserError.Log.d(TAG, "Current pen attach epoch: " + currentPenAttachTime.getPenTime());
                            changeNextState();
                        } else {
                            UserError.Log.e(TAG, "Current pen attach time invalid");
                        }
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not read after get attach time status: " + throwable);
                        if (throwable instanceof BleDisconnectedException) {
                            changeState(CLOSE);
                        } else {
                            changeNextState();
                        }
                    });

        } else {
            UserError.Log.d(TAG, "Skipping get attach time, already have epoch");
            changeNextState();
        }
    }


    private void getBattery() {
        if (JoH.pratelimit("inpen-battery-poll-" + I.address, 40000)) {
            I.connection.readCharacteristic(BATTERY).subscribe(
                    batteryValue -> {
                        final BatteryRx battery = new BatteryRx().fromBytes(batteryValue);
                        if (battery != null) {
                            lastBattery = battery.getBatteryPercent();
                            PersistentStore.setLong(STORE_INPEN_BATTERY + I.address, lastBattery);
                            UserError.Log.d(TAG, "GetBattery result: " + battery.getBatteryPercent());
                            changeNextState();
                        } else {
                            UserError.Log.e(TAG, "Invalid GetBattery result: " + dumpHexString(batteryValue));
                            changeNextState();
                        }

                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not read after Battery status: " + throwable);
                        changeNextState();
                    });

        } else {
            UserError.Log.d(TAG, "Skipping battery read");
            if (lastBattery == -1) {
                lastBattery = (int) PersistentStore.getLong(STORE_INPEN_BATTERY + I.address);
                if (lastBattery == 0) {
                    lastBattery = -1;
                } else {
                    UserError.Log.d(TAG, "Loaded battery from store: " + lastBattery);
                }

            }
            changeNextState();
        }

    }

    private void getIdentity(final Queue<UUID> queue) {
        // CHECK IF WE ALREADY HAVE
        if (queue == null) {
            UserError.Log.d(TAG, "IDENTITY: creating queue: " + I.characteristics.size());
            loadInfos();
            final ConcurrentLinkedQueue<UUID> newQueue = new ConcurrentLinkedQueue<>();
            for (final UUID uuid : INFO_CHARACTERISTICS) {
                if (I.characteristics.containsKey(uuid)) {
                    if (!(I.characteristics.get(uuid) instanceof byte[])) {
                        newQueue.add(uuid);
                    } else {
                        UserError.Log.d(TAG, "Already have value for: " + getCharactersticName(uuid.toString()));
                    }
                } else {
                    UserError.Log.d(TAG, "Characteristic not found in discover services: " + getCharactersticName(uuid.toString()));
                }
            }

            if (!newQueue.isEmpty()) {
                getIdentity(newQueue);
            } else {
                // already got everything
                changeNextState();
            }
            return;
        }
        if (!queue.isEmpty()) {

            final UUID uuid = queue.poll();
            UserError.Log.d(TAG, "IDENTITY: list not empty: uuid: " + uuid);
            I.connection.readCharacteristic(uuid).timeout(5, TimeUnit.SECONDS).subscribe(
                    infoValue -> {
                        UserError.Log.d(TAG, getCharactersticName(uuid.toString()) + " result: " + dumpHexString(infoValue));
                        I.characteristics.put(uuid, infoValue);
                        staticCharacteristics = I.characteristics;
                        getIdentity(queue);
                    }, throwable ->
                    {
                        UserError.Log.e(TAG, "Could not read after " + getCharactersticName(uuid.toString()) + " status: " + throwable);
                        changeNextState();
                    });

        } else {
            UserError.Log.d(TAG, "Info Queue empty");
            saveInfos();
            changeNextState();

        }
    }

    private void loadInfos() {
        try {
            if (infosLoaded) return;
            for (Map.Entry<UUID, Object> entrySet : I.characteristics.entrySet()) {
                if (entrySet.getValue() instanceof byte[]) {
                    UserError.Log.d(TAG, "Found item skipping load infos");
                    return;
                }
            }
            final Gson gson = new GsonBuilder().create();
            final String json = PersistentStore.getString(STORE_INPEN_INFOS + I.address);
            if (D) UserError.Log.d(TAG, "JSON: " + json);
            if (json.length() > 10) {
                final HashMap<UUID, Object> loaded = gson.fromJson(json, new TypeToken<Map<UUID, Object>>() {
                }.getType());
                for (Map.Entry<UUID, Object> entry : loaded.entrySet()) {
                    // this seems like an excessive amount of gymnastics to deserialize..
                    if (entry.getValue() instanceof ArrayList) {
                        if (D) UserError.Log.d(TAG, "Populating for: " + (entry.getKey()));
                        final ArrayList<Double> bytelist = ((ArrayList<Double>) entry.getValue());
                        final byte[] bytes = new byte[bytelist.size()];
                        for (int i = 0; i < bytes.length; i++) {
                            bytes[i] = bytelist.get(i).byteValue();
                        }
                        if (D) UserError.Log.d(TAG, entry.getKey() + " " + JoH.bytesToHex(bytes));
                        I.characteristics.put(entry.getKey(), bytes);
                    }
                }
            }
            staticCharacteristics = I.characteristics;
            infosLoaded = true;
            UserError.Log.d(TAG, "loadInfos() loaded");
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception in loadInfos " + e);
        }
    }

    private void saveInfos() {
        final Gson gson = new GsonBuilder().create();
        final String json = gson.toJson(staticCharacteristics);
        PersistentStore.setString(STORE_INPEN_INFOS + I.address, json);
        UserError.Log.d(TAG, json);
    }


    private void getRecords() {

        if (checkMissingIndex()) return; // processing from there instead

        if (lastIndex < 0) {
            UserError.Log.e(TAG, "Cannot get records as index is not defined");
            return;

        }
        final long highest = PenData.getHighestIndex(I.address);
        int firstIndex = highest > 0 ? (int) highest + 1 : 1;
        if (firstIndex > lastIndex) {
            UserError.Log.e(TAG, "First index is greater than last index: " + firstIndex + " " + lastIndex);
            return;
        }

        final int count = lastIndex - firstIndex;
        if (count > MAX_BACKLOG) {
            firstIndex = lastIndex - MAX_BACKLOG;
            UserError.Log.d(TAG, "Restricting first index to: " + firstIndex);
        }

        getRecords(firstIndex, lastIndex);
    }

    private void getRecords(final int firstIndex, final int lastIndex) {

        final int numberOfRecords = lastIndex - firstIndex;
        if (numberOfRecords > 30) {
            I.connection.writeCharacteristic(KEEPALIVE, new KeepAliveTx().getBytes()).subscribe(
                    value -> {
                        UserError.Log.d(TAG, "Wrote keep alive for " + numberOfRecords);
                    }, throwable -> {
                        UserError.Log.d(TAG, "Got exception in keep alive" + throwable);
                    });
        }

        final RecordTx packet = new RecordTx(firstIndex, lastIndex);
        UserError.Log.d(TAG, "getRecords called, loading: " + firstIndex + " to " + lastIndex);
        I.connection.setupIndication(RECORD_INDICATE).doOnNext(notificationObservable -> {

            I.connection.writeCharacteristic(RECORD_START, packet.startBytes()).subscribe(valueS -> {
                UserError.Log.d(TAG, "Wrote record start: " + bytesToHex(valueS));
                I.connection.writeCharacteristic(RECORD_END, packet.endBytes()).subscribe(valueE -> {
                    UserError.Log.d(TAG, "Wrote record end: " + bytesToHex(valueE));
                    I.connection.writeCharacteristic(RECORD_REQUEST, packet.triggerBytes()).subscribe(
                            characteristicValue -> {

                                if (D)
                                    UserError.Log.d(TAG, "Wrote record request request: " + bytesToHex(characteristicValue));
                            }, throwable -> {
                                UserError.Log.e(TAG, "Failed to write record request: " + throwable);
                                if (throwable instanceof BleGattCharacteristicException) {
                                    final int status = ((BleGattCharacteristicException) throwable).getStatus();
                                    UserError.Log.e(TAG, "Got status message: " + Helper.getStatusName(status));
                                }
                            });
                }, throwable -> {
                    UserError.Log.d(TAG, "Throwable in Record End write: " + throwable);
                });
            }, throwable -> {
                UserError.Log.d(TAG, "Throwable in Record Start write: " + throwable);
                // throws BleGattCharacteristicException status = 128 for "no resources" eg nothing matches
            });
        })
                .flatMap(notificationObservable -> notificationObservable)
                .timeout(120, TimeUnit.SECONDS)
                .observeOn(Schedulers.newThread())
                .subscribe(bytes -> {

                    records.add(bytes);
                    UserError.Log.d(TAG, "INDICATE INDICATE: " + HexDump.dumpHexString(bytes));

                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        if (throwable instanceof BleDisconnectedException) {
                            UserError.Log.d(TAG, "Disconnected when waiting to receive indication: " + throwable);

                        } else {
                            UserError.Log.e(TAG, "Error receiving indication: " + throwable);

                        }
                        Inevitable.task("check-records-queue", 100, this::processRecordsQueue);
                    }
                });

    }

    private synchronized void processRecordsQueue() {
        boolean newRecord = false;
        while (!records.isEmpty()) {
            final byte[] record = records.poll();
            if (record != null) {
                final RecordRx recordRx = new RecordRx(currentPenTime).fromBytes(record);
                if (recordRx != null) {
                    UserError.Log.d(TAG, "RECORD RECORD: " + recordRx.toS());
                    final PenData penData = PenData.create(I.address, ID_INPEN, recordRx.index, recordRx.units, recordRx.getRealTimeStamp(), recordRx.temperature, record);
                    if (penData == null) {
                        UserError.Log.wtf(TAG, "Error creating PenData record from " + HexDump.dumpHexString(record));
                    } else {
                        penData.battery = recordRx.battery;
                        penData.flags = recordRx.flags;
                        UserError.Log.d(TAG, "Saving Record index: " + penData.index);
                        penData.save();
                        newRecord = true;
                        gotIndex = (int) penData.index;
                        gotAll = lastIndex == gotIndex;
                        if (InPen.soundsEnabled() && JoH.ratelimit("pen_data_in", 1)) {
                            JoH.playResourceAudio(R.raw.bt_meter_data_in);
                        }
                        lastPenData = penData;
                    }
                } else {
                    UserError.Log.e(TAG, "Error creating record from: " + HexDump.dumpHexString(record));
                }
            }
        }
        if (newRecord) {
            Inevitable.task("process-inpen-data", 1000, ProcessPenData::process);
        }
    }

    private void keepAlive() {
        I.connection.writeCharacteristic(KEEPALIVE, new KeepAliveTx().getBytes()).subscribe(
                value -> {
                    UserError.Log.d(TAG, "Sent KeepAlive ok: ");
                    changeNextState();
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not write keepAlive " + throwable);
                });
    }

    private void bondAuthority() {
        final AdvertRx adv = new AdvertRx().fromBytes(PersistentStore.getBytes(STORE_INPEN_ADVERT + I.address));
        if (adv != null) {
            final float bondUnits = JoH.roundFloat((float) Pref.getStringToDouble("inpen_prime_units", DEFAULT_BOND_UNITS), 1);
            final BondTx btx = new BondTx(bondUnits, adv.getFlagBytes());
            I.connection.writeCharacteristic(BONDCONTROL, btx.getBytes()).subscribe(
                    value -> {
                        UserError.Log.d(TAG, "Sent BondAuthority ok: " + bytesToHex(value));
                        changeNextState();
                    }, throwable -> {
                        if (isErrorResponse(throwable)) {
                            // user dialed up the wrong number of units!
                            err("Cannot bond with pen as incorrect number of units dialed up for pairing. Should be " + bondUnits + " or other error");
                            bondAsRequired(false);
                        } else {
                            UserError.Log.e(TAG, "Could not write BondAuthority " + throwable);
                        }
                    });
        } else {
            err("Cannot find valid scan record for: " + I.address);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void bondAsRequired(final boolean wait) {
        final BluetoothDevice device = I.bleDevice.getBluetoothDevice();
        final int bondState = device.getBondState();
        if (bondState == BOND_NONE) {
            final boolean bondResultCode = device.createBond();
            UserError.Log.d(TAG, "Attempted create bond: result: " + bondResultCode);
        } else {
            UserError.Log.d(TAG, "Device is already in bonding state: " + Helper.bondStateToString(bondState));
        }

        if (wait) {
            for (int c = 0; c < 10; c++) {
                if (device.getBondState() == BOND_BONDED) {
                    UserError.Log.d(TAG, "Bond created!");
                    changeNextState();
                    break;
                } else {
                    UserError.Log.d(TAG, "Sleeping waiting for bond: " + c);
                    JoH.threadSleep(1000);
                }
            }
        }
    }

    public void tryGattRefresh() {
        if (JoH.ratelimit("inpen-gatt-refresh", 60)) {
            if (Pref.getBoolean("use_gatt_refresh", true)) {
                try {
                    if (I.connection != null)
                        UserError.Log.d(TAG, "Trying gatt refresh queue");
                    I.connection.queue((new GattRefreshOperation(0))).timeout(2, TimeUnit.SECONDS).subscribe(
                            readValue -> {
                                UserError.Log.d(TAG, "Refresh OK: " + readValue);
                            }, throwable -> {
                                UserError.Log.d(TAG, "Refresh exception: " + throwable);
                            });
                } catch (NullPointerException e) {
                    UserError.Log.d(TAG, "Probably harmless gatt refresh exception: " + e);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Got exception trying gatt refresh: " + e);
                }
            } else {
                UserError.Log.d(TAG, "Gatt refresh rate limited");
            }
        }
    }


    ///////////////

    static final List<UUID> huntCharacterstics = new ArrayList<>();

    static {
        huntCharacterstics.add(Constants.BATTERY); // specimen TODO improve
    }

    @Override
    protected void onServicesDiscovered(RxBleDeviceServices services) {
        boolean found = false;
        super.onServicesDiscovered(services);
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            if (D) UserError.Log.d(TAG, "Service: " + getUUIDName(service.getUuid()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if (D)
                    UserError.Log.d(TAG, "-- Character: " + getUUIDName(characteristic.getUuid()));

                for (final UUID check : huntCharacterstics) {
                    if (characteristic.getUuid().equals(check)) {
                        I.readCharacteristic = check;
                        found = true;
                    }
                }
            }
        }
        if (found) {
            I.isDiscoveryComplete = true;
            I.discoverOnce = true;
            loadInfos();
            changeState(mState.next());
        } else {
            UserError.Log.e(TAG, "Could not find characteristic during service discovery. This is very unusual");
            tryGattRefresh();
        }
    }

    private boolean isErrorResponse(final Object throwable) {
        return throwable instanceof BleGattCharacteristicException && ((BleGattException) throwable).getStatus() == 1;
    }


    private void setFailOverTimer() {
        if (shouldServiceRun()) {
            if (quietratelimit("inpen-failover-cooldown", 30)) {
                final long retry_in = MINUTE_IN_MS * 45;
                UserError.Log.d(TAG, "setFailOverTimer: Restarting in: " + (retry_in / SECOND_IN_MS) + " seconds");

                serviceFailoverIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), INPEN_SERVICE_FAILOVER_ID, "failover");
                failover_time = JoH.wakeUpIntent(this, retry_in, serviceFailoverIntent);
            }
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private static boolean shouldServiceRun() {
        return InPenEntry.isEnabled();
    }

    private static void msg(final String msg) {
        lastState = msg + " " + hourMinuteString();
        lastStateUpdated = JoH.tsl();
    }

    private void err(final String msg) {
        lastError = msg + " " + hourMinuteString();
        UserError.Log.wtf(TAG, msg);
    }


    // data for MegaStatus
    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        if (lastError != null) {
            l.add(new StatusItem("Last Error", lastError, BAD));
        }
        if (isStarted()) {
            l.add(new StatusItem("Service Running", JoH.niceTimeScalar(msSince(InPenEntry.started_at))));

            l.add(new StatusItem("Brain State", lastState));

            if (needsAuthentication) {
                l.add(new StatusItem("Authentication", "Required", BAD));
            }

        } else {
            l.add(new StatusItem("Service Stopped", "Not running"));
        }

        if (lastReceivedData != -1) {
            l.add(new StatusItem("Last Connected", dateTimeText(lastReceivedData)));
        }

        // TODO remaining records!

        if (lastPenData != null) {
            l.add(new StatusItem("Last record", lastPenData.brief(), gotAll ? GOOD : NORMAL));
        }

        if (lastBattery != -1) {
            l.add(new StatusItem("Battery", lastBattery + "%"));
        }

        for (final UUID uuid : INFO_CHARACTERISTICS) {
            addStatusForCharacteristic(l, getCharactersticName(uuid.toString()), uuid);
        }

        if ((currentPenAttachTime != null) && (currentPenTime != null)) {
            l.add(new StatusItem("Epoch time", dateTimeText(currentPenTime.getPenEpoch())));
            l.add(new StatusItem("Attach time", dateTimeText(currentPenTime.fromPenTime(currentPenAttachTime.getPenTime()))));
        }
        //
        return l;
    }

    private static void addStatusForCharacteristic(List<StatusItem> l, String name, UUID characteristic) {
        String result = null;
        if (Arrays.asList(PRINTABLE_INFO_CHARACTERISTICS).contains(characteristic)) {
            result = getCharacteristicString(characteristic);
        } else if (Arrays.asList(HEXDUMP_INFO_CHARACTERISTICS).contains(characteristic)) {
            result = getCharacteristicHexString(characteristic);
        }
        if (result != null) {
            l.add(new StatusItem(name, result));
        }
    }

    private static String getCharacteristicString(final UUID uuid) {
        if (staticCharacteristics == null) return null;
        final Object objx = staticCharacteristics.get(uuid);
        if (objx != null) {
            if (objx instanceof byte[]) {
                return new String((byte[]) objx);
            }
        }
        return null;
    }

    private static String getCharacteristicHexString(final UUID uuid) {
        if (staticCharacteristics == null) return null;
        final Object objx = staticCharacteristics.get(uuid);
        if (objx != null) {
            if (objx instanceof byte[]) {
                return JoH.bytesToHex((byte[]) objx);
            }
        }
        return null;
    }

}
