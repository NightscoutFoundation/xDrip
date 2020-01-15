package com.eveningoutpost.dexdrip.ui.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.Observable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.databinding.ObservableList;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BR;
import com.eveningoutpost.dexdrip.MegaStatus;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.databinding.ActivityThinJamBinding;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.ui.dialog.QuickSettingsDialogs;
import com.eveningoutpost.dexdrip.utils.AndroidBarcode;
import com.eveningoutpost.dexdrip.utils.LocationHelper;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.utils.bt.BtCallBack2;
import com.eveningoutpost.dexdrip.utils.bt.ScanMeister;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayService;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.zxing.integration.android.IntentIntegrator;
import com.polidea.rxandroidble2.scan.ScanFilter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.tatarka.bindingcollectionadapter2.ItemBinding;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_HUNT_MASK_STRING;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_HUNT_SERVICE_STRING;

// jamorham

public class ThinJamActivity extends AppCompatActivity implements BtCallBack2 {

    private static final String TAG = "ThinJamActivity";
    private static final String THINJAM_ACTIVITY_COMMAND = "THINJAM_ACTIVITY_COMMAND";
    private static final String REFRESH_FROM_STORED_MAC = "REFRESH_FROM_STORED_MAC";
    private static final String INSTALL_CORE = "INSTALL_CORE";
    private static final String UPDATE_CORE = "UPDATE_CORE";
    private static final String UPDATE_MAIN = "UPDATE_MAIN";
    private static final String REQUEST_QR = "REQUEST_QR";
    private static final String SCAN_QR = "SCAN_QR";

    @Getter
    private static final boolean D = false;

