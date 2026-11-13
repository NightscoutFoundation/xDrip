package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.google.common.truth.Truth.assertThat;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Base64;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import xrwd.li3.Li3SensorState;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link Li3Diagnostics}: the report a tester pastes into a bug report says what the collector saw, and
 * never carries the sensor's secrets or the account ID.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class Li3DiagnosticsTest extends RobolectricTestWithConfig {

    private static final String PATCH_INFO = "00a50001000200010060541e020401040c0130525243393839415151ff17";
    private static final byte[] PIN = {0x12, 0x34, 0x56, 0x78};
    private static final String ACCOUNT_ID = "acct-secret-1";

    private Context context;

    @Override
    public void setUp() {
        super.setUp();
        context = RuntimeEnvironment.getApplication();
        xdrip.setContextAlways(context);
        Li3Store.forgetSensor();
        // log rows from other test classes stay in the shared database
        new Delete().from(UserError.class).execute();
    }

    @After
    public void tearDown() {
        Li3Store.forgetSensor();
        Pref.removeItem(Li3Store.PREF_ACCOUNT_ID);
    }

    // ===================================================== text ======================================================

    /** Without a stored sensor the report says so, next to the app and phone facts. */
    @Test
    public void text_noSensor_saysNone() {
        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("Sensor: none");
        assertThat(text).contains("xDrip ");
        assertThat(text).contains("Location permission: no");
    }

    /** With a stored sensor the report names it and says whether it is paired. */
    @Test
    public void text_storedSensor_namesSensorFacts() {
        // :: Setup
        Li3SensorState state = sensor();
        Li3Store.saveState(state);

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains(state.serial());
        assertThat(text).contains("paired key no");
    }

    /** A sensor whose facts were never read shows them as unknown, not as "null". */
    @Test
    public void text_sensorWithoutFacts_saysUnknown() {
        // :: Setup
        Li3Store.saveState(new Li3SensorState("0RRC989AQ", "CC:22:DF:B8:F9:58", PIN));

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("product type unknown, generation unknown, region unknown, warm-up unknown, "
                + "wear unknown, identity none");
        assertThat(text).contains("last life count unknown");
        assertThat(text).doesNotContain("null");
    }

    /** A sensor with no history hole says it is owed none. */
    @Test
    public void text_noHole_saysHistoryOwedNone() {
        // :: Setup
        Li3Store.saveState(sensor());

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("history owed none");
    }

    /** An open history hole is shown with its life counts and the life count its last ask went out at. */
    @Test
    public void text_openHole_namesHoleAndAsk() {
        // :: Setup
        Li3SensorState state = sensor();
        state.openHole(1061, 1070);
        state.markHoleAskedAt(1073);
        Li3Store.saveState(state);

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("history owed 1061 to 1070, asked at 1073");
    }

    /** A paired sensor's PIN and key, and the account ID, are not in the report in any common spelling. */
    @Test
    public void text_pairedSensor_holdsNoSecrets() {
        // :: Setup
        byte[] kAuth = new byte[16];
        for (int i = 0; i < kAuth.length; i++) {
            kAuth[i] = (byte) (0xA0 + i);
        }
        Li3SensorState state = sensor();
        state.setKAuth(kAuth);
        Li3Store.saveState(state);
        Pref.setString(Li3Store.PREF_ACCOUNT_ID, ACCOUNT_ID);

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("paired key yes");
        for (byte[] secret : new byte[][]{PIN, kAuth}) {
            String hex = Hex.of(secret);
            assertThat(text).doesNotContain(hex.toUpperCase(Locale.ROOT));
            assertThat(text).doesNotContain(hex.toLowerCase(Locale.ROOT));
            assertThat(text).doesNotContain(Base64.encodeToString(secret, Base64.NO_WRAP));
        }
        assertThat(text).doesNotContain(ACCOUNT_ID);
    }

    /** The log part holds the collector's lines only. */
    @Test
    public void text_recentLog_onlyLi3Lines() {
        // :: Setup
        UserError.Log.uel("Li3", "paired line");
        UserError.Log.uel("OtherTag", "other line");

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("paired line");
        assertThat(text).doesNotContain("other line");
    }

    /** Lines older than a day are left out, so an old problem does not crowd out the one being reported. */
    @Test
    public void text_logLineOlderThanADay_leftOut() {
        // :: Setup
        UserError old = new UserError(3, "Li3", "yesterday's line");
        old.timestamp = tsl() - 25 * HOUR_IN_MS;
        old.save();
        UserError.Log.uel("Li3", "today's line");

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("today's line");
        assertThat(text).doesNotContain("yesterday's line");
    }

    /** A log line that names the account ID, as the NFC error for a refused command does, is shown without it. */
    @Test
    public void text_logLineWithAccountId_isMasked() {
        // :: Setup
        Pref.setString(Li3Store.PREF_ACCOUNT_ID, ACCOUNT_ID);
        UserError.Log.uel("Li3", "Check the account ID (" + ACCOUNT_ID + ")");

        // :: Act
        String text = Li3Diagnostics.text(context);

        // :: Verify
        assertThat(text).contains("Check the account ID (<account ID>)");
        assertThat(text).doesNotContain(ACCOUNT_ID);
    }

    // ================================================ copyToClipboard ================================================

    /** The copy button puts the report on the clipboard. */
    @Test
    public void copyToClipboard_putsTextOnClipboard() {
        // :: Act
        Li3Diagnostics.copyToClipboard(context);

        // :: Verify
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        assertThat(clipboard.getPrimaryClip().getItemAt(0).getText().toString()).startsWith("Li3 diagnostics");
    }

    // ==================================================== helpers ====================================================

    private static Li3SensorState sensor() {
        Li3SensorState state = new Li3SensorState("0RRC989AQ", "CC:22:DF:B8:F9:58", PIN);
        state.setActivationMs(tsl());
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(PATCH_INFO)));
        return state;
    }
}
