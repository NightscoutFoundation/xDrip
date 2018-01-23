package com.eveningoutpost.dexdrip.G5Model;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.NotificationChannels;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleGattCharacteristicException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import rx.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.G5Model.BluetoothServices.Authentication;
import static com.eveningoutpost.dexdrip.G5Model.BluetoothServices.Control;
import static com.eveningoutpost.dexdrip.Services.G5BaseService.G5_BATTERY_FROM_MARKER;
import static com.eveningoutpost.dexdrip.Services.G5BaseService.G5_BATTERY_LEVEL_MARKER;
import static com.eveningoutpost.dexdrip.Services.G5BaseService.G5_BATTERY_MARKER;
import static com.eveningoutpost.dexdrip.Services.G5BaseService.G5_BATTERY_WEARABLE_SEND;
import static com.eveningoutpost.dexdrip.Services.G5BaseService.G5_FIRMWARE_MARKER;


/**
 * Created by jamorham on 17/09/2017.
 * <p>
 * Handles OB1 G5 communication logic
 */

@SuppressWarnings("AccessStaticViaInstance")
public class Ob1G5StateMachine {

    private static final String TAG = "Ob1G5StateMachine";
    private static final int LOW_BATTERY_WARNING_LEVEL = Pref.getStringToInt("g5-battery-warning-level", 300); // voltage a < this value raises warnings;
    private static final long BATTERY_READ_PERIOD_MS = Constants.HOUR_IN_MS * 12; // how often to poll battery data (12 hours)

    private static final boolean getVersionDetails = true; // try to load firmware version details
    private static final boolean getBatteryDetails = true; // try to load battery info details

    private static boolean speakSlowly = false; // slow down bluetooth comms for android wear etc

    private static final boolean d = false;

