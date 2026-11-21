package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPausedAsyncTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for importing settings from a scanned xDrip settings QR code in {@link Preferences}.
 * <p>
 * Background tasks are captured instead of run, so the settings wizard never reaches the network.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class PreferencesQrImportTest extends RobolectricTestWithConfig {

    private final List<Runnable> startedTasks = new ArrayList<>();

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        ShadowAlertDialog.reset();
        ShadowPausedAsyncTask.overrideExecutor(startedTasks::add);
    }

    @After
    public void tearDown() {
        ShadowPausedAsyncTask.overrideExecutor(null);
    }

    // ===== Ordinary settings code ====================================================================================

    /** An ordinary settings code asks first and changes nothing until confirmed. */
    @Test
    public void ordinaryCodeAsksBeforeChangingSettings() throws Exception {
        // :: Setup
        final Map<String, String> settings = new HashMap<>();
        settings.put("highValue", "12");
        final Preferences activity = Robolectric.buildActivity(Preferences.class).setup().get();

        // :: Act
        scan(activity, settings);

        // :: Verify
        assertThat(shownDialog()).isNotNull();
        assertThat(store().getString("highValue", "")).isNotEqualTo("12");
    }

    // ===== Settings wizard code ======================================================================================

    /** A settings wizard code asks first and fetches nothing until confirmed. */
    @Test
    public void wizardCodeAsksBeforeFetchingSettings() throws Exception {
        // :: Setup
        final Preferences activity = Robolectric.buildActivity(Preferences.class).setup().get();

        // :: Act
        scan(activity, wizardSettings());

        // :: Verify
        assertThat(shownDialog()).isNotNull();
        assertThat(shadowOf(shownDialog()).getMessage().toString())
                .isEqualTo(activity.getString(R.string.import_qr_code_wizard_warning));
        assertThat(startedTasks).isEmpty();
    }

    /** Confirming a settings wizard code starts fetching the settings. */
    @Test
    public void wizardCodeFetchesSettingsAfterConfirming() throws Exception {
        // :: Setup
        final Preferences activity = Robolectric.buildActivity(Preferences.class).setup().get();
        scan(activity, wizardSettings());

        // :: Act
        shownDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(startedTasks).hasSize(1);
    }

    /** Declining a settings wizard code fetches nothing. */
    @Test
    public void wizardCodeFetchesNothingWhenDeclined() throws Exception {
        // :: Setup
        final Preferences activity = Robolectric.buildActivity(Preferences.class).setup().get();
        scan(activity, wizardSettings());

        // :: Act
        shownDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(startedTasks).isEmpty();
    }

    // =================================================================================================================

    /** Hands the activity a camera scan of a settings QR code carrying these settings, as the scanner app does. */
    private static void scan(final Preferences activity, final Map<String, String> settings) {
        final String contents = QRcodeUtils.qrmarker + JoH.compressString(new JSONObject(settings).toString());
        final Intent result = new Intent()
                .putExtra("SCAN_RESULT", contents)
                .putExtra("SCAN_RESULT_FORMAT", "QR_CODE");
        activity.onActivityResult(Constants.ZXING_CAM_REQ_CODE, Activity.RESULT_OK, result);
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** What the other phone's settings wizard puts in its QR code: the marker, the upload id and the key. */
    private static Map<String, String> wizardSettings() {
        final Map<String, String> settings = new HashMap<>();
        settings.put("all_settings_wizard", "t");
        settings.put("wizard_uuid", "0123456789abcdef0123456789abcdef");
        settings.put("wizard_key", "00112233445566778899aabbccddeeff");
        return settings;
    }

    private static AlertDialog shownDialog() {
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
