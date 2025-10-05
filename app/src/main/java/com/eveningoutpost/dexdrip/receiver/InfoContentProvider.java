package com.eveningoutpost.dexdrip.receiver;

import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;
import static com.eveningoutpost.dexdrip.utilitymodels.NanoStatus.nanoStatus;
import static com.eveningoutpost.dexdrip.utilitymodels.Pref.getBooleanDefaultFalse;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry.isNative;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.eassist.EmergencyAssist;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.BgSparklineBuilder;

import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.BlobCache;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.ByteArrayOutputStream;

/**
 * xDrip content provider
 * JamOrHam
 */
public class InfoContentProvider extends ContentProvider {

    private static final String TAG = "jamorham-content";
    private static final BlobCache dgCache = new BlobCache(60_000);
    private static final BlobCache graphCache = new BlobCache(60_000);

    {
        xdrip.setContext(getContext());
    }

    @Override
    public boolean onCreate() {
        xdrip.setContext(getContext());
        return enabled();
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (!enabled()) return null;
        try {
            if (selection == null) return null;

            switch (selection) {

                case "ping":
                    return matrixRow(
                            "version", BuildConfig.VERSION_NAME,
                            "versioncode", BuildConfig.buildVersion);

                case "alarms":
                    if (selectionArgs.length > 0 && selectionArgs[0].equals("snooze")) {
                        AlertPlayer.getPlayer().OpportunisticSnooze();
                        Log.d(TAG, "Opportunistic snooze");
                    }
                    break;

                case "eassist":
                    if (enabledWrite()) {
                        EmergencyAssist.test(EmergencyAssist.Reason.REQUESTED_ASSISTANCE, Constants.MINUTE_IN_MS);
                        UserError.Log.ueh(TAG, "Emergency assist triggered remotely");
                    }
                    break;

                case "bg": {
                    BestGlucose.DisplayGlucose dg = (BestGlucose.DisplayGlucose) dgCache.get();
                    if (dg == null) {
                        dg = BestGlucose.getDisplayGlucose();
                        dgCache.set(dg);
                    }
                    if (dg == null) return null;

                    int chint = getCol(ColorCache.X.color_inrange_bg_values);
                    if (dg.isHigh()) {
                        chint = getCol(ColorCache.X.color_high_bg_values);
                    } else if (dg.isLow()) {
                        chint = getCol(ColorCache.X.color_low_bg_values);
                    }
                    return matrixRow(
                            "timestamp", dg.timestamp,
                            "mgdl", dg.mgdl,
                            "delta_mgdl", dg.delta_mgdl,
                            "delta_arrow", dg.delta_arrow,
                            "delta_name", dg.delta_name,
                            "mssince", dg.mssince,
                            "unitized", dg.unitized,
                            "unitized_delta", dg.unitized_delta,
                            "unitized_delta_no_units", dg.unitized_delta_no_units,
                            "chint", chint,
                            "humansummary", dg.humanSummary(),
                            "ishigh", boolInt(dg.isHigh()),
                            "islow", boolInt(dg.isLow()),
                            "isstale", boolInt(dg.isStale()),
                            "isreallystale", boolInt(dg.isReallyStale())
                    );
                }

                case "status-line": {
                }
                break;

                case "nano-status": {
                    return matrixRow(
                            "collector", nanoStatus("collector"),
                            "sensor-expiry", nanoStatus("sensor-expiry"),
                            "s-expiry", nanoStatus("s-expiry")
                    );
                }

                case "info":
                    return matrixRow("last_ueh", UserError.newestBySeverity(6));

                case "iob-info": {
                }
                break;

                case "graph": {

                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }

                    final int width = Math.min(1000, Integer.parseInt(selectionArgs[0]));
                    final int height = Math.min(1000, Integer.parseInt(selectionArgs[1]));

                    byte[] blob = (byte[]) graphCache.get(width, height);
                    if (blob == null) {
                        String backgroundColor = "#80000000";
                        if (selectionArgs.length >= 3) {
                            backgroundColor = selectionArgs[2];
                        }

                        final BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(xdrip.getAppContext());

                        final Bitmap bitmap = new BgSparklineBuilder(xdrip.getAppContext())
                                .setBgGraphBuilder(bgGraphBuilder)
                                .setHeight(height)
                                .setDotSize(5)
                                .setWidth(width)
                                .showHighLine(true)
                                .showLowLine(true)
                                .build();
                        blob = convertBitmapToPNGByteArray(bitmap);
                        bitmap.recycle();
                        graphCache.set(blob, width, height);
                    }

                    final String[] columnNames = {"blob"};
                    final MatrixCursor cursor = new MatrixCursor(columnNames);
                    cursor.addRow(new Object[]{blob});
                    return cursor;
                }

                case "config":
                    if (enabledWrite()) {
                        try {
                            if (selectionArgs != null && projection != null
                                    && selectionArgs.length == projection.length) {
                                for (int i = 0; i < projection.length; i++) {
                                    final String item = projection[i];
                                    switch (sortOrder) {
                                        case "String":
                                            Pref.setString(item, selectionArgs[i]);
                                            break;
                                        case "Integer":
                                            Pref.setInt(item, Integer.parseInt(selectionArgs[i]));
                                            break;
                                        case "Long":
                                            Pref.setLong(item, Long.parseLong(selectionArgs[i]));
                                            break;
                                        case "Boolean":
                                            Pref.setBoolean(item, Boolean.parseBoolean(selectionArgs[i]));
                                            break;
                                        case "NewString":
                                            if (Pref.getString(item, "defaultvalue").equals("defaultvalue")) {
                                                Pref.setString(item, selectionArgs[i]);
                                            }
                                            break;
                                        case "NewInteger":
                                            if (Pref.getInt(item, -1000) == -1000) {
                                                Pref.setInt(item, Integer.parseInt(selectionArgs[i]));
                                            }
                                            break;
                                        case "NewLong":
                                            if (Pref.getLong(item, -1000) == -1000) {
                                                Pref.setLong(item, Long.parseLong(selectionArgs[i]));
                                            }
                                            break;
                                        case "NewBoolean":
                                            if (Pref.isPreferenceSet(item)) {
                                                Pref.setBoolean(item, Boolean.parseBoolean(selectionArgs[i]));
                                            }
                                            break;
                                        case "Remove":
                                            Pref.removeItem(item);
                                            break;
                                    }
                                }
                                CollectionServiceStarter.restartCollectionServiceBackground();
                                return matrixRow("processed", projection.length);
                            }
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "config error: " + e);
                        }
                    }
                    return null;

            } // end switch
        } catch (Exception e) {
            Log.d(TAG, "Got exception: " + e);
            e.printStackTrace();
        }
        return null;
    }

    private static MatrixCursor matrixRow(Object... x) {
        final String[] columnNames = new String[x.length / 2];
        final Object[] values = new Object[x.length / 2];
        for (int i = 0; i < x.length; i += 2) {
            columnNames[i / 2] = (String) x[i];
            values[i / 2] = x[i + 1];
        }
        try (
                MatrixCursor cursor = new MatrixCursor(columnNames)) {
            cursor.addRow(values);
            return cursor;
        } catch (Exception e) {
            Log.d(TAG, "Error with cursor: " + e);
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        if (!enabled()) return null;
        try {
            getContext().getContentResolver().notifyChange(uri, null);
        } catch (Exception e) {
            Log.e(TAG, "Got exception during insert " + e);
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    private int boolInt(final boolean bool) {
        return bool ? 1 : 0;
    }

    private static boolean enabled() {
        return isNative() || xdrip.getAppContext() != null && getBooleanDefaultFalse("host_content_provider");
    }

    private static boolean enabledWrite() {
        return enabled() && (isNative() || getBooleanDefaultFalse("content_provider_write"));
    }

    public static void ping(String channel) {
        if (enabled() && channel != null) {
            if (channel.equals("bg")) {
                dgCache.clear();
                graphCache.clear();
            }
            xdrip.getAppContext().getContentResolver().insert(Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".contentprovider/" + channel), null);
        }
    }

    public static byte[] convertBitmapToPNGByteArray(Bitmap bitmap) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }

}
