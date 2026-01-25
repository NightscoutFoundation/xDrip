package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

public class WidgetDisplayHelper {

    private static final boolean use_best_glucose = true;

    /**
     * Updates common widget display elements (BG, arrow, delta, age, graph, colors).
     * Both standard and extended widgets use this method.
     *
     * @param appWidgetManager Widget manager
     * @param appWidgetId Widget ID
     * @param context Application context
     * @param views RemoteViews to update
     * @param widgetRootViewId Root view ID (differs between widgets)
     * @param maxWidth Widget max width
     * @param maxHeight Widget max height
     * @param maxGraphHeight Maximum height for graph (null for no limit)
     */
    public static void updateCommonWidgetElements(
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            Context context,
            RemoteViews views,
            int widgetRootViewId,
            int maxWidth,
            int maxHeight,
            Integer maxGraphHeight) {

        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        BgReading lastBgReading = BgReading.lastNoSenssor();

        final boolean showLines = Pref.getBoolean("widget_range_lines", false);
        final boolean showExtraStatus = Pref.getBoolean("extra_status_line", false)
                && Pref.getBoolean("widget_status_line", false);

        if (lastBgReading != null) {
            double estimate;
            double estimated_delta = -9999;
            try {
                int height = maxHeight == -1
                        ? appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                        : maxHeight;
                int width = maxWidth == -1
                        ? appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                        : maxWidth;

                int graphHeight = height;
                if (maxGraphHeight != null) {
                    graphHeight = Math.min(height, maxGraphHeight);
                }

                if (width >= 100 && !Pref.getBooleanDefaultFalse("widget_hide_graph")) {
                    views.setImageViewBitmap(R.id.widgetGraph, new BgSparklineBuilder(context)
                            .setBgGraphBuilder(bgGraphBuilder)
                            .setHeight(graphHeight)
                            .setWidth(width)
                            .showHighLine(showLines).showLowLine(showLines).build());
                    views.setViewVisibility(R.id.widgetGraph, View.VISIBLE);
                } else {
                    views.setViewVisibility(R.id.widgetGraph, View.INVISIBLE);
                }

                views.setInt(widgetRootViewId, "setBackgroundColor",
                        ColorCache.getCol(ColorCache.X.color_widget_chart_background));

                final BestGlucose.DisplayGlucose dg = (use_best_glucose) ? BestGlucose.getDisplayGlucose() : null;
                estimate = (dg != null) ? dg.mgdl : lastBgReading.calculated_value;
                String extrastring = "";
                String slopeArrow = (dg != null) ? dg.delta_arrow : lastBgReading.slopeArrow();
                String stringEstimate;

                if (dg == null) {
                    if (BestGlucose.compensateNoise()) {
                        estimate = BgGraphBuilder.best_bg_estimate;
                        estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
                        slopeArrow = BgReading.slopeToArrowSymbol(estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000));
                        extrastring = " \u26A0";
                    }
                    if (Pref.getBooleanDefaultFalse("display_glucose_from_plugin")
                            && (PluggableCalibration.getCalibrationPluginFromPreferences() != null)) {
                        extrastring += " " + context.getString(R.string.p_in_circle);
                    }
                } else {
                    extrastring = " " + dg.extra_string + ((dg.from_plugin) ? " " + context.getString(R.string.p_in_circle) : "");
                    estimated_delta = dg.delta_mgdl;
                    if (dg.warning > 1) slopeArrow = "";
                }

                if ((new Date().getTime()) - Home.stale_data_millis() - lastBgReading.timestamp > 0) {
                    stringEstimate = bgGraphBuilder.unitized_string(estimate);
                    slopeArrow = "--";
                    views.setInt(R.id.widgetBg, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
                } else {
                    stringEstimate = bgGraphBuilder.unitized_string(estimate);
                    if (lastBgReading.hide_slope) {
                        slopeArrow = "--";
                    }
                    views.setInt(R.id.widgetBg, "setPaintFlags", 0);
                }

                if (Sensor.isActive() || Home.get_follower()) {
                    views.setTextViewText(R.id.widgetBg, stringEstimate);
                    views.setTextViewText(R.id.widgetArrow, slopeArrow);
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
                        views.setTextViewText(R.id.widgetDelta,
                                bgGraphBuilder.unitizedDeltaString(true, true, Home.get_follower()));
                    } else {
                        views.setTextViewText(R.id.widgetDelta, "--");
                    }
                } else {
                    views.setTextViewText(R.id.widgetDelta,
                            bgGraphBuilder.unitizedDeltaStringRaw(true, true, estimated_delta));
                }

                // Reading age
                int timeAgo = (int) Math.floor((new Date().getTime() - lastBgReading.timestamp) / (1000 * 60));
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

            } catch (RuntimeException e) {
                Log.e("WidgetDisplayHelper", "Got exception in updateCommonWidgetElements: " + e);
            }
        }
    }
}
