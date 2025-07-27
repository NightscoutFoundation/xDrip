package com.eveningoutpost.dexdrip.g5model;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Prediction;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.BroadcastGlucose;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.WholeHouse;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.utils.bt.Mimeograph;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.reflect.TypeToken;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.schedulers.Schedulers;
import lombok.val;

import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.Authentication;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.Control;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.ExtraData;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.ProbablyBackfill;
import static com.eveningoutpost.dexdrip.g5model.FirmwareCapability.isTransmitterG6Rev2;
import static com.eveningoutpost.dexdrip.models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.services.G5BaseService.G5_BATTERY_FROM_MARKER;
import static com.eveningoutpost.dexdrip.services.G5BaseService.G5_BATTERY_LEVEL_MARKER;
import static com.eveningoutpost.dexdrip.services.G5BaseService.G5_BATTERY_MARKER;
import static com.eveningoutpost.dexdrip.services.G5BaseService.G5_BATTERY_WEARABLE_SEND;
import static com.eveningoutpost.dexdrip.services.G5BaseService.G5_FIRMWARE_MARKER;
import static com.eveningoutpost.dexdrip.services.G5BaseService.ALT_LOW_BATTERY_WARNING_DEFAULT;
import static com.eveningoutpost.dexdrip.services.G5BaseService.setG6Defaults;
import static com.eveningoutpost.dexdrip.services.G5BaseService.unBondAndStop;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.GET_DATA;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.SCAN;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.android_wear;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.ignoreBonding;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.immediateBonding;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.onlyUsingNativeMode;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.resetSomeInternalState;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.wear_broadcast;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.getStatusName;


/**
 * Created by jamorham on 17/09/2017.
 * <p>
 * Handles OB1 G5 communication logic
 */

@SuppressWarnings("AccessStaticViaInstance")
public class Ob1G5StateMachine {

    private static final String TAG = "Ob1G5StateMachine";
    private static final String PREF_SAVED_QUEUE = "Ob1-saved-queue";

    public static final String PREF_QUEUE_DRAINED = "OB1-QUEUE-DRAINED";
    public static final String CLOSED_OK_TEXT = "Closed OK";

    private static int LOW_BATTERY_WARNING_LEVEL = Pref.getStringToInt("g5-battery-warning-level", 300); // voltage a < this value raises warnings;
    private static final long BATTERY_READ_PERIOD_MS = HOUR_IN_MS * 12; // how often to poll battery data (12 hours)
    private static final long MAX_BACKFILL_PERIOD_MS = HOUR_IN_MS * 3; // how far back to request backfill data
    private static final long MAX_BACKFILL_PERIOD_MS2 = HOUR_IN_MS * 24; // A larger backfill option
    private static final int BACKFILL_CHECK_SMALL = 3;

    private static final boolean getVersionDetails = true; // try to load firmware version details
    private static final boolean getBatteryDetails = true; // try to load battery info details

    private static final LinkedBlockingDeque<Ob1Work> commandQueue = new LinkedBlockingDeque<>();

    private static boolean speakSlowly = false; // slow down bluetooth comms for android wear etc
    private static int nextBackFillCheckSize = BACKFILL_CHECK_SMALL;

    private static final boolean d = false;

    private static volatile long lastGlucosePacket = 0;
    private static volatile long lastUsableGlucosePacket = 0;
    private static volatile long lastAuthenticationStream = 0;
    private static volatile BgReading lastGlucoseBgReading;
    private static volatile AuthRequestTxMessage lastAuthPacket;
    private static volatile boolean backup_loaded = false;
    private static final int OLDEST_RAW = 300 * 24 * 60 * 60; // 300 days
    private static long relAutoSessionStartTime = HOUR_IN_MS * 3;

    public static long maxBackfillPeriod_MS = 0;

    public static long maxBackfillPeriod_MS() {
        maxBackfillPeriod_MS = MAX_BACKFILL_PERIOD_MS;
        if (shortTxId()) { // If using G7
            maxBackfillPeriod_MS = MAX_BACKFILL_PERIOD_MS2;
        }
        return maxBackfillPeriod_MS;
    }

    public static int backfillCheckLarge() {
        return (int) (maxBackfillPeriod_MS() / DEXCOM_PERIOD);
    }

    // Auth Check + Request
    @SuppressLint("CheckResult")
    public static boolean doCheckAuth(final Ob1G5CollectionService parent, final RxBleConnection connection) {

        if (connection == null) return false;
        parent.msg("Authorizing");

        if (parent.android_wear) {
            speakSlowly = true;
            UserError.Log.d(TAG, "Setting speak slowly to true"); // WARN should be reactive or on named devices
        }
        if (Build.VERSION.SDK_INT >= 23 && usingG6()) {
            connection.setupIndication(Authentication)
                    // .timeout(10, TimeUnit.SECONDS)
                    .timeout(15, TimeUnit.SECONDS) // WARN
                    // .observeOn(Schedulers.newThread()) // needed?
                    .doOnNext(notificationObservable -> handleAuthenticationWrite(parent, connection))
                    .flatMap(notificationObservable -> notificationObservable)
                    //.timeout(5, TimeUnit.SECONDS)
                    //.observeOn(Schedulers.newThread())
                    .subscribe(bytes -> {
                        // incoming indications
                        lastAuthenticationStream = tsl();
                        UserError.Log.d(TAG, "Received Authentication indication bytes: " + bytesToHex(bytes));
                        authenticationProcessor(parent, connection, bytes);
                    }, throwable -> handleAuthenticationThrowable(throwable, parent));
        } else {
            connection.setupNotification(Authentication)
                    // .timeout(10, TimeUnit.SECONDS)
                    .timeout(15, TimeUnit.SECONDS) // WARN
                    // .observeOn(Schedulers.newThread()) // needed?
                    .doOnNext(notificationObservable -> handleAuthenticationWrite(parent, connection))
                    .flatMap(notificationObservable -> notificationObservable)
                    //.timeout(5, TimeUnit.SECONDS)
                    //.observeOn(Schedulers.newThread())
                    .subscribe(bytes -> {
                        // incoming notifications
                        lastAuthenticationStream = tsl();
                        UserError.Log.d(TAG, "Received Authentication notification bytes: " + bytesToHex(bytes));
                        authenticationProcessor(parent, connection, bytes);
                    }, throwable -> handleAuthenticationThrowable(throwable, parent));
        }
        return true;
    }


