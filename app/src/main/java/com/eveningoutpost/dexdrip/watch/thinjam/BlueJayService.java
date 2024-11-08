package com.eveningoutpost.dexdrip.watch.thinjam;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import androidx.databinding.ObservableField;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Pair;

import com.eveningoutpost.dexdrip.cloud.jamcm.Pusher;
import com.eveningoutpost.dexdrip.g5model.CalibrationState;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.BroadcastSnooze;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.ui.activities.ThinJamActivity;
import com.eveningoutpost.dexdrip.utils.BytesGenerator;
import com.eveningoutpost.dexdrip.utils.bt.Helper;
import com.eveningoutpost.dexdrip.utils.bt.ReplyProcessor;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.utils.time.SlidingWindowConstraint;
import com.eveningoutpost.dexdrip.watch.thinjam.firmware.FirmwareDownload;
import com.eveningoutpost.dexdrip.watch.thinjam.firmware.FirmwareInfo;
import com.eveningoutpost.dexdrip.watch.thinjam.io.ThinJamAudioStream;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.AuthReqTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.BackFillTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.BaseTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.BulkUpRequestTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.BulkUpTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.DefineWindowTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.GlucoseTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.PushRx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.RBulkUpTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.ResetPersistTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.SetGammaEtc;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.SetTimeTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.SetTxIdTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.StandbyTx;
import com.eveningoutpost.dexdrip.watch.thinjam.utils.BitmapTools;
import com.eveningoutpost.dexdrip.watch.thinjam.utils.BitmapTools.TJ_BitmapType;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;

import static com.eveningoutpost.dexdrip.models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.msTill;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.threadSleep;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.CLOSED;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.DISCOVER;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.SEND_QUEUE;
import static com.eveningoutpost.dexdrip.services.JamBaseBluetoothSequencer.BaseState.SLEEP;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.CRITICAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJay.shouldSendReadings;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJay.versionSufficient;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.ENABLE_NOTIFICATIONS;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.LOG_TESTS;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.OTAUI;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.PROTOTYPE;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.RUN_QUEUE;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.SCHEDULE_ITEMS;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService.ThinJamState.SET_TIME;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_INVALID;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_MISC;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_OK;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_OUT_OF_RANGE;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.FEATURE_TJ_DISP_A;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_BULK_R_XFER_0;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_EASY_AUTH;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_GET_STATUS_1;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_GET_STATUS_2;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_GET_STATUS_3;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_IDENTIFY;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_RESET_ALL;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SHOW_QRCODE;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_BULK;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_CANCEL;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_DIALOG;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_HIGH_ALERT;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_LOW_ALERT;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXTBOX1;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXTBOX2;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_OTA;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_WRITE;
import static com.eveningoutpost.dexdrip.watch.thinjam.firmware.BlueJayFirmware.parse;
import static com.eveningoutpost.dexdrip.watch.thinjam.firmware.BlueJayFirmware.split;
import static com.eveningoutpost.dexdrip.watch.thinjam.messages.NotifyTx.getPacketStreamForNotification;
import static com.eveningoutpost.dexdrip.xdrip.gs;

// jamorham

@SuppressLint("CheckResult")
public class BlueJayService extends JamBaseBluetoothSequencer {


    private static final String TAG = "BlueJayServiceR";
    private static final boolean D = false;      // TODO change to false for production

    private static volatile long awaiting_easy_auth = 0;
    private static volatile long lastLongPress1 = 0;
    private static volatile long lastUsableGlucoseTimestamp = 0;
    private static volatile long lastBulkOk = 0;

    private static final SlidingWindowConstraint connectionTracker = new SlidingWindowConstraint(50, 20 * Constants.MINUTE_IN_MS, "max_bluejay_reconnects");
    private final IBinder binder = new LocalBinder();
    @Getter
    private final DebugUnitTestLogger debug = new DebugUnitTestLogger(this);

    volatile Subscription notificationSubscription;
    @Setter
    private volatile Runnable postQueueRunnable;

    public static volatile boolean flashIsRunning = false;
    public volatile boolean sleepAfterReset = false;

    @Setter
    ObservableField<Integer> progressIndicator;

    @RequiredArgsConstructor
    public static class ThinJamItem {
        @Expose
        final int x, y, width, height;
        final byte[] buffer;
        int type = 1;
        int windowType = 1;
        int colourEffect = 0;
        boolean quiet = true;
        int retries = 20;
        int retried = 0;
        Integer specificId;
        int sequence = 0;
        long queuedTimestamp = System.currentTimeMillis();
        volatile boolean inProgress;

        public String toS() {
            return JoH.defaultGsonInstance().toJson(this);
        }

        public ThinJamItem setType(int type) {
            this.type = type;
            return this;
        }

        public ThinJamItem useAck() {
            this.quiet = false;
            return this;
        }

        public ThinJamItem setWindowType(int type) {
            this.windowType = type;
            return this;
        }

        public ThinJamItem setColourEffect(int type) {
            this.colourEffect = type;
            return this;
        }

        public ThinJamItem setSpecificId(int id) {
            this.specificId = id;
            return this;
        }

        public ThinJamItem setSequence(int id) {
            this.sequence = id;
            return this;
        }

        public boolean retryCounterOk() {
            if (retries == retried) {
                return false;
            } else {
                retried++;
                return true;
            }
        }

        public void dontRetryAgain() {
            retries = retried;
        }

        public int getId() {
            return this.specificId != null ? this.specificId : (this.width == 0 ? 0 : 1);
        }
    }

    public StringBuilder notificationString = new StringBuilder();
    public ObservableField<String> stringObservableField = new ObservableField<>();

    public static final ConcurrentLinkedQueue<ThinJamItem> commandQueue = new ConcurrentLinkedQueue<>();

    {
        mState = new ThinJamState().setLI(I);
        I.backgroundStepDelay = 0;
        //setAutoConnect();
        I.autoReConnect = true; // TODO control these two from preference?
        //   I.playSounds = true;
        I.connectTimeoutMinutes = 55;
        I.resetWhenAlreadyConnected = true;
        I.useReconnectHandler = true;
        I.reconnectConstraint = new SlidingWindowConstraint(30, MINUTE_IN_MS, "max_reconnections");

        I.queue_write_characterstic = THINJAM_WRITE;
        setMac(BlueJay.getMac());
        //I.resetWhenAlreadyConnected = true;
    }

    private void setAutoConnect() {
        I.autoConnect = true; // must always be true at the moment
        //I.autoConnect = (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) || Pref.getBoolean("bluetooth_trust_autoconnect", true);
    }

    public BlueJayInfo getInfo() {
        return BlueJayInfo.getInfo(I.address);
    }

    public void setMac(final String mac) {
        // TODO clear queue
        setAddress(mac);
        BlueJay.setMac(mac);

    }

    public void setSettings() {
        setSettings(Pref.getString("dex_txid", "").trim().toUpperCase());
    }

    public void setSettings(String txid) {
        if (txid.length() == 4) {
            txid = "0B" + txid;
        }
        if (txid.length() == 6) {
            queueGenericCommand(new SetTxIdTx(txid, "00:00:00:00:00:00").getBytes(), "Set TxId: " + txid, null);
        } else {
            JoH.static_toast_long("Invalid TXID: " + txid);
        }
    }


    private volatile SetTimeTx timeOutbound;

    public void setTime() {

        new QueueMe().setGenerator(new BytesGenerator() {
            @Override
            public byte[] produce() {
                timeOutbound = new SetTimeTx();
                return timeOutbound.getBytes();
            }
        })
                .setDescription("Set time")
                .expireInSeconds(30)
                .setProcessor(new ReplyProcessor(I.connection) {
                    @Override
                    public void process(byte[] bytes) {
                        final SetTimeTx reply = new SetTimeTx(bytes);
                        UserError.Log.d(TAG, "Time Process callback: " + JoH.bytesToHex(bytes));
                        getInfo().parseSetTime(reply, timeOutbound);
                        UserError.Log.d(TAG, "Time difference with watch: " + ((timeOutbound.getTimestamp() - reply.getTimestamp()) / 1000d));
                    }
                })
                .queue();
    }

    public void getBackFill(int records) {
        if (BlueJay.haveAuthKey(I.address)) {
            if (!flashRunning()) {
                val outbound = new BackFillTx(records);
                val item = new QueueMe().setBytes(outbound.getBytes())
                        .setDescription("Get BackFill " + records)
                        .expireInSeconds(30);

                item.setProcessor(new AuthReplyProcessor(new ReplyProcessor(I.connection) {
                    @Override
                    public void process(byte[] bytes) {
                        UserError.Log.d(TAG, "BackFill request response: " + JoH.bytesToHex(bytes));
                    }
                }).setTag(item))
                        .queue();
            } else {
                UserError.Log.d(TAG, "No backfill as flash running");
            }
        } else {
            UserError.Log.d(TAG, "Cannot back fill as we don't have auth key");
        }
    }

    private static int recordsPerHour() {
        return (int) (HOUR_IN_MS / DEXCOM_PERIOD); // TODO collection period
    }

