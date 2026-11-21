package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import xrwd.li3.Li3SensorState;

/**
 * The report a tester copies into a bug report: the phone's Bluetooth and location state, what the collector shows,
 * the sensor's facts, why the session retried, and the collector's recent log. It never holds the BLE PIN, the paired
 * key, the app credential or the account ID.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class Li3Diagnostics {

    private static final String TAG = "Li3";
    private static final String LABEL = "Li3 diagnostics";
    /** About 40 kB, under the 65 kB a GitHub comment holds. */
    private static final int LOG_LINES = 400;
    private static final long LOG_SPAN_MS = 24 * HOUR_IN_MS;
    private static final String ACCOUNT_MASK = "<account ID>";
    /** The report is read by a developer, so its own words stay English on a translated phone. */
    private static final String NONE = "none";
    private static final String UNKNOWN = "unknown";

    private Li3Diagnostics() {
    }

    public static String text(Context context) {
        final StringBuilder sb = new StringBuilder();
        line(sb, LABEL + ", " + dateTimeText(tsl()));
        line(sb, "xDrip " + BuildConfig.VERSION_NAME);
        line(sb, "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), " + Build.MANUFACTURER
                + " " + Build.MODEL);
        line(sb, "Collector: " + DexCollectionType.getDexCollectionType() + ", one-minute "
                + onOff(Pref.getBooleanDefaultFalse("libre_one_minute")));
        line(sb, "Bluetooth: " + bluetooth());
        line(sb, "Location permission: " + yesNo(hasLocationPermission(context)));
        line(sb, "Location services: " + onOff(LocationHelper.isLocationEnabled(context)));
        line(sb, "Battery optimisation: " + batteryOptimisation(context));
        sb.append('\n');

        for (StatusItem item : Li3Service.megaStatus()) {
            if (item.runnable == null) {
                line(sb, item.name + ": " + item.value);
            }
        }
        final Li3SensorState state = Li3Store.loadState();
        if (state != null) {
            line(sb, "Sensor details: product type " + orUnknown(state.productType()) + ", generation "
                    + orUnknown(state.generation()) + ", region " + orUnknown(state.region()) + ", warm-up "
                    + minutes(state.warmupMinutes()) + ", wear " + minutes(state.wearMinutes()) + ", identity "
                    + (state.identityLabel() == null ? NONE : state.identityLabel())
                    + ", paired key " + yesNo(state.kAuth() != null) + ", last life count "
                    + orUnknown(state.lastLifeCount()) + ", history owed " + historyOwed(state));
        }
        sb.append('\n');

        final Li3Service.Session session = Li3Service.currentSession();
        if (session != null) {
            final StringBuilder retries = new StringBuilder();
            for (Map.Entry<String, Integer> retry : session.retryCounts().entrySet()) {
                retries.append(retries.length() == 0 ? "" : ", ").append(retry.getKey()).append(" x")
                        .append(retry.getValue());
            }
            line(sb, "Retries since " + dateTimeText(session.startedAt()) + ": "
                    + (retries.length() == 0 ? NONE : retries.toString()));
            line(sb, "Recent stages:");
            for (String stage : session.recentStages()) {
                line(sb, stage);
            }
            sb.append('\n');
        }

        line(sb, "Recent log:");
        for (UserError row : recentLog()) {
            line(sb, dateTimeText(row.timestamp) + " " + row.message);
        }
        return withoutAccountId(sb.toString());
    }

    public static void copyToClipboard(Context context) {
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(LABEL, text(context)));
        JoH.static_toast_long(gs(R.string.li3_diagnostics_copied));
    }

    /** @return the collector's newest log rows from the last day, oldest first */
    private static List<UserError> recentLog() {
        final List<UserError> rows = new Select()
                .from(UserError.class)
                .where("shortError = ? and timestamp > ?", TAG, tsl() - LOG_SPAN_MS)
                .orderBy("timestamp desc")
                .limit(LOG_LINES)
                .execute();
        Collections.reverse(rows);
        return rows;
    }

    /** The NFC error for a refused command names the account ID, and that line is in the log. */
    private static String withoutAccountId(String text) {
        final String accountId = Li3Store.accountId();
        return accountId.isEmpty() ? text : text.replace(accountId, ACCOUNT_MASK);
    }

    private static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static String bluetooth() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter == null ? "none" : onOff(adapter.isEnabled());
    }

    private static String batteryOptimisation(Context context) {
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName()) ? "ignored" : "active";
    }

    /** A fact the sensor has not told us yet. */
    private static String orUnknown(Object value) {
        return value == null ? UNKNOWN : value.toString();
    }

    /**
     * The minutes the sensor still owes history for, and the life count its last ask went out at, so a report says
     * whether a hole is still being chased or was given up on.
     */
    private static String historyOwed(Li3SensorState state) {
        return state.backfillFrom() == 0 ? NONE : state.backfillFrom() + " to " + state.backfillTo()
                + ", asked at " + state.backfillAskedAt();
    }

    private static String minutes(Integer value) {
        return value == null ? UNKNOWN : value + " min";
    }

    private static String onOff(boolean on) {
        return on ? "on" : "off";
    }

    private static String yesNo(boolean yes) {
        return yes ? "yes" : "no";
    }

    private static void line(StringBuilder sb, String text) {
        sb.append(text).append('\n');
    }
}