    private static void doNext(final Ob1G5CollectionService parent, final RxBleConnection connection) {
        try {
            val p = parent.plugin.aNext();
            if (p == null) {
                UserError.Log.d(TAG, "Null value returned via plugin");
                if (shortTxId()) {
                    UserError.Log.d(TAG, "Attempting state reset");
                    parent.resetSomeInternalState();
                }
                return;
            }
            val cmd = p[0];

            if (p.length == 2) {
                val data = p[1];
                if (data != null) {
                    connection.getCharacteristic(ExtraData)
                            .blockingGet().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    val len = data.length;
                    for (int i = 0; i < len; i = i + 20) {
                        val size = Math.min(20, len - i);
                        val buf = new byte[size];
                        System.arraycopy(data, i, buf, 0, size);
                        UserError.Log.d(TAG, "Sending auth data: " + bytesToHex(buf));
                        connection.writeCharacteristic(ExtraData, nn(buf)).subscribe();
                        threadSleep(40);
                    }
                    // TODO wait for completion?
                    threadSleep(500);
                }
                if (cmd != null) {
                    UserError.Log.d(TAG, "Sending auth command: " + HexDump.dumpHexString(cmd));
                    connection.getCharacteristic(Authentication)
                            .blockingGet().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    connection.writeCharacteristic(Authentication, nn(cmd)).subscribe();
                }
            } else if (p.length == 1) {
                if (cmd != null) {
                    parent.changeState(GET_DATA);
                }
            } else if (p.length == 3) {
                parent.savePersist();
                if (parent.isDeviceLocallyBonded()) {
                    parent.changeState(Ob1G5CollectionService.STATE.UNBOND);
                } else {
                    UserError.Log.d(TAG, "Device does not appear to be bonded?");
                }
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception in doNext() " + e);
        }
    }

    @SuppressLint("CheckResult")
    public static boolean doCheckAuth2(final Ob1G5CollectionService parent, final RxBleConnection connection) {
        if (connection == null) return false;
        parent.msg("Authorizing 2");

        if (!usingG6()) {
            setG6Defaults();
        }

        if (shortTxId()) {
            LOW_BATTERY_WARNING_LEVEL = ALT_LOW_BATTERY_WARNING_DEFAULT;
            Pref.setString("g5-battery-warning-level", "" + ALT_LOW_BATTERY_WARNING_DEFAULT);
            parent.updateBatteryWarningLevel();
        }

        if (parent.android_wear) {
            speakSlowly = true;
            UserError.Log.d(TAG, "Setting speak slowly to true"); // WARN should be reactive or on named devices
        }

        try {
            parent.plugin.amConnected();
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in plugin: " + e);
            e.printStackTrace();
        }
        connection.setupNotification(ExtraData)
                .timeout(15, TimeUnit.SECONDS) // WARN
                .doOnNext(notificationObservable -> {
                    UserError.Log.d(TAG, "Extra data notifications enabled");
                    connection.setupIndication(Authentication)
                            .timeout(15, TimeUnit.SECONDS) // WARN
                            .doOnNext(notificationObservable2 -> doNext(parent, connection))
                            .flatMap(notificationObservable2 -> notificationObservable2)
                            .subscribe(bytes -> {
                                lastAuthenticationStream = tsl();
                                UserError.Log.d(TAG, "Received Authentication 2 indication bytes: " + bytesToHex(bytes));
                                try {
                                    if (parent.plugin.bondNow(bytes)) {
                                        UserError.Log.d(TAG, "Creating bond!!");
                                        parent.changeState(Ob1G5CollectionService.STATE.BOND);
                                    }
                                    if (parent.plugin.receivedResponse(bytes)) {
                                        doNext(parent, connection);
                                    }
                                } catch (Exception e) {
                                    UserError.Log.e(TAG, "Got exception in plugin: " + e);
                                    parent.lastSensorStatus = e.getMessage();
                                    if (e instanceof InvalidParameterException) {
                                        parent.resetSomeInternalState();
                                    } else if (e instanceof SecurityException) {
                                        parent.logFailure();
                                        parent.clearPersistStore();
                                        parent.changeState(SCAN);
                                    } else {
                                        e.printStackTrace();
                                    }
                                }
                            }, throwable -> handleAuthenticationThrowable(throwable, parent));
                })
                .flatMap(notificationObservable -> notificationObservable)
                .subscribe(bytes -> {
                    lastAuthenticationStream = tsl();
                    UserError.Log.d(TAG, "Received extra data indication bytes: " + bytesToHex(bytes));
                    try {
                        if (parent.plugin.receivedData(bytes)) {
                            doNext(parent, connection);
                        }
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Got exception in plugin: " + e);
                        e.printStackTrace();
                    }
                }, throwable -> handleAuthenticationThrowable(throwable, parent));
        return true;
    }

    @SuppressLint("CheckResult")
    private static void handleAuthenticationWrite(final Ob1G5CollectionService parent, final RxBleConnection connection) {
        final int specifiedSlot = Pref.getBooleanDefaultFalse("engineering_mode") ? Pref.getStringToInt("dex_specified_slot", -1) : -1;
        final AuthRequestTxMessage authRequest = (specifiedSlot == -1) ? new AuthRequestTxMessage(getTokenSize(), usingAlt())
                : new AuthRequestTxMessage(getTokenSize(), specifiedSlot);
        lastAuthPacket = authRequest;
        UserError.Log.i(TAG, "AuthRequestTX: " + bytesToHex(authRequest.byteSequence));

        connection.writeCharacteristic(Authentication, nn(authRequest.byteSequence))
                .subscribe(
                        characteristicValue -> {
                            // Characteristic value confirmed.
                            if (d)
                                UserError.Log.d(TAG, "Wrote authrequest, got: " + bytesToHex(characteristicValue));
                            speakSlowly();
                            if ((msSince(lastAuthenticationStream) > 500) && !isTransmitterG6Rev2(getTransmitterID())) {
                                connection.readCharacteristic(Authentication).subscribe(
                                        readValue -> {
                                            authenticationProcessor(parent, connection, readValue);
                                        }, throwable -> {
                                            UserError.Log.d(TAG, "Could not read after AuthRequestTX: " + throwable);
                                        });
                                //parent.background_automata();
                            }
                        },
                        throwable -> {
                            UserError.Log.e(TAG, "Could not write AuthRequestTX: " + throwable);
                            parent.incrementErrors();
                        }

                );
    }

    private static void handleAuthenticationThrowable(final Throwable throwable, final Ob1G5CollectionService parent) {
        if (!(throwable instanceof OperationSuccess)) {
            if (((parent.getState() == Ob1G5CollectionService.STATE.CLOSED)
                    || (parent.getState() == Ob1G5CollectionService.STATE.CLOSE))
                    && (throwable instanceof BleDisconnectedException)) {
                UserError.Log.d(TAG, "normal authentication notification throwable: (" + parent.getState() + ") " + throwable + " " + JoH.dateTimeText(tsl()));
                parent.connectionStateChange(CLOSED_OK_TEXT);
            } else if ((parent.getState() == Ob1G5CollectionService.STATE.BOND) && (throwable instanceof TimeoutException)) {
                // TODO Trigger on Error count / Android wear metric
                // UserError.Log.e(TAG,"Attempting to reset/create bond due to: "+throwable);
                // parent.reset_bond(true);
                // parent.unBond(); // WARN
            } else {
                UserError.Log.d(TAG, "authentication notification  throwable: (" + parent.getState() + ") " + throwable + " " + JoH.dateTimeText(tsl()));
                parent.incrementErrors();
                if (throwable instanceof BleCannotSetCharacteristicNotificationException
                        || throwable instanceof BleGattCharacteristicException) {
                    parent.tryGattRefresh();
                    parent.changeState(SCAN);
                }
            }
            if ((throwable instanceof BleDisconnectedException) || (throwable instanceof TimeoutException)) {
                if ((parent.getState() == Ob1G5CollectionService.STATE.BOND) || (parent.getState() == Ob1G5CollectionService.STATE.CHECK_AUTH)) {
                    parent.savePersist();
                    if (parent.getState() == Ob1G5CollectionService.STATE.BOND) {
                        UserError.Log.d(TAG, "SLEEPING BEFORE RECONNECT");
                        threadSleep(15000);
                    }
                    UserError.Log.d(TAG, "REQUESTING RECONNECT");
                    parent.changeState(SCAN);
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private static void authenticationProcessor(final Ob1G5CollectionService parent, final RxBleConnection connection, final byte[] readValue) {
        PacketShop pkt = classifyPacket(readValue);
        UserError.Log.d(TAG, "Read from auth request: " + pkt.type + " " + bytesToHex(readValue));

        switch (pkt.type) {
            case AuthChallengeRxMessage:
                // Respond to the challenge request
                verifyChallengeHash(((AuthChallengeRxMessage) pkt.msg).tokenHash);
                byte[] challengeHash = calculateChallengeHash(((AuthChallengeRxMessage) pkt.msg).challenge);
                if (d)
                    UserError.Log.d(TAG, "challenge hash" + Arrays.toString(challengeHash));
                if (challengeHash != null) {
                    if (d)
                        UserError.Log.d(TAG, "Transmitter trying auth challenge");

                    connection.writeCharacteristic(Authentication, nn(new BaseAuthChallengeTxMessage(challengeHash).byteSequence))
                            .subscribe(
                                    challenge_value -> {

                                        speakSlowly();
                                        if (msSince(lastAuthenticationStream) > 500) {
                                            connection.readCharacteristic(Authentication)
                                                    //.observeOn(Schedulers.io())
                                                    .subscribe(
                                                            status_value -> {
                                                                // interpret authentication response
                                                                authenticationProcessor(parent, connection, status_value);
                                                            }, throwable -> {
                                                                if (throwable instanceof OperationSuccess) {
                                                                    UserError.Log.d(TAG, "Stopping auth challenge listener due to success");
                                                                } else {
                                                                    UserError.Log.d(TAG, "Could not read reply to auth challenge: " + throwable);
                                                                    parent.incrementErrors();
                                                                    speakSlowly = true;
                                                                }
                                                            });
                                        }
                                    }, throwable -> {
                                        UserError.Log.e(TAG, "Could not write auth challenge reply: " + throwable);
                                        parent.incrementErrors();
                                    });

                } else {
                    UserError.Log.e(TAG, "Could not generate challenge hash! - resetting");
                    parent.changeState(Ob1G5CollectionService.STATE.INIT);
                    parent.incrementErrors();
                    return;
                }

                break;

            case AuthStatusRxMessage:
                final AuthStatusRxMessage status = (AuthStatusRxMessage) pkt.msg;
                if (d)
                    UserError.Log.d(TAG, ("Authenticated: " + status.isAuthenticated() + " " + status.isBonded()));
                if (status.isAuthenticated()) {

                    if (parent.unBondAndStop) {
                        UserError.Log.d(TAG, "Processing unbond and stop");
                        parent.changeState(Ob1G5CollectionService.STATE.UNBOND);

                    } else {
                        if (!status.isBonded()
                                && (parent.isDeviceLocallyBonded() || parent.isDeviceLocallyBonded())
                                && parent.incrementRetry() <= 3) {
                            UserError.Log.wtf(TAG, "Doing Android 12+ workaround");
                            JoH.threadSleep(1500);
                            handleAuthenticationWrite(parent, connection);
                            return;
                        }

                        if (status.isBonded()) {
                            parent.msg("Authenticated");
                            parent.authResult(true);
                            parent.changeState(GET_DATA);
                            throw new OperationSuccess("Authenticated");
                        } else {
                            //parent.unBond(); // bond must be invalid or not existing // WARN
                            parent.changeState(Ob1G5CollectionService.STATE.PREBOND);
                            // TODO what to do here?
                        }
                    }
                } else {
                    parent.msg("Not Authorized! (Wrong TxID?)");
                    UserError.Log.e(TAG, "Authentication failed!!!!");
                    parent.incrementErrors();
                    // TODO? try again?
                }
                break;

            case BondRequestRxMessage:
                UserError.Log.d(TAG, "Wrote bond request successfully");
                parent.waitingBondConfirmation = 1; // waiting

                parent.instantCreateBondIfAllowed();
                UserError.Log.d(TAG, "Sleeping for bond");
                for (int i = 0; i < 9; i++) {
                    if (parent.waitingBondConfirmation == 2) {
                        UserError.Log.d(TAG, "Bond confirmation received - continuing!");
                        break;
                    }
                    threadSleep(1000);
                }
                parent.changeState(Ob1G5CollectionService.STATE.BOND);
                break;

            default:
                UserError.Log.e(TAG, "Unhandled packet type in reply: " + pkt.type + " " + bytesToHex(readValue));
                parent.incrementErrors();
                // TODO what to do here?
                break;
        }
    }

    private static final int SPEAK_SLOWLY_DELAY = 300;

    private static int speakSlowlyDelay() {
        return speakSlowly ? SPEAK_SLOWLY_DELAY : 0;
    }

    private static void speakSlowly() {
        if (speakSlowly) {
            UserError.Log.d(TAG, "Speaking slowly");
            threadSleep(SPEAK_SLOWLY_DELAY);
        }
    }

    private static void threadSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Failed to sleep for " + ms + " due to: " + e);
        }
    }


    @SuppressLint("CheckResult")
    public synchronized static void doKeepAlive(Ob1G5CollectionService parent, RxBleConnection connection, Runnable runnable) {
        if (connection == null) return;
        connection.writeCharacteristic(Authentication, nn(new KeepAliveTxMessage(60).byteSequence))
                .timeout(3, TimeUnit.SECONDS)
                .subscribe(
                        characteristicValue -> {
                            UserError.Log.d(TAG, "Sent keep-alive " + ((runnable != null) ? "Running runnable chain" : ""));
                            if (runnable != null) {
                                runnable.run();
                            }
                            throw new OperationSuccess("keep-alive runnable complete");
                        }, throwable -> {
                            if (!(throwable instanceof OperationSuccess)) {
                                UserError.Log.e(TAG, "Got error sending keepalive: " + throwable);
                            }
                        });
    }

    // Handle bonding
    @SuppressLint("CheckResult")
    public synchronized static boolean doKeepAliveAndBondRequest(Ob1G5CollectionService parent, RxBleConnection connection) {

        if (connection == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserError.Log.d(TAG, "Requesting high priority");
            connection.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH, 500, TimeUnit.MILLISECONDS);
        }
        UserError.Log.e(TAG, "Sending keepalive..");
        connection.writeCharacteristic(Authentication, nn(new KeepAliveTxMessage(60).byteSequence))
                .subscribe(
                        characteristicValue -> {
                            UserError.Log.d(TAG, "Wrote keep-alive request successfully");
                            speakSlowly(); // is this really needed here?
                            parent.unBond();
                            parent.instantCreateBondIfAllowed();
                            speakSlowly();
                            connection.writeCharacteristic(Authentication, nn(new BondRequestTxMessage().byteSequence))
                                    .subscribe(
                                            bondRequestValue -> {
                                                UserError.Log.d(TAG, "Wrote bond request value: " + bytesToHex(bondRequestValue));
                                                speakSlowly();
                                                connection.readCharacteristic(Authentication)
                                                        .observeOn(Schedulers.io())
                                                        .timeout(10, TimeUnit.SECONDS)
                                                        .subscribe(
                                                                status_value -> {
                                                                    UserError.Log.d(TAG, "Got status read after keepalive " + bytesToHex(status_value));
                                                                    authenticationProcessor(parent, connection, status_value);
                                                                    throw new OperationSuccess("Bond requested");
                                                                }, throwable -> {
                                                                    UserError.Log.e(TAG, "Throwable when reading characteristic after keepalive: " + throwable);
                                                                });

                                                // Wrote bond request successfully was here moved above - is this right?
                                            }, throwable -> {
                                                // failed to write bond request retry?
                                                if (!(throwable instanceof OperationSuccess)) {
                                                    UserError.Log.e(TAG, "Failed to write bond request! " + throwable);
                                                }
                                            });

                        }, throwable -> {
                            // Could not write keep alive ? retry?
                            UserError.Log.e(TAG, "Failed writing keep-alive request! " + throwable);
                        });
        UserError.Log.d(TAG, "Exiting doKeepAliveBondRequest");
        final PowerManager.WakeLock linger = JoH.getWakeLock("jam-g5-bond-linger", 30000);
        return true;
    }

    private static void localUnbondAndStop(final Ob1G5CollectionService parent, RxBleConnection connection) {
        Inevitable.task("local unbond", 3000, () -> {
            parent.unBond(); // remove local bond
            Inevitable.task("shutdown collector", 2000, () -> {
                UserError.Log.d(TAG, "Shutting down collector");
                DexCollectionType.setDexCollectionType(DexCollectionType.Disabled);
                CollectionServiceStarter.restartCollectionServiceBackground();
                parent.unBondAndStop = false;
            });
        });
    }

    @SuppressLint("CheckResult")
    public static boolean doUnBond(final Ob1G5CollectionService parent, RxBleConnection connection) {
        if (connection == null) return false;
        parent.msg("Unbond Transmitter a:" + usingAlt());
        if (unBondAndStop) {
            localUnbondAndStop(parent, connection);
        } else {
            parent.unBond();
        }
        return true;
    }

    @SuppressLint("CheckResult")
    public static boolean doReset(Ob1G5CollectionService parent, RxBleConnection connection) {
        if (connection == null) return false;
        parent.msg("Hard Resetting Transmitter");
        connection.writeCharacteristic(Control, nn(new ResetTxMessage().byteSequence))
                .subscribe(characteristicValue -> {
                    if (d)
                        UserError.Log.d(TAG, "Wrote ResetTxMessage request!!");
                    parent.msg("Hard Reset Sent");
                }, throwable -> {
                    parent.msg("Hard Reset maybe Failed");
                    UserError.Log.e(TAG, "Failed to write ResetTxMessage: " + throwable);
                    if (throwable instanceof BleGattCharacteristicException) {
                        final int status = ((BleGattCharacteristicException) throwable).getStatus();
                        UserError.Log.e(TAG, "Got status message: " + getStatusName(status));
                    }
                });
        return true;
    }

    private static void reReadGlucoseData() {
        enqueueUniqueCommand(new GlucoseTxMessage(), "Re-read glucose");
    }

    @SuppressLint("CheckResult")
    public static void checkVersionAndBattery(final Ob1G5CollectionService parent, final RxBleConnection connection) {
        final int nextVersionRequest = requiredNextFirmwareDetailsType();
        if ((getVersionDetails) && (nextVersionRequest != -1)) {
            connection.writeCharacteristic(Control, nn(new VersionRequestTxMessage(nextVersionRequest).byteSequence))
                    .subscribe(versionValue -> {
                        UserError.Log.e(TAG, "Wrote version request: " + nextVersionRequest);
                    }, throwable -> {
                        UserError.Log.e(TAG, "Failed to write VersionRequestTxMessage: " + throwable);
                    });
        } else if ((getBatteryDetails) && (parent.getBatteryStatusNow || !haveCurrentBatteryStatus())) {

            enqueueUniqueCommand(new BatteryInfoTxMessage(), "Query battery");
            parent.getBatteryStatusNow = false;

        }
    }

    // Get Data
    @SuppressLint("CheckResult")
    public static boolean doGetData(Ob1G5CollectionService parent, RxBleConnection connection) {
        if (connection == null) return false;
        // TODO switch modes depending on conditions as to whether we are using internal
        final boolean use_g5_internal_alg = Pref.getBooleanDefaultFalse("ob1_g5_use_transmitter_alg");
        UserError.Log.d(TAG, use_g5_internal_alg ? ("Requesting Glucose Data " + (usingG6() ? (shortTxId() ? "G7" : "G6") : "G5")) : "Requesting Sensor Data");

        if (!use_g5_internal_alg) {
            parent.lastSensorStatus = null; // not applicable
            parent.lastUsableGlucosePacketTime = 0;
        }
        connection.getCharacteristic(Control)
                .blockingGet().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        connection.setupIndication(Control)

                .doOnNext(notificationObservable -> {

                    if (d) UserError.Log.d(TAG, "Notifications enabled");
                    speakSlowly();

                    connection.writeCharacteristic(Control, nn(use_g5_internal_alg ? (getEGlucose(parent) ? new EGlucoseTxMessage(shortTxId()).byteSequence : new GlucoseTxMessage().byteSequence) : new SensorTxMessage().byteSequence))
                            .subscribe(
                                    characteristicValue -> {
                                        if (d)
                                            UserError.Log.d(TAG, "Wrote SensorTxMessage request");
                                    }, throwable -> {
                                        UserError.Log.e(TAG, "Failed to write SensorTxMessage: " + throwable);
                                        if (throwable instanceof BleGattCharacteristicException) {
                                            final int status = ((BleGattCharacteristicException) throwable).getStatus();
                                            UserError.Log.e(TAG, "Got status message: " + getStatusName(status));
                                            if (status == 8) {
                                                UserError.Log.e(TAG, "Request rejected due to Insufficient Authorization failure!");
                                                parent.authResult(false);
                                            }
                                        }
                                    });

                })
                .flatMap(notificationObservable -> notificationObservable)
                .timeout(10, TimeUnit.SECONDS)
                .subscribe(bytes -> {
                    // incoming data notifications
                    UserError.Log.d(TAG, "Received indication bytes: " + bytesToHex(bytes));
                    final PacketShop data_packet = classifyPacket(bytes);
                    switch (data_packet.type) {
                        case SensorRxMessage:

                            try {
                                checkVersionAndBattery(parent, connection);
                            } finally {
                                processSensorRxMessage((SensorRxMessage) data_packet.msg);
                                parent.msg("Got data");
                                parent.updateLast(tsl());
                                parent.clearErrors();
                            }
                            break;

                        case VersionRequest1RxMessage:
                            if (!setStoredFirmwareBytes(getTransmitterID(), 1, bytes, true)) {
                                UserError.Log.e(TAG, "Could not save out firmware version!");
                            }
                            nextBackFillCheckSize = backfillCheckLarge();
                            if (JoH.ratelimit("g6-evaluate", 600)) {
                                Inevitable.task("evaluteG6Settings", 10000, () -> evaluateG6Settings());
                            }
                            break;

                        case VersionRequestRxMessage:
                            if (!setStoredFirmwareBytes(getTransmitterID(), 0, bytes, true)) {
                                UserError.Log.e(TAG, "Could not save out firmware version!");
                            }
                            nextBackFillCheckSize = backfillCheckLarge();
                            if (JoH.ratelimit("g6-evaluate", 600)) {
                                Inevitable.task("evaluteG6Settings", 10000, () -> evaluateG6Settings());
                            }
                            break;

                        case VersionRequest2RxMessage:
                            final boolean rev2Type = ((VersionRequest2RxMessage) data_packet.msg).isType2();
                            if (!setStoredFirmwareBytes(getTransmitterID(), rev2Type ? 3 : 2, bytes, true)) {
                                UserError.Log.e(TAG, "Could not save out firmware version!");
                            }
                            SensorDays.clearCache();
                            nextBackFillCheckSize = backfillCheckLarge();
                            if (JoH.ratelimit("g6-evaluate", 600)) {
                                Inevitable.task("evaluteG6Settings", 10000, () -> evaluateG6Settings());
                            }
                            break;

                        case BatteryInfoRxMessage:
                            if (!setStoredBatteryBytes(getTransmitterID(), bytes)) {
                                UserError.Log.e(TAG, "Could not save out battery data!");
                            } else {
                                if (parent.android_wear) {
                                    PersistentStore.setBoolean(G5_BATTERY_WEARABLE_SEND, true);
                                }
                            }
                            nextBackFillCheckSize = backfillCheckLarge();
                            break;

                        case SessionStartRxMessage:
                            final SessionStartRxMessage session_start = (SessionStartRxMessage) data_packet.msg;
                            if (session_start.isOkay()) {
                                // TODO persist this
                                parent.msg("Session Started Successfully: " + JoH.dateTimeText(session_start.getSessionStart()) + " " + JoH.dateTimeText(session_start.getRequestedStart()) + " " + JoH.dateTimeText(session_start.getTransmitterTime()));
                                DexResetHelper.cancel();
                                if (!parent.lastSensorState.sensorStarted()) {
                                    parent.lastSensorState = CalibrationState.SensorStarted;
                                    parent.lastSensorStatus = parent.lastSensorState.getExtendedText();
                                }
                            } else {
                                final String msg = "Session Start Failed: " + session_start.message();
                                parent.msg(msg);
                                UserError.Log.ueh(TAG, msg);
                                JoH.showNotification(devName() + " Start Failed", msg, null, Constants.G5_START_REJECT, true, true, false);
                                UserError.Log.ueh(TAG, "Session Start failed info: " + JoH.dateTimeText(session_start.getSessionStart()) + " " + JoH.dateTimeText(session_start.getRequestedStart()) + " " + JoH.dateTimeText(session_start.getTransmitterTime()));
                                if (session_start.isFubar()) {
                                    final long tk = DexTimeKeeper.getDexTime(getTransmitterID(), tsl());
                                    if (tk > 0) {
                                        if (FirmwareCapability.isTransmitterStandardFirefly(getTransmitterID())) {// Firefly, which cannot be hard reset
                                            UserError.Log.e(TAG, "Unusual session start failure");
                                        } else {
                                            DexResetHelper.offer("Unusual session start failure, is transmitter crashed? Try Hard Reset?");
                                        }
                                    } else {
                                        UserError.Log.e(TAG, "No reset as TimeKeeper reports invalid: " + tk);
                                    }
                                }
                                if (Sensor.isActive()) {
                                    if (Pref.getBooleanDefaultFalse("ob1_g5_restart_sensor")) {
                                        if (pratelimit("secondary-g5-start", 1800)) {
                                            UserError.Log.ueh(TAG, "Trying to Start sensor again");
                                            startSensor(tsl());
                                        } else {
                                            if (!parent.lastSensorState.sensorStarted()) {
                                                UserError.Log.uel(TAG, "Stopping sensor session due to repeated restart failure");
                                                Treatments.sensorStop(null, "Stopped due to repeated start failure");
                                                Sensor.stopSensor();
                                            }
                                        }
                                    } else {
                                        if (!parent.lastSensorState.sensorStarted()) {
                                            UserError.Log.uel(TAG, "Stopping sensor session due to start failure");
                                            Treatments.sensorStop(null, "Stopped due to start failure");
                                            Sensor.stopSensor();
                                        }
                                    }
                                }
                            }
                            reReadGlucoseData();

                            break;

                        case SessionStopRxMessage:
                            final SessionStopRxMessage session_stop = (SessionStopRxMessage) data_packet.msg;
                            if (session_stop.isOkay()) {
                                // TODO persist this
                                final String msg = "Session Stopped Successfully: " + JoH.dateTimeText(session_stop.getSessionStart()) + " " + JoH.dateTimeText(session_stop.getSessionStop());
                                parent.msg(msg);
                                UserError.Log.ueh(TAG, msg);
                                if (parent.lastSensorState.sensorStarted()) {
                                    parent.lastSensorState = CalibrationState.SensorStopped;
                                    parent.lastSensorStatus = parent.lastSensorState.getExtendedText();
                                } else {
                                    reReadGlucoseData();
                                }
                                enqueueUniqueCommand(new TimeTxMessage(), "Query time after stop");
                            } else {
                                // TODO what does an error when session isn't started look like? Probably best to downgrade those somewhat
                                final String msg = "Session Stop Failed: packet valid: " + session_stop.isValid() + "  Status code: " + session_stop.getStatus();
                                UserError.Log.uel(TAG, msg);
                            }
                            break;

                        case GlucoseRxMessage:
                            final GlucoseRxMessage glucose = (GlucoseRxMessage) data_packet.msg;
                            parent.processCalibrationState(glucose.calibrationState());

                            if (glucose.usable()) {
                                parent.msg("Got " + devName() + " glucose");
                            } else {
                                parent.msg("Got data from " + devName());
                            }

                            glucoseRxCommon(glucose, parent, connection);
                            break;

                        // TODO base class duplication
                        case EGlucoseRxMessage:
                            final EGlucoseRxMessage eglucose = (EGlucoseRxMessage) data_packet.msg;
                            parent.processCalibrationState(eglucose.calibrationState());

                            if (eglucose.usable()) {
                                parent.msg("Got G6 glucose");
                            } else {
                                parent.msg("Got data from G6");
                            }

                            glucoseRxCommon(eglucose, parent, connection);
                            break;

                        case EGlucoseRxMessage2:
                            val eglucose2 = (EGlucoseRxMessage2) data_packet.msg;
                            UserError.Log.d(TAG, "EG2 Debug: " + eglucose2);
                            if (eglucose2.isValid()) {
                                parent.processCalibrationState(eglucose2.adjustedCalibrationState());
                                DexTimeKeeper.updateAge(getTransmitterID(), (int) eglucose2.timestamp);
                                DexSessionKeeper.setStart(eglucose2.getRealSessionStartTime());
                                if (eglucose2.usable()) {
                                    parent.msg("Got glucose");
                                } else {
                                    parent.msg("Got data");
                                }
                                glucoseRxCommon(eglucose2, parent, connection);
                                parent.saveTransmitterMac();
                            } else {
                                parent.msg("Invalid Glucose");
                            }
                            break;


                        case CalibrateRxMessage:
                            final CalibrateRxMessage calibrate = (CalibrateRxMessage) data_packet.msg;
                            if (calibrate.accepted()) {
                                parent.msg("Calibration accepted");
                                UserError.Log.ueh(TAG, "Calibration accepted by transmitter");
                                parent.lastSensorState = CalibrationState.CalibrationSent;
                            } else {
                                final String msg = "Calibration rejected: " + calibrate.message();
                                UserError.Log.wtf(TAG, msg);
                                parent.msg(msg);
                                JoH.showNotification("Calibration rejected", msg, null, Constants.G5_CALIBRATION_REJECT, true, true, false);
                            }
                            reReadGlucoseData();
                            break;

                        case BackFillRxMessage:
                            final BackFillRxMessage backfill = (BackFillRxMessage) data_packet.msg;
                            if (backfill.valid()) {
                                UserError.Log.d(TAG, "Backfill request confirmed");
                            } else {
                                UserError.Log.wtf(TAG, "Backfill request corrupted!");
                            }
                            break;

                        case BackFillControlRxMessage:
                            final BackFillControlRxMessage backfill2 = (BackFillControlRxMessage) data_packet.msg;
                            if (backfill2.isValid()) {
                                UserError.Log.d(TAG, "Backfill2 request confirmed");
                            } else {
                                UserError.Log.wtf(TAG, "Backfill2 request corrupted!");
                            }
                            break;

                        case TransmitterTimeRxMessage:
                            // This message is received every 120-125m
                            final TransmitterTimeRxMessage txtime = (TransmitterTimeRxMessage) data_packet.msg;
                            DexTimeKeeper.updateAge(getTransmitterID(), txtime.getCurrentTime(), true);
                            if (txtime.sessionInProgress()) {
                                UserError.Log.e(TAG, "Session start time reports: "
                                        + JoH.dateTimeText(txtime.getRealSessionStartTime()) + " Duration: "
                                        + JoH.niceTimeScalar(txtime.getSessionDuration()));
                                DexSessionKeeper.setStart(txtime.getRealSessionStartTime());
                            } else {
                                UserError.Log.e(TAG, "Session start time reports: No session in progress");
                                DexSessionKeeper.clearStart();
                            }
                            if (Pref.getBooleanDefaultFalse("ob1_g5_preemptive_restart")
                                    && FirmwareCapability.isTransmitterPreemptiveRestartCapable(getTransmitterID())) {
                                int restartDaysThreshold = usingG6() ? 9 : 6;
                                if (txtime.getSessionDuration() > Constants.DAY_IN_MS * restartDaysThreshold
                                        && txtime.getSessionDuration() < Constants.MONTH_IN_MS) {
                                    UserError.Log.uel(TAG, "Requesting preemptive session restart");
                                    restartSensorWithTimeTravel();
                                }
                            }
                            break;

                        case F2DUnknownRxMessage:
                            UserError.Log.d(TAG, "Received F2D message");
                            handleNonSensorRxState(parent, connection);
                            break;

                        default:
                            val hex = bytesToHex(bytes);
                            UserError.Log.e(TAG, "Got unknown packet rx: " + hex);

                            switch (hex) {
                                case "2E01":
                                    UserError.Log.e(TAG, "Invalid settings, attempting to restore Dex defaults, Native mode, no plugin");
                                    setG6Defaults();
                                    break;
                            }

                            break;
                    }
                    if (!queued(parent, connection)) {
                        inevitableDisconnect(parent, connection);
                    }

                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        if (throwable instanceof BleDisconnectedException) {
                            UserError.Log.d(TAG, "Disconnected when waiting to receive indication: " + throwable);
                            parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                        } else {
                            UserError.Log.e(TAG, "Error receiving indication: " + throwable);
                            //throwable.printStackTrace();
                            disconnectNow(parent, connection);
                        }

                    }
                });


        return true;
    }

    private static void handleNonSensorRxState(final Ob1G5CollectionService parent, final RxBleConnection connection) {
        try {
            checkVersionAndBattery(parent, connection);
        } finally {
            parent.msg("Got needed");
            parent.updateLast(tsl());       // TODO verify if this is ok to do here
            parent.clearErrors();           // TODO verify if this is ok to do here
        }
    }

    private static void glucoseRxCommon(final BaseGlucoseRxMessage glucose, final Ob1G5CollectionService parent, final RxBleConnection connection) {
        if (JoH.ratelimit("ob1-g5-also-read-raw", 20)) {
            if (FirmwareCapability.isTransmitterRawCapable(getTransmitterID())) {
                enqueueUniqueCommand(new SensorTxMessage(), "Also read raw");
            } else {
                handleNonSensorRxState(parent, connection);
            }
        }

        if (JoH.pratelimit("g5-tx-time-since", 7200)
                || glucose.calibrationState().warmingUp()
                || !DexSessionKeeper.isStarted()) {
            if (JoH.ratelimit("g5-tx-time-governer", 30)) {
                if (getTransmitterID().length() > 4) {
                    enqueueUniqueCommand(new TimeTxMessage(), "Periodic Query Time");
                }
            }
        }

        // TODO check firmware version
        if (glucose.calibrationState().readyForBackfill() && !parent.getBatteryStatusNow) {
            backFillIfNeeded(parent, connection);
        }
        processGlucoseRxMessage(parent, glucose);
        parent.updateLast(tsl());
        parent.clearErrors();
    }

    private static void inevitableDisconnect(Ob1G5CollectionService parent, RxBleConnection connection) {
        inevitableDisconnect(parent, connection, speakSlowlyDelay());
    }

    private static void inevitableDisconnect(Ob1G5CollectionService parent, RxBleConnection connection, long guardTime) {
        Inevitable.task("Ob1G5 disconnect", 500 + guardTime + speakSlowlyDelay(), () -> disconnectNow(parent, connection));
    }

    @SuppressLint("CheckResult")
    private static void disconnectNow(Ob1G5CollectionService parent, RxBleConnection connection) {
        // tell device to disconnect now
        UserError.Log.d(TAG, "Disconnect NOW: " + JoH.dateTimeText(tsl()));
        speakSlowly();
        connection.writeCharacteristic(Control, nn(new DisconnectTxMessage().byteSequence))
                .timeout(2, TimeUnit.SECONDS)
                //  .observeOn(Schedulers.newThread())
                //  .subscribeOn(Schedulers.newThread())
                .subscribe(disconnectValue -> {
                    if (d) UserError.Log.d(TAG, "Wrote disconnect request");
                    parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                    throw new OperationSuccess("Requested Disconnect");
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        UserError.Log.d(TAG, "Disconnect NOW failure: " + JoH.dateTimeText(tsl()));
                        if (throwable instanceof BleDisconnectedException) {
                            UserError.Log.d(TAG, "Failed to write DisconnectTxMessage as already disconnected: " + throwable);

                        } else {
                            UserError.Log.d(TAG, "Failed to write DisconnectTxMessage: " + throwable);

                        }
                        parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                    }
                });
        UserError.Log.d(TAG, "Disconnect NOW exit: " + JoH.dateTimeText(tsl()));
    }

