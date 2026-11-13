package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.content.Context;
import android.nfc.NfcAdapter;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

/**
 * The setup steps the Li3 screen lists, in the order they are done, each ticked when done. The first step left is
 * marked next. The account step is only needed to take over a sensor started by an official app, so it is never
 * marked next.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
final class Li3Setup {

    private static final String DONE = "✓ ";
    private static final String OPEN = "○ ";
    /** NFCReaderX's setting for NFC scanning in xDrip. */
    static final String NFC_SCAN_PREF = "use_nfc_scan";

    private Li3Setup() {
    }

    /** The steps as they stand now on this phone. */
    static String text(Context context) {
        boolean sensor = Li3Store.loadState() != null;
        return text(LocationHelper.isLocationPermissionOk(context) && LocationHelper.isLocationEnabled(context),
                !Li3Credential.get(context).load().isEmpty(),
                !Li3Store.accountId().isEmpty(),
                sensor,
                // readings from a forgotten sensor stay recent for a while, and are not this setup's; a refused
                // reading, as in warm-up, is not a reading yet
                sensor && Li3Service.hasRecentReading());
    }

    /** The steps, given which of them are done. */
    static String text(boolean location, boolean credential, boolean accountId, boolean sensor, boolean readings) {
        StringBuilder sb = new StringBuilder(gs(R.string.li3_setup));
        boolean nextShown = false;
        nextShown |= step(sb, 1, R.string.li3_step_location, location, !nextShown);
        nextShown |= step(sb, 2, R.string.li3_step_credential, credential, !nextShown);
        step(sb, 3, R.string.li3_step_account, accountId, false);
        nextShown |= step(sb, 4, R.string.li3_step_scan, sensor, !nextShown);
        step(sb, 5, R.string.li3_step_readings, readings, !nextShown);
        return sb.toString();
    }

    /**
     * Adds one step line.
     *
     * @return true when the line was marked next
     */
    private static boolean step(StringBuilder sb, int number, int text, boolean done, boolean mayBeNext) {
        sb.append('\n').append(done ? DONE : OPEN).append(number).append(". ").append(gs(text));
        if (!done && mayBeNext) {
            sb.append(" (").append(gs(R.string.li3_setup_next)).append(')');
            return true;
        }
        return false;
    }

    /** What keeps NFC scanning from working, the first of them, if anything. */
    enum NfcGap {
        NONE,
        /** NFC scanning is off in xDrip, its default. */
        SCAN_OFF,
        /** NFC is switched off on the phone. */
        PHONE_OFF,
        /** The phone has no NFC. */
        MISSING
    }

    /** What keeps a scan of the sensor from reaching xDrip on this phone. */
    static NfcGap nfcGap(Context context) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
        if (adapter == null) {
            return NfcGap.MISSING;
        }
        if (!Pref.getBooleanDefaultFalse(NFC_SCAN_PREF)) {
            return NfcGap.SCAN_OFF;
        }
        return adapter.isEnabled() ? NfcGap.NONE : NfcGap.PHONE_OFF;
    }

    /** Setup is finished with a credential to pair with and a sensor to connect to. */
    static boolean finished(boolean credential, boolean sensor) {
        return credential && sensor;
    }
}