    private ActivityThinJamBinding binding;
    private final ScanMeister scanMeister = new ScanMeister();
    private final ScanFilter customFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(THINJAM_HUNT_SERVICE_STRING), ParcelUuid.fromString(THINJAM_HUNT_MASK_STRING)).build();
    private final ScanFilter nullFilter = new ScanFilter.Builder().build();

    private BlueJayService thinJam;
    private boolean mBound = false;
    private volatile Bundle savedExtras = null;

    private final Observable.OnPropertyChangedCallback changeRelayAdapter = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            binding.getVm().textWindow.set(((ObservableField<String>) sender).get());
        }
    };

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            final BlueJayService.LocalBinder binder = (BlueJayService.LocalBinder) service;
            thinJam = binder.getService();
            thinJam.stringObservableField.removeOnPropertyChangedCallback(changeRelayAdapter);
            thinJam.stringObservableField.addOnPropertyChangedCallback(changeRelayAdapter); // TODO can this leak? Better way to use same observable field?
            mBound = true;
            UserError.Log.d(TAG, "Connected to service");
            processIncomingBundle(savedExtras);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThinJamBinding.inflate(getLayoutInflater());
        binding.setVm(new ViewModel(this));
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);

        scanMeister.setFilter(customFilter);
        scanMeister.allowWide().unlimitedMatches();
        scanMeister.addCallBack2(this, TAG);

        ((TextView) findViewById(R.id.tjLogText)).setMovementMethod(new ScrollingMovementMethod());

        // handle incoming extras - TODO do we need to wait for service connect?
        final Bundle bundle = getIntent().getExtras();
        processIncomingBundle(bundle);

        LocationHelper.requestLocationForBluetooth(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_thinjam, menu);
        return true;
    }

    public void blueJayPowerStandby(MenuItem x) {

        GenericConfirmDialog.show(this, "Confirm Standby",
                "Are you sure you want to put the watch in to Standby mode?\n\nLong Press Watch Button to wake it again.",
                () -> {
                    // call standby
                    thinJam.standby();
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService();
    }

    private void bindService() {
        final Intent intent = new Intent(this, BlueJayService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        thinJam.stringObservableField.removeOnPropertyChangedCallback(changeRelayAdapter);
        unbindService(connection);
        mBound = false;
        super.onStop();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        processIncomingBundle(bundle);
    }

    /*  @Override
      protected void onDestroy() {
          thinJam.shutdown();
          super.onDestroy();
      }
  */
    @Override
    public void btCallback2(String mac, String status, String name, Bundle bundle) {
        UserError.Log.d(TAG, "BT scan: " + mac + " " + status + " " + name);
        if (status.equals("SCAN_FOUND")) {
            binding.getVm().addToList(mac, name);
        }
    }

    // View model item

    public class TjItem {
        public String name;
        public String mac;
        public int resource;
        public ViewModel model;

        public void onClick(View v) {
            UserError.Log.d(TAG, "Clicked: " + mac);
            binding.getVm().stopScan();

            binding.getVm().items.clear();
            binding.getVm().changeConnectedDevice(this);
        }

        TjItem(final String mac, final String name) {
            this.mac = mac;
            this.name = name;
        }
    }

    // bound view model
    @RequiredArgsConstructor
    public class ViewModel {
        Activity activity;

        public final ObservableField<String> connectedDevice = new ObservableField<>();
        public final ObservableField<String> status = new ObservableField<>();
        public final ObservableField<String> textWindow = new ObservableField<>();
        public final ObservableField<Integer> progressBar = new ObservableField<>();

        public final ObservableList<TjItem> items = new ObservableArrayList<>();
        public final ItemBinding<TjItem> itemBinding = ItemBinding.of(BR.item, R.layout.thinjam_scan_item);

        private String mac = null;


        ViewModel(Activity activity) {
            this.activity = activity;
            loadLastConnectedDevice();
        }


        public boolean showBack() {
            return true;
        }

        public boolean showScanList() {
            return items.size() > 0;
        }

        public void goBack(View v) {
            stopScan();

            items.clear();
        }

        public boolean doAction(final String action) {
            switch (action) {

                case "updateconfig":
                    JoH.static_toast_long("Refreshing watch config");
                    validateTxId();
                    break;

                case "asktxid":
                    // for debug only
                    setTx();
                    break;

                case "showqrcode":
                    JoH.static_toast_long("Showing QR code on watch");
                    thinJam.showQrCode();
                    break;

                case "easyauth":
                    JoH.static_toast_long("Doing non-QR code Easy Auth");
                    thinJam.easyAuth();
                    break;

                case "reboot":
                    JoH.static_toast_long("Rebooting");
                    thinJam.reboot();
                    break;

                case "factoryreset":
                    JoH.static_toast_long("Factory Resetting");
                    thinJam.factoryReset();
                    break;

                case "launchstatus":
                    xdrip.getAppContext().startActivity(JoH.getStartActivityIntent(MegaStatus.class).setAction("BlueJay").addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    break;

                case "launchhelp":
                    xdrip.getAppContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bluejay.website/quickhelp")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;

                case "launchsettings":
                    xdrip.getAppContext().startActivity(JoH.getStartActivityIntent(Preferences.class).setAction("bluejay_preference_screen").addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                break;



                default:
                    JoH.static_toast_long("Unknown action: "+action);
                    break;
            }
            return true;
        }

        public boolean scan(boolean legacy) {
            UserError.Log.d(TAG, "Start scan");
            if (JoH.ratelimit("bj-scan-startb",5)) {
                if (legacy) {
                    scanMeister.setFilter(nullFilter).scan();
                } else {
                    scanMeister.setFilter(customFilter).scan();
                }
            }
            return false;
        }

        public void getStatus() {
            thinJam.getStatus();
        }

        // boolean for long click
        public boolean unitTesting(final String test) {
            // call external test suite
            if (test.startsWith("confirm")) {
                val subtest = test.substring(7,test.length());
                GenericConfirmDialog.show(this.activity, "Confirm:", "Please confirm when ready for: " + subtest, () -> {
                    thinJam.setProgressIndicator(progressBar);
                    thinJam.getDebug().processTestSuite(subtest);
                });
            } else {
                thinJam.setProgressIndicator(progressBar);
                thinJam.getDebug().processTestSuite(test);
            }
            return true;
        }

        final String TJ_UI_TX_ID = "Last-tj-txid";

        // TODO remove?
        void setTx() {
            QuickSettingsDialogs.textSettingDialog(activity, TJ_UI_TX_ID, "Transmitter ID", "Enter TXID", InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, new Runnable() {
                @Override
                public void run() {
                    validateTxId();     // also sets settings
                }
            });
        }

        private void validateTxId() {
            Pref.setString(TJ_UI_TX_ID,Pref.getString("dex_txid","")); // for now just override but internal pref is probably not needed
            String txid = Pref.getString(TJ_UI_TX_ID, "").trim().toUpperCase();
            if (txid.length() > 6) {
                txid = "";
            }
            thinJam.setSettings(txid);
            thinJam.setTime();
        }


        // long click needs boolean
        public boolean doOtaC(View v) {
            thinJam.setProgressIndicator(progressBar);
            JoH.static_toast_long("Doing OTA CORE");
            Inevitable.task("do-ota-tj", 500, () -> thinJam.doOtaCore());
            return false;
        }

        // long click needs boolean
        public void doOtaM(View v) {
            thinJam.setProgressIndicator(progressBar);
            //  JoH.static_toast_long("Doing OTA CORE");
            Inevitable.task("do-ota-tj", 500, () -> thinJam.doOtaMain());
        }

        // long click needs boolean
        public boolean doIdentityc(View v) {
            identify();
            return false;
        }

        public void stopScan() {
            UserError.Log.d(TAG, "Stop scan");
            scanMeister.stop();
        }


        public void addToList(final String mac, final String name) {
            if (mac == null) return;
            synchronized (items) {
                if (!macInList(mac)) {
                    items.add(new TjItem(mac, name));
                    UserError.Log.d(TAG, "Added item " + mac + " " + name + " total:" + items.size());
                }
            }
        }

        public boolean macInList(final String mac) {
            if (mac == null) return false;
            for (TjItem item : items) {
                if (item.mac.equals(mac)) return true;
            }
            return false;
        }

        public void changeConnectedDevice(final TjItem item) {
            connectedDevice.set(item.mac + " " + item.name);
            setMac(item.mac);
            saveConnectedDevice();
            BlueJayEntry.setEnabled();
            thinJam.emptyQueue();
            thinJam.stringObservableField.set("");
            thinJam.notificationString.setLength(0);
            refreshFromStoredMac();
        }

        private static final String PREF_CONNECTED_DEVICE_INFO = "TJ_CONNECTED_DEVICE_INFO";

        private void saveConnectedDevice() {
            PersistentStore.setString(PREF_CONNECTED_DEVICE_INFO, connectedDevice.get());
        }

        private void loadLastConnectedDevice() {
            connectedDevice.set(PersistentStore.getString(PREF_CONNECTED_DEVICE_INFO));
            try {
                String[] split = connectedDevice.get().split(" ");
                if (split[0].length() == 17)
                    setMac(split[0]);
            } catch (Exception e) {
                //
            }
            UserError.Log.d(TAG, "Mac loaded in as: " + mac);
        }

        private void setMac(final String mac) {
            if (mac == null) return;
            this.mac = mac;
            thinJam.setMac(mac);
        }

        private void identify() {
          thinJam.identify();
        }

    } // end view model class

    public static void refreshFromStoredMac() {
        startWithCommand(REFRESH_FROM_STORED_MAC);
    }

    public static void installCorePrompt() {
        startWithCommand(INSTALL_CORE);
    }

    public static void updateCorePrompt() {
        startWithCommand(UPDATE_CORE);
    }

    public static void updateMainPrompt() {
        startWithCommand(UPDATE_MAIN);
    }

    public static void requestQrCode() { startWithCommand(REQUEST_QR);}

    public static void launchQRScan() {
        startWithCommand(SCAN_QR);
    }

    private static void startWithCommand(final String command) {
        xdrip.getAppContext().startActivity(new Intent(xdrip.getAppContext(), ThinJamActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(THINJAM_ACTIVITY_COMMAND, command));
    }

    private void processIncomingBundle(final Bundle bundle) {

        if (bundle != null) {
            Log.d(TAG, "Processing incoming bundle");

            if (!mBound) {
                // save to process when service connects
                savedExtras = bundle;
                return;
            } else {
                savedExtras = null; // clear if might have been set
            }

            final String command = bundle.getString(THINJAM_ACTIVITY_COMMAND);
            if (command != null) {
                switch (command) {
                    case REFRESH_FROM_STORED_MAC:
                        binding.getVm().connectedDevice.set(BlueJay.getMac());
                        binding.getVm().setMac(BlueJay.getMac()); // TODO this doesn't handle name
                        binding.getVm().identify(); // re(do) identification.
                        break;
                    case INSTALL_CORE:
                        promptForCoreUpdate(false);
                        break;
                    case UPDATE_CORE:
                        promptForCoreUpdate(true);
                        break;
                    case UPDATE_MAIN:
                        promptForMainUpdate();
                        break;
                    case REQUEST_QR:
                        binding.getVm().doAction("showqrcode");
                        break;
                    case SCAN_QR:
                        new AndroidBarcode(this).scan();
                        break;
                }
            }
        }
    }

    private void promptForCoreUpdate(boolean alreadyInstalled) {

        GenericConfirmDialog.show(this, alreadyInstalled ? "Update Core?" : "Install Core?",
                "Install xDrip core module?\n\nMake sure device is charging or this can break it!",
                () -> {
                    binding.getVm().doOtaC(null);
                });
    }

    private void promptForMainUpdate() {

        GenericConfirmDialog.show(this, "Update Firmware?",
                "Update BlueJay firmware?\n\nMake sure device is charging or this can break it!",
                () -> {
                    binding.getVm().doOtaM(null);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (scanResult == null || scanResult.getContents() == null) {
            return;
        }
        if (scanResult.getFormatName().equals("QR_CODE")) {

            try {
                BlueJay.processQRCode(scanResult.getRawBytes());
            } catch (Exception e) {
                // meh
            }

        }
    }
}