    private static void backFillIfNeeded(Ob1G5CollectionService parent, RxBleConnection connection) {
        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            UserError.Log.e(TAG, "Cannot process backfill evaluation as no active sensor");
            return;
        }

        final int check_readings = nextBackFillCheckSize;
        UserError.Log.d(TAG, "Checking " + check_readings + " for backfill requirement");
        final List<BgReading> lastReadings = BgReading.latest_by_size(check_readings);
        boolean ask_for_backfill = false;
        long earliest_timestamp = tsl() - maxBackfillPeriod_MS();
        long latest_timestamp = tsl();
        if ((lastReadings == null) || (lastReadings.size() != check_readings)) {
            ask_for_backfill = true;
        } else {
            for (int i = 0; i < lastReadings.size(); i++) {
                final BgReading reading = lastReadings.get(i);
                if ((reading == null) || (msSince(reading.timestamp) > ((DEXCOM_PERIOD * i) + Constants.MINUTE_IN_MS * 7))) {
                    ask_for_backfill = true;
                    if ((reading != null) && (msSince(reading.timestamp) <= maxBackfillPeriod_MS())) {
                        earliest_timestamp = reading.timestamp;
                    }
                    if (reading != null) {
                        UserError.Log.d(TAG, "Flagging backfill tripped by reading: " + i + " at time: " + JoH.dateTimeText(reading.timestamp) + " creating backfill window: " + JoH.dateTimeText(earliest_timestamp));
                    } else {
                        UserError.Log.d(TAG, "Flagging backfill tripped by null reading: " + i);
                    }
                    break;
                } else {
                    // good record
                    latest_timestamp = reading.timestamp;
                }
            }
        }

