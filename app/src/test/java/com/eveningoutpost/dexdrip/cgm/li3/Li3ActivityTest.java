package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.os.Looper;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.li3.Li3AccountLookupTest.FakeAccountServer;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowNfcAdapter;
import org.robolectric.shadows.ShadowToast;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import xrwd.li3.Li3SensorState;
import xrwd.li3.data.RealtimeReading;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link Li3Activity}: the screen asks for location, which the Bluetooth scan needs, without an app
 * credential it offers to download it, once per visit, it reads NFC scans itself while it is open, and its status
 * follows the collector while it is open. Looking up the account ID asks for an account login, never sends a blank
 * one, asks before replacing an ID already set, and fills the ID in.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class Li3ActivityTest extends RobolectricTestWithConfig {

    /** A US 15-day Li3: 60 minutes warm-up, 21600 minutes wear. */
    private static final String PATCH_INFO = "00a50001000200010060541e020401040c0130525243393839415151ff17";

    /** Minutes since activation of a reading the collector stores: well past warm-up, on the 5-minute interval. */
    private static final int STORED_LIFE_COUNT = 1000;

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        ShadowDialog.reset();
        Li3Service.forgetReadingTimes();
    }

    @After
    public void tearDown() {
        Pref.removeItem("use_nfc_scan");
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
        Pref.removeItem(Li3Store.PREF_ACCOUNT_ID);
        Li3Activity.lookupFactory = Li3Activity.DEFAULT_LOOKUP;
        Li3Store.forgetSensor();
        Sensor.stopSensor();
    }

    // ================================================ Offer download =================================================

    /** Opening the screen without a credential asks whether to download it now. */
    @Test
    public void onResume_noCredential_offersDownload() {
        // :: Setup
        grantLocation();

        // :: Act
        Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(shadowOf(dialog).getMessage().toString()).isEqualTo(gs(R.string.li3_offer_download));
    }

    /** Coming back to the screen after answering does not ask again. */
    @Test
    public void onResume_again_offersOnce() {
        // :: Setup
        grantLocation();
        ActivityController<Li3Activity> screen = Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().dismiss();

        // :: Act
        screen.pause().resume();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).hasSize(1);
    }

    // =================================================== Location ====================================================

    /** Without location permission the screen asks for it first, and the download offer waits for the next visit. */
    @Test
    public void onResume_noLocationPermission_asksForIt() {
        // :: Act
        Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).hasSize(1);
        assertThat(latestMessage()).isEqualTo(gs(R.string.without_location_scan_doesnt_work));
    }

    /** With location turned off the screen asks to turn it on. */
    @Test
    public void onResume_locationOff_asksToTurnItOn() {
        // :: Setup
        grantLocation();
        setLocationEnabled(false);

        // :: Act
        Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).hasSize(1);
        assertThat(latestMessage()).isEqualTo(gs(R.string.location_not_found_message));
    }

    // ====================================================== NFC ======================================================

    /** With NFC scanning on, the open screen reads a scan itself, so the start or takeover question is asked on it. */
    @Test
    public void onResume_nfcScanOn_readsScans() {
        // :: Setup
        grantLocation();
        enableNfcScan();

        // :: Act
        ActivityController<Li3Activity> screen = Robolectric.buildActivity(Li3Activity.class).setup();

        // :: Verify
        assertThat(shadowOf(NfcAdapter.getDefaultAdapter(screen.get())).isInReaderMode()).isTrue();
    }

    /** Leaving the screen stops it reading scans, so the next screen gets them. */
    @Test
    public void onPause_nfcScanOn_stopsReadingScans() {
        // :: Setup
        grantLocation();
        enableNfcScan();
        ActivityController<Li3Activity> screen = Robolectric.buildActivity(Li3Activity.class).setup();

        // :: Act
        screen.pause();

        // :: Verify
        assertThat(shadowOf(NfcAdapter.getDefaultAdapter(screen.get())).isInReaderMode()).isFalse();
    }

    // ==================================================== Status =====================================================

    /** A sensor started on the open screen shows there as the collector connects, without leaving the screen. */
    @Test
    public void onResume_sensorStartedWhileOpen_statusFollows() {
        // :: Setup
        grantLocation();
        Li3Store.forgetSensor();
        ActivityController<Li3Activity> screen = Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(screenText(screen.get())).contains(gs(R.string.li3_stage_no_sensor));

        // :: Act
        startSensor("Connecting");
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));

        // :: Verify
        String text = screenText(screen.get());
        assertThat(text).contains("Connecting");
        assertThat(text).doesNotContain(gs(R.string.li3_stage_no_sensor));
    }

    /** A screen that is left no longer follows the collector. */
    @Test
    public void onPause_sensorStartedAfterwards_statusKept() {
        // :: Setup
        grantLocation();
        Li3Store.forgetSensor();
        ActivityController<Li3Activity> screen = Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();
        screen.pause();

        // :: Act
        startSensor("Connecting");
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));

        // :: Verify
        assertThat(screenText(screen.get())).contains(gs(R.string.li3_stage_no_sensor));
    }

    // ================================================== Setup steps ==================================================

    /** Without location the screen names allowing it as the next step. */
    @Test
    public void onResume_noLocation_locationIsNextStep() {
        // :: Act
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        String text = screenText(screen);
        assertThat(text).contains(
                "○ 1. " + gs(R.string.li3_step_location) + " (" + gs(R.string.li3_setup_next) + ")");
        assertThat(text).contains("○ 5. " + gs(R.string.li3_step_readings));
    }

    /** Location allowed but turned off still leaves the location step open. */
    @Test
    public void onResume_locationOff_locationStepOpen() {
        // :: Setup
        grantLocation();
        setLocationEnabled(false);

        // :: Act
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(screenText(screen)).contains("○ 1. " + gs(R.string.li3_step_location));
    }

    /** With location allowed and no credential, the screen ticks location and names the download as next. */
    @Test
    public void onResume_noCredential_downloadIsNextStep() {
        // :: Setup
        grantLocation();

        // :: Act
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        String text = screenText(screen);
        assertThat(text).contains("✓ 1. " + gs(R.string.li3_step_location) + "\n");
        assertThat(text).contains(
                "○ 2. " + gs(R.string.li3_step_credential) + " (" + gs(R.string.li3_setup_next) + ")");
    }

    /** An account ID that is set shows as a ticked account step. */
    @Test
    public void onResume_accountIdSet_accountStepTicked() {
        // :: Setup
        grantLocation();
        Pref.setString(Li3Store.PREF_ACCOUNT_ID, "ABCDEF01-2345-6789-ABCD-EF0123456789");

        // :: Act
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(screenText(screen)).contains("✓ 3. " + gs(R.string.li3_step_account));
    }

    /** A sensor scanned while the screen is open ticks the scan step, without leaving the screen. */
    @Test
    public void onResume_sensorStartedWhileOpen_scanStepTicked() {
        // :: Setup
        grantLocation();
        ActivityController<Li3Activity> screen = Robolectric.buildActivity(Li3Activity.class).setup();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(screenText(screen.get())).contains("○ 4. " + gs(R.string.li3_step_scan));

        // :: Act
        startSensor("Connecting");
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));

        // :: Verify
        assertThat(screenText(screen.get())).contains("✓ 4. " + gs(R.string.li3_step_scan));
    }

    /** A reading stored moments ago ticks the last step. */
    @Test
    public void onResume_readingStored_readingsStepTicked() {
        // :: Setup
        grantLocation();
        Li3Service.Session session = startSensor("Streaming");

        // :: Act
        session.onReading(reading(STORED_LIFE_COUNT, 120));
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(screenText(screen)).contains("✓ 5. " + gs(R.string.li3_step_readings));
    }

    /** A reading that came but was refused, here one during warm-up, leaves the last step open: nothing is stored. */
    @Test
    public void onResume_readingRefused_readingsStepOpen() {
        // :: Setup
        grantLocation();
        Li3Service.Session session = startSensor("Streaming");

        // :: Act
        session.onReading(reading(1, 120));
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(screenText(screen)).contains("○ 5. " + gs(R.string.li3_step_readings));
    }

    /** A forgotten sensor's last reading does not tick the last step: no sensor is set up. */
    @Test
    public void onResume_readingThenForget_readingsStepOpen() {
        // :: Setup
        grantLocation();
        startSensor("Streaming").onReading(reading(STORED_LIFE_COUNT, 120));

        // :: Act
        Li3Store.forgetSensor();
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(screenText(screen)).contains("○ 5. " + gs(R.string.li3_step_readings));
    }

    // ============================================== Look up account ID ===============================================

    /** The button asks for the account email and a hidden password. */
    @Test
    public void lookupButton_opensLoginDialog() {
        // :: Setup
        Li3Activity screen = openScreen();

        // :: Act
        clickLookup(screen);

        // :: Verify
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(shadowOf(dialog).getMessage().toString()).isEqualTo(gs(R.string.li3_lookup_hint));
        List<EditText> fields = editTexts(dialog);
        assertThat(fields).hasSize(2);
        EditText password = fields.get(1);
        assertThat(password.getInputType())
                .isEqualTo(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        assertThat(password.getTransformationMethod()).isInstanceOf(PasswordTransformationMethod.class);
    }

    /** With an account ID already set, the button asks before replacing it. */
    @Test
    public void lookupButton_withExistingId_asksBeforeReplacing() {
        // :: Setup
        Pref.setString(Li3Store.PREF_ACCOUNT_ID, "existing");
        Li3Activity screen = openScreen();

        // :: Act
        clickLookup(screen);

        // :: Verify
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(shadowOf(dialog).getMessage().toString()).isEqualTo(gs(R.string.li3_lookup_replace));
        assertThat(editTexts(dialog)).isEmpty();
    }

    /** OK with a blank field sends nothing and says what is missing. */
    @Test
    public void lookupDialog_blankFields_sendsNothing() {
        // :: Setup
        AtomicInteger lookups = new AtomicInteger();
        Li3Activity.lookupFactory = () -> {
            lookups.incrementAndGet();
            return Li3AccountLookupTest.lookup(new FakeAccountServer());
        };
        Li3Activity screen = openScreen();
        clickLookup(screen);

        // :: Act
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(lookups.get()).isEqualTo(0);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(gs(R.string.li3_lookup_blank));
    }

    /** OK with an email and password fills the account ID in and says so. */
    @Test
    public void lookupDialog_ok_fillsAccountId() {
        // :: Setup
        Li3Activity.lookupFactory = () -> Li3AccountLookupTest.lookup(new FakeAccountServer().answer(200,
                "{\"status\":0,\"data\":{\"user\":{\"id\":\"ABCDEF01-2345-6789-ABCD-EF0123456789\"}}}"));
        Li3Activity screen = openScreen();
        clickLookup(screen);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        editTexts(dialog).get(0).setText("someone@example.com");
        editTexts(dialog).get(1).setText("pass-word");

        // :: Act
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        awaitToast(gs(R.string.li3_lookup_done));

        // :: Verify
        assertThat(Pref.getString(Li3Store.PREF_ACCOUNT_ID, "")).isEqualTo("abcdef01-2345-6789-abcd-ef0123456789");
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(gs(R.string.li3_lookup_done));
    }

    // ==================================================== helpers ====================================================

    /** The screen opened with location granted, the download offer dismissed. */
    private static Li3Activity openScreen() {
        grantLocation();
        Li3Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();
        return screen;
    }

    private static void clickLookup(Activity screen) {
        Button button = findButton(screen.getWindow().getDecorView(), gs(R.string.li3_lookup_account));
        assertThat(button).isNotNull();
        button.performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static Button findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) {
            return (Button) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButton(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<EditText> editTexts(AlertDialog dialog) {
        List<EditText> fields = new ArrayList<>();
        collectEditTexts(dialog.getWindow().getDecorView(), fields);
        return fields;
    }

    private static void collectEditTexts(View view, List<EditText> fields) {
        if (view instanceof EditText) {
            fields.add((EditText) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectEditTexts(group.getChildAt(i), fields);
            }
        }
    }

    /** Waits up to five seconds for the lookup's thread to post the given toast to the screen. */
    private static void awaitToast(String text) {
        long deadline = JoH.tsl() + 5_000;
        while (!text.equals(ShadowToast.getTextOfLatestToast()) && JoH.tsl() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.yield();
        }
    }

    /**
     * Stores a sensor as an NFC scan does, and has the collector report the given stage for it.
     *
     * @return the collector's session for the sensor
     */
    private static Li3Service.Session startSensor(String stage) {
        Li3SensorState state = new Li3SensorState("0RRC989AQ", "CC:22:DF:B8:F9:58", new byte[]{1, 2, 3, 4});
        state.setActivationMs(JoH.tsl() - STORED_LIFE_COUNT * MINUTE_IN_MS);
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(PATCH_INFO)));
        Li3Store.saveState(state);
        Li3Store.ensureSensor(state);
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(state));
        session.onStage(stage);
        return session;
    }

    /** A realtime reading at the given life count and value, with the quality flags a stored reading has. */
    private static RealtimeReading reading(int lifeCount, int mgdl) {
        byte[] b = new byte[RealtimeReading.SIZE];
        b[0] = (byte) lifeCount;
        b[1] = (byte) (lifeCount >> 8);
        b[14] = 3;
        b[15] = (byte) mgdl;
        b[16] = (byte) (mgdl >> 8);
        return RealtimeReading.parse(b);
    }

    /** The text of every view on the screen, one per line. */
    private static String screenText(Activity activity) {
        StringBuilder text = new StringBuilder();
        collectText(activity.getWindow().getDecorView(), text);
        return text.toString();
    }

    private static void collectText(View view, StringBuilder text) {
        if (view instanceof TextView) {
            text.append(((TextView) view).getText()).append('\n');
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectText(group.getChildAt(i), text);
            }
        }
    }

    /** The message of the newest dialog, whichever AlertDialog class built it. */
    private static String latestMessage() {
        TextView message = ShadowDialog.getLatestDialog().findViewById(android.R.id.message);
        return message.getText().toString();
    }

    /** NFC scanning turned on with the Li3 collector, on a phone with NFC switched on. */
    private static void enableNfcScan() {
        Pref.setBoolean("use_nfc_scan", true);
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
        ShadowNfcAdapter.setNfcHardwareExists(true);
        shadowOf(RuntimeEnvironment.getApplication().getPackageManager())
                .setSystemFeature(PackageManager.FEATURE_NFC, true);
        shadowOf(NfcAdapter.getDefaultAdapter(RuntimeEnvironment.getApplication())).setEnabled(true);
    }

    private static void grantLocation() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        setLocationEnabled(true);
    }

    private static void setLocationEnabled(boolean enabled) {
        LocationManager locationManager = (LocationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.LOCATION_SERVICE);
        shadowOf(locationManager).setLocationEnabled(enabled);
    }
}