    private static Pair<Long, Long> getBackFillStatus() {
        final long maxBackfillPeriodMs = getMaxBackFillHours() * HOUR_IN_MS; // how far back to request backfill data
        final int check_readings = getMaxBackFillHours() * recordsPerHour();
        UserError.Log.d(TAG, "Checking " + check_readings + " for backfill requirement");
        final List<BgReading> lastReadings = BgReading.latest_by_size(check_readings);
        boolean ask_for_backfill = false;
        long earliest_timestamp = tsl() - maxBackfillPeriodMs;
        long latest_timestamp = tsl();
        if ((lastReadings == null) || (lastReadings.size() != check_readings)) {
            ask_for_backfill = true;
        } else {
            for (int i = 0; i < lastReadings.size(); i++) {
                final BgReading reading = lastReadings.get(i);
                if ((reading == null) || (msSince(reading.timestamp) > ((DEXCOM_PERIOD * i) + Constants.MINUTE_IN_MS * 7))) {
                    ask_for_backfill = true;
                    if ((reading != null) && (msSince(reading.timestamp) <= maxBackfillPeriodMs)) {
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
        return new Pair<>(ask_for_backfill ? earliest_timestamp : -1, latest_timestamp);
    }


    @Override
    public void onDestroy() {
        // TODO empty queue?
        super.onDestroy();
    }


    public void getStatus() {
        setTime();
        setTime();
        getStatus1();
        getStatus2();
        getStatus3();
        doQueue();
    }

    public Inst getI() {
        return I;
    }

    public BaseState getmState() {
        return mState;
    }

    public void getStatus1() {
        new QueueMe().setBytes(new byte[]{OPCODE_GET_STATUS_1})
                .setDescription("Get Status 1")
                .expireInSeconds(30)
                .setProcessor(new ReplyProcessor(I.connection) {
                    @Override
                    public void process(byte[] bytes) {
                        UserError.Log.d(TAG, "Status 1 Process callback: " + JoH.bytesToHex(bytes));
                        BlueJayInfo.getInfo(I.address).parseStatus1(bytes);
                    }
                })
                .queue();
    }

    public void getStatus2() {
        new QueueMe().setBytes(new byte[]{OPCODE_GET_STATUS_2})
                .setDescription("Get Status 2")
                .expireInSeconds(30)
                .setProcessor(new ReplyProcessor(I.connection) {
                    @Override
                    public void process(byte[] bytes) {
                        UserError.Log.d(TAG, "Status 2 Process callback: " + JoH.bytesToHex(bytes));
                        BlueJayInfo.getInfo(I.address).parseStatus2(bytes);
                        UserError.Log.d(TAG, BlueJayInfo.getInfo(I.address).toS());
                        addToLog("Status information received");
                        // TODO check validity of status2 msg??
                        if (BlueJay.isCollector()) {
                            processInboundGlucose();
                        }
                    }
                })
                .queueUnique();
    }

    public void getStatus3() {
        if (BlueJayInfo.getInfo(I.address).buildNumber >= 2092) {
            new QueueMe().setBytes(new byte[]{OPCODE_GET_STATUS_3})
                    .setDescription("Get Status 3")
                    .expireInSeconds(30)
                    .setProcessor(new ReplyProcessor(I.connection) {
                        @Override
                        public void process(byte[] bytes) {
                            UserError.Log.d(TAG, "Status 3 Process callback: " + JoH.bytesToHex(bytes));
                            BlueJayInfo.getInfo(I.address).parseStatus3(bytes);
                            UserError.Log.d(TAG, BlueJayInfo.getInfo(I.address).toS());
                        }
                    })
                    .queueUnique();
        }
    }

    // only called for status 2 readings
    public void processInboundGlucose() {
        final BlueJayInfo info = BlueJayInfo.getInfo(I.address);
        final long inboundTimestamp = info.getTimestamp();
        UserError.Log.d(TAG, "Processing inbound glucose: " + JoH.dateTimeText(info.getTimestamp())+" "+info.toS());
        // TODO allow only tighter windows for sync
        if (inboundTimestamp > 1561900000000L && inboundTimestamp < (tsl() + (Constants.MINUTE_IN_MS * 5))) {
            final BgReading bgReading = BgReading.last();

            if (bgReading == null || msSince(bgReading.timestamp) > Constants.MINUTE_IN_MS * 4) { // TODO collector frequency de-dupe period instead
                // is a new reading
                Ob1G5CollectionService.processCalibrationStateLite(CalibrationState.parse(info.state), inboundTimestamp);       /// TODO revisit
                if (D && info.glucose == 1) {
                    info.glucose = 123;         // TODO THIS IS DEBUG ONLY!!
                }
                if (Ob1G5CollectionService.lastSensorState.usableGlucose()) {
                    UserError.Log.d(TAG, "USABLE GLUCOSE");
                    lastUsableGlucoseTimestamp = inboundTimestamp;
                    final BgReading existing = BgReading.getForPreciseTimestamp(inboundTimestamp, Constants.MINUTE_IN_MS * 4, false);
                    if (existing == null) {
                        val last = BgReading.last();
                        final BgReading bgr = BgReading.bgReadingInsertFromG5(info.glucose, inboundTimestamp, "BlueJay");
                        try {
                            bgr.calculated_value_slope = info.getTrend() / Constants.MINUTE_IN_MS; // note this is different to the typical calculated slope, (normally delta)
                            if (bgReading.calculated_value_slope == Double.NaN) {
                                bgReading.hide_slope = true;
                            }
                        } catch (Exception e) {
                            // not a good number - does this exception ever actually fire?
                        }
                        if (bgr != null && bgr.timestamp > last.timestamp) {
                            UserError.Log.d(TAG, "Post processing new reading: " + JoH.dateTimeText(bgr.timestamp));
                            bgr.postProcess(false);
                        }
                    } else {
                        UserError.Log.d(TAG, "Ignoring status glucose reading as we already have one within 4 mins");
                    }
                } else {
                    UserError.Log.d(TAG, "Glucose value not reported as usable");
                }
                // TODO add trend - done in post process
            }
        } else {
            UserError.Log.d(TAG, "No valid timestamp for inbound glucose data");
        }
    }

    public void sendGlucose() {
        val last = BgReading.last();
        if (last != null && msSince(last.timestamp) < Constants.HOUR_IN_MS) {
            val info = BlueJayInfo.getInfo(BlueJay.getMac());
            if (Math.abs(info.lastReadingTime - last.timestamp) > Constants.MINUTE_IN_MS * 3) {
                val glucoseTx = new GlucoseTx(last);
                if (glucoseTx.isValid()) {
                    val item = new QueueMe()
                            .setBytes(glucoseTx.getBytes())
                            .expireInSeconds(60)
                            .setDescription("Glucose to watch");

                    item.setProcessor(new AuthReplyProcessor(new ReplyProcessor(I.connection) {
                        @Override
                        public void process(byte[] bytes) {
                            UserError.Log.d(TAG, "Glucose Incoming reply processor: " + HexDump.dumpHexString(bytes));
                            info.displayUpdated();
                        }
                    }).setTag(item));

                    item.queue();
                    doQueue();
                } else {
                    UserError.Log.d(TAG, "GlucoseTX wasn't valid so not sending.");
                }
            } else {
                UserError.Log.d(TAG, "Watch already has recent reading");
                // watch reading too close to the reading we were going to send
            }
        }
    }

    public void setGamma(int value) {
        if (value <= 255) {
            queueGenericCommand(new SetGammaEtc(value).getBytes(), "Set Gamma " + value, null);
        } else {
            UserError.Log.e(TAG, "Gamma out of range: " + value);
        }
    }

    public void standby() {
        if (BlueJayInfo.getInfo(I.address).buildNumber > 39) {
            addToLog("Requesting watch standby");
            val description = "Watch Standby";
            val item = new QueueMe()
                    .setBytes(new StandbyTx().getBytes());
            item.setDescription(description)
                    .setProcessor(new AuthReplyProcessor(new ReplyProcessor(I.connection) {
                        @Override
                        public void process(byte[] bytes) {
                            UserError.Log.d(TAG, "Reply for: " + description + " " + JoH.bytesToHex(bytes));
                        }
                    }).setTag(item))
                    .expireInSeconds(60);

            item.queueUnique();
            doQueue();
            invalidateCache();
        } else {
            JoH.static_toast_long("Needs BlueJay firmware upgrade to support standby");
        }
    }

    public void reboot() {
        queueSingleByteCommand(OPCODE_RESET_ALL, "Reset all");
        invalidateCache();
    }

    public void factoryReset() {
        queueGenericCommand(new ResetPersistTx(sleepAfterReset).getBytes(), "Factory Reset", null);
        invalidateCache();
    }

    public void showQrCode() {
        addToLog("Asking watch to show a QR code");
        queueSingleByteCommand(OPCODE_SHOW_QRCODE, "Show QR Code", () -> ThinJamActivity.launchQRScan());
    }

    public void easyAuth() {
        addToLog("Attempting easy authentication");
        val item = new QueueMe()
                .setBytes(new byte[]{OPCODE_EASY_AUTH})
                .setDescription("Get easy auth")
                .expireInSeconds(60);

        item.setProcessor(new ReplyProcessor(I.connection) {
            @Override
            public void process(byte[] bytes) {
                if (bytes.length == 18) {
                    if (bytes[0] == (byte) 0x41 && bytes[1] == (byte) 0x93) {
                        final String identityHex = JoH.bytesToHex(Arrays.copyOfRange(bytes, 2, 18));
                        UserError.Log.d(TAG, "Storing easy auth " + I.address + " of: " + identityHex);
                        BlueJay.storeAuthKey(I.address, identityHex);
                        identify();
                    }
                } else if (bytes.length == 2) {
                    switch (bytes[1]) {
                        case ERROR_INVALID:
                            addToLog("Cannot easy auth as not connected to charger");
                            break;
                        case ERROR_OUT_OF_RANGE:
                            addToLog("Cannot easy auth as button is already pressed");
                            break;
                        case ERROR_MISC:
                            addToLog("Already authenticated, no need to easy auth");
                            break;
                        case ERROR_OK:
                            addToLog("Easy Auth mode - press the button for 2 seconds");
                            awaiting_easy_auth = tsl();
                            break;
                    }
                } else {
                    UserError.Log.d(TAG, "Wrong size for easy auth reply: " + bytes.length);
                }
            }
        }).queue();
        doQueue();
    }

    public void identify() {

        addToLog("Pairing / Identifying Watch");
        val item = new QueueMe()
                .setBytes(new byte[]{OPCODE_IDENTIFY})
                .setDescription("Get identity value")
                .setDelayMs(300)
                .expireInSeconds(60);

        item.setProcessor(new AuthReplyProcessor(new ReplyProcessor(I.connection) {
            @Override
            public void process(byte[] bytes) {
                if (bytes.length == 18) {
                    if (bytes[0] == (byte) 0x41 && bytes[1] == (byte) 0x92) {
                        final String identityHex = JoH.bytesToHex(Arrays.copyOfRange(bytes, 2, 18));
                        UserError.Log.d(TAG, "Storing identity for " + I.address + " of: " + identityHex);
                        BlueJay.storeIdentityKey(I.address, identityHex);
                    }
                } else {
                    addToLog("Got wrong reply from pairing - try again");
                    UserError.Log.d(TAG, "Wrong size for identity reply: " + bytes.length + " " + JoH.bytesToHex(bytes));
                }
            }

        }).setTag(item)).queue();
        doQueue();

    }

    private boolean isAuthRequiredPacket(final byte[] bytes) {
        return (bytes != null && bytes.length == 2 && bytes[1] == 0x0F);
    }

    private void authenticate(final QueueMe requeueItem) {
        val authReply = AuthReqTx.getNextAuthPacket(null);
        new QueueMe()
                .setBytes(authReply.getBytes())
                .setDescription("Auth hello packet")
                .setProcessor(new ReplyProcessor(I.connection) {
                    @Override
                    public void process(byte[] bytes) {
                        UserError.Log.d(TAG, "Processing likely auth challenge");
                        val authReply2 = AuthReqTx.getNextAuthPacket(bytes);
                        if (authReply2 != null) {
                            new QueueMe()
                                    .setBytes(authReply2.getBytes())
                                    .setDescription("Auth challenge reply")
                                    .setProcessor(new ReplyProcessor(I.connection) {
                                        @Override
                                        public void process(byte[] bytes) {
                                            if (AuthReqTx.isAccessGranted(bytes)) {
                                                UserError.Log.d(TAG, "Authentication complete!");
                                                if (requeueItem != null) {
                                                    // retry the item that we got stopped for authentication
                                                    requeueItem.insert();
                                                }
                                            } else {
                                                UserError.Log.e(TAG, "Authentication failed! " + JoH.bytesToHex(bytes));
                                                // TODO something here
                                            }
                                        }
                                    })
                                    .insert();
                        }
                    }
                })
                .expireInSeconds(60)
                .insert();
    }

    public void queueSingleByteCommand(final byte cmd, final String description) {
        queueSingleByteCommand(cmd, description, null);
    }

    public void queueSingleByteCommand(final byte cmd, final String description, final Runnable runnable) {
        queueGenericCommand(new byte[]{cmd}, description, runnable);
    }

    void queueGenericCommand(final byte[] cmd, final String description, final Runnable runnable) {
        queueGenericCommand(cmd, description, runnable, null);
    }

    QueueMe queueGenericCommand(final byte[] cmd, final String description, final Runnable runnable, final ReplyProcessor processor) {
        val item = new QueueMe()
                .setBytes(cmd);
        if (processor != null) processor.setTag(item);
        item.setDescription(description)
                .setProcessor(processor != null ? processor : new AuthReplyProcessor(new ReplyProcessor(I.connection) {
                    @Override
                    public void process(byte[] bytes) {
                        UserError.Log.d(TAG, "Reply for: " + description + " " + JoH.bytesToHex(bytes));
                    }
                }).setTag(item))
                .expireInSeconds(60);
        if (runnable != null) {
            item.setRunnable(runnable);
        }
        item.queue();
        doQueue();
        return item;
    }

    public QueueMe getQitemInstance() {
        return new QueueMe();
    }

    public void shutdown() {
        setProgressIndicator(null);
        UserError.Log.d(TAG, "Shutdown!!");
        changeState(CLOSE);
        stopSelf();
    }

    private static final int max_buffer_size = 256;

    public void enqueueWrappedBitmap(BitmapTools.Wrapper image, int x, int y) {
        // check valid
        if (image != null) {
            if (image.width > 0 && image.height > 0 && image.width <= 240 && image.height <= 240) {
                UserError.Log.d(TAG, "Wrapped bitmap: width: " + x + " height:" + y);
                enqueue(x, y, image.width, image.height, image.body, image.type);
            } else {
                UserError.Log.e(TAG, "Wrapped bitmap out of range: " + image.toS());
            }
        } else {
            UserError.Log.e(TAG, "Wrapped bitmap is null");
        }
    }

    public void enqueue(int x, int y, int width, int height, byte[] buffer, TJ_BitmapType bitmapType) {
        if (buffer.length > max_buffer_size) {
            UserError.Log.d(TAG, "Breaking image up in to smaller");
            enqueueBig(x, y, width, height, buffer, bitmapType);
        } else {
            UserError.Log.d(TAG, "Added new queue item: " + x + " " + y + " " + width + " " + height + " size: " + buffer.length);
            commandQueue.add(new ThinJamItem(x, y, width, height, buffer).setWindowType(bitmapType.getValue()));
            Inevitable.task("run-thinjam-queue", 100, this::background_automata);
        }
    }

    private class ImageSegment {
        final int width;
        final int height;
        final int start_x;
        final int start_y;
        int width_left;
        int height_left;
        int current_x;
        int current_y;
        TJ_BitmapType bitmapType;
        byte[] buffer;
        byte[] unpacked;

        ImageSegment(final int width, final int height, final int start_x, final int start_y, byte[] buffer, TJ_BitmapType bitmapType) {
            this.width = width;
            this.height = height;
            this.start_x = start_x;
            this.start_y = start_y;
            this.buffer = buffer;
            this.bitmapType = bitmapType;
            width_left = width;
            height_left = height;
        }

        byte[] getUnpacked() {
            if (unpacked == null) {
                unpacked = BitmapTools.unpackMonoBytesToRGB565(buffer);
            }
            return unpacked;
        }

        String toS() {
            return this.width + " " + this.height + " " + this.bitmapType + " " + this.width_left + " " + this.height_left + " " + this.current_x + " " + this.current_y;
        }
    }

    private void cropAndQueueSegment(final ImageSegment s, final int segmentHeight, final int segmentWidth) {
        final byte[] cropped;
        switch (s.bitmapType) {
            case RGB565:
                cropped = cropRGB565(s.current_x, s.current_y, segmentWidth, segmentHeight, s.width, s.height, s.buffer);
                break;
            case Mono:
                cropped = BitmapTools.packRGB565bytesToMono(cropRGB565(s.current_x, s.current_y, segmentWidth, segmentHeight, s.width, s.height, s.getUnpacked()));
                break;

            default:
                throw new RuntimeException("Invalid type in crop and queue segment");

        }
        if (cropped != null && cropped.length > 0) {
            enqueue(s.start_x + s.current_x, s.start_y + s.current_y, segmentWidth, segmentHeight, cropped, s.bitmapType);
        } else {
            UserError.Log.e(TAG, "Cropped bytes invalid: " + s.toS());
        }
        s.current_x += segmentWidth;
        s.width_left -= segmentWidth;
    }

    private void processSegment(final ImageSegment s, final int segmentHeight, final int segmentWidth) {
        while (s.height_left >= segmentHeight) {
            s.current_x = 0;
            s.width_left = s.width;
            while (s.width_left > 0) {
                cropAndQueueSegment(s, segmentHeight, s.width_left >= segmentWidth ? segmentWidth : s.width_left);
            }
            s.current_y += segmentHeight;
            s.height_left -= segmentHeight;
        }
    }

    private void processSegmentProgression(final ImageSegment s, int segmentStartHeight, int segmentStartWidth) {
        while (segmentStartHeight > 0) {
            processSegment(s, segmentStartHeight, segmentStartWidth);
            segmentStartHeight /= 2;
            segmentStartWidth *= 2;
        }
    }

    private Pair<Integer, Integer> getBestProgressionParameters(final int width, final int height, TJ_BitmapType bitmapType) {
        int sheight = 16;
        int swidth = 8;
        switch (bitmapType) {
            case Mono:
                if (height <= 256) {
                    sheight = height;
                    swidth = 2048 / sheight;
                }
                break;
        }
        return new Pair<>(sheight, swidth);
    }

    public void enqueueBig(final int start_x, final int start_y, final int width, final int height, final byte[] buffer, TJ_BitmapType bitmapType) {
        UserError.Log.d(TAG, "Big enqueue for: " + start_x + " " + start_y + " w:" + width + " h:" + height + " size: " + buffer.length);
        val segment = new ImageSegment(width, height, start_x, start_y, buffer, bitmapType);
        val parameters = getBestProgressionParameters(width, height, bitmapType);
        processSegmentProgression(segment, parameters.first, parameters.second);

    }

    public byte[] cropRGB565(int x, int y, int width, int height, int parent_width, int parent_height, final byte[] buffer) {
        final byte[] output = new byte[width * height * 2];
        final int row_width = parent_width * 2;
        int p = 0;
        int i = 0, j = 0;
        int start_ptr = x * 2 + y * row_width;
        try {
            UserError.Log.d(TAG, "cropRGB565 Start ptr: " + start_ptr);
            for (j = 0; j < height; j++) {
                for (i = 0; i < width * 2; i++) {
                    output[p++] = buffer[start_ptr + i];
                }
                start_ptr = start_ptr + row_width;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            UserError.Log.wtf(TAG, "ARRAY OUT OF BOUNDS: @ p" + p + " start_pyt:" + start_ptr + " i:" + i + " j:" + j + " " + x + " " + y + " " + width + " " + height + " " + parent_width + " " + parent_height + " " + buffer.length);
        }
        return output;
    }

    private static int getMaxBackFillHours() {
        int max_backfill_hours = (Pref.getInt("bluejay_backfill_hours", 3));
        if (max_backfill_hours == 0) {
            max_backfill_hours = 1; // cannot actually be disabled at the moment
        }
        return max_backfill_hours;
    }

    public void scheduleInterimEvents() {
        if (JoH.pratelimit("bluejay-interim-events", 600)) {
            UserError.Log.d(TAG, "Interim event trigger");
            schedulePeriodicEvents();
            doQueue();
        }
    }

    public void schedulePeriodicEvents() {
        final BlueJayInfo info = getInfo();
        if (info.status1Due()) {
            getStatus1();
            if (Home.get_engineering_mode()) {
                getStatus3();
            }
        }
        if (BlueJay.isCollector() && info.status2Due()) {
            getStatus2();
        }
        if (info.isTimeSetDue()) {
            setTime();
        }
        if (info.isDisplayUpdatedue()) {
            if (shouldSendReadings()) {
                Inevitable.task("bj-periodic-display-update", 3000, BlueJay::showLatestBG);
            }
        }


        if (BlueJay.isCollector() && (JoH.msSince(lastUsableGlucoseTimestamp) < MINUTE_IN_MS * 20)) {
            val bf_status = getBackFillStatus();
            if (bf_status.first > 0 && backFillOkayToAsk(bf_status.first)) {
                int records = (int) ((JoH.msSince(bf_status.first) / DEXCOM_PERIOD) + 2); // TODO collector period
                UserError.Log.d(TAG, "Earliest backfill time: " + JoH.dateTimeText(bf_status.first) + " Would like " + records + " backfill records");
                final int max_backfill_hours = getMaxBackFillHours();
                getBackFill(Math.min(records, max_backfill_hours * recordsPerHour()));
            }
        } else {
            UserError.Log.d(TAG, "Not checking for backfill data as bluejay is not set as collector or doesn't have recent good data, last timestamp: " + JoH.dateTimeText(lastUsableGlucoseTimestamp));
        }
        UserError.Log.d(TAG, "schedule periodic events done");
        changeNextState();
    }

    public synchronized void processQueue() {
        // TODO should probably check we are still in correct state to run this
        final ThinJamItem item = commandQueue.peek();
        if (item != null) {
            if (item.retryCounterOk()) {
                runQueueItem(item);
            } else {
                UserError.Log.d(TAG, "ThinJam queue item exceeded retries - removing");
                commandQueue.poll();
                Inevitable.task("tj-next-queue", 500, this::processQueue);
            }
        } else {
            UserError.Log.d(TAG, "Queue is empty");
            if (postQueueRunnable != null) {
                postQueueRunnable.run();
            }
        }
    }

    static volatile boolean busy = false;

    private void invalidateCache() {
        getInfo().invalidateStatus();
        getInfo().invalidateTime();
    }

    private synchronized void sendOtaChunks(final List<byte[]> chunks) {

        val thread = new Thread(() -> {
            try {
                flashIsRunning = true;

                UserError.Log.d(TAG, "Running on thread: " + Thread.currentThread());
                if (chunks == null) {
                    UserError.Log.e(TAG, "OTA chunks null");
                    return;
                }
                UserError.Log.d(TAG, "Running OTA sequence");
                doSafetyQueue(); // change off ota sequence
                JoH.threadSleep(2000);
                int chunks_sent = 0;
                int spinner = 0;

                addToLog("Starting to send firmware data\n");

                for (byte[] chunk : chunks) {

                    busy = true;

                    if (!sendOtaChunk(THINJAM_OTA, chunk)) {
                        UserError.Log.d(TAG, "Failed to send OTA chunk: " + chunks_sent);
                        busy = false;
                        break;
                    }

                    val timeoutAt = tsl() + Constants.SECOND_IN_MS * 20;
                    boolean timedOut = false;
                    while (busy && I.isConnected) {
                        if (tsl() > timeoutAt) {
                            timedOut = true;
                            break;
                        }
                        JoH.threadSleep(20);
                    }
                    if (timedOut) {
                        UserError.Log.e(TAG, "Timed out during send: " + chunks_sent);
                        busy = false;
                        break;
                    }

                    // start slow and speed up
                    if (chunks_sent < 200) {
                        JoH.threadSleep(200 - chunks_sent); // DEBUG
                    }
                    chunks_sent++;

                    if (spinner++ % 30 == 1) {
                        if (progressIndicator != null) {
                            val fspinner = spinner;
                            JoH.runOnUiThreadDelayed(() -> progressIndicator.set((fspinner * 100) / (chunks.size()) + 1), 100);
                        }
                    }
                }

                if (chunks_sent == chunks.size()) {
                    UserError.Log.d(TAG, "ALL CHUNKS SENT");
                    addToLog("All firmware data sent\n");
                } else {
                    UserError.Log.d(TAG, "ERROR SENDING CHUNKS: " + chunks_sent + " vs " + chunks.size());
                    addToLog("Failed to send all firmware data\n");
                }

                invalidateCache();

                threadSleep(10000);
                try {
                    progressIndicator.set(0);
                    progressIndicator = null;
                } catch (NullPointerException e) {
                    //
                }
                Inevitable.task("bj-recheck-status", 500, this::getStatus);
            } finally {
                flashIsRunning = false;
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    boolean sendOtaChunk(final UUID uuid, final byte[] bytes) {
        if (I.connection == null || !I.isConnected) return false;
        I.connection.writeCharacteristic(uuid, bytes)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(

                        characteristicValue -> {
                            if (D)
                                UserError.Log.d(TAG, "Wrote record request request: " + bytesToHex(characteristicValue));
                            busy = false;
                        }, throwable -> {
                            UserError.Log.e(TAG, "Failed to write record request: " + throwable);
                            if (throwable instanceof BleGattCharacteristicException) {
                                final int status = ((BleGattCharacteristicException) throwable).getStatus();
                                UserError.Log.e(TAG, "Got status message: " + Helper.getStatusName(status));
                            } else {
                                if (throwable instanceof BleDisconnectedException) {
                                    changeState(CLOSE);
                                }
                                UserError.Log.d(TAG, "Throwable in Record End write: " + throwable);
                            }
                        });
        return true; // only that we didn't fail in setup
    }


    private volatile int revisedOffset = 0;
    private volatile int lastActionedRevisedOffset = -1;

    // write the data packets
    private void bulkSend(final int opcode, final byte[] buffer, final int offset, boolean quiet) {
        UserError.Log.d(TAG, "bulksend called: opcode " + opcode + " total " + buffer.length + " offset: " + offset + " quiet:" + quiet);
        if (buffer != null && offset < buffer.length) {
            final BulkUpTx packet = (opcode >= OPCODE_BULK_R_XFER_0 ? new RBulkUpTx(opcode, buffer, offset) : new BulkUpTx(opcode, buffer, offset));
            if (quiet) {
                packet.setQuiet();
            }
            if (offset == 0) {
                revisedOffset = 0; // reset counter as this is new bulk up
                lastActionedRevisedOffset = -1;
                // TODO we should check it if we have failed to act on revised offset
            }

            I.connection.writeCharacteristic(quiet ? THINJAM_BULK : THINJAM_WRITE, packet.getBytes())
                    .observeOn(Schedulers.newThread())
                    .subscribe(

                            response -> {
                                if (D)
                                    UserError.Log.d(TAG, "Bulk Up Send response: " + bytesToHex(response));

                                if (packet.responseOk(response)) {
                                    // WARNING recursion
                                    final int nextOffset;
                                    if (revisedOffset != 0 && revisedOffset < offset && revisedOffset != lastActionedRevisedOffset) {
                                        nextOffset = revisedOffset;     // TODO we only catch this if we send a packet
                                        lastActionedRevisedOffset = nextOffset;
                                        UserError.Log.d(TAG, "Retrying bulk send from: " + nextOffset);
                                    } else {
                                        nextOffset = offset + packet.getBytesIncluded();
                                    }
                                    revisedOffset = 0;
                                    if (nextOffset < buffer.length) {
                                        if (!quiet) {
                                            JoH.threadSleep(100);
                                        } else {
                                            JoH.threadSleep(1);
                                        }
                                        bulkSend(opcode, buffer, nextOffset, packet.isQuiet());
                                    } else {
                                        UserError.Log.d(TAG, "Bulk send completed!");

                                        if (!(packet instanceof RBulkUpTx)) {
                                            commandQueue.poll(); // removes first item from the queue which should be the one we just processed!
                                            Inevitable.task("tj-next-queue", 10, this::processQueue); //
                                        } else {
                                            Inevitable.task("tj-next-queue", 4000, this::processQueue); // wait 1 second and then retry this upload if we get success reply notification then we remove it elsewhere
                                        }
                                    }
                                } else {
                                    UserError.Log.d(TAG, "Bulk Send failed: " + packet.responseText(response));
                                    if (!quiet) {
                                        Inevitable.task("tj-next-queue", 20000, this::processQueue); // retry shortly
                                        if (JoH.ratelimit("tj-allow-bulk-retry", 2)) {
                                            UserError.Log.d(TAG, "Retrying packet");
                                            bulkSend(opcode, buffer, offset, quiet); // retry packet send
                                        }
                                    } else {
                                        UserError.Log.d(TAG, "Quiet is set so not attempting any response to failure here");
                                    }

                                }

                            }, throwable -> {
                                UserError.Log.e(TAG, "Failed to write bulk Send: " + throwable);
                                if (throwable instanceof BleGattCharacteristicException) {
                                    final int status = ((BleGattCharacteristicException) throwable).getStatus();
                                    UserError.Log.e(TAG, "Got status message: " + Helper.getStatusName(status));
                                } else {
                                    UserError.Log.d(TAG, "Throwable in Bulk SEnd write: " + throwable);
                                }

                            });
        } else {
            UserError.Log.d(TAG, "Invalid buffer in bulkSend");
            Inevitable.task("tj-next-queue", 4000, this::processQueue);
        }
    }

    private synchronized void requestBulk(final ThinJamItem item) {
        if (item.buffer == null) {
            UserError.Log.d(TAG, "ThinJamItem buffer is null! " + item.toS());
            return;
        }
        if (item.inProgress) {
            UserError.Log.d(TAG, "Blocking duplicate request for in progress item: " + item.queuedTimestamp);
            return;
        }
        item.inProgress = true;
        final BulkUpRequestTx packet = new BulkUpRequestTx(item.type, item.getId(), BulkUpRequestTx.encodeLength(item.sequence, item.buffer.length), item.buffer, item.quiet);

        if (D)
            UserError.Log.d(TAG, "Bulk request request: " + item.sequence + " " + bytesToHex(packet.getBytes()));
        // value will get notification result itself
        if (I.connection == null) {
            item.inProgress = false;
            UserError.Log.d(TAG, "Connection is null skipping");
        }
        I.connection.writeCharacteristic(THINJAM_WRITE, packet.getBytes()).subscribe(
                // I.connection.writeCharacteristic(THINJAM_BULK, packet.getBytes()).subscribe(

                response -> {
                    if (D)
                        UserError.Log.d(TAG, "Bulk request response: " + bytesToHex(response));

                    if (packet.responseOk(response)) {
                        lastBulkOk = tsl();
                        UserError.Log.d(TAG, "Bulk channel opcode: " + packet.getBulkUpOpcode(response));
                        bulkSend(packet.getBulkUpOpcode(response), item.buffer, 15, item.quiet);
                    } else {
                        UserError.Log.d(TAG, "Bulk request failed: " + packet.responseText(response));
                        if (packet.responseText(response).toLowerCase().contains("busy")
                                && item.retryCounterOk()) {
                            UserError.Log.d(TAG, "Device is busy, scheduling retry");
                            Inevitable.task("bulk-retry-" + item.toS(), 3000, () -> {
                                UserError.Log.d(TAG, "Retrying requestBulk: " + item.toS());
                                item.inProgress = false;
                                requestBulk(item);
                            });
                        } else if (packet.responseText(response).toLowerCase().contains("out of range")
                                || packet.responseText(response).toLowerCase().contains("unknown error")) {
                            UserError.Log.d(TAG, "Setting item to not retry again due to out of range");
                            item.inProgress = false;
                            item.dontRetryAgain();
                            Inevitable.task("tj-next-queue", 0, this::processQueue);
                        }
                    }
                    item.inProgress = false;
                }, throwable -> {
                    UserError.Log.e(TAG, "Failed to write bulk request: " + throwable);
                    if (throwable instanceof BleGattCharacteristicException) {
                        final int status = ((BleGattCharacteristicException) throwable).getStatus();
                        UserError.Log.e(TAG, "Got status message: " + Helper.getStatusName(status));
                    } else {
                        UserError.Log.d(TAG, "Throwable in Bulk End write: " + throwable);
                    }
                    item.inProgress = false;
                });
    }


    public void doQueue() {
        ((ThinJamState) mState).getPacketQueueSequence();
        changeState(INIT);
    }

    public void doThinJamQueue() {
        ((ThinJamState) mState).thinJamQueueSequence();
        changeState(INIT);
    }

    public QueueMe qInstance() {
        return new QueueMe();
    }

    // just change but don't start
    public void doSafetyQueue() {
        ((ThinJamState) mState).getPacketQueueSequence();
    }

    public void doOtaMain() {
        otaFlashUpdate(1);
    }

    public void doOtaCore() {
        otaFlashUpdate(2);
    }

    public void otaFlashUpdate(final int type) {
        addToLog("OTA flash request: " + type);
        val bytes = FirmwareDownload.getLatestFirmwareBytes(I.address, type);
        UserError.Log.d(TAG, "Got bytes: " + ((bytes != null) ? bytes.length : "null!"));
        if (bytes != null) {
            Inevitable.task("bluejay-background-fw-up,", 200, () -> sendOtaChunks(split(parse(bytes))));
        } else {
            addToLog("Unable to get firmware for update");
        }
    }

    // Not using packet queue due to reactive time sensitive nature
    private void sendTime() {
        final String func = "SetTime";
        final SetTimeTx outbound = new SetTimeTx();
        UserError.Log.d(TAG, "Outbound: " + bytesToHex(outbound.getBytes()));
        I.connection.writeCharacteristic(THINJAM_WRITE, outbound.getBytes()).subscribe(
                response -> {
                    SetTimeTx reply = new SetTimeTx(response);
                    if (D)
                        UserError.Log.d(TAG, func + " response: " + bytesToHex(response) + " " + reply.toS());

                    UserError.Log.e(TAG, "Time difference with watch: " + ((outbound.getTimestamp() - reply.getTimestamp()) / 1000d));
                    changeNextState();

                }, throwable -> {
                    UserError.Log.e(TAG, "Failed to write " + func + " request: " + throwable);
                    if (throwable instanceof BleGattCharacteristicException) {
                        final int status = ((BleGattCharacteristicException) throwable).getStatus();
                        UserError.Log.e(TAG, "Got status message: " + Helper.getStatusName(status));
                    } else {
                        UserError.Log.d(TAG, "Throwable in " + func + " " + throwable);
                        if (throwable instanceof BleCharacteristicNotFoundException) {
                            UserError.Log.d(TAG, "Assuming wrong firmware version");
                            changeNextState();
                        } else {
                            changeState(CLOSE);
                        }
                    }
                });
    }


    public void sendNotification(final String message_type, final String msg) {
        UserError.Log.d(TAG, "SendNotification: " + message_type + " " + msg);
        if (!versionSufficient(getInfo().buildNumber, FEATURE_TJ_DISP_A)) return;
        if (BlueJayAsset.queueBusy()) {
            UserError.Log.d(TAG, "Refusing to send notification as asset queue is busy");
            return;
        }
        int notificationType = 1;
        if (message_type != null) {
            switch (message_type.toUpperCase()) {
                case THINJAM_NOTIFY_TYPE_CANCEL:
                    if (msSince(lastLongPress1) < SECOND_IN_MS) {
                        UserError.Log.d(TAG, "Not doing circular cancel");
                        return;
                    }
                    notificationType = 0;
                    break;
                case THINJAM_NOTIFY_TYPE_TEXTBOX1:
                case THINJAM_NOTIFY_TYPE_TEXTBOX2:
                    notificationType = 6;
                    break;
                case THINJAM_NOTIFY_TYPE_DIALOG:
                    notificationType = 9;
                    break;
                case THINJAM_NOTIFY_TYPE_LOW_ALERT:
                    notificationType = 4;
                    break;
                case THINJAM_NOTIFY_TYPE_HIGH_ALERT:
                    notificationType = 3;
                    break;

            }
        }

        val list = getPacketStreamForNotification(notificationType, msg);
        int counter = 0;
        for (val part : list) {
            counter++;
            val item = new QueueMe()
                    .setBytes(part.getBytes())
                    .expireInSeconds(60)
                    .setDescription("Notify type:" + message_type + " part:" + counter);

            item.setProcessor(new AuthReplyProcessor(new ReplyProcessor(I.connection) {
                @Override
                public void process(byte[] bytes) {
                    UserError.Log.d(TAG, "Notify  reply processor: " + HexDump.dumpHexString(bytes));
                }
            }).setTag(item));
            item.queue();
        }
        doQueue();
    }

    private void sendPng(final BitmapTools.Wrapper bitmap) {
        if (bitmap != null) {
            val info = BlueJayInfo.getInfo(I.address);
            enqueueWrappedBitmap(bitmap, 0, info.buildNumber > 2000 ? 199 : 110);
            doThinJamQueue();
        } else {
            UserError.Log.d(TAG, "Null bitmap in sendPng:");
        }
    }

    private void sendColourPng(final byte[] bytes, final String parameter) {
        sendPng(BitmapTools.loadPNGBytesToRGB565(bytes));
    }

    private void sendMonoPng(final byte[] bytes, final String parameter) {
        sendPng(BitmapTools.convertWrappedToMono(BitmapTools.loadPNGBytesToRGB565(bytes)));
    }

    public void addToLog(final String text) {
        notificationString.append(text);
        notificationString.append("\n");
        Inevitable.task("tj update notifi", 250, new Runnable() {
            @Override
            public void run() {
                stringObservableField.set(notificationString.toString());
            }
        });
    }

    public void processPushRxActions(final PushRx pushRx) {
        switch (pushRx.type) {

            case LongPress1:
                lastLongPress1 = JoH.tsl();
                if (JoH.msSince(awaiting_easy_auth) < Constants.MINUTE_IN_MS * 5) {
                    addToLog("Processing easy auth");
                    awaiting_easy_auth = 0;
                    easyAuth();
                } else {
                    if (!AlertPlayer.getPlayer().OpportunisticSnooze()) {
                        if (pratelimit("bluejay-snooze", 60)) {
                            if (Pusher.enabled()) {
                                UserError.Log.d(TAG, "Attempting full snooze");
                                AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);
                            } else {
                                UserError.Log.d(TAG, "xDrip cloud not enabled so avoiding networked snooze");
                            }
                        }
                    }
                    BroadcastSnooze.send();
                    BlueJayEmit.sendButtonPress(1);
                }
                // TODO cancel any alert queue
                break;

            case BackFill:
                boolean changed = false;
                boolean currentReading = false;
                for (val backsie : pushRx.backfills) {
                    final long since = JoH.msSince(backsie.timestamp);
                    if ((since > HOUR_IN_MS * 6) || (since < 0)) {
                        UserError.Log.wtf(TAG, "Backfill timestamp unrealistic: " + JoH.dateTimeText(backsie.timestamp) + " (ignored)");
                    } else {

                        if (backsie.mgdl > 12) { // TODO check this cut off
                            if (BgReading.getForPreciseTimestamp(backsie.timestamp, Constants.MINUTE_IN_MS * 4, false) == null) {
                                try {
                                    final BgReading bgr = BgReading.bgReadingInsertFromG5(backsie.mgdl, backsie.timestamp, "Backfill").appendSourceInfo("BlueJay", true);
                                    UserError.Log.d(TAG, "Adding backfilled reading: " + JoH.dateTimeText(backsie.timestamp) + " " + Unitized.unitized_string_static(backsie.mgdl));
                                    if (since < MINUTE_IN_MS) {
                                        lastUsableGlucoseTimestamp = backsie.timestamp;
                                        currentReading = true;
                                    }
                                    changed = true;
                                } catch (NullPointerException e) {
                                    UserError.Log.e(TAG, "Got null pointer when trying to add backfilled data");
                                }
                            }
                        } else {
                            //
                        }
                        UserError.Log.d(TAG, "Backsie: " + JoH.dateTimeText(backsie.timestamp) + " " + Unitized.unitized_string_static(backsie.mgdl));
                    }
                }
                if (changed) {
                    Home.staticRefreshBGChartsOnIdle();
                    scheduleInterimEvents();
                }
                if (!currentReading && JoH.quietratelimit("bluejay-backfill-received", 20)) {
                    backFillReceived();
                }
                break;

            case Choice:
                UserError.Log.d(TAG, "Push Choice: " + pushRx.value + " :: " + pushRx.text);
                BlueJayEmit.sendChoice(pushRx.getValue(), pushRx.text);
                break;

            case AssetRequest:
                UserError.Log.d(TAG, "Asset request: " + pushRx.value);
                BlueJayAsset.queueAssetRequest(pushRx.getValue());
                if ((commandQueue.size() == 0 || msSince(lastBulkOk) > MINUTE_IN_MS)) {
                    BlueJayAsset.processAssetQueue(this);
                }
                break;

            case CarbInfo:
                UserError.Log.d(TAG, "Carb info: " + pushRx.getNumber() + " " + JoH.dateTimeText(pushRx.getTimestamp()));
                Treatments.create(pushRx.getNumber(), 0, pushRx.getTimestamp());
                break;

            case InsulinInfo:
                UserError.Log.d(TAG, "Insulin info: " + pushRx.getNumber() + " " + JoH.dateTimeText(pushRx.getTimestamp()));
                Treatments.create(0, pushRx.getNumber(), pushRx.getTimestamp());
                break;
        }
    }


    private ThinJamAudioStream audioStream;

    private void audioStreamCallBack(final byte[] bytes) {
        if (audioStream != null) {
            audioStream.process(bytes);
        }
    }

    public void registerAudioStreamCallback(ThinJamAudioStream object) {
        audioStream = object;
    }

    private void enableNotifications() {
        UserError.Log.d(TAG, "enableNotifications called()");
        if (I.isNotificationEnabled) {
            UserError.Log.d(TAG, "Notifications already enabled");
            changeNextState();
            return;
        }
        if (notificationSubscription != null) {
            notificationSubscription.unsubscribe();
        }
        JoH.threadSleep(500);
        UserError.Log.d(TAG, "Requesting to enable notifications");

        if (I.connection == null) {
            UserError.Log.d(TAG, "Connection went away before we could enable notifications");
            return;
        }

        notificationSubscription = new Subscription(
                I.connection.setupNotification(THINJAM_WRITE)
                        // .timeout(15, TimeUnit.SECONDS) // WARN
                        .observeOn(Schedulers.newThread()) // needed?
                        .doOnNext(notificationObservable -> {

                                    UserError.Log.d(TAG, "Notifications enabled");
                                    I.connection.writeCharacteristic(THINJAM_BULK, new byte[]{0x55});

                                    JoH.threadSleep(500); // Debug sleep to make sure notifications are actually enabled???
                                    I.isNotificationEnabled = true;
                                    changeNextState();
                                }

                        ).flatMap(notificationObservable -> notificationObservable)
                        //.timeout(5, TimeUnit.SECONDS)
                        .observeOn(Schedulers.newThread())
                        .subscribe(bytes -> {
                            // incoming notifications
                            UserError.Log.d(TAG, "Received notification bytes: " + JoH.bytesToHex(bytes));
                            val pushRx = PushRx.parse(bytes);
                            if (pushRx != null) {
                                UserError.Log.d(TAG, "Received PushRX: " + pushRx.toS());
                                getInfo().processPushRx(pushRx);
                                processPushRxActions(pushRx);
                            } else if (bytes[0] == 0x06) {
                                // TODO move this to parsed response
                                final int replyParam = ((int) bytes[1]) & 0xff;
                                if (replyParam == 0) {
                                    UserError.Log.d(TAG, "Bulk up success reply marker received! - removing queue head item");
                                    commandQueue.poll(); // removes first item from the queue which should be the one we just processed!
                                    UserError.Log.d(TAG, "Scheduling immediate run of queue");
                                    Inevitable.kill("tj-next-queue"); // remove timeout retry task
                                    Inevitable.task("tj-next-queue", 0, this::processQueue);
                                } else {
                                    revisedOffset = replyParam;
                                    UserError.Log.d(TAG, "Bulk up failure at: " + revisedOffset); // race condition on display
                                }
                            } else if (bytes[0] == (byte) 0xFE) {
                                audioStreamCallBack(bytes);
                            } else {
                                if (ThinJamActivity.isD()) {
                                    notificationString.append(new String(bytes));
                                    Inevitable.task("tj update notifi", 250, new Runnable() {
                                        @Override
                                        public void run() {
                                            stringObservableField.set(notificationString.toString());
                                        }
                                    });
                                }
                            }
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in Record Notification: " + throwable);
                            I.isNotificationEnabled = false;

                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                // maybe legacy - ignore for now but needs better handling
                                UserError.Log.d(TAG, "Characteristic not found for notification");
                                debug.processTestSuite("logcharerror");
                                tryGattRefresh(getI().connection);
                            }
                            if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                UserError.Log.e(TAG, "Problems setting notifications - disconnecting");
                                changeState(CLOSE);
                            }
                            if (throwable instanceof BleDisconnectedException) {
                                UserError.Log.d(TAG, "Disconnected while enabling notifications");
                                changeState(CLOSE);
                            }

                        }));
    }


    void queueBufferForAssetStorage(int assetid, byte[] buffer) {
        UserError.Log.d(TAG, "QUEUE BUFFER FOR ASSET STORAGE: " + assetid + " " + buffer.length);

        int startChunk = 1;
        if (buffer.length > 65535) {
            throw new RuntimeException("To big for page");
        }
        commandQueue.clear(); // warning nixes all pending items! but otherwise we don't have sequence set up
        while (buffer.length > 0) {
            val chunk = Arrays.copyOfRange(buffer, 0, Math.min(buffer.length, 256));
            UserError.Log.d(TAG, "Buffer Chunk size: " + chunk.length);
            buffer = Arrays.copyOfRange(buffer, chunk.length, buffer.length);
            UserError.Log.d(TAG, "buffer size remaining: " + buffer.length + " chunk: " + startChunk);

            final ThinJamItem item = new ThinJamItem(0, 0, 0, 0, chunk)
                    .setSpecificId(((assetid >> 7) & 0xff) | (byte) 0x80)
                    .setType(((assetid & 0x7f)))
                    .useAck()
                    .setSequence(startChunk);
            commandQueue.add(item);
            startChunk++;
        }
    }

    // TJ protocol queue items
    private synchronized void runQueueItem(final ThinJamItem item) {
        UserError.Log.d(TAG, "Running queue item queued: " + item.queuedTimestamp);

        if (item.width > 0) {
            final BaseTx packet = new DefineWindowTx((byte) 1, (byte) item.windowType, (byte) item.x, (byte) item.y, (byte) item.width, (byte) item.height, (byte) 0, (byte) item.colourEffect);
            queueGenericCommand(packet.getBytes(), "define window: (" + item.windowType + ") " + item.x + "," + item.y + " w:" + item.width + " h:" + item.height, null, getARInstance(new ReplyProcessor(I.connection) {
                @Override
                public void process(byte[] response) {
                    if (D)
                        UserError.Log.d(TAG, "Wrote qui record request request: " + bytesToHex(response));
                    if (packet.responseOk(response)) {
                        requestBulk(item);
                    } else {
                        UserError.Log.d(TAG, "Define Window failed: " + packet.responseText(response));
                    }
                }
            }));

        } else {
            // is not a window request - is flash write
            requestBulk(item);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("bluejay service", 60000);
        try {
            if (shouldServiceRun()) {
                setAddress(BlueJay.getMac());

                I.playSounds = BlueJay.shouldBeepOnConnect();
              //  setAutoConnect();

                if (intent != null) {
                    final String function = intent.getStringExtra("function");
                    if (function != null) {
                        UserError.Log.d(TAG, "RUNNING FUNCTION: " + function);
                        switch (function) {

                            case "wakeup":
                                checkConnection();
                                break;

                            case "refresh":
                                setSettings();
                                if (JoH.pratelimit("bj-set-time-via-refresh-" + BlueJay.getMac(), 300)) {
                                    setTime();
                                }
                                break;

                            case "sendglucose":
                                if (shouldSendReadings()) {
                                    sendGlucose();
                                }
                                break;

                            case "message":
                                final String message = intent.getStringExtra("message");
                                final String message_type = intent.getStringExtra("message_type");
                                if (JoH.ratelimit("bj-sendmessage" + message_type, 15)) {
                                    sendNotification(message_type, message);
                                }
                                break;

                            case "png":
                                final byte[] bytes = intent.getByteArrayExtra("bytes_payload");
                                final String params = intent.getStringExtra("params");
                                final String type = intent.getStringExtra("type");
                                if (JoH.ratelimit("bj-sendpng", 15)) {
                                    switch (type) {
                                        case "colour":
                                            sendColourPng(bytes, params);
                                            break;
                                        case "mono":
                                            sendMonoPng(bytes, params);
                                            break;
                                        default:
                                            UserError.Log.wtf(TAG, "Invalid png type: " + type);
                                    }
                                }
                                break;

                            case "audioin":
                                UserError.Log.d(TAG, "Audio in request received");
                                break;

                            case "audioout":
                                UserError.Log.d(TAG, "Audio out request received");
                                break;
                        }
                    } else {
                        // no specific function
                        UserError.Log.d(TAG, "SET TIME CALLED");
                        changeState(SET_TIME);
                    }
                }

                //doQueue(); // start things in motion // This was previously enabled

                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                I.autoConnect = false; // avoid reconnection when shutting down
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }


    private boolean shouldServiceRun() {
        return BlueJayEntry.isEnabled();
    }

    // binding

    public class LocalBinder extends Binder {
        public BlueJayService getService() {
            return BlueJayService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private boolean flashRunning() {
        return flashIsRunning;
    }

    // sequencer

    static class ThinJamState extends JamBaseBluetoothSequencer.BaseState {
        static final String PROTOTYPE = "Prototype Test";
        static final String OTAUI = "OTA UI";
        static final String OTACORE = "OTA CORE";
        static final String RUN_QUEUE = "Run Queue";
        static final String ENABLE_NOTIFICATIONS = "Enable Notifications";
        static final String SCHEDULE_ITEMS = "Schedule Items";
        static final String GET_BATTERY = "Get Battery";
        static final String SET_TIME = "Set Time";
        static final String LOG_TESTS = "Log Tests";

        {
            setTimeSequence();
        }

        void setTimeSequence() {
            UserError.Log.d(TAG, "SET TIME SEQUENCE");
            sequence.clear();

            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(DISCOVER); // automatically executed
            //  sequence.add(ENABLE_NOTIFICATIONS); // automatically executed
            sequence.add(SCHEDULE_ITEMS);
            //  sequence.add(RUN_QUEUE);
            sequence.add(SET_TIME);
            sequence.add(SLEEP);

        }

        void thinJamQueueSequence() {
            UserError.Log.d(TAG, "RUN QUEUE SEQUENCE");
            sequence.clear();

            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(DISCOVER); // automatically executed
            //  sequence.add(ENABLE_NOTIFICATIONS); // automatically executed
            //sequence.add(SCHEDULE_ITEMS);
            sequence.add(ENABLE_NOTIFICATIONS); // automatically executed
            sequence.add(RUN_QUEUE);
            sequence.add(SLEEP);
        }


        void getPacketQueueSequence() {
            sequence.clear();

            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(DISCOVER); // automatically executed
            sequence.add(ENABLE_NOTIFICATIONS); // automatically executed
            sequence.add(SCHEDULE_ITEMS);
            sequence.add(SEND_QUEUE);
            // sequence.add(SET_TIME); could this go in here too?
            sequence.add(SLEEP);
        }

        public List<String> getSequence() {
            return sequence;
        }

    } // end class

    @Override
    protected synchronized boolean automata() {
        extendWakeLock(1000);
        if (D) UserError.Log.d(TAG, "Automata called in " + TAG);

        if (shouldServiceRun()) {

            switch (I.state) {

                case INIT:
                    // connect by default
                    changeNextState();
                    break;

                case DISCOVER:
                    if (msSince(I.getLastConnected()) < 500) {
                        connectionTracker.add(1);
                    }
                    return super.automata();

                case ENABLE_NOTIFICATIONS:
                    enableNotifications();
                    //changeNextState();
                    break;

                case SCHEDULE_ITEMS:
                    cancelRetryTimer();
                    if (commandQueue.peek() == null) {
                        schedulePeriodicEvents();
                    } else {
                        UserError.Log.d(TAG, "Skipping schedule items as queue is not empty");
                        changeNextState();
                    }
                    break;

                // TJ queue not send queue
                case RUN_QUEUE:
                    cancelRetryTimer();
                    processQueue();
                    break;

                case SET_TIME:
                    // TODO inject in to packet send sequence but with ratelimit
                    sendTime();
                    break;

                case PROTOTYPE:
                    break;

                case OTAUI:
                    debug.processTestSuite("otaprototype-pending");
                    break;

                case LOG_TESTS:
                    debug.processTestSuite("gl");
                    break;

                case SLEEP:
                    cancelRetryTimer();
                    return super.automata();

                case CLOSED:
                    setRetryTimerReal(); // local retry strategy
                    I.isNotificationEnabled = false; // should be handled by throwable but just to be sure
                    I.discoverOnce = false; // rediscover on reconnect
                    return super.automata();

                case SEND_QUEUE:
                    if (!flashRunning()) {
                        return super.automata();
                    } else {
                        UserError.Log.d(TAG, "Not sending queue as firmware update in progress");
                    }
                    break;

                default:
                    // if (shouldServiceRun()) {
                    //     if (msSince(lastReceivedData) < MINUTE_IN_MS) {
                    //         Inevitable.task("bluejay-set-failover", 1000, this::setFailOverTimer);
                    //     }
                    return super.automata();
                // } else {
                //     UserError.Log.d(TAG, "Service should be shut down so stopping automata");
                // }
            }

        } else {
            UserError.Log.d(TAG, "Service should not be running inside automata");
            stopSelf();
        }

        return true; // lies
    }


    static final List<UUID> huntCharacterstics = new ArrayList<>();

    static {
        huntCharacterstics.add(Const.THINJAM_WRITE); // TODO improve
    }

    void checkConnection() {
        if (!I.isConnected) {
            UserError.Log.d(TAG, "Attempting connect as we are not connected");
            changeState(INIT);
        }
    }

    // TODO move to base class as is duplicated!
    public void tryGattRefresh() {
        if (JoH.ratelimit("jam-gatt-refresh", 60)) {
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


    @Override
    public void btCallback(String address, String status) {
        // TODO ratelimiting? Detect whether we are using background scanning? for conditional
        UserError.Log.d(TAG, "Ignoring callback: " + address + " :: " + status);
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
        found = true; // test only TODO update to filter
        if (found) {
            I.isDiscoveryComplete = true;
            I.discoverOnce = true;
            changeNextState();
        } else {
            UserError.Log.e(TAG, "Could not find characteristic during service discovery. This is very unusual");
            tryGattRefresh();
        }
    }


    @Override
    protected void setRetryTimerReal() {
        if (shouldServiceRun()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            I.serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.BLUEJAY_SERVICE_RETRY_ID, "wakeup");
            I.retry_time = JoH.wakeUpIntent(xdrip.getAppContext(), retry_in, I.serviceIntent);
            I.wakeup_time = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private void cancelRetryTimer() {
        JoH.cancelAlarm(xdrip.getAppContext(), I.serviceIntent);
        I.wakeup_time = 0;
    }


    private long whenToRetryNext() {
        //I.retry_backoff += Constants.SECOND_IN_MS;
        // if (I.retry_backoff > MAX_RETRY_BACKOFF_MS) {
        //     I.retry_backoff = MAX_RETRY_BACKOFF_MS;
        // }
        return Constants.MINUTE_IN_MS * 10;
    }

    public AuthReplyProcessor getARInstance(final ReplyProcessor secondary) {
        return new AuthReplyProcessor(secondary);
    }

    // reply processor wrapper which handles authentication
    class AuthReplyProcessor extends ReplyProcessor {

        final ReplyProcessor secondary;

        public AuthReplyProcessor(final ReplyProcessor secondary) {
            super(secondary.getConnection());
            this.secondary = secondary;
        }

        @Override
        public void process(byte[] bytes) {
            if (isAuthRequiredPacket(bytes)) {
                // handle setTag on either inner or outer
                UserError.Log.d(TAG, "Authentication is required");
                if (getTag() != null) {
                    authenticate((QueueMe) getTag());
                } else {
                    if (secondary.getTag() != null) {
                        authenticate((QueueMe) secondary.getTag());
                    } else {
                        UserError.Log.d(TAG, "AuthReplyProcessor: neither inner or outer contain reference tag for packet");
                    }
                }
            } else {
                UserError.Log.d(TAG, "Authentication is not required");
                secondary.process(bytes);
            }
        }
    }

    private static final String PREF_BACKFILL_ASKED = "bluejay-backfill-asked";
    private static final String PREF_BACKFILL_RECEIVED = "bluejay-backfill-received";
    private static final String PREF_BACKFILL_RECEIVED_COUNTER = "bluejay-backfill-counter";

    private boolean backFillOkayToAsk(long time) {
        // Todo add significant delay when asking for max records so we can handle situation where watch has nothing usefulbut keeps being asked
        if (flashRunning()) return false;
        if (!BlueJay.hasIdentityKey()) return false;
        final long granularity = Constants.MINUTE_IN_MS * 5;
        val last_asked = PersistentStore.getLong(PREF_BACKFILL_ASKED + I.address);
        UserError.Log.d(TAG, "Backfill Okay To Ask: " + time + " vs " + last_asked);
        if (time / granularity == last_asked / granularity) {
            if (PersistentStore.getLong(PREF_BACKFILL_RECEIVED + I.address) / granularity == last_asked / granularity) {
                val counter = PersistentStore.getLong(PREF_BACKFILL_RECEIVED_COUNTER + I.address);
                UserError.Log.d(TAG, "Backfill counter at: " + counter);
                if (counter > 4) {
                    // already received this backfill
                    UserError.Log.d(TAG, "Already received a backfill when asked for: " + JoH.dateTimeText(time));
                    return false;
                } else {

                }
            }
        } else {
            // not same ask
            UserError.Log.d(TAG, "Zero backfill counter");
            PersistentStore.setLong(PREF_BACKFILL_RECEIVED_COUNTER + I.address, 0);
        }
        // not asked or not received
        PersistentStore.setLong(PREF_BACKFILL_ASKED + I.address, time);
        return true;
    }

    // TODO move to info structure?
    private void backFillReceived() {
        PersistentStore.setLong(PREF_BACKFILL_RECEIVED + I.address, PersistentStore.getLong(PREF_BACKFILL_ASKED + I.address));
        PersistentStore.incrementLong(PREF_BACKFILL_RECEIVED_COUNTER + I.address);
    }

    // Mega Status
    public static List<StatusItem> megaStatus() {

        final List<StatusItem> l = new ArrayList<>();
        final Inst II = Inst.get(BlueJayService.class.getSimpleName());

        val info = BlueJayInfo.getInfo(II.address);
        l.add(new StatusItem("Mac address", II.address));

        if (BlueJay.getAuthKey(II.address) == null) {
            l.add(new StatusItem("Pairing", "Not Paired! Tap to pair", CRITICAL, "long-press", () -> ThinJamActivity.requestQrCode()));
        } else if (BlueJay.getIdentityKey(II.address) == null) {
            l.add(new StatusItem("Identity", "Not Identified! Tap to pair", CRITICAL, "long-press", () -> ThinJamActivity.requestQrCode()));
        }

        l.add(new StatusItem("Connected", II.isConnected ? gs(R.string.yes) : gs(R.string.no)));
        if (II.wakeup_time != 0 && !II.isConnected) {
            final long till = msTill(II.wakeup_time);
            if (till > 0) l.add(new StatusItem("Retry Wake Up", niceTimeScalar(till)));
        }

        if (Home.get_engineering_mode()) {
            val totalConnections = (int) connectionTracker.totalRecords();
            l.add(new StatusItem("Connections", totalConnections + " last 20 min"));
            if (info.buildNumber >= 2092 && info.fragmentationLevel > 0) {
                l.add(new StatusItem("Fragmentation", info.fragmentationLevel + "%  (" + info.fragmentationCounter + "," + (info.fragmentationIndex - 4096) + ")", info.fragmentationLevel > 0 ? StatusItem.Highlight.NOTICE : StatusItem.Highlight.NORMAL));
            }
        }
        l.add(new StatusItem("State", II.state));

        final int qsize = II.getQueueSize();
        final int tjQueueSize = commandQueue.size();
        if (tjQueueSize > 0 || qsize > 0) {
            if (tjQueueSize > 0) {
                l.add(new StatusItem("Queue", "(" + tjQueueSize + ")  " + qsize + " items"));
            } else {
                l.add(new StatusItem("Queue", qsize + " items"));
            }
        }

        if (info.hasCoreModule()) {
            l.add(new StatusItem("xDrip Core", "Installed", GOOD));
        } else {
            l.add(new StatusItem("xDrip Core", "Not Installed", BAD, "long-press", new Runnable() {
                @Override
                public void run() {
                    ThinJamActivity.installCorePrompt();
                }
            }));
        }

        val successPercent = info.captureSuccessPercent();
        if (successPercent > -1) {
            l.add(new StatusItem("Capture Rate", String.format(Locale.US, "%d%%   (%d)", successPercent, info.successfulReplies)));
        }


        if (info.thinJamVersion != 2) {
            l.add(new StatusItem("ThinJam Version", info.thinJamVersion, NOTICE));
        }

        val lastReadingTime = info.getTimestamp();
        if (lastReadingTime > 0 && lastReadingTime > 1728497951000L) {
            l.add(new StatusItem("Last Reading", String.format("%s ago", JoH.niceTimeScalar(msSince(info.getTimestamp())))));
        }
        l.add(new StatusItem("Charger", (info.isChargerConnected() ? "Connected" : "Not connected") + (Home.get_engineering_mode() ? (info.batteryPercent != -1 ? ("  " + info.batteryPercent + "%") : "") : "")));

        l.add(new StatusItem("Uptime", niceTimeScalar(info.getUptimeTimeStamp())));

        val latestMainVersion = FirmwareInfo.getLatestMainFirmware();
        l.add(new StatusItem("Firmware Version", "" + info.buildNumber, info.buildNumber < info.coreNumber ? NOTICE : NORMAL, "long-press", new Runnable() {
            @Override
            public void run() {
                // prompt update for main?
            }
        }));

        if (latestMainVersion > info.buildNumber && II.isConnected && qsize == 0) {
            l.add(new StatusItem("Update Available", "Tap to update " + latestMainVersion, NOTICE, "long-press", new Runnable() {
                @Override
                public void run() {
                    ThinJamActivity.updateMainPrompt();
                }
            }));
        }

        if (info.coreNumber > 0 && II.isConnected) {
            val latestCoreVersion = FirmwareInfo.getLatestCoreFirmware();
            l.add(new StatusItem("Core Module", "xDrip v" + info.coreNumber, info.coreNumber < info.buildNumber ? NOTICE : NORMAL, "long-press", new Runnable() {
                @Override
                public void run() {
                    // prompt upgrade for core?
                }
            }));
            if (latestCoreVersion > info.coreNumber && qsize == 0) {
                l.add(new StatusItem("Core Update Available", "Tap to update " + latestCoreVersion, NOTICE, "long-press", new Runnable() {
                    @Override
                    public void run() {
                        ThinJamActivity.updateCorePrompt();
                    }
                }));
            }
        }

        l.add(new StatusItem("Admin Panel", "Tap to open", NORMAL, "long-press", new Runnable() {
            @Override
            public void run() {
                JoH.startActivity(ThinJamActivity.class);
            }
        }));

        return l;
    }

}
