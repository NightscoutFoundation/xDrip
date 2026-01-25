package com.eveningoutpost.dexdrip;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.WidgetDisplayHelper;

import java.util.List;

/**
 * Extended xDrip Widget with 7-day and 30-day averages.
 * Shows current BG, trend graph, arrow, delta, reading age, and averages.
 */
public class xDripWidgetExtended extends AppWidgetProvider {

    public static final String TAG = "xDripWidgetExtended";
    private static final double CUTOFF = 38.0;
    private static final int DEFAULT_GRAPH_HEIGHT_DP = 110;
    private static final double MIN_VALID_AVG = 40.0;  // Minimum valid average in mg/dL

    // Memoization cache for averages
    private static double cachedAvg7d = 0;
    private static long cachedAvg7dTime = 0;
    private static final long AVG_7D_CACHE_MS = 60 * 60 * 1000L;  // 1 hour

    private static double cachedAvg30d = 0;
    private static long cachedAvg30dTime = 0;
    private static final long AVG_30D_CACHE_MS = 6 * 60 * 60 * 1000L;  // 6 hours

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
            Log.e(TAG, "Got exception in widget update: " + e);
        }
    }

    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views) {
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views, -1, -1);
    }

    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views, int maxWidth, int maxHeight) {
        // Update common widget elements (BG, arrow, delta, age, graph, colors)
        WidgetDisplayHelper.updateCommonWidgetElements(
                appWidgetManager, appWidgetId, context, views,
                R.id.xDripWidgetExtended, maxWidth, maxHeight, DEFAULT_GRAPH_HEIGHT_DP);

        // Add extended-specific: 7-day and 30-day averages
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);

        // Calculate and display 7-day average with trend
        double avg7d = getAverageDays(7);
        String avg7dString = bgGraphBuilder.unitized_string(avg7d);
        views.setTextViewText(R.id.widgetAvg7d, avg7dString);

        // Calculate 7-day trend (compare to previous 7 days, days 8-14)
        double avg7dPrev = getAverageDaysOffset(7, 7);  // days 8-14
        String trend7d = (avg7d >= MIN_VALID_AVG && avg7dPrev >= MIN_VALID_AVG)
                ? getTrendArrow(avg7d, avg7dPrev)
                : "–";  // em dash for insufficient data
        views.setTextViewText(R.id.widgetAvg7dTrend, trend7d);

        // Calculate and display 30-day average with trend
        double avg30d = getAverageDays(30);
        String avg30dString = bgGraphBuilder.unitized_string(avg30d);
        views.setTextViewText(R.id.widgetAvg30d, avg30dString);

        // Calculate 30-day trend (compare to previous 30 days, days 31-60)
        double avg30dPrev = getAverageDaysOffset(30, 30);  // days 31-60
        String trend30d = (avg30d >= MIN_VALID_AVG && avg30dPrev >= MIN_VALID_AVG)
                ? getTrendArrow(avg30d, avg30dPrev)
                : "–";  // em dash for insufficient data
        views.setTextViewText(R.id.widgetAvg30dTrend, trend30d);

        // Averages use neutral color (white)
        views.setTextColor(R.id.widgetAvg7d, Color.WHITE);
        views.setTextColor(R.id.widgetAvg30d, Color.WHITE);
    }

    /**
     * Calculate average glucose over specified number of days with memoization.
     * @param days Number of days (7 or 30)
     * @return Average glucose in mg/dL, or 0 if insufficient data
     */
    private static double getAverageDays(int days) {
        long now = System.currentTimeMillis();

        if (days == 7) {
            // Check cache for 7-day average (1 hour cache)
            if (cachedAvg7dTime > 0 && (now - cachedAvg7dTime) < AVG_7D_CACHE_MS) {
                return cachedAvg7d;
            }
            // Cache miss - calculate and cache
            long startTime = now - (7 * 24L * 60L * 60L * 1000L);
            cachedAvg7d = getAverageForPeriod(startTime, now);
            cachedAvg7dTime = now;
            return cachedAvg7d;
        } else if (days == 30) {
            // Check cache for 30-day average (6 hour cache)
            if (cachedAvg30dTime > 0 && (now - cachedAvg30dTime) < AVG_30D_CACHE_MS) {
                return cachedAvg30d;
            }
            // Cache miss - calculate and cache
            long startTime = now - (30 * 24L * 60L * 60L * 1000L);
            cachedAvg30d = getAverageForPeriod(startTime, now);
            cachedAvg30dTime = now;
            return cachedAvg30d;
        }

        // Fallback for any other day count (no caching)
        long startTime = now - (days * 24L * 60L * 60L * 1000L);
        return getAverageForPeriod(startTime, now);
    }

    /**
     * Calculate average glucose for a period starting at an offset.
     * @param days Number of days to average
     * @param offsetDays Days to skip before starting (e.g., 7 for days 8-14)
     * @return Average glucose in mg/dL, or 0 if insufficient data
     */
    private static double getAverageDaysOffset(int days, int offsetDays) {
        long endTime = System.currentTimeMillis() - (offsetDays * 24L * 60L * 60L * 1000L);
        long startTime = endTime - (days * 24L * 60L * 60L * 1000L);
        return getAverageForPeriod(startTime, endTime);
    }

    /**
     * Calculate average glucose for a specific time period.
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return Average glucose in mg/dL, or 0 if insufficient data
     */
    private static double getAverageForPeriod(long startTime, long endTime) {
        try {
            List<BgReading> readings = BgReading.latestForGraph(Integer.MAX_VALUE, startTime, endTime);
            if (readings == null || readings.isEmpty()) {
                return 0;
            }

            double sum = 0;
            int count = 0;
            for (BgReading reading : readings) {
                double value = reading.calculated_value;
                if (value > CUTOFF) {
                    sum += value;
                    count++;
                }
            }

            return count > 0 ? sum / count : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating average: " + e);
            return 0;
        }
    }

    /**
     * Get trend arrow for average comparison by reusing slopeToArrowSymbol.
     * Treats the two averages as consecutive readings 5 minutes apart.
     * @param current Current average value
     * @param previous Previous average value
     * @return Trend arrow string from BgReading.slopeToArrowSymbol()
     */
    private static String getTrendArrow(double current, double previous) {
        if (previous <= 0) {
            return "→";  // No valid previous data
        }

        // Compute slope as if these were two readings 5 minutes apart
        double slope = (current - previous) / 5;  // mg/dL per 5 minutes -> mg/dL per minute

        // Reuse the existing slopeToArrowSymbol function
        return BgReading.slopeToArrowSymbol(slope);
    }
}
