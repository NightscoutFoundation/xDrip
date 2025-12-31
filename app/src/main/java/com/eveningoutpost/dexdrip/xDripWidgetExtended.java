package com.eveningoutpost.dexdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusLine;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import com.activeandroid.Cache;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Extended xDrip Widget with 7-day and 30-day averages.
 * Shows current BG, trend graph, arrow, delta, reading age, and averages.
 */
public class xDripWidgetExtended extends AppWidgetProvider {

    public static final String TAG = "xDripWidgetExtended";
    private static final boolean use_best_glucose = true;
    private static final String CUTOFF = "38";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-widget-extended-onupdate", 20000);
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
        JoH.releaseWakeLock(wl);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "Extended widget enabled");
        context.startService(new Intent(context, WidgetUpdateService.class));
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "Extended widget disabled");
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.x_drip_widget_extended);
        Log.d(TAG, "Update extended widget signal received");

        // Add behaviour: open xDrip on click
        Intent intent = new Intent(context, Home.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.xDripWidgetExtended, pendingIntent);
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Got exception in widget update: " + e);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        // update look after resize
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.x_drip_widget_extended);
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views, maxWidth, maxHeight);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Got Rexception in widget update: " + e);
        }
    }

    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views) {
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views, -1, -1);
    }

    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views, int maxWidth, int maxHeight) {
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        BgReading lastBgreading = BgReading.lastNoSenssor();

        final boolean showLines = Pref.getBoolean("widget_range_lines", false);
        final boolean showExtraStatus = Pref.getBoolean("extra_status_line", false) && Pref.getBoolean("widget_status_line", false);

        if (lastBgreading != null) {
            double estimate = 0;
            double estimated_delta = -9999;
            try {
                int height = maxHeight == -1 ? appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) : maxHeight;
                int width = maxWidth == -1 ? appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) : maxWidth;
                if (width >= 100 && !Pref.getBooleanDefaultFalse("widget_hide_graph")) {
                    // render bg graph - constrain to top portion
                    int graphHeight = Math.min(height, 110);
                    views.setImageViewBitmap(R.id.widgetGraph, new BgSparklineBuilder(context)
                            .setBgGraphBuilder(bgGraphBuilder)
                            .setHeight(graphHeight)
                            .setWidth(width)
                            .showHighLine(showLines).showLowLine(showLines).build());
                    views.setViewVisibility(R.id.widgetGraph, View.VISIBLE);
                } else {
                    // hide bg graph
                    views.setViewVisibility(R.id.widgetGraph, View.INVISIBLE);
                }

                views.setInt(R.id.xDripWidgetExtended, "setBackgroundColor", ColorCache.getCol(ColorCache.X.color_widget_chart_background));

                final BestGlucose.DisplayGlucose dg = (use_best_glucose) ? BestGlucose.getDisplayGlucose() : null;
                estimate = (dg != null) ? dg.mgdl : lastBgreading.calculated_value;
                String extrastring = "";
                String slope_arrow = (dg != null) ? dg.delta_arrow : lastBgreading.slopeArrow();
                String stringEstimate;

                if (dg == null) {
                    if (BestGlucose.compensateNoise()) {
                        estimate = BgGraphBuilder.best_bg_estimate;
                        estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
                        slope_arrow = BgReading.slopeToArrowSymbol(estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000));
                        extrastring = " \u26A0";
                    }
                    if (Pref.getBooleanDefaultFalse("display_glucose_from_plugin") && (PluggableCalibration.getCalibrationPluginFromPreferences() != null)) {
                        extrastring += " " + context.getString(R.string.p_in_circle);
                    }
                } else {
                    extrastring = " " + dg.extra_string + ((dg.from_plugin) ? " " + context.getString(R.string.p_in_circle) : "");
                    estimated_delta = dg.delta_mgdl;
                    if (dg.warning > 1) slope_arrow = "";
                }

                if ((new Date().getTime()) - Home.stale_data_millis() - lastBgreading.timestamp > 0) {
                    Log.d(TAG, "old value, estimate " + estimate);
                    stringEstimate = bgGraphBuilder.unitized_string(estimate);
                    slope_arrow = "--";
                    views.setInt(R.id.widgetBg, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
                } else {
                    stringEstimate = bgGraphBuilder.unitized_string(estimate);
                    if (lastBgreading.hide_slope) {
                        slope_arrow = "--";
                    }
                    Log.d(TAG, "newish value, estimate " + stringEstimate + slope_arrow);
                    views.setInt(R.id.widgetBg, "setPaintFlags", 0);
                }

                if (Sensor.isActive() || Home.get_follower()) {
                    views.setTextViewText(R.id.widgetBg, stringEstimate);
                    views.setTextViewText(R.id.widgetArrow, slope_arrow);
                    if (stringEstimate.length() > 3) {
                        views.setFloat(R.id.widgetBg, "setTextSize", 45);
                    } else {
                        views.setFloat(R.id.widgetBg, "setTextSize", 55);
                    }
                } else {
                    views.setTextViewText(R.id.widgetBg, "");
                    views.setTextViewText(R.id.widgetArrow, "");
                }

                // Delta
                List<BgReading> bgReadingList = BgReading.latest(2, Home.get_follower());
                if (estimated_delta == -9999) {
                    if (bgReadingList != null && bgReadingList.size() == 2) {
                        views.setTextViewText(R.id.widgetDelta, bgGraphBuilder.unitizedDeltaString(true, true, Home.get_follower()));
                    } else {
                        views.setTextViewText(R.id.widgetDelta, "--");
                    }
                } else {
                    views.setTextViewText(R.id.widgetDelta, bgGraphBuilder.unitizedDeltaStringRaw(true, true, estimated_delta));
                }

                // Reading age
                int timeAgo = (int) Math.floor((new Date().getTime() - lastBgreading.timestamp) / (1000 * 60));
                final String fmt = context.getString(R.string.minutes_ago);
                final String minutesAgo = MessageFormat.format(fmt, timeAgo);
                views.setTextViewText(R.id.readingAge, minutesAgo + extrastring);
                if (timeAgo > 15) {
                    views.setTextColor(R.id.readingAge, Color.parseColor("#FFBB33"));
                } else {
                    views.setTextColor(R.id.readingAge, Color.WHITE);
                }

                if (showExtraStatus) {
                    views.setTextViewText(R.id.widgetStatusLine, StatusLine.extraStatusLine());
                    views.setViewVisibility(R.id.widgetStatusLine, View.VISIBLE);
                } else {
                    views.setTextViewText(R.id.widgetStatusLine, "");
                    views.setViewVisibility(R.id.widgetStatusLine, View.GONE);
                }

                // Calculate and display 7-day average with trend
                double avg7d = getAverageDays(7);
                String avg7dString = bgGraphBuilder.unitized_string(avg7d);
                views.setTextViewText(R.id.widgetAvg7d, avg7dString);

                // Calculate 7-day trend (compare to previous 7 days, days 8-14)
                double avg7dPrev = getAverageDaysOffset(7, 7);  // days 8-14
                String trend7d = getTrendArrow(avg7d, avg7dPrev, 3.0);  // 3% threshold
                views.setTextViewText(R.id.widgetAvg7dTrend, trend7d);

                // Calculate and display 30-day average with trend
                double avg30d = getAverageDays(30);
                String avg30dString = bgGraphBuilder.unitized_string(avg30d);
                views.setTextViewText(R.id.widgetAvg30d, avg30dString);

                // Calculate 30-day trend (compare to previous 30 days, days 31-60)
                double avg30dPrev = getAverageDaysOffset(30, 30);  // days 31-60
                String trend30d = getTrendArrow(avg30d, avg30dPrev, 3.0);  // 3% threshold
                views.setTextViewText(R.id.widgetAvg30dTrend, trend30d);

                // Color for current BG based on range
                if (bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
                    views.setTextColor(R.id.widgetBg, getCol(ColorCache.X.color_low_bg_values));
                    views.setTextColor(R.id.widgetDelta, getCol(ColorCache.X.color_low_bg_values));
                    views.setTextColor(R.id.widgetArrow, getCol(ColorCache.X.color_low_bg_values));
                } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
                    views.setTextColor(R.id.widgetBg, getCol(ColorCache.X.color_high_bg_values));
                    views.setTextColor(R.id.widgetDelta, getCol(ColorCache.X.color_high_bg_values));
                    views.setTextColor(R.id.widgetArrow, getCol(ColorCache.X.color_high_bg_values));
                } else {
                    views.setTextColor(R.id.widgetBg, getCol(ColorCache.X.color_inrange_bg_values));
                    views.setTextColor(R.id.widgetDelta, getCol(ColorCache.X.color_inrange_bg_values));
                    views.setTextColor(R.id.widgetArrow, getCol(ColorCache.X.color_inrange_bg_values));
                }

                // Averages use neutral color (white)
                views.setTextColor(R.id.widgetAvg7d, Color.WHITE);
                views.setTextColor(R.id.widgetAvg30d, Color.WHITE);

            } catch (RuntimeException e) {
                Log.e(TAG, "Got exception in displayCurrentInfo: " + e);
            }
        }
    }

    /**
     * Calculate average glucose over specified number of days.
     * @param days Number of days (7 or 30)
     * @return Average glucose in mg/dL
     */
    private static double getAverageDays(int days) {
        try {
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (days * 24L * 60L * 60L * 1000L);

            SQLiteDatabase db = Cache.openDatabase();
            Cursor cur = db.query("bgreadings",
                    new String[]{"calculated_value"},
                    "timestamp >= ? AND timestamp <= ? AND calculated_value > ?",
                    new String[]{"" + startTime, "" + endTime, CUTOFF},
                    null, null, null);

            double sum = 0;
            int count = 0;
            if (cur.moveToFirst()) {
                do {
                    double value = cur.getDouble(0);
                    sum += value;
                    count++;
                } while (cur.moveToNext());
            }
            cur.close();

            return count > 0 ? sum / count : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating average: " + e);
            return 0;
        }
    }

    /**
     * Calculate average glucose for a period starting at an offset.
     * @param days Number of days to average
     * @param offsetDays Days to skip before starting (e.g., 7 for days 8-14)
     * @return Average glucose in mg/dL
     */
    private static double getAverageDaysOffset(int days, int offsetDays) {
        try {
            long endTime = System.currentTimeMillis() - (offsetDays * 24L * 60L * 60L * 1000L);
            long startTime = endTime - (days * 24L * 60L * 60L * 1000L);

            SQLiteDatabase db = Cache.openDatabase();
            Cursor cur = db.query("bgreadings",
                    new String[]{"calculated_value"},
                    "timestamp >= ? AND timestamp <= ? AND calculated_value > ?",
                    new String[]{"" + startTime, "" + endTime, CUTOFF},
                    null, null, null);

            double sum = 0;
            int count = 0;
            if (cur.moveToFirst()) {
                do {
                    double value = cur.getDouble(0);
                    sum += value;
                    count++;
                } while (cur.moveToNext());
            }
            cur.close();

            return count > 0 ? sum / count : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating average with offset: " + e);
            return 0;
        }
    }

    /**
     * Get trend arrow based on comparison of two values.
     * @param current Current average value
     * @param previous Previous average value
     * @param thresholdPercent Threshold percentage for "no change" (e.g., 5.0 = 5%)
     * @return Trend arrow string: ↗, ↘, or →
     */
    private static String getTrendArrow(double current, double previous, double thresholdPercent) {
        if (previous <= 0) {
            return "→";  // No valid previous data
        }

        double percentChange = ((current - previous) / previous) * 100;

        if (Math.abs(percentChange) <= thresholdPercent) {
            return "→";  // Roughly the same
        } else if (percentChange > 0) {
            return "↗";  // Higher
        } else {
            return "↘";  // Lower
        }
    }
}