        if (ask_for_backfill) {
            nextBackFillCheckSize = backfillCheckLarge();
            monitorBackFill(parent, connection);

            final long txStartTime = DexTimeKeeper.getTxStartTimestamp(getTransmitterID()); // the time the transmitter reports as starting or 0 if we don't know
            final long startTime = Math.max(earliest_timestamp - DEXCOM_PERIOD, Math.max(txStartTime + DEXCOM_PERIOD, sensor.started_at));
            final long endTime = latest_timestamp + DEXCOM_PERIOD;

            if (startTime >= endTime) {
                UserError.Log.e(TAG, "Cannot process backfill request where start time would be after end time");
                return;
            }

            UserError.Log.d(TAG, "Requesting backfill between: " + JoH.dateTimeText(startTime) + " " + JoH.dateTimeText(endTime));

            if (!shortTxId()) {
                enqueueUniqueCommand(
                        BackFillTxMessage.get(getTransmitterID(), startTime, endTime),
                        "Get backfill since: " + JoH.hourMinuteString(startTime));
            } else {
                enqueueUniqueCommand(
                        BackFillTxMessage2.get(getTransmitterID(), startTime, endTime),
                        "Get backfill2 since: " + JoH.hourMinuteString(startTime));
            }
        } else {
            nextBackFillCheckSize = BACKFILL_CHECK_SMALL;
        }
    }

    public static boolean shortTxId() {
        return getTransmitterID().length() < 6;
    }


    private static Ob1Work enqueueCommand(BaseMessage tm, String msg) {
        if (tm != null) {
            final Ob1Work item = new Ob1Work(tm, msg);
            synchronized (commandQueue) {
                commandQueue.add(item);
            }
            streamCheck(item);
            backupCheck(item);
            return item;
        }
        return null;
    }

    private static void streamCheck(Ob1Work item) {
        if (item.streamable()) {
            Inevitable.task("check wear stream", 5000, WatchUpdaterService::checkOb1Queue);
        }
    }

    private static void backupCheck(Ob1Work item) {
        if (item.streamable()) {
            saveQueue();
        }
    }

    private static Ob1Work enqueueUniqueCommand(BaseMessage tm, String msg) {
        if (tm != null) {
            final Class searchClass = tm.getClass();
            Ob1Work item;
            synchronized (commandQueue) {
                if (searchQueue(searchClass)) {
                    UserError.Log.d(TAG, "Not adding duplicate: " + searchClass.getSimpleName());
                    return null;
                }
                item = new Ob1Work(tm, msg);
                if (d) {
                    UserError.Log.d(TAG, "Adding to queue packet: " + msg + " " + HexDump.dumpHexString(tm.byteSequence));
                }
                commandQueue.add(item);
                streamCheck(item);
            }
            backupCheck(item);
            return item;
        }
        return null;
    }

    private static boolean queueContains(BaseMessage tm) {
        final Class searchClass = tm.getClass();
        return queueContains(searchClass);
    }

    private static boolean queueContains(Class searchClass) {
        synchronized (commandQueue) {
            return searchQueue(searchClass);
        }
    }

    // note not synchronized here
    private static boolean searchQueue(Class searchClass) {
        for (Ob1Work item : commandQueue) {
            if (item.msg.getClass() == searchClass) {
                return true;
            }
        }
        return false;
    }

    public static void restoreQueue() {
        if (!backup_loaded) {
            loadQueue();
        }
    }

    private synchronized static void loadQueue() {
        if (commandQueue.size() == 0) {
            injectQueueJson(PersistentStore.getString(PREF_SAVED_QUEUE));
            UserError.Log.d(TAG, "Loaded queue stream backup.");
        }
        backup_loaded = true;
    }


    private static void saveQueue() {
        final String queue_json = extractQueueJson();
        if (!(queue_json == null ? "" : queue_json).equals(PersistentStore.getString(PREF_SAVED_QUEUE))) {
            PersistentStore.setString(PREF_SAVED_QUEUE, queue_json);
            UserError.Log.d(TAG, "Saved queue stream backup: " + queue_json);
        }
    }

    public static String extractQueueJson() {
        synchronized (commandQueue) {
            final List<Ob1Work> queue = new ArrayList<>(commandQueue.size());
            for (Ob1Work item : commandQueue) {
                if (item.streamable()) queue.add(item);
            }
            return JoH.defaultGsonInstance().toJson(queue);
        }
    }

    // used in backup restore and wear
    @SuppressWarnings("WeakerAccess")
    public static void injectQueueJson(String json) {
        try {
            if (json == null || json.length() == 0) return;
            final Type queueType = new TypeToken<ArrayList<Ob1Work>>() {
            }.getType();
            final List<Ob1Work> queue = JoH.defaultGsonInstance().fromJson(json, queueType);
            synchronized (commandQueue) {
                commandQueue.clear();
                commandQueue.addAll(queue);
            }
            UserError.Log.d(TAG, "Replaced queue with stream: " + json);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Got error processing queue injection: " + e);
        }
    }

    public static String extractDexTime() {
        return DexTimeKeeper.extractForStream(getTransmitterID());
    }

    @SuppressWarnings("unused")
    public static void injectDexTime(String stream) {
        DexTimeKeeper.injectFromStream(stream);
    }


    public static boolean pendingStop() {
        return queueContains(SessionStopTxMessage.class);
    }

    public static boolean pendingStart() {
        return queueContains(SessionStartTxMessage.class);
    }

    public static boolean pendingCalibration() {
        return queueContains(CalibrateTxMessage.class);
    }

    public static int queueSize() {
        return commandQueue.size();
    }

    public static void emptyQueue() {
        synchronized (commandQueue) {
            if (commandQueue.size() > 0) {
                UserError.Log.d(TAG, "Queue drained on wear, clearing: " + commandQueue.size() + " commands");
                commandQueue.clear();
                Inevitable.task("Save cleared G5 queue", 1000, Ob1G5StateMachine::saveQueue);
            } else {
                if (d) UserError.Log.d(TAG, "Local command queue is already empty");
            }
        }
    }

    public static boolean deleteFirstQueueCalibration(final int mgdl) {
        synchronized (commandQueue) {
            final Ob1Work item = commandQueue.peek();
            if (item != null) {
                if (item.msg instanceof CalibrateTxMessage) {
                    final CalibrateTxMessage cal = (CalibrateTxMessage) item.msg;
                    if (mgdl == -1 || cal.glucose == mgdl) {
                        commandQueue.poll(); // eat this entry
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getFirstQueueItemName() {
        synchronized (commandQueue) {
            final Ob1Work item = commandQueue.peek();
            return item != null ? item.text : "";
        }
    }

    private static boolean acceptCommands() {
        return DexCollectionType.hasDexcomRaw() && Pref.getBooleanDefaultFalse("ob1_g5_use_transmitter_alg");
    }

    // actual limit is something like 20-30 mins but due to propagation delays its too risky to adjust
    private static final long MAX_START_TIME_REWIND = Constants.MINUTE_IN_MS * 5;

    public static void startSensor(long when) {
        if (acceptCommands()) {
            if (msSince(when) > MAX_START_TIME_REWIND) {
                when = tsl() - MAX_START_TIME_REWIND;
                UserError.Log.e(TAG, "Cannot rewind sensor start time beyond: " + JoH.dateTimeText(when));
            }
            if (usingG6()) {
                final String code = G6CalibrationParameters.getCurrentSensorCode();
                if (code == null) {
                    UserError.Log.wtf(TAG, "Cannot start G6 sensor as calibration code not set!");
                } else {
                    UserError.Log.ueh(TAG, "Starting G6 sensor using calibration code: " + code);
                    enqueueUniqueCommand(new SessionStartTxMessage(when,
                                    DexTimeKeeper.getDexTime(getTransmitterID(), when), code),
                            "Start G6 Sensor");
                }

            } else {
                UserError.Log.ueh(TAG, "Starting G5 sensor");
                enqueueUniqueCommand(new SessionStartTxMessage(when,
                                DexTimeKeeper.getDexTime(getTransmitterID(), when)),
                        "Start G5 Sensor");
            }
        }
    }

    private static void reprocessTxMessage(BaseMessage tm) {
        // rewrite session start messages in case our clock was wrong
        if (tm instanceof SessionStartTxMessage) {
            final SessionStartTxMessage ssm = (SessionStartTxMessage) tm;
            if (usingG6()) {
                final String code = G6CalibrationParameters.getCurrentSensorCode();
                if (code == null) {
                    UserError.Log.wtf(TAG, "Cannot reprocess start G6 sensor as calibration code not set!");
                } else {
                    // g6
                    tm.byteSequence = new SessionStartTxMessage(ssm.getStartTime(), DexTimeKeeper.getDexTime(getTransmitterID(), ssm.getStartTime()), code).byteSequence;
                }
            } else {
                // g5
                tm.byteSequence = new SessionStartTxMessage(ssm.getStartTime(), DexTimeKeeper.getDexTime(getTransmitterID(), ssm.getStartTime())).byteSequence;
            }
            UserError.Log.d(TAG, "New session start: " + ssm.getDexTime() + " for time: " + JoH.dateTimeText(ssm.getStartTime()));
            if (d) {
                UserError.Log.d(TAG, "New packet: " + HexDump.dumpHexString(tm.byteSequence));
            }
        }
    }

    public static void twoPartUpdate() {
        if (acceptCommands()) {
            new Thread(() -> {
                for (int part = 0; part < 2; part++) {
                    final String code = G6CalibrationParameters.getCurrentSensorCode();
                    if (code != null) {
                        final long n = tsl();
                        final int d = DexTimeKeeper.getDexTime(getTransmitterID(), n);
                        enqueueCommand(new SessionStopTxMessage(d), "Part " + part + " A");
                        enqueueCommand(new SessionStartTxMessage(n, d, code), "Part " + part + " B");
                        threadSleep(30_000);
                    }
                }
            }).start();
        }
    }

    public static void stopSensor() {
        if (acceptCommands()) {
            enqueueCommand(
                    new SessionStopTxMessage(
                            DexTimeKeeper.getDexTime(getTransmitterID(), tsl())),
                    "Stop Sensor");
        }
    }

    private static void postExtension() {
        Inevitable.task("post-extension", 2000, () -> {
            DexSyncKeeper.clear(getTransmitterID());
            clearStoredFirmwareBytes(getTransmitterID());
            emptyQueue();
            resetSomeInternalState();
        });
    }

    public static void enableExtensionParameter() {
        if (acceptCommands()) {
            enqueueUniqueCommand(new ExtensionTxMessage(ExtensionTxMessage.PARAM_ENABLE), "Enable extension")
                    .setDontRetry()
                    .setPreWrite(Ob1G5StateMachine::postExtension);
        }
    }

    public static void disableExtensionParameter() {
        if (acceptCommands()) {
            enqueueUniqueCommand(new ExtensionTxMessage(ExtensionTxMessage.PARAM_DISABLE), "Disable extension")
                    .setDontRetry()
                    .setPreWrite(Ob1G5StateMachine::postExtension);
        }
    }

    public static void restartSensorWithTimeTravel() {
        restartSensorWithTimeTravel(tsl() -
                (useExtendedTimeTravel() ? DAY_IN_MS * 3 + HOUR_IN_MS * 2 : HOUR_IN_MS * 2 - MINUTE_IN_MS * 10));
    }

    public static boolean useExtendedTimeTravel() {
        return Pref.getBooleanDefaultFalse("ob1_g5_preemptive_restart_extended_time_travel")
                && (FirmwareCapability.isTransmitterTimeTravelCapable(getTransmitterID())
                || (Pref.getBooleanDefaultFalse("ob1_g5_defer_preemptive_restart_all_firmwares") && Home.get_engineering_mode()));
    }

    public static void restartSensorWithTimeTravel(long when) {
        if (acceptCommands()) {
            enqueueUniqueCommand(
                    new SessionStopTxMessage(
                            DexTimeKeeper.getDexTime(getTransmitterID(), when)),
                    "Auto Stop Sensor");
            final long when_started = when + SECOND_IN_MS;
            enqueueUniqueCommand(new SessionStartTxMessage(when,
                            DexTimeKeeper.getDexTime(getTransmitterID(), when_started)),
                    "Auto Start Sensor");
            if (Pref.getBoolean("ob1_g5_preemptive_restart_alert", true)) {
                Notifications.ob1SessionRestartRequested();
            }
            Treatments.create_note(xdrip.getAppContext().getString(R.string.ob1_session_restarted_note), JoH.tsl());
        }
    }

    public static void addCalibration(final int glucose, long timestamp) {
        if (acceptCommands()) {
            long since = msSince(timestamp);
            if (since < 0) {
                final String msg = "Cannot send calibration in future to transmitter: " + glucose + " @ " + JoH.dateTimeText(timestamp);
                JoH.static_toast_long(msg);
                UserError.Log.wtf(TAG, msg);
                return;
            }
            if (since > HOUR_IN_MS) {
                final String msg = "Cannot send calibration older than 1 hour to transmitter: " + glucose + " @ " + JoH.dateTimeText(timestamp);
                JoH.static_toast_long(msg);
                UserError.Log.wtf(TAG, msg);
                return;
            }
            if ((glucose < 40 || glucose > 400)) {
                final String msg = "Calibration glucose value out of range: " + glucose;
                JoH.static_toast_long(msg);
                UserError.Log.wtf(TAG, msg);
                return;
            }

            UserError.Log.uel(TAG, "Queuing Calibration for transmitter: " + BgGraphBuilder.unitized_string_with_units_static(glucose) + " " + JoH.dateTimeText(timestamp));

            enqueueCommand(new CalibrateTxMessage(
                            glucose, DexTimeKeeper.getDexTime(getTransmitterID(), timestamp)),
                    "Calibrate " + BgGraphBuilder.unitized_string_with_units_static_short(glucose));
        }
    }

    private static boolean queued(Ob1G5CollectionService parent, RxBleConnection connection) {
        if (!commandQueue.isEmpty()) {
            processQueueCommand(parent, connection);
            return true;
        }
        return false;
    }

    @SuppressLint("CheckResult")
    private static void processQueueCommand(Ob1G5CollectionService parent, RxBleConnection connection) {
        boolean changed = false;
        synchronized (commandQueue) {
            if (!commandQueue.isEmpty()) {
                final Ob1Work unit = commandQueue.poll();
                if (unit != null) {
                    changed = true;
                    reprocessTxMessage(unit.msg);
                    if (unit.retry < 5 && JoH.msSince(unit.timestamp) < HOUR_IN_MS * 8) {
                        unit.preWrite();
                        connection.writeCharacteristic(Control, nn(unit.msg.byteSequence))
                                .timeout(3, TimeUnit.SECONDS)
                                .subscribe(value -> {
                                    UserError.Log.d(TAG, "Wrote Queue Message: " + unit.text);
                                    final long guardTime = unit.msg.guardTime();
                                    inevitableDisconnect(parent, connection, guardTime);
                                    if (guardTime > 0) {
                                        UserError.Log.d(TAG, "Sleeping post execute: " + unit.text + " " + guardTime + "ms");
                                        JoH.threadSleep(guardTime);
                                    }
                                    unit.postWrite();
                                    throw new OperationSuccess("Completed: " + unit.text);

                                }, throwable -> {
                                    if (!(throwable instanceof OperationSuccess)) {
                                        if (!unit.dontRetry) {
                                            unit.retry++;
                                            UserError.Log.d(TAG, "Re-adding: " + unit.text);
                                            synchronized (commandQueue) {
                                                commandQueue.push(unit);
                                            }
                                        } else {
                                            UserError.Log.d(TAG, "Not re-adding " + unit.text);
                                        }
                                        UserError.Log.d(TAG, "Failure: " + unit.text + " " + JoH.dateTimeText(tsl()));
                                        if (throwable instanceof BleDisconnectedException) {
                                            UserError.Log.d(TAG, "Disconnected: " + unit.text + " " + throwable);
                                            parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                                        } else {
                                            UserError.Log.e(TAG, "Failed to write: " + unit.text + " " + throwable);
                                        }
                                        parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                                    } else {
                                        queued(parent, connection); // turtles all the way down
                                    }
                                });
                    } else {
                        UserError.Log.e(TAG, "Ejected command from queue due to being too old: " + unit.text + " " + JoH.dateTimeText(unit.timestamp));
                        queued(parent, connection); // move on to next command if we just ejected something
                    }
                }
                if (commandQueue.isEmpty()) {
                    if (d) UserError.Log.d(TAG, "Command Queue Drained");
                    if (android_wear) {
                        PersistentStore.setBoolean(PREF_QUEUE_DRAINED, true);
                    }
                }
            } else {
                UserError.Log.d(TAG, "Command Queue is Empty");
            }
        }
        if (changed) saveQueue();
    }

    private static void checkAndActivateSensor() {
        // automagically start an xDrip sensor session if transmitter already has active sensor
        if (!Sensor.isActive() && Ob1G5CollectionService.isG5SensorStarted() && (!Sensor.stoppedRecently() || shortTxId())) {
            JoH.static_toast_long(xdrip.gs(R.string.auto_starting_sensor));
            if (shortTxId()) relAutoSessionStartTime = HOUR_IN_MS * 24; // If we are using a G7
            final List<BgReading> last = BgReading.latest(1); // Last reading
            if ((last != null) && (last.size() > 0)) { // Have we had a reading?
                final long now = JoH.tsl();
                final long since = now - last.get(0).timestamp; // Time since last reading
                if (since < relAutoSessionStartTime) { // If the last reading was less than 3 hours ago, or if we are using G7 and the last reading was less than 24 hours ago
                    relAutoSessionStartTime = since; // We will start the new session starting from the last reading.
                }
            }
            Sensor.create(tsl() - relAutoSessionStartTime);
        }
    }

    private static void processGlucoseRxMessage(Ob1G5CollectionService parent, final BaseGlucoseRxMessage glucose) {
        if (glucose == null) return;
        lastGlucosePacket = tsl();
        DexTimeKeeper.updateAge(getTransmitterID(), glucose.timestamp);
        if (glucose.calibrationState().warmingUp()) {
            checkAndActivateSensor();
        }
        if (glucose.usable() || (glucose.insufficient() && Pref.getBoolean("ob1_g5_use_insufficiently_calibrated", true))) {
            UserError.Log.d(TAG, "Got usable glucose data from transmitter!!");
            final long rxtimestamp = glucose.getRealTimestamp();
            checkAndActivateSensor();
            DexSyncKeeper.store(getTransmitterID(), rxtimestamp, parent.static_last_connected, lastGlucosePacket);
            final BgReading bgReading = BgReading.bgReadingInsertFromG5(glucose.glucose, rxtimestamp);
            if (bgReading != null) {
                try {
                    bgReading.calculated_value_slope = glucose.getTrend() / Constants.MINUTE_IN_MS; // note this is different to the typical calculated slope, (normally delta)
                    if (bgReading.calculated_value_slope == Double.NaN) {
                        bgReading.hide_slope = true;
                    }
                } catch (Exception e) {
                    // not a good number - does this exception ever actually fire?
                }
                if (!FirmwareCapability.isTransmitterRawCapable(getTransmitterID())) {
                    bgReading.noRawWillBeAvailable();
                }
                if (glucose.insufficient()) {
                    bgReading.appendSourceInfo("Insufficient").save();
                }
            } else {
                UserError.Log.wtf(TAG, "New BgReading was null in processGlucoseRxMessage!");
            }
            lastGlucoseBgReading = bgReading;
            lastUsableGlucosePacket = lastGlucosePacket;
            parent.lastUsableGlucosePacketTime = lastUsableGlucosePacket;
            if (glucose.getPredictedGlucose() != null) {
                // not really supported on wear yet
                if (!android_wear) {
                    Prediction.create(rxtimestamp, glucose.getPredictedGlucose(), "EGlucoseRx").save();
                }
            }

            if (android_wear && wear_broadcast && bgReading != null && bgReading.timestamp == rxtimestamp) {
                // emit local broadcast on wear if reading was newly created
                BroadcastGlucose.sendLocalBroadcast(bgReading);
            }

            if (WholeHouse.isLive()) {
                Mimeograph.poll(false);
            }

        } else {
            // TODO this is duplicated in processCalibrationState()
            if (glucose.calibrationState().sensorFailed() && Sensor.isActive()) {
                if (JoH.pratelimit("G5 Sensor Failed", 3600 * 3)) {
                    JoH.showNotification(devName() + " SENSOR FAILED", "Sensor reporting failed", null, Constants.G5_SENSOR_ERROR, true, true, false);
                    UserError.Log.uel(TAG, "Sensor reporting failed");
                }
            }
        }
    }


    private static void processSensorRxMessage(SensorRxMessage sensorRx) {
        if (sensorRx == null) return;

        // TODO, is this accurate or needed?
        int sensor_battery_level = 0;
        if (sensorRx.status == TransmitterStatus.BRICKED) {
            sensor_battery_level = 206; //will give message "EMPTY"
        } else if (sensorRx.status == TransmitterStatus.LOW) {
            sensor_battery_level = 209; //will give message "LOW"
        } else {
            sensor_battery_level = 216; //no message, just system status "OK"
        }

        UserError.Log.d(TAG, "SUCCESS!! unfiltered: " + sensorRx.unfiltered + " filtered: " + sensorRx.filtered + " timestamp: " + sensorRx.timestamp + " " + JoH.qs((double) sensorRx.timestamp / 86400, 1) + " days :: (" + sensorRx.status + ")");
        if (FirmwareCapability.isTransmitterModified(getTransmitterID()) && sensorRx.timestamp > OLDEST_RAW) { // Raw timestamp reported by a mod TX is incorrect until after sensor start.
            UserError.Log.d(TAG, "Will not update age since raw timestamp is incorrect.");
            Ob1G5CollectionService.setLast_transmitter_timestamp(0);
        } else { // Update age, based on the raw timestamp, only if the raw timestamp is correct.
            DexTimeKeeper.updateAge(getTransmitterID(), sensorRx.timestamp);
            Ob1G5CollectionService.setLast_transmitter_timestamp(sensorRx.timestamp);
        }
        if (sensorRx.unfiltered == 0) {
            UserError.Log.e(TAG, "Transmitter sent raw sensor value of 0 !! This isn't good. " + JoH.hourMinuteString());
        } else {
            //   final boolean g6 = usingG6();
            //    final boolean g6r2 = g6 && FirmwareCapability.isTransmitterG6Rev2(getTransmitterID());
            //    processNewTransmitterData(g6 ? (int)(sensorRx.unfiltered * (g6r2 ? G6_REV2_SCALING : G6_SCALING)) : sensorRx.unfiltered, g6 ? (int)(sensorRx.filtered * (g6r2 ? G6_REV2_SCALING : G6_SCALING)) : sensorRx.filtered, sensor_battery_level, new Date().getTime());
            processNewTransmitterData((int) RawScaling.scale(sensorRx.unfiltered, getTransmitterID(), false),
                    (int) RawScaling.scale(sensorRx.filtered, getTransmitterID(), true),
                    sensor_battery_level, new Date().getTime());
        }

        if (WholeHouse.isLive()) {
            Mimeograph.poll(false);
        }
    }

    // Save/process the data in xDrip style
    private static synchronized void processNewTransmitterData(int raw_data, int filtered_data, int sensor_battery_level, long captureTime) {

        final TransmitterData transmitterData = TransmitterData.create(raw_data, filtered_data, sensor_battery_level, captureTime);
        if (transmitterData == null) {
            UserError.Log.e(TAG, "TransmitterData.create failed: Duplicate packet");
            return;
        } else {
            UserError.Log.d(TAG, "Created transmitter data " + transmitterData.uuid + " " + JoH.dateTimeText(transmitterData.timestamp));
            // TODO timeInMillisecondsOfLastSuccessfulSensorRead = captureTime;
        }

        if (transmitterData.unchangedRaw() && !SensorSanity.allowTestingWithDeadSensor()) {
            UserError.Log.wtf(TAG, "Raw values are not changing - blocking further processing: " + raw_data + " " + filtered_data);
            return;
        }

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            UserError.Log.e(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        //TODO : LOG if unfiltered or filtered values are zero

        Sensor.updateBatteryLevel(sensor, transmitterData.sensor_battery_level);
        if (d)
            UserError.Log.i(TAG, "timestamp create: " + Long.toString(transmitterData.timestamp));

        if ((lastGlucoseBgReading != null) && (msSince(lastUsableGlucosePacket) < Constants.SECOND_IN_MS * 30)) {
            UserError.Log.d(TAG, "Updating BgReading provided by transmitter");
            // use sensor data to update previous record instead of trying to calculate with it
            lastGlucoseBgReading.raw_data = transmitterData.raw_data / 1000;
            lastGlucoseBgReading.filtered_data = transmitterData.filtered_data / 1000;
            // TODO calculate filtered calculated value from internal alg??
            lastGlucoseBgReading.calculateAgeAdjustedRawValue();
            lastGlucoseBgReading.save();
        } else {
            if (!Ob1G5CollectionService.usingNativeMode() || Ob1G5CollectionService.fallbackToXdripAlgorithm() || BgReading.latest(3).size() < 3) {
                final BgReading bgreading = BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
                UserError.Log.d(TAG, "BgReading created: " + bgreading.uuid + " " + JoH.dateTimeText(bgreading.timestamp));
            }
        }

        //   UserError.Log.d(TAG, "Dex raw_data " + Double.toString(transmitterData.raw_data));//KS
        //   UserError.Log.d(TAG, "Dex filtered_data " + Double.toString(transmitterData.filtered_data));//KS
        //   UserError.Log.d(TAG, "Dex sensor_battery_level " + Double.toString(transmitterData.sensor_battery_level));//KS
        //   UserError.Log.d(TAG, "Dex timestamp " + JoH.dateTimeText(transmitterData.timestamp));//KS


        // TODO static_last_timestamp =  transmitterData.timestamp;

    }

    public static void evaluateG6Settings() {
        if (haveFirmwareDetails()) {
            if (FirmwareCapability.isTransmitterG6(getTransmitterID())) {
                if (!usingG6()) {
                    setG6Defaults();
                    JoH.showNotification("Enabled defaults", "Default settings automatically enabled", null, Constants.G6_DEFAULTS_MESSAGE, false, true, false);
                    UserError.Log.uel(TAG, "Default Dex settings automatically enabled");
                } else if (!onlyUsingNativeMode() && !Home.get_engineering_mode()) {
                    // TODO revisit this now that there is scaling
                    setG6Defaults();
                    UserError.Log.wtf(TAG, "Dex Native mode enabled.  For your device, non-native mode is either not possible or not recommended.");
                    JoH.showNotification("Enabled Native", "Native mode enabled", null, Constants.G6_DEFAULTS_MESSAGE, false, true, false);
                }
                if (FirmwareCapability.isTransmitterRawIncapable(getTransmitterID())) { // If we are using a G7 or G6 Firefly (mod or not)
                    if (Pref.getBooleanDefaultFalse("ob1_g5_restart_sensor") && !Home.get_engineering_mode()) { // If restart is enabled, not in engineering mode
                        Pref.setBoolean("ob1_g5_restart_sensor", false); // Disable restart
                        UserError.Log.wtf(TAG, "Restart sensor disabled.  You cannot simply restart sensor with your device.");
                    }
                }
            }
        }
    }

    private static boolean haveFirmwareDetails() {
        return getTransmitterID().length() >= 4 && getStoredFirmwareBytes(getTransmitterID()).length >= 10;
    }


    private static int requiredNextFirmwareDetailsType() {
        final String txid = getTransmitterID();
        if (txid.length() > 3) {
            final byte[] v1b = getStoredFirmwareBytes(txid, 1);
            if (v1b.length < 10) return 1;
            if (txid.length() == 6) {
                final byte[] v0b = getStoredFirmwareBytes(txid, 0);
                if (v0b.length < 10) return 0;
            }
            if (FirmwareCapability.isTransmitterG6Rev2(txid)) {
                final byte[] v3b = getStoredFirmwareBytes(txid, 3);
                if (v3b.length < 9) return 3;
            }
            final byte[] v2b = getStoredFirmwareBytes(txid, 2);
            if (v2b.length < 10) return 2;
        }
        return -1; // nothing required
    }


    private static boolean haveCurrentBatteryStatus() {
        return getTransmitterID().length() >= 4 && (msSince(PersistentStore.getLong(G5_BATTERY_FROM_MARKER + getTransmitterID())) < BATTERY_READ_PERIOD_MS);
    }

    private static byte[] getStoredFirmwareBytes(final String transmitterId) {
        return getStoredFirmwareBytes(transmitterId, 1);
    }

    private static byte[] getStoredFirmwareBytes(final String transmitterId, final int type) {
        if (transmitterId.length() < 4) return new byte[0];
        return PersistentStore.getBytes(G5_FIRMWARE_MARKER + transmitterId + "-" + type);
    }

    // from wear sync
    public static boolean setStoredFirmwareBytes(String transmitterId, byte[] data) {
        return setStoredFirmwareBytes(transmitterId, data, false);
    }

    public static boolean setStoredFirmwareBytes(String transmitterId, byte[] data, boolean from_bluetooth) {
        return setStoredFirmwareBytes(transmitterId, 1, data, from_bluetooth);
    }

    public static boolean setStoredFirmwareBytes(String transmitterId, int type, byte[] data, boolean from_bluetooth) {
        if (from_bluetooth) UserError.Log.e(TAG, "Store: VersionRX dbg: " + bytesToHex(data));
        if (transmitterId.length() < 4) return false;
        if (data.length < 9) return false;
        if (JoH.ratelimit("store-firmware-bytes" + type, 60)) {
            PersistentStore.setBytes(G5_FIRMWARE_MARKER + transmitterId + "-" + type, data);
        }
        return true;
    }

    public static void clearStoredFirmwareBytes(String transmitterId) {
        for (int type = 1; type < 3; type++) {
            PersistentStore.removeItem(G5_FIRMWARE_MARKER + transmitterId + "-" + type);
        }
    }

    public synchronized static boolean setStoredBatteryBytes(String transmitterId, byte[] data) {
        UserError.Log.e(TAG, "Store: BatteryRX dbg: " + bytesToHex(data));
        if (transmitterId.length() < 4) return false;
        if (data.length < 10) return false;
        final BatteryInfoRxMessage batteryInfoRxMessage = new BatteryInfoRxMessage(data);
        UserError.Log.uel(TAG, "Saving battery data: " + batteryInfoRxMessage.toString());
        PersistentStore.cleanupOld(G5_BATTERY_MARKER);
        PersistentStore.cleanupOld(G5_BATTERY_FROM_MARKER);
        PersistentStore.setBytes(G5_BATTERY_MARKER + transmitterId, data);
        PersistentStore.setLong(G5_BATTERY_FROM_MARKER + transmitterId, tsl());

        // TODO logic also needs to handle battery replacements of same transmitter id
        final long old_level = PersistentStore.getLong(G5_BATTERY_LEVEL_MARKER + transmitterId);
        if ((batteryInfoRxMessage.voltagea < old_level) || (old_level == 0)) {
            if (batteryInfoRxMessage.voltagea < LOW_BATTERY_WARNING_LEVEL) {
                if (JoH.pratelimit("g5-low-battery-warning", 40000)) {
                    final boolean loud = !PowerStateReceiver.is_power_connected();
                    JoH.showNotification("Battery Low", "Transmitter battery has dropped to: " + batteryInfoRxMessage.voltagea + " it may fail soon",
                            null, 770, NotificationChannels.LOW_TRANSMITTER_BATTERY_CHANNEL, loud, loud, null, null, null);
                    UserError.Log.uel(TAG, "Dex battery has dropped to: " + batteryInfoRxMessage.voltagea);
                }
            }
            PersistentStore.cleanupOld(G5_BATTERY_LEVEL_MARKER);
            PersistentStore.setLong(G5_BATTERY_LEVEL_MARKER + transmitterId, batteryInfoRxMessage.voltagea);
        }
        return true;
    }

    public static BatteryInfoRxMessage getBatteryDetails(final String tx_id) {
        try {
            final byte[] batteryStoredBytes = PersistentStore.getBytes(G5_BATTERY_MARKER + tx_id);
            return batteryStoredBytes.length > 0 ? new BatteryInfoRxMessage(batteryStoredBytes) : null;
        } catch (Exception e) {
            if (JoH.quietratelimit("bi-exception", 15))
                UserError.Log.e(TAG, "Exception in getBatteryDetails: " + e);
            return null;
        }
    }

    public static VersionRequest1RxMessage getFirmwareDetails(String tx_id) {
        if (tx_id == null) {
            if (JoH.quietratelimit("txid-null", 15))
                UserError.Log.e(TAG, "TX ID is null in getFirmwareDetails");
            return null;
        }
        try {
            byte[] stored = getStoredFirmwareBytes(tx_id);
            if ((stored != null) && (stored.length > 9)) {
                return new VersionRequest1RxMessage(stored);
            }
        } catch (Exception e) {
            if (JoH.quietratelimit("fi-exception", 15))
                UserError.Log.e(TAG, "Exception in getFirmwareDetails: " + e);
            return null;
        }
        return null;
    }

    public static BaseMessage getFirmwareXDetails(final String tx_id, final int type) {
        if (tx_id == null) {
            if (JoH.quietratelimit("txid-null", 15))
                UserError.Log.e(TAG, "TX ID is null in getFirmwareXDetails");
            return null;
        }
        try {
            byte[] stored = getStoredFirmwareBytes(tx_id, type);
            if ((stored != null) && (stored.length >= 9)) {
                switch (type) {
                    case 1:
                        return new VersionRequest1RxMessage(stored);
                    case 2:
                    case 3:
                        return new VersionRequest2RxMessage(stored);
                    default:
                        return new VersionRequestRxMessage(stored);
                }
            }
        } catch (Exception e) {
            if (JoH.quietratelimit("fi-exception", 15))
                UserError.Log.e(TAG, "Exception in getFirmwareDetails: " + e);
            return null;
        }
        return null;
    }

    public static String getRawFirmwareVersionString(final String tx_id) {
        final VersionRequest1RxMessage vr = getFirmwareDetails(tx_id);
        if (vr != null) {
            if (vr.firmware_version_string == null) {
                UserError.Log.d(TAG, "Clearing firmware version as evaluated to null");
                setStoredFirmwareBytes(tx_id, new byte[0], false);
                return "error";
            }
            return vr.firmware_version_string;
        } else {
            return tx_id.length() > 4 ? "" : "N/A";
        }
    }

    private static void updateStreamedTillTimeForBackfill() {
        // interact with ListenerService
        if (JoH.areWeRunningOnAndroidWear()) {
            final String pref_last_send_previous = "last_send_previous";
            final long last_send_previous = PersistentStore.getLong(pref_last_send_previous);
            PersistentStore.setLong(pref_last_send_previous, Math.min(last_send_previous, tsl() - maxBackfillPeriod_MS()));
        }
    }

    private static void processBacksies(List<BackFillStream.Backsie> backsies) {
        boolean changed = false;
        for (BackFillStream.Backsie backsie : backsies) {
            final long time = DexTimeKeeper.fromDexTime(getTransmitterID(), backsie.getDextime());

            final long since = JoH.msSince(time);
            if ((!shortTxId() && since > HOUR_IN_MS * 6) || (since > HOUR_IN_MS * 25) || (since < 0)) {
                UserError.Log.wtf(TAG, "Backfill timestamp unrealistic: " + JoH.dateTimeText(time) + " (ignored)");
            } else {
                if (BgReading.getForPreciseTimestamp(time, Constants.MINUTE_IN_MS * 4) == null) {
                    final BgReading bgr = BgReading.bgReadingInsertFromG5(backsie.getGlucose(), time, "Backfill");
                    lastGlucoseBgReading = bgr;
                    UserError.Log.d(TAG, "Adding backfilled reading: " + JoH.dateTimeText(time) + " " + BgGraphBuilder.unitized_string_static(backsie.getGlucose()));
                    changed = true;
                }
                UserError.Log.d(TAG, "Backsie: " + JoH.dateTimeText(time) + " " + BgGraphBuilder.unitized_string_static(backsie.getGlucose()));
            }
        }
        if (changed) {
            updateStreamedTillTimeForBackfill();
            Home.staticRefreshBGChartsOnIdle();
        }
    }

    private static void monitorBackFill(Ob1G5CollectionService parent, RxBleConnection connection) {
        if (d) UserError.Log.d(TAG, "monitor backfill enter");

        final BackFillStream backfill = new BackFillStream();

        connection.setupNotification(ProbablyBackfill)
                .timeout(15, TimeUnit.SECONDS) // WARN
                .observeOn(Schedulers.newThread())
                .flatMap(notificationObservable -> notificationObservable)
                .subscribe(bytes -> {
                            UserError.Log.d(TAG, "Received backfill notification bytes: " + bytesToHex(bytes));
                            backfill.pushNew(bytes);
                            inevitableDisconnect(parent, connection);
                            Inevitable.task("Process G5 backfill", 3000, () -> processBacksies(backfill.decode()));
                        }, throwable -> {
                            UserError.Log.d(TAG, "backfill throwable: " + throwable);
                        }
                );
        if (d) UserError.Log.d(TAG, "monitor backfill exit");
    }

    private static boolean verifyChallengeHash(final byte[] token) {
        if (lastAuthPacket == null) {
            UserError.Log.d(TAG, "Impossible sequence for verification");
            return false;
        }
        byte[] result;
        result = calculateChallengeHash(lastAuthPacket.singleUseToken);
        if (Arrays.equals(result, token)) {
            UserError.Log.d(TAG, "Challenge hash verified");
        } else {
            UserError.Log.wtf(TAG, "Could not verify challenge");
            return false;
        }
        return true;
    }


    private static synchronized byte[] calculateChallengeHash(final byte[] challenge) {
        final byte[] key = getCryptKey();
        if (key == null) {
            return null;
        }
        return calculateChallengeHash(challenge, key);
    }


    private static synchronized byte[] calculateChallengeHash(final byte[] challenge, final byte[] key) {
        if (challenge == null || challenge.length != 8) {
            UserError.Log.e(TAG, "Challenge length must be 8");
            return null;
        }
        if (key == null || key.length != 16) {
            UserError.Log.e(TAG, "Key length must be 16");
            return null;
        }

        final byte[] plainText = new byte[16];
        System.arraycopy(challenge, 0, plainText, 0, 8);
        System.arraycopy(challenge, 0, plainText, 8, 8);

        try {
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            @SuppressLint("GetInstance") final Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Arrays.copyOfRange(aesCipher.doFinal(plainText, 0, plainText.length), 0, 8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException | InvalidKeyException e) {
            UserError.Log.wtf(TAG, "System Encryption problem: " + e);
            return null;
        }

    }

    private static byte[] getCryptKey() {
        final String transmitterId = getTransmitterID();
        if (transmitterId.length() != 6)
            UserError.Log.e(TAG, "cryptKey: Wrong transmitter id length!: " + transmitterId.length());
        try {
            final String padding = "00";
            return (padding + transmitterId + padding + transmitterId).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            UserError.Log.wtf(TAG, "System encoding problem: " + e);
        }
        return null;
    }

    // types of packet we receive
    private enum PACKET {
        NULL,
        UNKNOWN,
        AuthChallengeRxMessage,
        AuthStatusRxMessage,
        SensorRxMessage,
        VersionRequestRxMessage,
        VersionRequest1RxMessage,
        VersionRequest2RxMessage,
        BatteryInfoRxMessage,
        SessionStartRxMessage,
        SessionStopRxMessage,
        GlucoseRxMessage,
        EGlucoseRxMessage,
        EGlucoseRxMessage2,
        CalibrateRxMessage,
        BackFillRxMessage,
        BackFillControlRxMessage,
        TransmitterTimeRxMessage,
        BondRequestRxMessage,
        F2DUnknownRxMessage,
        InvalidRxMessage,

    }

    // unified data structure
    private static class PacketShop {
        private PACKET type;
        private BaseMessage msg;

        PacketShop(PACKET type, BaseMessage msg) {
            this.type = type;
            this.msg = msg;
        }
    }

    // work out what type of packet we received and wrap it up nicely
    private static PacketShop classifyPacket(byte[] packet) {
        if ((packet == null) || (packet.length == 0)) return new PacketShop(PACKET.NULL, null);
        switch ((int) packet[0]) {
            case AuthChallengeRxMessage.opcode:
                return new PacketShop(PACKET.AuthChallengeRxMessage, new AuthChallengeRxMessage(packet));
            case AuthStatusRxMessage.opcode:
                return new PacketShop(PACKET.AuthStatusRxMessage, new AuthStatusRxMessage(packet));
            case SensorRxMessage.opcode:
                return new PacketShop(PACKET.SensorRxMessage, new SensorRxMessage(packet));
            case VersionRequestRxMessage.opcode:
                return new PacketShop(PACKET.VersionRequestRxMessage, new VersionRequestRxMessage(packet));
            case VersionRequest1RxMessage.opcode:
            case VersionRequest1RxMessage.opcode2:
                return new PacketShop(PACKET.VersionRequest1RxMessage, new VersionRequest1RxMessage(packet));
            case VersionRequest2RxMessage.opcode:
            case VersionRequest2RxMessage.opcode2:
                return new PacketShop(PACKET.VersionRequest2RxMessage, new VersionRequest2RxMessage(packet));
            case BatteryInfoRxMessage.opcode:
            case BatteryInfoRxMessage.opcode2:
                return new PacketShop(PACKET.BatteryInfoRxMessage, new BatteryInfoRxMessage(packet));
            case SessionStartRxMessage.opcode:
                return new PacketShop(PACKET.SessionStartRxMessage, new SessionStartRxMessage(packet, getTransmitterID()));
            case SessionStopRxMessage.opcode:
                return new PacketShop(PACKET.SessionStopRxMessage, new SessionStopRxMessage(packet, getTransmitterID()));
            case GlucoseRxMessage.opcode:
                return new PacketShop(PACKET.GlucoseRxMessage, new GlucoseRxMessage(packet));
            case EGlucoseRxMessage.opcode:
                return new PacketShop(PACKET.EGlucoseRxMessage, new EGlucoseRxMessage(packet));
            case CalibrateRxMessage.opcode:
                return new PacketShop(PACKET.CalibrateRxMessage, new CalibrateRxMessage(packet));
            case BackFillRxMessage.opcode:
                return new PacketShop(PACKET.BackFillRxMessage, new BackFillRxMessage(packet));
            case BackFillControlRxMessage.opcode:
                return new PacketShop(PACKET.BackFillControlRxMessage, new BackFillControlRxMessage(packet));
            case TransmitterTimeRxMessage.opcode:
                return new PacketShop(PACKET.TransmitterTimeRxMessage, new TransmitterTimeRxMessage(packet));
            case BondRequestTxMessage.opcode:
                return new PacketShop(PACKET.BondRequestRxMessage, null);
            case EGlucoseRxMessage2.opcode:
                return new PacketShop(PACKET.EGlucoseRxMessage2, new EGlucoseRxMessage2(packet));
            case F2DUnknownRxMessage.opcode:
                return new PacketShop(PACKET.F2DUnknownRxMessage, new F2DUnknownRxMessage(packet));
            case InvalidRxMessage.opcode:
                return new PacketShop(PACKET.InvalidRxMessage, new InvalidRxMessage(packet));

        }
        return new PacketShop(PACKET.UNKNOWN, null);
    }

    private static int getTokenSize() {
        return 8;
    }

    public static boolean usingG6() {
        return Pref.getBooleanDefaultFalse("using_g6");
    }

    private static boolean getEGlucose(final Ob1G5CollectionService parent) {
        // if (android_wear) {
        return usingG6() && Pref.getBooleanDefaultFalse("show_g_prediction") || parent.plugin != null;
        //  } else {
        //       return usingG6();
        //  }
    }

    public static boolean usingAlt() {
        return (android_wear && !Pref.getBooleanDefaultFalse("only_ever_use_wear_collector"))
                || (immediateBonding() && !ignoreBonding()) || WholeHouse.isLive();
    }

    private static class OperationSuccess extends RuntimeException {
        private OperationSuccess(String message) {
            super(message);
            UserError.Log.d(TAG, "Operation Success: " + message);
        }
    }

    private static byte[] nn(final byte[] array) {
        if (array == null) {
            if (JoH.ratelimit("never-null", 60)) {
                UserError.Log.wtf("NeverNullOb1", "Attempt to pass null!!! " + JoH.backTrace());
                return new byte[1];
            }
        }
        return array;
    }

    private static String devName() {
        return usingG6() ? "G6" : "G5";
    }
}
