package com.eveningoutpost.dexdrip;

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

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.ColorCache;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusLine;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;


/**
 * Implementation of App Widget functionality.
 */
public class xDripWidget extends AppWidgetProvider {

    public static final String TAG = "xDripWidget";
    private static final boolean use_best_glucose = true;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-widget-onupdate", 20000);
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {

            //update the widget
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);

        }
        JoH.releaseWakeLock(wl);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "Widget enabled");
        context.startService(new Intent(context, WidgetUpdateService.class));
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "Widget disabled");
        // Enter relevant functionality for when the last widget is disabled
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.x_drip_widget);
        Log.d(TAG, "Update widget signal received");

        //Add behaviour: open xDrip on click
        Intent intent = new Intent(context, Home.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.xDripwidget, pendingIntent);
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views);
            // needed to catch RuntimeException and DeadObjectException
        } catch (Exception e) {
            Log.e(TAG, "Got Rexception in widget update: " + e);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        // update look after resize
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.x_drip_widget);
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views, maxWidth, maxHeight);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views);
            // needed to catch RuntimeException and DeadObjectException
        } catch (Exception e) {
            Log.e(TAG, "Got Rexception in widget update: " + e);
        }
    }

    public static RemoteViews displayCurrentInfo(final Context context, final int maxWidth, final int maxHeight) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.x_drip_widget);
        displayCurrentInfo(null, 0, context, views, maxWidth, maxHeight);
        return views;
    }

    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views) {
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views, -1, -1);
    }

    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views, int maxWidth, int maxHeight) {
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        BgReading lastBgreading = BgReading.lastNoSenssor();

        final boolean showLines = Pref.getBoolean("widget_range_lines", false);
        final boolean showExstraStatus = Pref.getBoolean("extra_status_line", false) && Pref.getBoolean("widget_status_line", false);

        if (lastBgreading != null) {
            double estimate = 0;
            double estimated_delta = -9999;
            try {
                int height = maxHeight == -1 ? appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) : maxHeight;
                int width = maxWidth == -1 ? appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) : maxWidth;
                if (width >= 100 && !Pref.getBooleanDefaultFalse("widget_hide_graph")) {
                    // render bg graph if the widget has enough space to be useful
                    views.setImageViewBitmap(R.id.widgetGraph, new BgSparklineBuilder(context)
                            .setBgGraphBuilder(bgGraphBuilder)
                            //.setShowFiltered(Home.getBooleanDefaultFalse("show_filtered_curve"))
                            //.setBackgroundColor(ColorCache.getCol(ColorCache.X.color_widget_chart_background))
                            .setHeight(height)
                            .setWidth(width)
                            .showHighLine(showLines).showLowLine(showLines).build());
                    views.setViewVisibility(R.id.widgetGraph, View.VISIBLE);
                } else {
                    // hide bg graph
                    views.setViewVisibility(R.id.widgetGraph, View.INVISIBLE);
                }

                views.setInt(R.id.xDripwidget, "setBackgroundColor", ColorCache.getCol(ColorCache.X.color_widget_chart_background));

                final BestGlucose.DisplayGlucose dg = (use_best_glucose) ? BestGlucose.getDisplayGlucose() : null;
                estimate = (dg != null) ? dg.mgdl : lastBgreading.calculated_value;
                String extrastring = "";
                String slope_arrow = (dg != null) ? dg.delta_arrow : lastBgreading.slopeArrow();
                String stringEstimate;

                if (dg == null) {
                    // if not using best glucose helper
                    if (BestGlucose.compensateNoise()) {
                        estimate = BgGraphBuilder.best_bg_estimate; // this needs scaling based on noise intensity
                        estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
                        slope_arrow = BgReading.slopeToArrowSymbol(estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000)); // delta by minute
                        //currentBgValueText.setTypeface(null, Typeface.ITALIC);
                        extrastring = " \u26A0"; // warning symbol !

                    }
                    // TODO functionize this check as it is in multiple places
                    if (Pref.getBooleanDefaultFalse("display_glucose_from_plugin") && (PluggableCalibration.getCalibrationPluginFromPreferences() != null)) {
                        extrastring += " " + context.getString(R.string.p_in_circle);
                    }
                } else {
                    // TODO make a couple of getters in dg for these functions
                    extrastring = " " + dg.extra_string + ((dg.from_plugin) ? " " + context.getString(R.string.p_in_circle) : "");
                    estimated_delta = dg.delta_mgdl;
                    // TODO properly illustrate + standardize warning level
                    if (dg.warning > 1) slope_arrow = "";
                }

                // TODO use dg stale calculation and/or preformatted text
                if ((new Date().getTime()) - Home.stale_data_millis() - lastBgreading.timestamp > 0) {
//                estimate = lastBgreading.calculated_value;
                    Log.d(TAG, "old value, estimate " + estimate);
                    stringEstimate = bgGraphBuilder.unitized_string(estimate);

                    //views.setTextViewText(R.id.widgetArrow, "--");
                    slope_arrow = "--";
                    views.setInt(R.id.widgetBg, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
                } else {
//                estimate = lastBgreading.calculated_value;
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
                    if (stringEstimate.length() > 3) {  // affects mmol xx.x
                        views.setFloat(R.id.widgetBg, "setTextSize", 45);
                    } else {
                        views.setFloat(R.id.widgetBg, "setTextSize", 55);
                    }
                } else {
                    views.setTextViewText(R.id.widgetBg, "");
                    views.setTextViewText(R.id.widgetArrow, "");
                }

                // is it really necessary to read this data once here and again in unitizedDeltaString?
                // couldn't we just use the unitizedDeltaString to detect the error condition?
                List<BgReading> bgReadingList = BgReading.latest(2, Home.get_follower());

                if (estimated_delta == -9999) {
                    // use original delta
                    if (bgReadingList != null && bgReadingList.size() == 2) {

                        views.setTextViewText(R.id.widgetDelta, bgGraphBuilder.unitizedDeltaString(true, true, Home.get_follower()));
                    } else {
                        views.setTextViewText(R.id.widgetDelta, "--");
                    }
                } else {
                    // use compensated estimate
                    views.setTextViewText(R.id.widgetDelta, bgGraphBuilder.unitizedDeltaStringRaw(true, true, estimated_delta));
                }

                // render information about last value age
                int timeAgo = (int) Math.floor((new Date().getTime() - lastBgreading.timestamp) / (1000 * 60));
                final String fmt = context.getString(R.string.minutes_ago);
                final String minutesAgo = MessageFormat.format(fmt, timeAgo);
                views.setTextViewText(R.id.readingAge, minutesAgo + extrastring);
                if (timeAgo > 15) {
                    views.setTextColor(R.id.readingAge, Color.parseColor("#FFBB33"));
                } else {
                    views.setTextColor(R.id.readingAge, Color.WHITE);
                }

                if (showExstraStatus) {
                    views.setTextViewText(R.id.widgetStatusLine, StatusLine.extraStatusLine());
                    views.setViewVisibility(R.id.widgetStatusLine, View.VISIBLE);
                } else {
                    views.setTextViewText(R.id.widgetStatusLine, "");
                    views.setViewVisibility(R.id.widgetStatusLine, View.GONE);
                }
                if (bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
                    views.setTextColor(R.id.widgetBg, Color.parseColor("#C30909"));
                    views.setTextColor(R.id.widgetDelta, Color.parseColor("#C30909"));
                    views.setTextColor(R.id.widgetArrow, Color.parseColor("#C30909"));
                } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
                    views.setTextColor(R.id.widgetBg, Color.parseColor("#FFBB33"));
                    views.setTextColor(R.id.widgetDelta, Color.parseColor("#FFBB33"));
                    views.setTextColor(R.id.widgetArrow, Color.parseColor("#FFBB33"));
                } else {
                    views.setTextColor(R.id.widgetBg, Color.WHITE);
                    views.setTextColor(R.id.widgetDelta, Color.WHITE);
                    views.setTextColor(R.id.widgetArrow, Color.WHITE);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Got exception in displaycurrentinfo: " + e);
            }
        }
    }
}


