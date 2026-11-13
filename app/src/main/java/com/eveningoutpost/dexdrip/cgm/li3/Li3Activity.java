package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.nfc.NFControl;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

import java.security.GeneralSecurityException;
import java.util.function.Supplier;

/**
 * The Li3 screen: the setup steps and which are done, what the collector knows, asking for location, downloading
 * the app credential, looking up the account ID, and forgetting the sensor.
 * Starting or taking over a sensor is done by scanning it with NFC; the screen reads scans itself while it is open.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3Activity extends ActivityWithMenu {

    private static final String TAG = "Li3";
    /** How often the open screen shows the collector's status again, so a scan or a reconnect on it shows. */
    private static final long REFRESH_MS = 5_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, REFRESH_MS);
        }
    };
    @VisibleForTesting
    static final Supplier<Li3AccountLookup> DEFAULT_LOOKUP =
            () -> new Li3AccountLookup(OkHttpWrapper.getClient());
    /** Where the account ID lookup comes from; tests answer it without the network. */
    @VisibleForTesting
    static Supplier<Li3AccountLookup> lookupFactory = DEFAULT_LOOKUP;

    private TextView steps;
    private TextView status;
    private boolean offeredDownload;
    /** A lookup is running; a second OK would send the login twice. */
    private volatile boolean lookingUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);

        steps = new TextView(this);
        layout.addView(steps);

        status = new TextView(this);
        status.setPadding(0, pad, 0, pad);
        layout.addView(status);

        Button download = new Button(this);
        download.setText(R.string.li3_download_credential);
        download.setOnClickListener(v -> downloadCredential());
        layout.addView(download);

        Button lookup = new Button(this);
        lookup.setText(R.string.li3_lookup_account);
        lookup.setOnClickListener(v -> lookUpAccountId());
        layout.addView(lookup);

        Button forget = new Button(this);
        forget.setText(R.string.li3_forget_sensor);
        forget.setOnClickListener(v -> confirmForget());
        layout.addView(forget);

        Button diagnostics = new Button(this);
        diagnostics.setText(R.string.li3_copy_diagnostics);
        diagnostics.setOnClickListener(v -> Li3Diagnostics.copyToClipboard(this));
        layout.addView(diagnostics);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
        JoH.fixActionBar(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // a scan on this screen is read here, so the start or takeover question is asked on it
        NFControl.initNFC(this, false);
        handler.removeCallbacks(refresher);
        refresher.run();
        if (locationReady()) {
            offerDownload();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refresher);
        NFControl.initNFC(this, true); // disables
    }

    /**
     * The Bluetooth scan needs location permission and location turned on; asks for whichever is missing, as the
     * other Bluetooth collectors do. The credential offer waits, so two dialogs never stack.
     *
     * @return true when nothing was asked
     */
    private boolean locationReady() {
        try {
            return LocationHelper.requestLocationForBluetooth(this) && LocationHelper.isLocationEnabled(this);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception requesting location: " + e.getMessage());
            return true;
        }
    }

    /** Without a credential, asks once per visit whether to download it now. */
    private void offerDownload() {
        if (offeredDownload || !Li3Credential.get(this).load().isEmpty()) {
            return;
        }
        offeredDownload = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.li3_download_credential)
                .setMessage(R.string.li3_offer_download)
                .setPositiveButton(R.string.yes, (dialog, which) -> downloadCredential())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void refresh() {
        steps.setText(Li3Setup.text(this));
        StringBuilder sb = new StringBuilder();
        for (StatusItem item : Li3Service.megaStatus()) {
            if (item.runnable == null) {
                sb.append(item.name).append(": ").append(item.value).append('\n');
            }
        }
        status.setText(sb.toString().trim());
    }

    private void downloadCredential() {
        JoH.static_toast_short(gs(R.string.li3_downloading));
        new Thread(() -> {
            String message;
            try {
                Li3Credential.get(this).download(OkHttpWrapper.getClient());
                message = gs(R.string.li3_credential_ready);
                CollectionServiceStarter.restartCollectionServiceBackground();
            } catch (GeneralSecurityException e) {
                UserError.Log.e(TAG, e.getMessage());
                message = gs(R.string.li3_download_failed);
            }
            String result = message;
            runOnUiThread(() -> {
                JoH.static_toast_long(result);
                refresh();
            });
        }).start();
    }

    /** Asks before replacing an account ID already set, then for the login. */
    private void lookUpAccountId() {
        if (Li3Store.accountId().isEmpty()) {
            askLogin();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.li3_lookup_account)
                .setMessage(R.string.li3_lookup_replace)
                .setPositiveButton(R.string.ok, (dialog, which) -> askLogin())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void askLogin() {
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(pad, 0, pad, 0);
        EditText email = new EditText(this);
        email.setHint(R.string.li3_email);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        fields.addView(email);
        EditText password = new EditText(this);
        password.setHint(R.string.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        fields.addView(password);
        new AlertDialog.Builder(this)
                .setTitle(R.string.li3_lookup_account)
                .setMessage(R.string.li3_lookup_hint)
                .setView(fields)
                .setPositiveButton(R.string.ok, (dialog, which) ->
                        fillAccountId(email.getText().toString().trim(), password.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void fillAccountId(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            JoH.static_toast_long(gs(R.string.li3_lookup_blank));
            return;
        }
        if (lookingUp) {
            return;
        }
        lookingUp = true;
        JoH.static_toast_short(gs(R.string.li3_looking_up));
        new Thread(() -> {
            int message = Li3AccountLookup.fillAccountId(lookupFactory.get(), email, password);
            runOnUiThread(() -> {
                lookingUp = false;
                JoH.static_toast_long(gs(message));
                refresh();
            });
        }).start();
    }

    private void confirmForget() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.li3_forget_sensor)
                .setMessage(R.string.li3_forget_sensor_confirm)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Li3Store.forgetSensor();
                    UserError.Log.uel(TAG, "Li3 sensor forgotten by user");
                    CollectionServiceStarter.restartCollectionServiceBackground();
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public String getMenuName() {
        return "";
    }
}
