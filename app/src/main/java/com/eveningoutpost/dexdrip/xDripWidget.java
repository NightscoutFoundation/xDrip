package com.eveningoutpost.dexdrip;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.WidgetDisplayHelper;


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
        WidgetDisplayHelper.updateCommonWidgetElements(
                appWidgetManager, appWidgetId, context, views,
                R.id.xDripwidget, maxWidth, maxHeight, null);
    }
}


