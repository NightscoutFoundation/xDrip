package com.eveningoutpost.dexdrip;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;

import java.util.Date;
import java.util.List;


/**
 * Implementation of App Widget functionality.
 */
public class xDripWidget extends AppWidgetProvider {

    public static final String TAG = "xDripWidget";


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {

            //update the widget
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);

        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "Widget enabled");
        context.startService(new Intent(context, widgetUpdateService.class));
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
        views.setOnClickPendingIntent(R.id.xDripwidget, pendingIntent);;
        displayCurrentInfo(appWidgetManager, appWidgetId, context, views);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    private static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId, Context context, RemoteViews views) {
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        BgReading lastBgreading = BgReading.lastNoSenssor();
        if (lastBgreading != null) {
            double estimate = 0;
            int height = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            int width = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            views.setImageViewBitmap(R.id.widgetGraph, new BgSparklineBuilder(context)
                    .setBgGraphBuilder(bgGraphBuilder)
                    .setHeight(height).setWidth(width).build());

            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.timestamp > 0) {
                estimate = lastBgreading.calculated_value;
                Log.d(TAG, "old value, estimate " + estimate);
                views.setTextViewText(R.id.widgetBg, bgGraphBuilder.unitized_string(estimate));
                views.setTextViewText(R.id.widgetArrow, "--");
                views.setInt(R.id.widgetBg, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                estimate = lastBgreading.calculated_value;
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                String slope_arrow = BgReading.slopeArrow((lastBgreading.calculated_value_slope * 60000));
                if (lastBgreading.hide_slope) {
                    slope_arrow = "--";
                }
                Log.d(TAG, "newish value, estimate " + stringEstimate + slope_arrow);
                views.setTextViewText(R.id.widgetBg, stringEstimate);
                views.setTextViewText(R.id.widgetArrow, slope_arrow);
                views.setInt(R.id.widgetBg, "setPaintFlags", 0);
            }
            List<BgReading> bgReadingList =  BgReading.latest(2);
            if(bgReadingList != null && bgReadingList.size() == 2) {

                views.setTextViewText(R.id.widgetDelta, bgGraphBuilder.unitizedDeltaString(lastBgreading.calculated_value - bgReadingList.get(1).calculated_value));
            } else {
                views.setTextViewText(R.id.widgetDelta, "--");
            }
            int timeAgo =(int) Math.floor((new Date().getTime() - lastBgreading.timestamp)/(1000*60));
            if (timeAgo == 1) {
                views.setTextViewText(R.id.readingAge, timeAgo + " Minute ago");
            } else {
                views.setTextViewText(R.id.readingAge, timeAgo + " Minutes ago");
            }
            if (timeAgo > 15) {
                views.setTextColor(R.id.readingAge, Color.parseColor("#FFBB33"));
            } else {
                views.setTextColor(R.id.readingAge, Color.WHITE);
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
        }
    }
}


