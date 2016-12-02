package com.eveningoutpost.dexdrip.Services;

/**
 * Created by jcostik1 on 3/15/16.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.G5Model.AuthChallengeRxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthChallengeTxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthStatusRxMessage;
import com.eveningoutpost.dexdrip.G5Model.BluetoothServices;
import com.eveningoutpost.dexdrip.G5Model.BondRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.DisconnectTxMessage;
import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.GlucoseRxMessage;
import com.eveningoutpost.dexdrip.G5Model.GlucoseTxMessage;
import com.eveningoutpost.dexdrip.G5Model.KeepAliveTxMessage;
import com.eveningoutpost.dexdrip.G5Model.SensorRxMessage;
import com.eveningoutpost.dexdrip.G5Model.SensorTxMessage;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;
import com.eveningoutpost.dexdrip.G5Model.TransmitterStatus;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import static com.eveningoutpost.dexdrip.G5Model.BluetoothServices.getStatusName;
import static com.eveningoutpost.dexdrip.G5Model.BluetoothServices.getUUIDName;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class G5CollectionService extends Service {

    public final static String TAG = G5CollectionService.class.getSimpleName();

    private static final Object short_lock = new Object();
    private final Object mLock = new Object();
    private static boolean cycling_bt = false;
    private static boolean service_running = false;
    private static boolean scan_scheduled = false;

    private ForegroundServiceStarter foregroundServiceStarter;

    public Service service;
    private BgToSpeech bgToSpeech;
    private static PendingIntent pendingIntent;

    private android.bluetooth.BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private BluetoothGatt mGatt;
    private Transmitter defaultTransmitter;
    public AuthStatusRxMessage authStatus = null;
    public AuthRequestTxMessage authRequest = null;

    private BluetoothGattService cgmService;// = gatt.getService(UUID.fromString(BluetoothServices.CGMService));
    private BluetoothGattCharacteristic authCharacteristic;// = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
    private BluetoothGattCharacteristic controlCharacteristic;//
    //private BluetoothGattCharacteristic commCharacteristic;//

    private BluetoothDevice device;
    private Boolean isBondedOrBonding = false;
    private Boolean isBonded = false;
    private int currentBondState = 0;
    public static boolean keep_running = true;

    private ScanSettings settings;
    private List<ScanFilter> filters;
    private SharedPreferences prefs;

    private static boolean isScanning = false;
    private boolean isConnected = false;
    private boolean encountered133 = false;
    //private Handler handler;
    private final int max133Retries = 5;
    public int max133RetryCounter = 0;
    private static int disconnected133 = 0;
    private static int disconnected59 = 0;
    public boolean isIntialScan = true;
    public static Timer scan_interval_timer = new Timer();
    public ArrayList<Long> advertiseTimeMS = new ArrayList<Long>();
    public long timeInMillisecondsOfLastSuccessfulSensorRead = new Date().getTime();
    private int maxScanIntervalInMilliseconds = 5 * 1000; //seconds *1k
    private int maxScanCycles = 24;
    private int scanCycleCount = 0;
    private boolean delays = false;


    // test params
    private static final boolean ignoreLocalBondingState = false; // don't try to bond gives: GATT_ERR_UNLIKELY but no more 133s
    private static final boolean delayOnBond = false; // delay while bonding also gives ERR_UNLIKELY but no more 133s
    private static final boolean tryPreBondWithDelay = true; // prebond with delay seems to help
    private static final boolean delayOn133Errors = true; // add some delays with 133 errors
    private static final boolean useKeepAlive = true; // add some delays with 133 errors


    StringBuilder log = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initScanCallback();
        }
        advertiseTimeMS.add((long)0);
        service = this;
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
        foregroundServiceStarter.start();

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();

        // TODO check this
        bgToSpeech = BgToSpeech.setupTTS(getApplicationContext()); //keep reference to not being garbage collected
        // handler = new Handler(getApplicationContext().getMainLooper());

        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//KS turn on
        bondintent.addAction(BluetoothDevice.ACTION_FOUND);//KS add
        registerReceiver(mPairReceiver, bondintent);//KS turn on
    }

    final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive ACTION: " + action);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice parcel_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // TODO do we need to filter on the last 2 characters of the device name here?
                currentBondState = parcel_device.getBondState();
                Log.d(TAG, "onReceive FOUND: " + parcel_device.getName() + " STATE: " + parcel_device.getBondState());
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice parcel_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // TODO do we need to filter on the last 2 characters of the device name here?
                currentBondState = parcel_device.getBondState();
                Log.d(TAG, "onReceive UPDATE Name " + parcel_device.getName() + " Value " + parcel_device.getAddress() + " Bond state " + parcel_device.getBondState() + bondState(parcel_device.getBondState()));
            }
        }
    };

    private String bondState(int bs) {
        String bondState;
        if (bs == BluetoothDevice.BOND_NONE) {
            bondState = " Unpaired";
        } else if (bs == BluetoothDevice.BOND_BONDING) {
            bondState = " Pairing";
        } else if (bs == BluetoothDevice.BOND_BONDED) {
            bondState = " Paired";
        } else if (bs == 0) {
            bondState = " Startup";
        } else {
            bondState = " Unknown bond state: " + bs;
        }
        return bondState;
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if(key.compareTo("run_service_in_foreground") == 0) {
                Log.d("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
                    foregroundServiceStarter.start();
                    Log.i(TAG, "Moving to foreground");
                } else {
                    service.stopForeground(true);
                    Log.i(TAG, "Removing from foreground");
                }
            }

            if(key.compareTo("run_ble_scan_constantly") == 0 || key.compareTo("always_unbond_G5") == 0
                    || key.compareTo("always_get_new_keys") == 0 || key.compareTo("run_G5_ble_tasks_on_uithread") == 0) {
                Log.i(TAG, "G5 Setting Change");
                cycleScan(0);
            }


        }
    };

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        // TODO do we need an unregister!?
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       // byte[] testbytes =  { 0x31,0x00,0x68,0x0a,0x00,0x00,(byte)0x8a,0x71,0x57,0x00,(byte)0xcc,0x00,0x06,(byte)0xff,(byte)0xc4,0x2a } ;
       // GlucoseRxMessage rx = new GlucoseRxMessage(testbytes);
       // new AuthRequestTxMessage(8);
       // new AuthRequestTxMessage(16);
       // fullAuthenticate();

        final PowerManager.WakeLock wl = JoH.getWakeLock("g5-start-service", 120000);
        try {
            if ((!service_running) && (keep_running)) {
                service_running = true;

                // extra debugging
                if (useG5NewMethod()) {
                    if (!Home.getPreferencesStringDefaultBlank("extra_tags_for_logging").contains("G5CollectionService:v")) {
                        Home.setPreferencesString("extra_tags_for_logging", "G5CollectionService:v,");
                        String extraLogs = Home.getPreferencesStringDefaultBlank("extra_tags_for_logging");
                        UserError.ExtraLogTags.readPreference(extraLogs);
                    }
                    Home.setPreferencesBoolean("enable_bugfender", true);
                    xdrip.initBF();
                }

                Log.d(TAG, "onG5StartCommand wakeup: "+JoH.dateTimeText(JoH.tsl()));
                Log.e(TAG, "settingsToString: " + settingsToString());
                //Log.d(TAG, "SDK: " + Build.VERSION.SDK_INT);
                //stopScan();
                if (!CollectionServiceStarter.isBTG5(xdrip.getAppContext())) {
                    Log.e(TAG,"Shutting down as no longer using G5 data source");
                    service_running = false;
                    keep_running = false;
                    stopSelf();
                    return START_NOT_STICKY;
                } else {

                    scanCycleCount = 0;
                    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = mBluetoothManager.getAdapter();

                    if (mGatt != null) {
                        mGatt.close();
                        mGatt = null;
                    }

                    if (Sensor.isActive()) {
                        setupBluetooth();
                        Log.d(TAG, "Active Sensor");

                    } else {
                        stopScan();
                        Log.d(TAG, "No Active Sensor");
                    }

                    service_running=false;

                    return START_STICKY;
                }
            } else {
                Log.e(TAG,"G5 service already active!");
                keepAlive();
                return START_NOT_STICKY;
            }

        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private synchronized void getTransmitterDetails() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d(TAG, "Transmitter: " + prefs.getString("dex_txid", "ABCDEF"));
        defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
        final boolean previousBondedState = isBonded;
        isBondedOrBonding = false;
        isBonded = false;
        if (mBluetoothAdapter == null) {
            Log.wtf(TAG, "No bluetooth adapter");
            return;
        }
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {

                    final String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    final String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        isBondedOrBonding = true;
                        isBonded=true;
                        if (!previousBondedState) Log.e(TAG,"Device is now detected as bonded!");
                    // TODO should we break here for performance?
                    } else {
                        isIntialScan = true;
                    }
                }
            }
        }
        if (previousBondedState && !isBonded) Log.e(TAG,"Device is no longer detected as bonded!");
        Log.d(TAG, "getTransmitterDetails() result: Bonded? " + isBondedOrBonding.toString()+(isBonded ? " localed bonded" : " not locally bonded"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScan();

        Log.d(TAG, "onDestroy");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        scan_interval_timer.cancel();
        if (pendingIntent != null) {
            Log.d(TAG, "onDestroy stop Alarm pendingIntent");
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.cancel(pendingIntent);
        }

        // TODO do we need to gatt disconnect or close??
//        close();
//        setRetryTimer();
//        foregroundServiceStarter.stop();
//        unregisterReceiver(mPairReceiver);
        try {
            unregisterReceiver(mPairReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Got exception unregistering bonding receiver: ", e);
        }
//        BgToSpeech.tearDownTTS();
        Log.i(TAG, "SERVICE STOPPED");
    }

    public synchronized void keepAlive() {
        keepAlive(0);
    }

    public synchronized void keepAlive(int wake_in_ms) {
        if (!keep_running) return;
        if (JoH.ratelimit("G5-keepalive", 5)) {
            long wakeTime;
            if (wake_in_ms==0) {
                wakeTime = getNextAdvertiseTime() - 60 * 1000;
            } else {
                wakeTime = Calendar.getInstance().getTimeInMillis() + wake_in_ms;
            }
            //Log.e(TAG, "Delay Time: " + minuteDelay);
            Log.e(TAG, "Scheduling Wake Time: in " +  JoH.qs((wakeTime-JoH.tsl())/1000,0)+ " secs "+ JoH.dateTimeText(wakeTime));
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (pendingIntent != null)
                alarm.cancel(pendingIntent);
            pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            // TODO use wakeIntent feature
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else {
            Log.e(TAG, "Ignoring keepalive call due to ratelimit");
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setupBluetooth() {

        getTransmitterDetails();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //First time using the app or bluetooth was turned off?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Timer single_timer = new Timer();
            single_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mBluetoothAdapter != null) mBluetoothAdapter.enable();
                }
            }, 1000);
            single_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setupBluetooth();
                }
            }, 10000);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();
                //Only look for CGM.
                //filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothServices.Advertisement)).build());
                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                filters.add(new ScanFilter.Builder().setDeviceName("Dexcom" + transmitterIdLastTwo).build());
            }

            // unbond here to avoid clashes when we are mid-connection
            if (alwaysUnbond()) {
                forgetDevice();
            }
            cycleScan(0);
        }
    }

    public synchronized void stopScan(){
        if (!isScanning) {
            Log.d(TAG, "alreadyStoppedScanning");
            return;
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {

                try {

                    if (enforceMainThread()){
                        Handler iHandler = new Handler(Looper.getMainLooper());
                        iHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                stopLogic();
                            }
                        });
                    } else {
                        stopLogic();;
                    }


                } catch (NullPointerException e) {
                    //Known bug in Samsung API 21 stack
                    Log.e(TAG,"stopscan() Caught the NullPointerException");
                }
            }
        }
    }

    private synchronized void stopLogic() {
        try {
            Log.e(TAG, "stopScan");
            try {
                mLEScanner.stopScan(mScanCallback);
            } catch (NullPointerException | IllegalStateException e) {
                Log.e(TAG, "Exception in stopLogic: " + e);
            }
            isScanning = false;
        } catch (IllegalStateException is) {

        }
    }

    public synchronized void cycleScan(int delay) {

        if (!keep_running) return;
        if (JoH.ratelimit("G5-timeout",60) || !scan_scheduled) {
            Log.d(TAG,"cycleScan running");
            scan_scheduled=true;
            //Log.e(TAG, "Scheduling cycle scan, delay: " + delay);
            final Timer single_timer = new Timer();
            single_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (scanConstantly()) {
                        startScan();
                    } else {
                        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } else {

                                try {
                                    if (enforceMainThread()) {
                                        Handler iHandler = new Handler(Looper.getMainLooper());
                                        iHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                scanLogic();
                                            }
                                        });

                                    } else {
                                        scanLogic();
                                    }
                                } catch
                                        (NullPointerException e) {
                                    //Known bug in Samsung API 21 stack
                                    Log.e(TAG,"Caught the NullPointerException in cyclescan");
                                } finally {
                                    scan_scheduled=false;
                                }
                            }
                        }
                    }
                    scan_scheduled=false;
                }
            }, delay);
        } else {
            Log.e(TAG,"jamorham blocked excessive scan schedule");
        }
    }

    private synchronized void scanLogic() {
        if (!keep_running) return;
        if (JoH.ratelimit("G5-scanlogic", 2)) {
            try {
                mLEScanner.stopScan(mScanCallback);
                isScanning = false;
                if (!isConnected) {
                    mLEScanner.startScan(filters, settings, mScanCallback);
                    Log.w(TAG, "scan cycle start");
                }
                isScanning = true;
            } catch (IllegalStateException | NullPointerException is) {
                setupBluetooth();
            }


            scanCycleCount++;

            //Log.e(TAG, "MSSinceSensorRx: " + getMillisecondsSinceLastSuccesfulSensorRead());
            //if it isn't the initial scan, rescan for maxScanCycles
            if (!isIntialScan && scanCycleCount > maxScanCycles) {
                scan_interval_timer.cancel();
                scan_interval_timer = new Timer();
                scan_interval_timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //Log.e(TAG, "cycling scan to stop until expected advertisement");
                        if (isScanning) {
                            keepAlive();
                        }
                        stopScan();
                    }
                }, maxScanIntervalInMilliseconds);
            }
            //last ditch
            else if (!isIntialScan && getMillisecondsSinceLastSuccesfulSensorRead() > 11 * 60 * 1000) {
                Log.e(TAG, "MSSinceSensorRx: " + getMillisecondsSinceLastSuccesfulSensorRead());
                isIntialScan = true;
                cycleBT();
            }
            //startup or re-auth, sit around and wait for tx to advertise
            else {
                scan_interval_timer.cancel();
                scan_interval_timer = new Timer();
                scan_interval_timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //Log.e(TAG, "cycling scan");
                        cycleScan(0);
                    }
                }, maxScanIntervalInMilliseconds);
            }
        }
    }

    public synchronized void startScan() {
        UserError.Log.e(TAG, "Initial scan?" + isIntialScan);
        if (isScanning) {
            Log.d(TAG, "alreadyScanning");
            scan_interval_timer.cancel();
            return;
        }

        getTransmitterDetails();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            setupBluetooth();
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                setupLeScanCallback();
                mBluetoothAdapter.startLeScan(new UUID[]{BluetoothServices.Advertisement}, mLeScanCallback);
            } else {
                if (enforceMainThread()){
                    Handler iHandler = new Handler(Looper.getMainLooper());
                    iHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startLogic();
                        }
                    });
                } else {
                    startLogic();
                }
                Log.e(TAG, "startScan normal");
            }
        }
    }

    private synchronized void startLogic() {
        try {
            isScanning = true;
            mLEScanner.startScan(filters, settings, mScanCallback);
        } catch (Exception e) {
            isScanning = false;
            setupBluetooth();
        }
    }

    private synchronized void cycleBT(boolean t) {
        Log.e(TAG, "cycleBT special: count:" + disconnected133 + " / "+ disconnected59);
        if ((disconnected133 < 2) && (disconnected59 < 2)) {
            cycleBT();
        } else {
            Log.e(TAG, "jamorham special restart");
            keepAlive(10000); // retry in 10 seconds

            // close gatt
            if (mGatt != null) {
                try {
                    mGatt.close();
                } catch (NullPointerException e) {
                    Log.d(TAG, "concurrency related null pointer exception in close");
                }
            }
            disconnected133 = 0;
            disconnected59 = 0;
            stopSelf();
        }
    }

    private synchronized void cycleBT() {
        synchronized (short_lock) {
            if (JoH.ratelimit("cyclebt", 20)) {

                // TODO cycling_bt not used as never set to true - rate limit any sync used instead
                if (cycling_bt) {
                    Log.e(TAG, "jamorham Already concurrent BT cycle in progress!");
                    return;
                }
                encountered133 = false;
                stopScan();
                if (g5BluetoothWatchdog()) {
                    Log.e(TAG, "Cycling BT-gatt - disabling BT");
                    mBluetoothAdapter.disable();
                    Timer single_timer = new Timer();
                    single_timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mBluetoothAdapter.enable();
                            Log.e(TAG, "Cycling BT-gatt - enableing BT");
                            cycling_bt = false;
                        }
                    }, 3000);
                } else {
                    Log.e(TAG, "Wanted to cycle g5 bluetooth but is disabled in advanced bluetooth preferences!");
                    waitFor(3000);
                }
            }
            keepAlive();
        }
    }

    private synchronized void forgetDevice() {
        Log.d(TAG,"forgetDevice() start");
        final Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF")); // should be cached?
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {

                    final String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    final String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());
                    //Log.e(TAG, "removeBond: "+transmitterIdLastTwo+" vs "+deviceNameLastTwo);
                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        try {
                            Log.e(TAG, "removingBond: "+transmitterIdLastTwo+" vs "+deviceNameLastTwo);
                            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(device, (Object[]) null);
                            getTransmitterDetails();
                        } catch (Exception e) { Log.e(TAG, e.getMessage(), e); }
                    }

                }
            }
        }
        Log.d(TAG,"forgetDevice() finished");
    }

    // API 18 - 20
    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setupLeScanCallback() {
        if (mLeScanCallback == null) {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
                    // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
                    // If they match, connect to the device.
                    if (device.getName() != null) {
                        String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                        String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                        if (transmitterIdLastTwo.toUpperCase().equals(deviceNameLastTwo.toUpperCase())) {
                            connectToDevice(device);
                        }
                    }
                }
            };
        }
    }

    private ScanCallback mScanCallback;

    //@TargetApi(21)
    private void initScanCallback(){
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                UserError.Log.i(TAG, "result: " + result.toString());
                BluetoothDevice btDevice = result.getDevice();
//                // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
//                // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
//                // If they match, connect to the device.
                if (btDevice.getName() != null) {
                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(btDevice.getName());

                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        if (advertiseTimeMS.size() > 0)
                            if ((new Date().getTime() - advertiseTimeMS.get(advertiseTimeMS.size()-1)) > 2.5*60*1000)
                                advertiseTimeMS.clear();
                        advertiseTimeMS.add(new Date().getTime());
                        isIntialScan = false;
                        //device = btDevice;
                        device = mBluetoothAdapter.getRemoteDevice(btDevice.getAddress());
                        stopScan();
                        connectToDevice(btDevice);
                    } else {
                        //stopScan(10000);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan Failed Error Code: " + errorCode);
                if (errorCode == 1) {
                    UserError.Log.e(TAG, "Already Scanning: " + isScanning);
                    //isScanning = true;
                } else if (errorCode == 2){
                    cycleBT();
                }
            }
        };
    }

    public synchronized void fullAuthenticate() {
        Log.e(TAG, "fullAuthenticate() start");
        if (alwaysUnbond()) {
            forgetDevice();
        }
        try {
            Log.i(TAG, "Start Auth Process(fullAuthenticate)");
            if (authCharacteristic != null) {
                sendAuthRequestTxMessage(authCharacteristic);
            } else {
                Log.e(TAG, "fullAuthenticate: authCharacteristic is NULL!");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer in fullAuthenticate: " + e);
        }
    }

    public synchronized void authenticate() {
        Log.e(TAG,"authenticate() start");
        try {
            mGatt.setCharacteristicNotification(authCharacteristic, true);
            if (!mGatt.readCharacteristic(authCharacteristic)) {
                Log.e(TAG, "onCharacteristicRead : ReadCharacteristicError");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got Nullpointer exception in authenticate(): " + e);
        }
    }

    public synchronized void getSensorData() {
        Log.i(TAG, "Request Sensor Data");
        try {
            if (mGatt != null) {
                mGatt.setCharacteristicNotification(controlCharacteristic, true);
                final BluetoothGattDescriptor descriptor = controlCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                if (useG5NewMethod()) {
                    // new style
                    GlucoseTxMessage glucoseTxMessage = new GlucoseTxMessage();
                    controlCharacteristic.setValue(glucoseTxMessage.byteSequence);
                } else {
                    // old style
                    SensorTxMessage sensorTx = new SensorTxMessage();
                    controlCharacteristic.setValue(sensorTx.byteSequence);
                }
                Log.d(TAG,"getSensorData(): writing desccrptor");
                mGatt.writeDescriptor(descriptor);
            } else {
                Log.e(TAG,"getSensorData() mGatt was null");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer in getSensorData() " + e);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    private synchronized void connectToDevice(BluetoothDevice device) {
        if (JoH.ratelimit("G5connect-rate", 2)) {

            Log.d(TAG, "connectToDevice() start");
            if (mGatt != null) {
                Log.i(TAG, "BGatt isnt null, Closing.");
                try {
                    mGatt.close();
                } catch (NullPointerException e) {
                    // concurrency related null pointer
                }
                mGatt = null;
            }
            Log.i(TAG, "Request Connect");
            final BluetoothDevice mDevice = device;
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectGatt(mDevice);
                    }
                });
            } else {
                connectGatt(mDevice);
            }

        } else {
            Log.e(TAG, "connectToDevice baulking due to rate-limit");
        }
    }

    private synchronized void connectGatt(BluetoothDevice mDevice) {
        Log.i(TAG, "mGatt Null, connecting...");
        Log.i(TAG, "connectToDevice On Main Thread? " + isOnMainThread());

        if (delayOn133Errors && max133RetryCounter > 1) {
            // should we only be looking at disconnected 133 here?
            Log.e(TAG, "Adding a delay before connecting to 133 count of: " + max133RetryCounter);
            waitFor(600);
            Log.e(TAG, "connectGatt() delay completed");
        }
        mGatt = mDevice.connectGatt(getApplicationContext(), false, gattCallback);
    }


    // Sends the disconnect tx message to our bt device.
    private synchronized void doDisconnectMessage(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
           Log.d(TAG, "doDisconnectMessage() start");
           mGatt.setCharacteristicNotification(controlCharacteristic, false);
           final DisconnectTxMessage disconnectTx = new DisconnectTxMessage();
           characteristic.setValue(disconnectTx.byteSequence);
           mGatt.writeCharacteristic(characteristic);
           mGatt.disconnect();
           Log.d(TAG, "doDisconnectMessage() finished");
    }

    private synchronized void discoverServices() {
        if (JoH.ratelimit("G5-discservices", 2)) {

            Log.i(TAG, "discoverServices() started " + (isOnMainThread() ? "on main thread" : "not on main thread"));
            if (mGatt != null) {
                if (delayOn133Errors && max133RetryCounter > 1) {
                    // should we only be looking at disconnected 133 here?
                    Log.e(TAG, "Adding a delay before discovering services due to 133 count of: " + max133RetryCounter);
                    waitFor(1600);
                }
                mGatt.discoverServices();
            } else {
                Log.e(TAG, "discoverServices: mGatt is null");
            }
        } else {
            Log.e(TAG, "discoverServices rate limited!");
        }
    }

    // big bluetooth gatt callback
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      processOnStateChange(gatt, status, newState);
                                  }
                              }
                );
            } else {
                processOnStateChange(gatt, status, newState);
            }
        }


        private void processOnStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            switch (newState) {


                case BluetoothProfile.STATE_CONNECTED:
                    Log.e(TAG, "STATE_CONNECTED");
                    isConnected = true;

                    // TODO we should already be on the correct thread
                    if (enforceMainThread()) {
                        if (!isOnMainThread()) {
                            Log.d(TAG, "We are not on the main thread so this section is still needed!!");
                        }
                        Handler iHandler = new Handler(Looper.getMainLooper());
                        iHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                discoverServices();
                            }
                        });
                    } else {
                        discoverServices();
                    }


                    stopScan();
                    scan_interval_timer.cancel();
                    keepAlive();
                    break;


                case BluetoothProfile.STATE_DISCONNECTED:
                    isConnected = false;
                    if (isScanning) {
                        stopScan();
                    }
                    Log.e(TAG, "STATE_DISCONNECTED: " + getStatusName(status));
                    if (mGatt != null) {
                        try {
                            mGatt.close();
                        } catch (NullPointerException e) { //
                        }
                    }

                    mGatt = null;
                    if (status == 0 && !encountered133) {// || status == 59) {
                        Log.i(TAG, "clean disconnect");
                        max133RetryCounter = 0;
                        if (scanConstantly())
                            cycleScan(15000);
                    } else if (status == 133 || max133RetryCounter >= max133Retries) {
                        Log.e(TAG, "max133RetryCounter? " + max133RetryCounter);
                        Log.e(TAG, "Encountered 133: " + encountered133);
                        max133RetryCounter = 0;
                        disconnected133++;
                        cycleBT(true);
                    } else if (encountered133) {
                        Log.e(TAG, "max133RetryCounter? " + max133RetryCounter);
                        Log.e(TAG, "Encountered 133: " + encountered133);
                        if (scanConstantly())
                            startScan();
                        else
                            cycleScan(0);
                        max133RetryCounter++;
                    } else if (status == 129) {
                        Log.d(TAG, "Forgetting device due to status: " + status);
                        forgetDevice();
                    } else {
                        if (status == 59) {
                            disconnected59++;
                        }
                        if (disconnected59 > 2) {
                            cycleBT(true);
                        } else {
                            if (scanConstantly())
                                startScan();
                            else
                                cycleScan(0);
                            max133RetryCounter = 0;
                        }
                    }
                    break;


                default:
                    Log.e(TAG, "STATE_OTHER: " + newState);
            }
        }


        @Override
        public synchronized void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processOnServicesDiscovered(gatt, status);
                    }
                });
            } else {
                processOnServicesDiscovered(gatt, status);
            }
        }

        private synchronized void processOnServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.i(TAG, "onServicesDiscovered On Main Thread? " + isOnMainThread());
            Log.e(TAG, "onServicesDiscovered: " + getStatusName(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mGatt != null) {
                    try {
                        cgmService = mGatt.getService(BluetoothServices.CGMService);
                        if (cgmService != null) {
                            authCharacteristic = cgmService.getCharacteristic(BluetoothServices.Authentication);
                            controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);
                            // TODO can we remove the below comm line
                            //commCharacteristic = cgmService.getCharacteristic(BluetoothServices.Communication);
                        }
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Got Null pointer in OnServices discovered 2");
                    }
                    mBluetoothAdapter.cancelDiscovery();
                }

                //TODO : ADD option in settings!
                if (alwaysAuthenticate() || alwaysUnbond()) {
                    fullAuthenticate();
                } else {
                    authenticate();
                }

            } else {
                Log.w(TAG, "onServicesDiscovered received error status: " + getStatusName(status));
            }

            if (status == 133) {
                encountered133 = true;
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            Log.e(TAG, "OnDescriptor WRITE started: status: " + getStatusName(status));
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processonDescrptorWrite(gatt, descriptor, status);
                    }
                });
            } else {
                processonDescrptorWrite(gatt, descriptor, status);
            }
        }

        private void processonDescrptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            Log.i(TAG, "onDescriptorWrite On Main Thread? " + isOnMainThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Writing to characteristic: " + getUUIDName(descriptor.getCharacteristic().getUuid()));
                mGatt.writeCharacteristic(descriptor.getCharacteristic());
            } else {
                Log.e(TAG, "not writing characteristic due to Unknown error writing descriptor");
            }

            if (status == 133) {
                encountered133 = true;
            }
            Log.e(TAG, "OnDescriptor WRITE finished: status: " + getStatusName(status));
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            Log.e(TAG, "OnCharacteristic WRITE started: "
                    + getUUIDName(characteristic.getUuid())
                    + " status: " + getStatusName(status));
            //Log.e(TAG, "Write Status " + String.valueOf(status));
            //Log.e(TAG, "Characteristic " + String.valueOf(characteristic.getUuid()));

            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processOnCharacteristicWrite(gatt, characteristic, status);
                    }
                });
            } else {
                processOnCharacteristicWrite(gatt, characteristic, status);
            }


        }

        private synchronized void processOnCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            Log.i(TAG, "processOnCharacteristicWrite On Main Thread? " + isOnMainThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // is this being written to the auth characterstic?
                if (String.valueOf(characteristic.getUuid()).equalsIgnoreCase(String.valueOf(authCharacteristic.getUuid()))) {
                    Log.i(TAG, "Auth ow Char Value: " + Arrays.toString(characteristic.getValue()));
                    Log.i(TAG, "Auth ow auth? name: " + getUUIDName(characteristic.getUuid()));
                    if (characteristic.getValue() != null) {
                        Log.e(TAG, "Auth ow: got opcode: " + characteristic.getValue()[0]);
                        if (characteristic.getValue()[0] != KeepAliveTxMessage.opcode) { /* opcode keepalive? */
                            if (delayOn133Errors && max133RetryCounter > 1) {
                                // should we only be looking at disconnected 133 here?
                                Log.e(TAG, "Adding a delay before reading characteristic with 133 count of: " + max133RetryCounter);
                                waitFor(300);
                            }
                            mGatt.readCharacteristic(characteristic);
                        } else {
                            Log.e(TAG, "Auth ow: got keepalive");
                            if (useKeepAlive) {
                                Log.e(TAG, "Keepalive written, now trying bond");
                                performBondWrite(characteristic);
                            }
                        }
                    } else {
                        Log.e(TAG, "Auth ow: got NULL opcode!");
                    }
                } else {
                    Log.i(TAG, "ow unexpected? characteristic: "+ getUUIDName(characteristic.getUuid()));
                  //  Log.i(TAG, "ow status? " + status);
                }
            }

            if (status == 133) {
                encountered133 = true;
            }
            Log.e(TAG, "OnCharacteristic WRITE finished: status: " + getStatusName(status));
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            Log.e(TAG, "OnCharacteristic READ started: " + getUUIDName(characteristic.getUuid()) + " status: " + status);
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processOnCharacteristicRead(gatt, characteristic, status);
                    }
                });
            } else {
                processOnCharacteristicRead(gatt, characteristic, status);
            }
        }

        private synchronized void performBondWrite(BluetoothGattCharacteristic characteristic)
        {
            Log.d(TAG,"performBondWrite() started");
            final BondRequestTxMessage bondRequest = new BondRequestTxMessage();
            characteristic.setValue(bondRequest.byteSequence);
            mGatt.writeCharacteristic(characteristic);
            if (delayOnBond) {
                Log.e(TAG, "Delaying before bond");
                waitFor(1000);
                Log.e(TAG, "Delay finished");
            }
            isBondedOrBonding = true;
            device.createBond();
            Log.d(TAG,"performBondWrite() finished");
        }

        private synchronized void processOnCharacteristicRead (BluetoothGatt gatt,
                                                  final BluetoothGattCharacteristic characteristic, final int status)
        {
            Log.e(TAG, "processOnCRead: Status value: " + getStatusName(status) + (isOnMainThread() ? " on main thread" : " not on main thread"));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "CharBytes-or " + Arrays.toString(characteristic.getValue()));
                Log.i(TAG, "CharHex-or " + Extensions.bytesToHex(characteristic.getValue()));

                final byte[] buffer = characteristic.getValue();
                byte code = buffer[0];
                //Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
                Log.e(TAG,"processOncRead: code:"+code);
                mBluetoothAdapter = mBluetoothManager.getAdapter();

                switch (code) {
                    case 5:
                        authStatus = new AuthStatusRxMessage(buffer);

                        // TODO KS check here
                        if (authStatus.authenticated == 1 && authStatus.bonded == 1 && !isBondedOrBonding) {
                            Log.e(TAG, "Special bonding test case!");

                            if (tryPreBondWithDelay) {
                                Log.e(TAG,"Trying prebonding with delay!");
                                isBondedOrBonding = true;
                                device.createBond();
                                waitFor(1600);
                                Log.e(TAG,"Prebond delay finished");
                            }

                            getTransmitterDetails(); // try to refresh on the off-chance
                        }

                        if (ignoreLocalBondingState) Log.e(TAG,"Ignoring local bonding state!!");


                        if (authStatus.authenticated == 1 && authStatus.bonded == 1 && (isBondedOrBonding || ignoreLocalBondingState)) {
                            // TODO check bonding logic here and above
                            isBondedOrBonding = true; // statement has no effect?
                            getSensorData();
                        } else if ((authStatus.authenticated == 1 && authStatus.bonded == 2)
                                || (authStatus.authenticated == 1 && authStatus.bonded == 1 && !isBondedOrBonding)) {
                            Log.i(TAG, "Let's Bond! " + (isBondedOrBonding ? "locally bonded" : "not locally bonded"));

                            if (useKeepAlive) {
                                Log.e(TAG,"Trying keepalive..");
                                final KeepAliveTxMessage keepAliveRequest = new KeepAliveTxMessage(25);
                                characteristic.setValue(keepAliveRequest.byteSequence);
                                mGatt.writeCharacteristic(characteristic);
                            } else {
                             performBondWrite(characteristic);
                            }
                        } else {
                            Log.i(TAG, "Transmitter NOT already authenticated");
                            sendAuthRequestTxMessage(characteristic);
                        }
                        break;

                    case 3:
                        AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());
                        if (authRequest == null) {
                            authRequest = new AuthRequestTxMessage(getTokenSize());
                        }
                        Log.i(TAG, "tokenHash " + Arrays.toString(authChallenge.tokenHash));
                        Log.i(TAG, "singleUSe " + Arrays.toString(calculateHash(authRequest.singleUseToken)));

                        byte[] challengeHash = calculateHash(authChallenge.challenge);
                        Log.d(TAG, "challenge hash" + Arrays.toString(challengeHash));
                        if (challengeHash != null) {
                            Log.d(TAG, "Transmitter try auth challenge");
                            AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);
                            Log.i(TAG, "Auth Challenge: " + Arrays.toString(authChallengeTx.byteSequence));
                            characteristic.setValue(authChallengeTx.byteSequence);
                            mGatt.writeCharacteristic(characteristic);
                        }
                        break;

                    //case 7:
                    //    Log.d(TAG,"Received Bond request - trying bond");
                    //    isBondedOrBonding = true;
                    //   Log.e(TAG,"Bond state pre: "+device.getBondState());
                    //    device.createBond();
                    //    Log.e(TAG,"Bond state post: "+device.getBondState());
                    //    break;

                    default:
                        if ((code == 7) && (delayOnBond)) {
                            Log.e(TAG, "Delaying response to onRead for code: " + code);
                            waitFor(1500);
                            Log.e(TAG, "Delayed response to onRead finished");
                        }
                        Log.i(TAG, "Read code: " + code + " - Transmitter NOT already authenticated?");
                        sendAuthRequestTxMessage(characteristic);
                        break;
                }

            }

            if (status == 133) {
                encountered133 = true;
            }
            Log.e(TAG, "OnCharacteristic READ finished: status: " + getStatusName(status));
        }



        @Override
        // Characteristic notification
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "OnCharacteristic CHANGED started: " + getUUIDName(characteristic.getUuid()));
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processRxCharacteristic(gatt, characteristic);
                    }
                });
            } else {
                processRxCharacteristic(gatt, characteristic);
            }
        }


        private synchronized void processRxCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            Log.i(TAG, "onCharacteristicChanged On Main Thread? " + isOnMainThread());
            Log.e(TAG, "CharBytes-nfy" + Arrays.toString(characteristic.getValue()));
            Log.i(TAG, "CharHex-nfy" + Extensions.bytesToHex(characteristic.getValue()));


            byte[] buffer = characteristic.getValue();
            byte firstByte = buffer[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && gatt != null) {
                mGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            }
            Log.d(TAG, "Received opcode reply: " + JoH.bytesToHex(new byte[] { firstByte }));
            if (firstByte == 0x2f) {
                SensorRxMessage sensorRx = new SensorRxMessage(characteristic.getValue());

                ByteBuffer sensorData = ByteBuffer.allocate(buffer.length);
                sensorData.order(ByteOrder.LITTLE_ENDIAN);
                sensorData.put(buffer, 0, buffer.length);

                int sensor_battery_level = 0;
                if (sensorRx.status == TransmitterStatus.BRICKED) {
                    //TODO Handle this in UI/Notification
                    sensor_battery_level = 206; //will give message "EMPTY"
                } else if (sensorRx.status == TransmitterStatus.LOW) {
                    sensor_battery_level = 209; //will give message "LOW"
                } else {
                    sensor_battery_level = 216; //no message, just system status "OK"
                }

                //Log.e(TAG, "filtered: " + sensorRx.filtered);
                disconnected133 = 0; // reset as we got a reading
                disconnected59 = 0;
                Log.e(TAG, "unfiltered: " + sensorRx.unfiltered);
                doDisconnectMessage(gatt, characteristic);
                processNewTransmitterData(sensorRx.unfiltered, sensorRx.filtered, sensor_battery_level, new Date().getTime());
            } else if (firstByte == GlucoseRxMessage.opcode) {
                disconnected133 = 0; // reset as we got a reading
                disconnected59 = 0;
                GlucoseRxMessage glucoseRx = new GlucoseRxMessage(characteristic.getValue());
                Log.e(TAG, "glucose unfiltered: " + glucoseRx.unfiltered);
                doDisconnectMessage(gatt, characteristic);
                processNewTransmitterData(glucoseRx.unfiltered, glucoseRx.filtered, 216, new Date().getTime());
            } else {
                Log.e(TAG,"onCharacteristic CHANGED unexpected opcode: "+firstByte+" (have not disconnected!)");
            }
            Log.e(TAG, "OnCharacteristic CHANGED finished: ");
        }
    };
    // end BluetoothGattCallback


    private synchronized void sendAuthRequestTxMessage(BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, "Sending new AuthRequestTxMessage to " + getUUIDName(characteristic.getUuid()) + " ...");
        authRequest = new AuthRequestTxMessage(getTokenSize());
        Log.i(TAG, "AuthRequestTX: " + JoH.bytesToHex(authRequest.byteSequence));
        characteristic.setValue(authRequest.byteSequence);
        mGatt.writeCharacteristic(characteristic);
    }

    private synchronized void processNewTransmitterData(int raw_data , int filtered_data,int sensor_battery_level, long captureTime) {

        final TransmitterData transmitterData = TransmitterData.create(raw_data, filtered_data, sensor_battery_level, captureTime);
        if (transmitterData == null) {
            Log.e(TAG, "TransmitterData.create failed: Duplicate packet");
            return;
        } else {
            timeInMillisecondsOfLastSuccessfulSensorRead = captureTime;
        }
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.e(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        //TODO : LOG if unfiltered or filtered values are zero

        Sensor.updateBatteryLevel(sensor, transmitterData.sensor_battery_level);
        Log.i(TAG,"timestamp create: "+ Long.toString(transmitterData.timestamp));

        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, this, transmitterData.timestamp);

        Log.d(TAG,"Dex raw_data "+ Double.toString(transmitterData.raw_data));//KS
        Log.d(TAG,"Dex filtered_data "+ Double.toString(transmitterData.filtered_data));//KS
        Log.d(TAG,"Dex sensor_battery_level "+ Double.toString(transmitterData.sensor_battery_level));//KS
        Log.d(TAG,"Dex timestamp "+ JoH.dateTimeText(transmitterData.timestamp));//KS

    }

    @SuppressLint("GetInstance")
    private synchronized byte[] calculateHash(byte[] data) {
        if (data.length != 8) {
            Log.e(TAG, "Decrypt Data length should be exactly 8.");
            return null;
        }

        byte[] key = cryptKey();
        if (key == null)
            return null;

        byte[] doubleData;
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(data);
        bb.put(data);

        doubleData = bb.array();

        Cipher aesCipher;
        try {
            aesCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
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

    private byte[] cryptKey() {
        if (defaultTransmitter.transmitterId.length() != 6) Log.e(TAG,"cryptKey: Wrong transmitter id length!: "+defaultTransmitter.transmitterId.length());
        try {
            return ("00" + defaultTransmitter.transmitterId + "00" + defaultTransmitter.transmitterId).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isOnMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private long getNextAdvertiseTime() {
        long millisecondsSinceTx = getMillisecondsSinceTxLastSeen();
        long timeToExpected  = (300*1000 - (millisecondsSinceTx%(300*1000)));
        long expectedTxTime = new Date().getTime() + timeToExpected - 3*1000;
        Log.e(TAG, "millisecondsSinceTxAd: " + millisecondsSinceTx );
        //Log.e(TAG, "timeToExpected: " + timeToExpected );
        //Log.e(TAG, "expectedTxTime: " + expectedTxTime );

        return expectedTxTime;
    }

    protected void waitFor(final int millis) {
        synchronized (mLock) {
            try {
                Log.e(TAG, "waiting " + millis + "ms");
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                Log.e(TAG, "Sleeping interrupted", e);
            }
        }
    }

    private long getMillisecondsSinceTxLastSeen() {
        return new Date().getTime() - advertiseTimeMS.get(0);
    }

    private long getMillisecondsSinceLastSuccesfulSensorRead() {
        return new Date().getTime() - timeInMillisecondsOfLastSuccessfulSensorRead;
    }

    private boolean scanConstantly() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("run_ble_scan_constantly", false);
    }

    private boolean alwaysUnbond() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("always_unbond_G5", false);
    }

    private boolean alwaysAuthenticate() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("always_get_new_keys", false);
    }

    private boolean enforceMainThread() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("run_G5_ble_tasks_on_uithread", false);
    }

    // TODO this could be cached for performance
    private boolean useG5NewMethod() {
        return Home.getPreferencesBooleanDefaultFalse("g5_non_raw_method");
    }

    private boolean g5BluetoothWatchdog() {
        return Home.getPreferencesBoolean("g5_bluetooth_watchdog", true);
    }


    private int getTokenSize() {
            return 8; // d
    }

    private String settingsToString() {
        return ((scanConstantly() ? "scanConstantly " : "")
                + (alwaysUnbond() ? "alwaysUnbond " : "")
                + (alwaysAuthenticate() ? "alwaysAuthenticate " : "")
                + (enforceMainThread() ? "enforceMainThread " : "")
                + (useG5NewMethod() ? "useG5NewMethod " : "")
                + (ignoreLocalBondingState ? "ignoreLocalBondingState " : "")
                + (delayOnBond ? "delayOnBond " : "")
                + (delayOn133Errors ? "delayOn133Errors " : "")
                + (tryPreBondWithDelay ? "tryPreBondWithDelay " : ""));
    }

}