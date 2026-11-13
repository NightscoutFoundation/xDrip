package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.nfc.tech.NfcV;
import android.os.Looper;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.NFCFilterX;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm.SensorType;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import xrwd.li3.Li3SensorState;
import xrwd.li3.nfc.NfcCommand;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.pairing.ReceiverId;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link Li3Nfc}: which scans it takes from NFCReaderX, what it decides to send for a fresh or a running
 * sensor, the confirmed choice the second scan picks up, and what it keeps from the sensor's reply. Patch info and
 * replies are the RoundWhiteDiscKit 7dd6396 test vectors for serial 0RRC989AQ.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3NfcTest extends RobolectricTestWithConfig {

    private static final String FRESH = "00a50001000200010060541e020401040c0130525243393839415151ff17";
    private static final String ACTIVE = "00a50001000200010060541e020401040c04305252433938394151c6ca";
    private static final String ACTIVATED_REPLY = "00a50058f9b8df22cc3225ec7200000000ad06";
    private static final String SWITCH_REPLY = "00a50058f9b8df22cc02bafbbbfc2bee698e6e";
    private static final String SERIAL = "0RRC989AQ";
    private static final String ACCOUNT = "2977dec2-492a-11ea-9702-0242ac110002";
    private static final String UNIQUE_ID = "0123456789abcdef0123456789abcdef";
    private static final byte[] KAUTH = Hex.toBytes("00112233445566778899aabbccddeeff");
    private static final long NOW = 1_800_000_000_000L;

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        Li3Store.forgetSensor();
        Li3Nfc.clearPending(); // static, so a choice confirmed in one test would carry into the next
    }

    @After
    public void tearDown() {
        Pref.removeItem("use_nfc_scan");
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
        Pref.removeItem(Li3Store.PREF_ACCOUNT_ID);
        Li3Store.forgetSensor();
        Li3Nfc.clearPending();
    }

    // ==================================================== useNFC =====================================================

    /** With NFC scanning on, the Li3 collector listens for scans although it is not one of the older NFC collectors. */
    @Test
    public void useNFC_li3WithNfcScan_true() {
        // :: Setup
        Pref.setBoolean("use_nfc_scan", true);
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);

        // :: Act
        boolean used = NFCReaderX.useNFC();

        // :: Verify
        assertThat(used).isTrue();
    }

    /** With NFC scanning off, the Li3 collector does not listen for scans. */
    @Test
    public void useNFC_li3WithoutNfcScan_false() {
        // :: Setup
        Pref.setBoolean("use_nfc_scan", false);
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);

        // :: Act
        boolean used = NFCReaderX.useNFC();

        // :: Verify
        assertThat(used).isFalse();
    }

    // ==================================================== handles ====================================================

    /** A Li3 scanned while an older collector is selected stays with the existing NFC code. */
    @Test
    public void handles_li3UnderLibreReceiver_false() {
        // :: Act
        boolean handled = Li3Nfc.handles(SensorType.Libre3, DexCollectionType.LibreReceiver);

        // :: Verify
        assertThat(handled).isFalse();
    }

    /** An older sensor scanned while the Li3 collector is selected is not taken here. */
    @Test
    public void handles_olderSensorUnderLi3_false() {
        // :: Act
        boolean handled = Li3Nfc.handles(SensorType.Libre2, DexCollectionType.Li3);

        // :: Verify
        assertThat(handled).isFalse();
    }

    /** A Li3 scanned while the Li3 collector is selected is taken here. */
    @Test
    public void handles_li3UnderLi3_true() {
        // :: Act
        boolean handled = Li3Nfc.handles(SensorType.Libre3, DexCollectionType.Li3);

        // :: Verify
        assertThat(handled).isTrue();
    }

    // ==================================================== ignores ====================================================

    /** An older sensor scanned while the Li3 collector is selected is ignored, so its readings never reach it. */
    @Test
    public void ignores_olderSensorUnderLi3_true() {
        // :: Act
        boolean ignored = Li3Nfc.ignores(SensorType.Libre2, DexCollectionType.Li3);

        // :: Verify
        assertThat(ignored).isTrue();
    }

    /** A Li3 scanned while the Li3 collector is selected is not ignored. */
    @Test
    public void ignores_li3UnderLi3_false() {
        // :: Act
        boolean ignored = Li3Nfc.ignores(SensorType.Libre3, DexCollectionType.Li3);

        // :: Verify
        assertThat(ignored).isFalse();
    }

    /** Under an older collector the existing NFC code handles every sensor as before. */
    @Test
    public void ignores_libre2UnderLibreReceiver_false() {
        // :: Act
        boolean ignored = Li3Nfc.ignores(SensorType.Libre2, DexCollectionType.LibreReceiver);

        // :: Verify
        assertThat(ignored).isFalse();
    }

    // =============================================== unusedNfcSettings ===============================================

    /**
     * Under the Li3 collector the NFC settings screen hides what only the older NFC code reads. The filter length
     * stays: the Li3 readings are smoothed with it.
     */
    @Test
    public void unusedNfcSettings_li3_olderNfcOnly() {
        // :: Act
        List<String> unused = Li3Nfc.unusedNfcSettings(DexCollectionType.Li3);

        // :: Verify
        assertThat(unused).containsExactly("libre2_enable_bluetooth_streaming", "nfc_show_age", "nfc_expiry_days",
                "use_nfc_multiblock", "use_nfc_any_tag");
    }

    /** Under an older collector the screen shows everything, as before. */
    @Test
    public void unusedNfcSettings_libreReceiver_none() {
        // :: Act
        List<String> unused = Li3Nfc.unusedNfcSettings(DexCollectionType.LibreReceiver);

        // :: Verify
        assertThat(unused).isEmpty();
    }

    /** Every hidden key is on the NFC settings screen, so a renamed setting cannot slip back into view. */
    @Test
    public void unusedNfcSettings_li3_allOnScreen() throws Exception {
        // :: Act
        List<String> keys = preferenceKeys(R.xml.pref_data_source);

        // :: Verify
        assertThat(keys).containsAtLeastElementsIn(Li3Nfc.unusedNfcSettings(DexCollectionType.Li3));
    }

    // ==================================================== decide =====================================================

    /** A fresh sensor with an account is started with the account's receiver ID, and the user is warned first. */
    @Test
    public void decide_freshWithAccount_activatesWithAccountId() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(FRESH), null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.error).isNull();
        assertThat(decision.code).isEqualTo(NfcCommand.Code.ACTIVATE);
        assertThat(decision.receiverId)
                .isEqualTo(ReceiverId.fromAccount(ACCOUNT, ReceiverId.Fold.LI3_APP));
        assertThat(decision.warning).contains("The Li3 app will not get its readings");
    }

    /** A fresh sensor without an account gets the per-install ID, with the warning that it can never move back. */
    @Test
    public void decide_freshWithoutAccount_activatesAccountless() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(FRESH), null, " ",
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.error).isNull();
        assertThat(decision.code).isEqualTo(NfcCommand.Code.ACTIVATE);
        assertThat(decision.receiverId).isEqualTo(ReceiverId.accountless(UNIQUE_ID));
        assertThat(decision.warning).contains("can never move to an official app");
        assertThat(decision.warning).contains("lost if xDrip is reinstalled");
    }

    /** A running sensor is taken over with the receiver ID the chosen app's fold gives for the account. */
    @Test
    public void decide_activeWithAccount_switchesWithChosenFold() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(ACTIVE), null, ACCOUNT,
                ReceiverId.Fold.MULTI_SENSOR_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.error).isNull();
        assertThat(decision.code).isEqualTo(NfcCommand.Code.SWITCH_RECEIVER);
        assertThat(decision.receiverId)
                .isEqualTo(ReceiverId.fromAccount(ACCOUNT, ReceiverId.Fold.MULTI_SENSOR_APP));
        assertThat(decision.warning).contains("over from the Multi-sensor app");
    }

    /**
     * Without an account a running sensor is taken over with this install's ID, so a sensor this install started can
     * be picked up again after a lost reply or "Forget sensor". The user is warned first.
     */
    @Test
    public void decide_activeWithoutAccount_takesOverWithInstallId() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(ACTIVE), null, "",
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.error).isNull();
        assertThat(decision.code).isEqualTo(NfcCommand.Code.SWITCH_RECEIVER);
        assertThat(decision.receiverId).isEqualTo(ReceiverId.accountless(UNIQUE_ID));
        assertThat(decision.account).isEmpty();
        assertThat(decision.warning).contains("started by this xDrip install");
    }

    /** The running sensor xDrip already uses is not taken over again: that would only reset its pairing. */
    @Test
    public void decide_runningSensorInUse_refusedAsAlreadyInUse() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(ACTIVE), stored(SERIAL), ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.code).isNull();
        assertThat(decision.error).isEqualTo(gs(R.string.li3_already_in_use));
    }

    /** A running sensor other than the one xDrip uses is still taken over. */
    @Test
    public void decide_runningSensorOtherInUse_takesOver() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(ACTIVE), stored("0AAA111BB"), ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.error).isNull();
        assertThat(decision.code).isEqualTo(NfcCommand.Code.SWITCH_RECEIVER);
    }

    /** A sensor that has never run is started, even when xDrip has a sensor stored under its serial. */
    @Test
    public void decide_freshSensorWithStoredSerial_activates() {
        // :: Act
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(FRESH), stored(SERIAL), ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Verify
        assertThat(decision.error).isNull();
        assertThat(decision.code).isEqualTo(NfcCommand.Code.ACTIVATE);
    }

    // ==================================================== onScan =====================================================

    /** Scanning the sensor in use shows that it is in use, asks nothing, and leaves its pairing as it was. */
    @Test
    public void onScan_sensorInUse_showsAlreadyInUseAndKeepsPairing() {
        // :: Setup
        Li3SensorState state = stored(SERIAL);
        state.setKAuth(KAUTH);
        Li3Store.saveState(state);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        // :: Act
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE)); // nothing is sent, so no tag is needed
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).hasSize(1);
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertThat(String.valueOf(shadowOf(dialog).getMessage())).isEqualTo(gs(R.string.li3_already_in_use));
        assertThat(Li3Store.loadState().kAuth()).isEqualTo(KAUTH);
    }

    /** A message for a screen that has closed comes as a toast, since a dialog there would crash xDrip. */
    @Test
    public void onScan_screenClosed_messageAsToast() {
        // :: Setup
        Li3Store.saveState(stored(SERIAL));
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.finish();

        // :: Act
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).isEmpty();
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(gs(R.string.li3_already_in_use));
    }

    /** The takeover question is not shown on a screen that has closed, and nothing is confirmed. */
    @Test
    public void onScan_screenClosedBeforeQuestion_nothingConfirmed() {
        // :: Setup
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.finish();

        // :: Act
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).isEmpty();
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNull();
    }

    /**
     * A scan made when not in xDrip asks nothing there, since the scanning screen covers it and it closes by itself;
     * the Li3 screen opens instead, to scan the sensor again on, and nothing is confirmed.
     */
    @Test
    public void onScan_notInXdrip_opensLi3Screen() {
        // :: Setup
        Activity filter = Robolectric.buildActivity(NFCFilterX.class).create().get(); // resuming it reads its intent

        // :: Act
        Li3Nfc.onScan(filter, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).isEmpty();
        Intent opened = shadowOf(filter).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(Li3Activity.class.getName());
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(gs(R.string.li3_scan_on_li3_screen));
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNull();
    }

    /**
     * The screen a scan made when not in xDrip comes on closes a second after the scanning screen covers it, which a
     * slow read outlasts; the Li3 screen still opens, as a new task since the screen it came from is gone.
     */
    @Test
    public void onScan_notInXdripScreenClosing_stillOpensLi3Screen() {
        // :: Setup
        Activity filter = Robolectric.buildActivity(NFCFilterX.class).create().get(); // resuming it reads its intent
        filter.finish();

        // :: Act
        Li3Nfc.onScan(filter, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        Intent opened = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(Li3Activity.class.getName());
        assertThat(opened.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(gs(R.string.li3_scan_on_li3_screen));
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNull();
    }

    /**
     * A Li3 screen that is already open is brought back for a scan made when not in xDrip, the way the status page
     * opens it, so no second Li3 screen comes on top of it.
     */
    @Test
    public void onScan_notInXdrip_reusesOpenLi3Screen() {
        // :: Setup
        Activity filter = Robolectric.buildActivity(NFCFilterX.class).create().get(); // resuming it reads its intent
        filter.finish();
        final int reuse = Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP;

        // :: Act
        Li3Nfc.onScan(filter, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        Intent opened = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(Li3Activity.class.getName());
        assertThat(opened.getFlags() & reuse).isEqualTo(reuse);
    }

    /** A message for a scan made when not in xDrip comes as a toast, since the scanning screen would hide a dialog. */
    @Test
    public void onScan_notInXdripSensorInUse_messageAsToast() {
        // :: Setup
        Li3Store.saveState(stored(SERIAL));
        Activity filter = Robolectric.buildActivity(NFCFilterX.class).create().get(); // resuming it reads its intent

        // :: Act
        Li3Nfc.onScan(filter, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowDialog.getShownDialogs()).isEmpty();
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(gs(R.string.li3_already_in_use));
    }

    /** Lost contact while sending keeps the confirmed choice, so the next scan sends it without asking again. */
    @Test
    public void onScan_contactLost_keepsConfirmedChoice() throws Exception {
        // :: Setup
        Li3Nfc.confirm(SERIAL, Li3Nfc.decide(patchInfo(ACTIVE), null, Li3Store.accountId(), Li3Store.fold(),
                Li3Store.uniqueId()), tsl());
        NfcV tag = mock(NfcV.class);
        when(tag.transceive(any(byte[].class))).thenThrow(new IOException("Tag was lost."));
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        // :: Act
        Li3Nfc.onScan(activity, tag, Hex.toBytes(ACTIVE));

        // :: Verify
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNotNull();
        assertThat(Li3Store.loadState()).isNull();
    }

    // =============================================== Takeover question ===============================================

    /**
     * Taking over a running sensor without an account ID works only for a sensor that no official app reads, so the
     * question offers to look the ID up first, and scanning anyway is the second choice.
     */
    @Test
    public void onScan_takeoverWithoutAccount_offersLookupFirst() {
        // :: Setup
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        // :: Act
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString())
                .isEqualTo(gs(R.string.li3_lookup_account));
        assertThat(dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString())
                .isEqualTo(gs(R.string.li3_scan_anyway));
        assertThat(String.valueOf(shadowOf(dialog).getMessage()))
                .isEqualTo(gs(R.string.li3_warn_takeover_accountless) + "\n\n" + gs(R.string.li3_scan_anyway_again));
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNull();
    }

    /** Scan anyway confirms the takeover without an account, so the next scan sends it. */
    @Test
    public void onScan_takeoverWithoutAccountScanAnyway_confirms() {
        // :: Setup
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Act
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        Li3Nfc.Decision pending = Li3Nfc.pending(SERIAL, tsl());
        assertThat(pending).isNotNull();
        assertThat(pending.code).isEqualTo(NfcCommand.Code.SWITCH_RECEIVER);
        assertThat(pending.account).isEmpty();
    }

    /** Looking the ID up from a screen other than the Li3 screen opens the Li3 screen, and confirms nothing. */
    @Test
    public void onScan_takeoverWithoutAccountLookup_opensLi3Screen() {
        // :: Setup
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Act
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        Intent opened = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(Li3Activity.class.getName());
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNull();
    }

    /** Looking the ID up on the Li3 screen asks for the account login there, and confirms nothing. */
    @Test
    public void onScan_takeoverWithoutAccountLookupOnLi3Screen_asksLogin() {
        // :: Setup
        Activity screen = Robolectric.buildActivity(Li3Activity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();
        Li3Nfc.onScan(screen, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();

        // :: Act
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        AlertDialog login = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(login.isShowing()).isTrue();
        assertThat(String.valueOf(shadowOf(login).getMessage())).isEqualTo(gs(R.string.li3_lookup_hint));
        assertThat(Li3Nfc.pending(SERIAL, tsl())).isNull();
    }

    /** With an account ID set, the takeover question keeps OK, which confirms the takeover with that account. */
    @Test
    public void onScan_takeoverWithAccount_okConfirms() {
        // :: Setup
        Pref.setString(Li3Store.PREF_ACCOUNT_ID, ACCOUNT);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Li3Nfc.onScan(activity, null, Hex.toBytes(ACTIVE));
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()).isEqualTo(gs(R.string.ok));

        // :: Act
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        Li3Nfc.Decision pending = Li3Nfc.pending(SERIAL, tsl());
        assertThat(pending).isNotNull();
        assertThat(pending.account).isEqualTo(ACCOUNT);
    }

    /** Starting a sensor without an account ID needs nothing more, so its question keeps OK. */
    @Test
    public void onScan_freshWithoutAccount_okConfirms() {
        // :: Setup
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Li3Nfc.onScan(activity, null, Hex.toBytes(FRESH));
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()).isEqualTo(gs(R.string.ok));

        // :: Act
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(Li3Nfc.pending(SERIAL, tsl()).code).isEqualTo(NfcCommand.Code.ACTIVATE);
    }

    // ==================================================== pending ====================================================

    /** The second scan of the same sensor within five minutes picks up the choice confirmed after the first. */
    @Test
    public void pending_sameSerialWithinFiveMinutes_returnsDecision() {
        // :: Setup
        Li3Nfc.Decision decision = Li3Nfc.decide(patchInfo(ACTIVE), null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);
        Li3Nfc.confirm(SERIAL, decision, NOW);

        // :: Act
        Li3Nfc.Decision pending = Li3Nfc.pending(SERIAL, NOW + 4 * 60_000);

        // :: Verify
        assertThat(pending).isSameInstanceAs(decision);
    }

    /** A different sensor never picks up a choice confirmed for another one. */
    @Test
    public void pending_otherSerial_returnsNull() {
        // :: Setup
        Li3Nfc.confirm(SERIAL, Li3Nfc.decide(patchInfo(ACTIVE), null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID), NOW);

        // :: Act
        Li3Nfc.Decision pending = Li3Nfc.pending("0RRC989AX", NOW + 60_000);

        // :: Verify
        assertThat(pending).isNull();
    }

    /** A choice older than five minutes has lapsed, so the scan starts over with a warning. */
    @Test
    public void pending_afterFiveMinutes_returnsNull() {
        // :: Setup
        Li3Nfc.confirm(SERIAL, Li3Nfc.decide(patchInfo(ACTIVE), null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID), NOW);

        // :: Act
        Li3Nfc.Decision pending = Li3Nfc.pending(SERIAL, NOW + 5 * 60_000 + 1);

        // :: Verify
        assertThat(pending).isNull();
    }

    // ================================================== onResponse ===================================================

    /** A reply that is not an activation reply names the app and account tried, and stores no sensor. */
    @Test
    public void onResponse_unparseableReply_errorNamesFoldAndAccount() {
        // :: Setup
        PatchInfo info = patchInfo(ACTIVE);
        Li3Nfc.Decision decision = Li3Nfc.decide(info, null, ACCOUNT, ReceiverId.Fold.MULTI_SENSOR_APP, UNIQUE_ID);

        // :: Act
        String error = Li3Nfc.onResponse(info, decision, new byte[]{0x00, 0x01}, NOW);

        // :: Verify
        assertThat(error).contains("Multi-sensor app");
        assertThat(error).contains(ACCOUNT);
        assertThat(Li3Store.loadState()).isNull();
    }

    /** The refused command is logged without the account ID, since the log is shared in bug reports. */
    @Test
    public void onResponse_unparseableReply_logsWithoutAccount() {
        // :: Setup
        PatchInfo info = patchInfo(ACTIVE);
        Li3Nfc.Decision decision = Li3Nfc.decide(info, null, ACCOUNT, ReceiverId.Fold.MULTI_SENSOR_APP, UNIQUE_ID);

        // :: Act
        Li3Nfc.onResponse(info, decision, new byte[]{0x00, 0x01}, NOW);

        // :: Verify
        List<UserError> rows = new Select().from(UserError.class).where("shortError = ?", "Li3").execute();
        assertThat(rows).isNotEmpty();
        for (UserError row : rows) {
            assertThat(row.message).doesNotContain(ACCOUNT);
        }
        assertThat(rows.get(rows.size() - 1).message).contains("Multi-sensor app");
    }

    /** A takeover keeps the address, PIN, receiver ID, fold, cycle and the start time the sensor reports. */
    @Test
    public void onResponse_switchReply_storesSensorWithReportedStart() {
        // :: Setup
        PatchInfo info = patchInfo(ACTIVE);
        Li3Nfc.Decision decision = Li3Nfc.decide(info, null, ACCOUNT, ReceiverId.Fold.MULTI_SENSOR_APP, UNIQUE_ID);

        // :: Act
        String error = Li3Nfc.onResponse(info, decision, Hex.toBytes(SWITCH_REPLY), NOW);

        // :: Verify
        assertThat(error).isNull();
        Li3SensorState state = Li3Store.loadState();
        assertThat(state.serial()).isEqualTo(SERIAL);
        assertThat(state.bleAddress()).isEqualTo("CC:22:DF:B8:F9:58");
        assertThat(state.receiverId()).isEqualTo(decision.receiverId);
        assertThat(state.fold()).isEqualTo(ReceiverId.Fold.MULTI_SENSOR_APP);
        assertThat(state.activationMs()).isEqualTo(0x69ee2bfcL * 1000);
        assertThat(state.wearMinutes()).isEqualTo(21600);
        assertThat(state.region()).isEqualTo(PatchInfo.Region.US);
    }

    /** A fresh start reports no start time, so the time the command was sent is kept as the start. */
    @Test
    public void onResponse_activateReply_storesCommandTimeAsStart() {
        // :: Setup
        PatchInfo info = patchInfo(FRESH);
        Li3Nfc.Decision decision = Li3Nfc.decide(info, null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);

        // :: Act
        String error = Li3Nfc.onResponse(info, decision, Hex.toBytes(ACTIVATED_REPLY), NOW);

        // :: Verify
        assertThat(error).isNull();
        assertThat(Li3Store.loadState().activationMs()).isEqualTo(NOW);
    }

    /** Scanning the same started sensor again with a reply that reports no start time keeps the stored start. */
    @Test
    public void onResponse_sameSerialAgain_keepsFirstStart() {
        // :: Setup
        PatchInfo info = patchInfo(FRESH);
        Li3Nfc.Decision decision = Li3Nfc.decide(info, null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);
        Li3Nfc.onResponse(info, decision, Hex.toBytes(ACTIVATED_REPLY), NOW);

        // :: Act
        Li3Nfc.onResponse(info, decision, Hex.toBytes(ACTIVATED_REPLY), NOW + 3_600_000);

        // :: Verify
        assertThat(Li3Store.loadState().activationMs()).isEqualTo(NOW);
    }

    /**
     * Scanning the same sensor again when the reply reports a start time takes that time, so a wrong start time
     * stored before can be put right by a new scan.
     */
    @Test
    public void onResponse_sameSerialReportedStart_takesReportedStart() {
        // :: Setup
        PatchInfo info = patchInfo(FRESH);
        Li3Nfc.Decision decision = Li3Nfc.decide(info, null, ACCOUNT,
                ReceiverId.Fold.LI3_APP, UNIQUE_ID);
        Li3Nfc.onResponse(info, decision, Hex.toBytes(ACTIVATED_REPLY), NOW);

        // :: Act
        Li3Nfc.onResponse(info, decision, Hex.toBytes(SWITCH_REPLY), NOW + 3_600_000);

        // :: Verify
        assertThat(Li3Store.loadState().activationMs()).isEqualTo(0x69ee2bfcL * 1000);
    }

    /** A stored sensor finishes the setup, so the setup notification goes away. */
    @Test
    public void onResponse_stored_cancelsSetupNotification() {
        // :: Setup
        JoH.showNotification("Li3", "setup", null, Constants.LI3_CREDENTIAL_NOTIFICATION_ID, false, false, false);
        NotificationManager manager = (NotificationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        assertThat(shadowOf(manager).getNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID)).isNotNull();
        PatchInfo info = patchInfo(FRESH);

        // :: Act
        Li3Nfc.onResponse(info, Li3Nfc.decide(info, null, ACCOUNT, ReceiverId.Fold.LI3_APP, UNIQUE_ID),
                Hex.toBytes(ACTIVATED_REPLY), NOW);

        // :: Verify
        assertThat(shadowOf(manager).getNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID)).isNull();
    }

    /** A reply that is not understood stores nothing, so the setup notification stays. */
    @Test
    public void onResponse_unparseableReply_keepsSetupNotification() {
        // :: Setup
        JoH.showNotification("Li3", "setup", null, Constants.LI3_CREDENTIAL_NOTIFICATION_ID, false, false, false);
        NotificationManager manager = (NotificationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        PatchInfo info = patchInfo(FRESH);

        // :: Act
        Li3Nfc.onResponse(info, Li3Nfc.decide(info, null, ACCOUNT, ReceiverId.Fold.LI3_APP, UNIQUE_ID),
                new byte[0], NOW);

        // :: Verify
        assertThat(shadowOf(manager).getNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID)).isNotNull();
    }

    private static PatchInfo patchInfo(String hex) {
        return PatchInfo.parse(Hex.toBytes(hex));
    }

    private static Li3SensorState stored(String serial) {
        return new Li3SensorState(serial, "CC:22:DF:B8:F9:58", new byte[]{0x32, 0x25, (byte) 0xec, 0x72});
    }

    /** The android:key of every preference in the XML resource. */
    private static List<String> preferenceKeys(int xml) throws Exception {
        List<String> keys = new ArrayList<>();
        XmlResourceParser parser = RuntimeEnvironment.getApplication().getResources().getXml(xml);
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                String key = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }
}
