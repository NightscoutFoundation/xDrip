package com.eveningoutpost.dexdrip.nocturne;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Handles the UI flow for connecting xDrip+ to a Nocturne instance
 * via the OAuth 2.0 Device Authorization flow.
 */
public class NocturneConnectHelper {

    private static final String TAG = "NocturneConnect";

    /**
     * Existing entry point from settings UI. Shows the "already connected?"
     * prompt, auto-opens the browser when the user code is shown, and has
     * no completion callback.
     */
    public static void startConnectFlow(final Activity activity) {
        startConnectFlow(activity, null, true, false);
    }

    /**
     * Full-featured entry point with options.
     *
     * @param activity           context for dialogs and UI thread marshaling
     * @param onFinish           optional callback run on UI thread when the flow ends (any outcome)
     * @param autoOpenBrowser    if true, "Open Browser" button launches the verification URL;
     *                           if false, only a Cancel button is shown (user enters code on
     *                           their already-logged-in desktop browser)
     * @param skipConnectedCheck if true, skip the "already connected, disconnect first?" prompt
     *                           (caller is responsible for switch confirmation + token revocation)
     */
    public static void startConnectFlow(
            final Activity activity,
            final Runnable onFinish,
            final boolean autoOpenBrowser,
            final boolean skipConnectedCheck) {

        if (!NocturneUploader.isSupported()) {
            Toast.makeText(activity, R.string.nocturne_requires_android_8, Toast.LENGTH_LONG).show();
            runOnUiThreadSafely(activity, onFinish);
            return;
        }

        if (!skipConnectedCheck && NocturneOAuthService.isConnected()) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.nocturne)
                    .setMessage(R.string.nocturne_already_connected_disconnect)
                    .setPositiveButton(R.string.nocturne_disconnect, (dialog, which) -> {
                        new Thread(() -> {
                            new NocturneOAuthService().revokeToken();
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, R.string.nocturne_disconnected, Toast.LENGTH_SHORT).show();
                                runOnUiThreadSafely(activity, onFinish);
                            });
                        }).start();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> runOnUiThreadSafely(activity, onFinish))
                    .show();
            return;
        }

        final String url = Pref.getString("nocturne_instance_url", "").trim();
        if (url.isEmpty()) {
            Toast.makeText(activity, R.string.nocturne_set_url_first, Toast.LENGTH_LONG).show();
            runOnUiThreadSafely(activity, onFinish);
            return;
        }

        Toast.makeText(activity, R.string.nocturne_connecting, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            final NocturneOAuthService oauthService = new NocturneOAuthService();
            final NocturneOAuthService.DeviceCodeResponse response = oauthService.startDeviceFlow();

            if (response == null) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, R.string.nocturne_start_auth_failed, Toast.LENGTH_LONG).show();
                    runOnUiThreadSafely(activity, onFinish);
                });
                return;
            }

            activity.runOnUiThread(() ->
                    showDeviceCodeDialog(activity, oauthService, response, onFinish, autoOpenBrowser));
        }).start();
    }

    private static void showDeviceCodeDialog(
            final Activity activity,
            final NocturneOAuthService oauthService,
            final NocturneOAuthService.DeviceCodeResponse response,
            final Runnable onFinish,
            final boolean autoOpenBrowser) {

        final int expiryMinutes = response.expiresIn / 60;
        final String message = activity.getString(
                autoOpenBrowser
                    ? R.string.nocturne_device_code_body_auto
                    : R.string.nocturne_device_code_body_manual,
                response.userCode,
                expiryMinutes);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.nocturne_authorize_title)
                .setMessage(message)
                .setCancelable(false);

        if (autoOpenBrowser) {
            builder.setPositiveButton(R.string.nocturne_open_browser, (d, which) -> {
                try {
                    final String url = response.verificationUriComplete != null
                            ? response.verificationUriComplete
                            : response.verificationUri;
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Could not open browser: " + e.getMessage());
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (d, w) -> runOnUiThreadSafely(activity, onFinish));
        } else {
            builder.setPositiveButton(android.R.string.cancel, (d, w) -> runOnUiThreadSafely(activity, onFinish));
        }

        final AlertDialog dialog = builder.create();
        dialog.show();

        new Thread(() -> {
            int interval = response.interval;
            final long deadline = JoH.tsl() + (response.expiresIn * 1000L);

            while (JoH.tsl() < deadline) {
                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    return;
                }

                final NocturneOAuthService.TokenPollResult result = oauthService.pollForToken(response.deviceCode);

                switch (result) {
                    case SUCCESS:
                        // Trigger an immediate upload so the user sees data flow to
                        // Nocturne right after connecting, instead of waiting for the
                        // next periodic tick.
                        try {
                            SyncService.startSyncService(0);
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "Immediate upload trigger failed: " + e.getMessage());
                        }
                        activity.runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(activity, R.string.nocturne_connected, Toast.LENGTH_LONG).show();
                            runOnUiThreadSafely(activity, onFinish);
                        });
                        return;

                    case SLOW_DOWN:
                        interval += 5;
                        break;

                    case PENDING:
                        break;

                    case EXPIRED:
                        activity.runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(activity, R.string.nocturne_auth_expired, Toast.LENGTH_LONG).show();
                            runOnUiThreadSafely(activity, onFinish);
                        });
                        return;

                    case DENIED:
                        activity.runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(activity, R.string.nocturne_auth_denied, Toast.LENGTH_SHORT).show();
                            runOnUiThreadSafely(activity, onFinish);
                        });
                        return;
                }
            }

            activity.runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(activity, R.string.nocturne_auth_timed_out, Toast.LENGTH_LONG).show();
                runOnUiThreadSafely(activity, onFinish);
            });
        }).start();
    }

    /**
     * Safely runs a Runnable on the activity's UI thread.
     * <p>
     * The OAuth device-flow polling thread can outlive its activity (rotation,
     * backgrounding, low-memory kill), so this helper is a no-op when:
     * <ul>
     *     <li>the runnable is null,</li>
     *     <li>the activity is null, or</li>
     *     <li>the activity is finishing or already destroyed.</li>
     * </ul>
     * This makes the silence intentional rather than relying on
     * {@link Activity#runOnUiThread} silently dropping posts to a dead window
     * token. When called from a click handler already on the UI thread the
     * runnable is still scheduled via runOnUiThread which is equivalent to
     * calling directly.
     */
    private static void runOnUiThreadSafely(final Activity activity, final Runnable runnable) {
        if (runnable == null || activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        activity.runOnUiThread(runnable);
    }
}
