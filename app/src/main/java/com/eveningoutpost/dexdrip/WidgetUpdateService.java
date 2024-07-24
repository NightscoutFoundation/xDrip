package com.eveningoutpost.dexdrip;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

public class WidgetUpdateService extends Service {
    private static final String TAG = "WidgetUpdateService";

    private boolean isRegistered = false;

    public static void staticRefreshWidgets()
    {
        try {
            Context context = xdrip.getAppContext();
            if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0) {
                context.startService(new Intent(context, WidgetUpdateService.class));
            }
        } catch (Exception e)
        {
            Log.e(TAG,"Got exception in staticRefreshWidgets: "+e);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-widget-bcast", 20000);
            //Log.d(TAG, "onReceive("+intent.getAction()+")");
            if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                updateCurrentBgInfo();
            } else if (intent.getAction().compareTo(Intent.ACTION_SCREEN_ON) == 0) {
                enableClockTicks();
                updateCurrentBgInfo();
            } else if (intent.getAction().compareTo(Intent.ACTION_SCREEN_OFF) == 0) {
                disableClockTicks();
            }
            JoH.releaseWakeLock(wl);
        }
    };

    public WidgetUpdateService() {}
    @Override
    public IBinder onBind(Intent intent) { throw new UnsupportedOperationException("Not yet implemented"); }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Service.POWER_SERVICE);
        Log.d(TAG, "onCreate");
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && pm.isInteractive()) ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && pm.isScreenOn()))
            enableClockTicks();
        else
            disableClockTicks();
    }

    private void enableClockTicks() {
        Log.d(TAG, "enableClockTicks");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        if (isRegistered)
            unregisterReceiver(broadcastReceiver);
        registerReceiver(broadcastReceiver, intentFilter);
        isRegistered = true;
    }

    private void disableClockTicks() {
        Log.d(TAG, "disableClockTicks");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        if (isRegistered)
            unregisterReceiver(broadcastReceiver);
        registerReceiver(broadcastReceiver, intentFilter);
        isRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateCurrentBgInfo();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            isRegistered = false;
        }
    }

    public void updateCurrentBgInfo() {
        Log.d(TAG, "Sending update flag to widget");
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), xDripWidget.class));
        Log.d(TAG, "Updating " + ids.length + " widgets");
        Intent intent = new Intent(this,xDripWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);
    }
}
