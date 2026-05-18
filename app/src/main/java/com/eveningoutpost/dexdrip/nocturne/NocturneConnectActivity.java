package com.eveningoutpost.dexdrip.nocturne;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Handles xdrip://connect/nocturne?url={instanceUrl} deep links.
 *
 * Flow:
 *   1. Validates the URL (parseable; https preferred, http warned)
 *   2. If already connected to the same URL: shows toast and finishes
 *   3. If already connected to a different URL: prompts to switch, revokes old tokens
 *   4. Saves nocturne_instance_url and enables nocturne_upload_enable + nocturne_upload_sgv
 *   5. Clears nocturne_client_id so a fresh registerClient() runs (handles reinstalls / revocations)
 *   6. Delegates to NocturneConnectHelper with autoOpenBrowser=false, skipConnectedCheck=true
 */
public class NocturneConnectActivity extends Activity {

    private static final String TAG = "NocturneConnectAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri data = getIntent().getData();
        if (data == null) {
            UserError.Log.e(TAG, "No intent data");
            finish();
            return;
        }

        final String url = data.getQueryParameter("url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, R.string.nocturne_connect_err_missing_url, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Validate via parsed Uri — check both host and scheme.
        // (Uri.parse never throws on Android, so no try/catch needed.)
        final String trimmedUrl = url.trim();
        final Uri parsed = Uri.parse(trimmedUrl);
        final String scheme = parsed.getScheme();
        if (parsed.getHost() == null || parsed.getHost().isEmpty() || scheme == null) {
            Toast.makeText(this, R.string.nocturne_connect_err_invalid_url, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // https preferred; warn on http but allow (local dev, self-hosted LAN)
        if ("http".equalsIgnoreCase(scheme)) {
            Toast.makeText(this, R.string.nocturne_connect_warn_http, Toast.LENGTH_LONG).show();
        } else if (!"https".equalsIgnoreCase(scheme)) {
            Toast.makeText(this, R.string.nocturne_connect_err_bad_scheme, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String existingUrl = Pref.getString("nocturne_instance_url", "").trim();
        final boolean alreadyConnected = NocturneOAuthService.isConnected();

        if (alreadyConnected && existingUrl.equals(trimmedUrl)) {
            // Same-instance re-scan — just show a toast
            Toast.makeText(this, getString(R.string.nocturne_connect_already_connected, trimmedUrl), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!alreadyConnected) {
            // Fresh connect — confirm with user before enabling uploads / starting device flow.
            // The activity is exported, so any app can fire this intent; require explicit consent.
            new AlertDialog.Builder(this)
                    .setTitle(R.string.nocturne_connect_confirm_title)
                    .setMessage(getString(R.string.nocturne_connect_confirm_message, parsed.getHost()))
                    .setPositiveButton(R.string.nocturne_connect_confirm_positive, (dialog, which) -> connectToInstance(trimmedUrl))
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        if (!existingUrl.isEmpty() && !existingUrl.equals(trimmedUrl)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.nocturne_connect_switch_title)
                    .setMessage(getString(R.string.nocturne_connect_switch_message, existingUrl, trimmedUrl))
                    .setPositiveButton(R.string.nocturne_connect_switch_positive, (dialog, which) -> {
                        new Thread(() -> {
                            new NocturneOAuthService().revokeToken();
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                connectToInstance(trimmedUrl);
                            });
                        }).start();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            connectToInstance(trimmedUrl);
        }
    }

    private void connectToInstance(final String url) {
        Pref.setString("nocturne_instance_url", url);
        Pref.setBoolean("nocturne_upload_enable", true);
        Pref.setBoolean("nocturne_upload_sgv", true);
        // Clear stale client_id so registerClient() runs fresh.
        // This handles revoked clients and reinstalls.
        PersistentStore.setString("nocturne_client_id", "");
        UserError.Log.d(TAG, "Set nocturne_instance_url=" + url + " and cleared client_id");

        NocturneConnectHelper.startConnectFlow(
                this,
                this::finish,
                false,  // autoOpenBrowser — user is likely on a different device
                true    // skipConnectedCheck — we handled switch confirmation above
        );
    }
}