    // Auth Check + Request
    public static boolean doCheckAuth(Ob1G5CollectionService parent, RxBleConnection connection) {

        if (connection == null) return false;
        parent.msg("Authorizing");

        if (parent.android_wear) {
            speakSlowly = true;
            UserError.Log.d(TAG, "Setting speak slowly to true"); // WARN should be reactive or on named devices
        }

        final AuthRequestTxMessage authRequest = new AuthRequestTxMessage(getTokenSize());
        UserError.Log.i(TAG, "AuthRequestTX: " + JoH.bytesToHex(authRequest.byteSequence));

        connection.setupNotification(Authentication)
                // .timeout(10, TimeUnit.SECONDS)
                .timeout(15, TimeUnit.SECONDS) // WARN
                // .observeOn(Schedulers.newThread()) // needed?
                .doOnNext(notificationObservable -> {
                    connection.writeCharacteristic(Authentication, authRequest.byteSequence)
                            .subscribe(
                                    characteristicValue -> {
                                        // Characteristic value confirmed.
                                        if (d)
                                            UserError.Log.d(TAG, "Wrote authrequest, got: " + JoH.bytesToHex(characteristicValue));
                                        speakSlowly();
                                        connection.readCharacteristic(Authentication).subscribe(
                                                readValue -> {
                                                    PacketShop pkt = classifyPacket(readValue);
                                                    UserError.Log.d(TAG, "Read from auth request: " + pkt.type + " " + JoH.bytesToHex(readValue));

                                                    switch (pkt.type) {
                                                        case AuthChallengeRxMessage:
                                                            // Respond to the challenge request
                                                            byte[] challengeHash = calculateHash(((AuthChallengeRxMessage) pkt.msg).challenge);
                                                            if (d)
                                                                UserError.Log.d(TAG, "challenge hash" + Arrays.toString(challengeHash));
                                                            if (challengeHash != null) {
                                                                if (d)
                                                                    UserError.Log.d(TAG, "Transmitter trying auth challenge");

                                                                connection.writeCharacteristic(Authentication, new AuthChallengeTxMessage(challengeHash).byteSequence)
                                                                        .subscribe(
                                                                                challenge_value -> {

                                                                                    speakSlowly();

                                                                                    connection.readCharacteristic(Authentication)
                                                                                            //.observeOn(Schedulers.io())
                                                                                            .subscribe(
                                                                                                    status_value -> {
                                                                                                        // interpret authentication response
                                                                                                        final PacketShop status_packet = classifyPacket(status_value);
                                                                                                        UserError.Log.d(TAG, status_packet.type + " " + JoH.bytesToHex(status_value));
                                                                                                        if (status_packet.type == PACKET.AuthStatusRxMessage) {
                                                                                                            final AuthStatusRxMessage status = (AuthStatusRxMessage) status_packet.msg;
                                                                                                            if (d)
                                                                                                                UserError.Log.d(TAG, ("Authenticated: " + status.isAuthenticated() + " " + status.isBonded()));
                                                                                                            if (status.isAuthenticated()) {
                                                                                                                if (status.isBonded()) {
                                                                                                                    parent.msg("Authenticated");
                                                                                                                    parent.authResult(true);
                                                                                                                    parent.changeState(Ob1G5CollectionService.STATE.GET_DATA);
                                                                                                                    throw new OperationSuccess("Authenticated");
                                                                                                                } else {
                                                                                                                    //parent.unBond(); // bond must be invalid or not existing // WARN
                                                                                                                    parent.changeState(Ob1G5CollectionService.STATE.PREBOND);
                                                                                                                    // TODO what to do here?
                                                                                                                }
                                                                                                            } else {
                                                                                                                parent.msg("Not Authorized! (Wrong TxID?)");
                                                                                                                UserError.Log.e(TAG, "Authentication failed!!!!");
                                                                                                                parent.incrementErrors();
                                                                                                                // TODO? try again?
                                                                                                            }
                                                                                                        } else {
                                                                                                            UserError.Log.e(TAG, "Got unexpected packet when looking for auth status: " + status_packet.type + " " + JoH.bytesToHex(status_value));
                                                                                                            parent.incrementErrors();
                                                                                                            // TODO what to do here?
                                                                                                        }

                                                                                                    }, throwable -> {
                                                                                                        if (throwable instanceof OperationSuccess) {
                                                                                                            UserError.Log.d(TAG, "Stopping auth challenge listener due to success");
                                                                                                        } else {
                                                                                                            UserError.Log.e(TAG, "Could not read reply to auth challenge: " + throwable);
                                                                                                            parent.incrementErrors();
                                                                                                            speakSlowly = true;
                                                                                                        }
                                                                                                    });
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

                                                        default:
                                                            UserError.Log.e(TAG, "Unhandled packet type in reply: " + pkt.type + " " + JoH.bytesToHex(readValue));
                                                            parent.incrementErrors();
                                                            // TODO what to do here?
                                                            break;
                                                    }

                                                }, throwable -> {
                                                    UserError.Log.e(TAG, "Could not read after AuthRequestTX: " + throwable);
                                                });
                                        //parent.background_automata();
                                    },
                                    throwable -> {
                                        UserError.Log.e(TAG, "Could not write AuthRequestTX: " + throwable);
                                        parent.incrementErrors();
                                    }

                            );
                }).flatMap(notificationObservable -> notificationObservable)
                //.timeout(5, TimeUnit.SECONDS)
                //.observeOn(Schedulers.newThread())
                .subscribe(bytes -> {
                    // incoming notifications
                    UserError.Log.e(TAG, "Received Authentication notification bytes: " + JoH.bytesToHex(bytes));

                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        if ((parent.getState() == Ob1G5CollectionService.STATE.CLOSED) && (throwable instanceof BleDisconnectedException)) {
                            UserError.Log.d(TAG, "normal authentication notification throwable: (" + parent.getState() + ") " + throwable + " " + JoH.dateTimeText(JoH.tsl()));
                            parent.connectionStateChange("Closed OK");
                        } else if ((parent.getState() == Ob1G5CollectionService.STATE.BOND) && (throwable instanceof TimeoutException)) {
                            // TODO Trigger on Error count / Android wear metric
                            // UserError.Log.e(TAG,"Attempting to reset/create bond due to: "+throwable);
                            // parent.reset_bond(true);
                            // parent.unBond(); // WARN
                        } else {
                            UserError.Log.e(TAG, "authentication notification  throwable: (" + parent.getState() + ") " + throwable + " " + JoH.dateTimeText(JoH.tsl()));
                            parent.incrementErrors();
                            if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                parent.tryGattRefresh();
                            }
                        }
                        if ((throwable instanceof BleDisconnectedException) || (throwable instanceof TimeoutException)) {
                            if ((parent.getState() == Ob1G5CollectionService.STATE.BOND) || (parent.getState() == Ob1G5CollectionService.STATE.CHECK_AUTH)) {

                                if (parent.getState() == Ob1G5CollectionService.STATE.BOND) {
                                    UserError.Log.d(TAG, "SLEEPING BEFORE RECONNECT");
                                    threadSleep(15000);
                                }
                                UserError.Log.d(TAG, "REQUESTING RECONNECT");
                                parent.changeState(Ob1G5CollectionService.STATE.SCAN);
                            }
                        }
                    }
                });
        return true;
    }

    private static void speakSlowly() {
        if (speakSlowly) {
            UserError.Log.d(TAG, "Speaking slowly");
            threadSleep(300);
        }
    }

    private static void threadSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Failed to sleep for " + ms + " due to: " + e);
        }
    }

    // Handle bonding
    public synchronized static boolean doKeepAliveAndBondRequest(Ob1G5CollectionService parent, RxBleConnection connection) {

        if (connection == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserError.Log.d(TAG, "Requesting high priority");
            connection.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH, 500, TimeUnit.MILLISECONDS);
        }
        UserError.Log.e(TAG, "Sending keepalive..");
        connection.writeCharacteristic(Authentication, new KeepAliveTxMessage(25).byteSequence)
                .subscribe(
                        characteristicValue -> {
                            UserError.Log.d(TAG, "Wrote keep-alive request successfully");
                            speakSlowly(); // is this really needed here?
                            parent.unBond();
                            parent.instantCreateBond();
                            speakSlowly();
                            connection.writeCharacteristic(Authentication, new BondRequestTxMessage().byteSequence)
                                    .subscribe(
                                            bondRequestValue -> {
                                                UserError.Log.d(TAG, "Wrote bond request value: " + JoH.bytesToHex(bondRequestValue));
                                                speakSlowly();
                                                connection.readCharacteristic(Authentication)
                                                        .observeOn(Schedulers.io())
                                                        .timeout(10, TimeUnit.SECONDS)
                                                        .subscribe(
                                                                status_value -> {
                                                                    UserError.Log.d(TAG, "Got status read after keepalive " + JoH.bytesToHex(status_value));

                                                                    UserError.Log.d(TAG, "Wrote bond request successfully");
                                                                    parent.waitingBondConfirmation = 1; // waiting

                                                                    parent.instantCreateBond();
                                                                    UserError.Log.d(TAG, "Sleeping for bond");
                                                                    for (int i = 0; i < 9; i++) {
                                                                        if (parent.waitingBondConfirmation == 2) {
                                                                            UserError.Log.d(TAG, "Bond confirmation received - continuing!");
                                                                            break;
                                                                        }
                                                                        threadSleep(1000);
                                                                    }
                                                                    parent.changeState(Ob1G5CollectionService.STATE.BOND);
                                                                    throw new OperationSuccess("Bond requested");

//
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

    // Get Data
    public static boolean doGetData(Ob1G5CollectionService parent, RxBleConnection connection) {
        if (connection == null) return false;

        UserError.Log.d(TAG, "Requesting Sensor Data");

        connection.setupIndication(Control)

                .doOnNext(notificationObservable -> {

                    if (d) UserError.Log.d(TAG, "Notifications enabled");
                    speakSlowly();
                    connection.writeCharacteristic(Control, new SensorTxMessage().byteSequence)
                            .subscribe(
                                    characteristicValue -> {
                                        if (d)
                                            UserError.Log.d(TAG, "Wrote SensorTxMessage request");
                                    }, throwable -> {
                                        UserError.Log.e(TAG, "Failed to write SensorTxMessage: " + throwable);
                                        if (throwable instanceof BleGattCharacteristicException) {
                                            final int status = ((BleGattCharacteristicException) throwable).getStatus();
                                            UserError.Log.e(TAG, "Got status message: " + BluetoothServices.getStatusName(status));
                                            if (status == 8) {
                                                UserError.Log.e(TAG, "Request rejected due to Insufficient Authorization failure!");
                                                parent.authResult(false);
                                            }
                                        }
                                    });

                })
                .flatMap(notificationObservable -> notificationObservable)
                .timeout(6, TimeUnit.SECONDS)
                .subscribe(bytes -> {
                    // incoming data notifications
                    UserError.Log.d(TAG, "Received indication bytes: " + JoH.bytesToHex(bytes));
                    final PacketShop data_packet = classifyPacket(bytes);
                    switch (data_packet.type) {
                        case SensorRxMessage:

                            try {
                                if ((getVersionDetails) && (!haveFirmwareDetails())) {
                                    connection.writeCharacteristic(Control, new VersionRequestTxMessage().byteSequence)
                                            .subscribe(versionValue -> {
                                                UserError.Log.d(TAG, "Wrote version request");
                                            }, throwable -> {
                                                UserError.Log.e(TAG, "Failed to write VersionRequestTxMessage: " + throwable);
                                            });
                                } else if ((getBatteryDetails) && (parent.getBatteryStatusNow || !haveCurrentBatteryStatus())) {
                                    connection.writeCharacteristic(Control, new BatteryInfoTxMessage().byteSequence)
                                            .subscribe(batteryValue -> {
                                                UserError.Log.d(TAG, "Wrote battery info request");
                                                parent.getBatteryStatusNow = false;
                                            }, throwable -> {
                                                UserError.Log.e(TAG, "Failed to write BatteryInfoRequestTxMessage: " + throwable);
                                            });

                                } else {
                                    disconnectNow(parent, connection);
                                    throw new OperationSuccess("Got Data!");

                                }
                            } finally {
                                processSensorRxMessage((SensorRxMessage) data_packet.msg);
                                parent.msg("Got data");
                                parent.updateLast(JoH.tsl());
                                parent.clearErrors();
                            }


                            break;

                        case VersionRequestRxMessage:
                            if (!setStoredFirmwareBytes(Ob1G5CollectionService.getTransmitterID(), bytes, true)) {
                                UserError.Log.e(TAG, "Could not save out firmware version!");
                            }
                            disconnectNow(parent, connection);
                            throw new OperationSuccess("Received Version Info");
                            //break;
                        case BatteryInfoRxMessage:
                            if (!setStoredBatteryBytes(Ob1G5CollectionService.getTransmitterID(), bytes)) {
                                UserError.Log.e(TAG, "Could not save out battery data!");
                            } else {
                                if (parent.android_wear) {
                                    PersistentStore.setBoolean(G5_BATTERY_WEARABLE_SEND, true);
                                }
                            }
                            disconnectNow(parent, connection);
                            throw new OperationSuccess("Received Battery Info");
                            //break;

                        default:
                            UserError.Log.e(TAG, "Got unknown packet instead of sensor rx: " + JoH.bytesToHex(bytes));
                            break;
                    }
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        if (throwable instanceof BleDisconnectedException) {
                            UserError.Log.d(TAG, "Disconnected when waiting to receive indication: " + throwable);
                        } else {
                            UserError.Log.e(TAG, "Error receiving indication: " + throwable);
                        }
                    }
                });


        return true;
    }

    private static void disconnectNow(Ob1G5CollectionService parent, RxBleConnection connection) {
        // tell device to disconnect now
        UserError.Log.d(TAG, "Disconnect NOW: " + JoH.dateTimeText(JoH.tsl()));
        speakSlowly();
        connection.writeCharacteristic(Control, new DisconnectTxMessage().byteSequence)
                .timeout(2, TimeUnit.SECONDS)
                //  .observeOn(Schedulers.newThread())
                //  .subscribeOn(Schedulers.newThread())
                .subscribe(disconnectValue -> {
                    if (d) UserError.Log.d(TAG, "Wrote disconnect request");
                    parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                    throw new OperationSuccess("Requested Disconnect");
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        UserError.Log.d(TAG, "Disconnect NOW failure: " + JoH.dateTimeText(JoH.tsl()));
                        if (throwable instanceof BleDisconnectedException) {
                            UserError.Log.d(TAG, "Failed to write DisconnectTxMessage as already disconnected: " + throwable);

                        } else {
                            UserError.Log.e(TAG, "Failed to write DisconnectTxMessage: " + throwable);

                        }
                        parent.changeState(Ob1G5CollectionService.STATE.CLOSE);
                    }
                });
        UserError.Log.d(TAG, "Disconnect NOW exit: " + JoH.dateTimeText(JoH.tsl()));
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

        UserError.Log.d(TAG, "SUCCESS!! unfiltered: " + sensorRx.unfiltered + " timestamp: " + sensorRx.timestamp + " " + JoH.qs((double) sensorRx.timestamp / 86400, 1) + " days");
        if (sensorRx.unfiltered == 0) {
            UserError.Log.e(TAG, "Transmitter sent raw sensor value of 0 !! This isn't good. " + JoH.hourMinuteString());
        } else {
            processNewTransmitterData(sensorRx.unfiltered, sensorRx.filtered, sensor_battery_level, new Date().getTime());
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
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            UserError.Log.e(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        //TODO : LOG if unfiltered or filtered values are zero

        Sensor.updateBatteryLevel(sensor, transmitterData.sensor_battery_level);
        if (d)
            UserError.Log.i(TAG, "timestamp create: " + Long.toString(transmitterData.timestamp));

        BgReading bgreading = BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);

        UserError.Log.d(TAG, "Dex raw_data " + Double.toString(transmitterData.raw_data));//KS
        UserError.Log.d(TAG, "Dex filtered_data " + Double.toString(transmitterData.filtered_data));//KS
        UserError.Log.d(TAG, "Dex sensor_battery_level " + Double.toString(transmitterData.sensor_battery_level));//KS
        UserError.Log.d(TAG, "Dex timestamp " + JoH.dateTimeText(transmitterData.timestamp));//KS

        UserError.Log.d(TAG, "BgReading created: " + bgreading.uuid + " " + JoH.dateTimeText(bgreading.timestamp));

        // TODO static_last_timestamp =  transmitterData.timestamp;

    }

    private static boolean haveFirmwareDetails() {
        return Ob1G5CollectionService.getTransmitterID().length() == 6 && getStoredFirmwareBytes(Ob1G5CollectionService.getTransmitterID()).length >= 10;
    }


    private static boolean haveCurrentBatteryStatus() {
        return Ob1G5CollectionService.getTransmitterID().length() == 6 && (JoH.msSince(PersistentStore.getLong(G5_BATTERY_FROM_MARKER + Ob1G5CollectionService.getTransmitterID())) < BATTERY_READ_PERIOD_MS);
    }

    private static byte[] getStoredFirmwareBytes(String transmitterId) {
        if (transmitterId.length() != 6) return new byte[0];
        return PersistentStore.getBytes(G5_FIRMWARE_MARKER + transmitterId);
    }

    // from wear sync
    public static boolean setStoredFirmwareBytes(String transmitterId, byte[] data) {
        return setStoredFirmwareBytes(transmitterId, data, false);
    }

    public static boolean setStoredFirmwareBytes(String transmitterId, byte[] data, boolean from_bluetooth) {
        if (from_bluetooth) UserError.Log.e(TAG, "Store: VersionRX dbg: " + JoH.bytesToHex(data));
        if (transmitterId.length() != 6) return false;
        if (data.length < 10) return false;
        if (JoH.ratelimit("store-firmware-bytes", 60)) {
            PersistentStore.setBytes(G5_FIRMWARE_MARKER + transmitterId, data);
        }
        return true;
    }



    public synchronized static boolean setStoredBatteryBytes(String transmitterId, byte[] data) {
        UserError.Log.e(TAG, "Store: BatteryRX dbg: " + JoH.bytesToHex(data));
        if (transmitterId.length() != 6) return false;
        if (data.length < 10) return false;
        final BatteryInfoRxMessage batteryInfoRxMessage = new BatteryInfoRxMessage(data);
        UserError.Log.e(TAG, "Saving battery data: " + batteryInfoRxMessage.toString());
        PersistentStore.setBytes(G5_BATTERY_MARKER + transmitterId, data);
        PersistentStore.setLong(G5_BATTERY_FROM_MARKER + transmitterId, JoH.tsl());

        // TODO logic also needs to handle battery replacements of same transmitter id
        final long old_level = PersistentStore.getLong(G5_BATTERY_LEVEL_MARKER + transmitterId);
        if ((batteryInfoRxMessage.voltagea < old_level) || (old_level == 0)) {
            if (batteryInfoRxMessage.voltagea < LOW_BATTERY_WARNING_LEVEL) {
                if (JoH.pratelimit("g5-low-battery-warning", 40000)) {
                    final boolean loud = !PowerStateReceiver.is_power_connected();
                    JoH.showNotification("G5 Battery Low", "G5 Transmitter battery has dropped to: " + batteryInfoRxMessage.voltagea + " it may fail soon",
                            null, 770, NotificationChannels.LOW_TRANSMITTER_BATTERY_CHANNEL, loud, loud, null, null, null);
                }
            }
            PersistentStore.setLong(G5_BATTERY_LEVEL_MARKER + transmitterId, batteryInfoRxMessage.voltagea);
        }
        return true;
    }

    public static BatteryInfoRxMessage getBatteryDetails(String tx_id) {
        try {
            return new BatteryInfoRxMessage(PersistentStore.getBytes(G5_BATTERY_MARKER + tx_id));
        } catch (Exception e) {
            if (JoH.quietratelimit("bi-exception", 15))
                UserError.Log.e(TAG, "Exception in getBatteryDetails: " + e);
            return null;
        }
    }

    public static VersionRequestRxMessage getFirmwareDetails(String tx_id) {
        if (tx_id == null) {
            if (JoH.quietratelimit("txid-null", 15))
                UserError.Log.e(TAG, "TX ID is null in getFirmwareDetails");
            return null;
        }
        try {
            byte[] stored = getStoredFirmwareBytes(tx_id);
            if ((stored != null) && (stored.length > 9)) {
                return new VersionRequestRxMessage(stored);
            }
        } catch (Exception e) {
            if (JoH.quietratelimit("fi-exception", 15))
                UserError.Log.e(TAG, "Exception in getFirmwareDetails: " + e);
            return null;
        }
        return null;
    }

    public static String getFirmwareVersionString(String tx_id) {
        VersionRequestRxMessage vr = getFirmwareDetails(tx_id);
        if (vr != null) {
            return "FW: " + vr.firmware_version_string;
        } else {
            return "";
        }
    }


    private static synchronized byte[] calculateHash(byte[] data) {
        if (data.length != 8) {
            UserError.Log.e(TAG, "Data length should be exactly 8.");
            return null;
        }

        final byte[] key = cryptKey();
        if (key == null)
            return null;

        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(data);
        bb.put(data);

        final byte[] doubleData = bb.array();

        final Cipher aesCipher;
        try {
            aesCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] aesBytes = aesCipher.doFinal(doubleData, 0, doubleData.length);

            bb = ByteBuffer.allocate(8);
            bb.put(aesBytes, 0, 8);

            return bb.array();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static byte[] cryptKey() {
        final String transmitterId = Ob1G5CollectionService.getTransmitterID();
        if (transmitterId.length() != 6)
            UserError.Log.e(TAG, "cryptKey: Wrong transmitter id length!: " + transmitterId.length());
        try {
            return ("00" + transmitterId + "00" + transmitterId).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
        BatteryInfoRxMessage

    }

    // unified data structure
    private static class PacketShop {
        private PACKET type;
        private TransmitterMessage msg;

        PacketShop(PACKET type, TransmitterMessage msg) {
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
            case BatteryInfoRxMessage.opcode:
                return new PacketShop(PACKET.BatteryInfoRxMessage, new BatteryInfoRxMessage(packet));
        }
        return new PacketShop(PACKET.UNKNOWN, null);
    }

    private static int getTokenSize() {
        return 8;
    }

    private static class OperationSuccess extends RuntimeException {
        private OperationSuccess(String message) {
            super(message);
            UserError.Log.d(TAG, "Operation Success: " + message);
        }
    }
}
