package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.eveningoutpost.dexdrip.Models.UserError;

import java.util.Map;

public class WatchBroadcastService extends Service {
    protected String TAG = this.getClass().getSimpleName();
    protected Map<String, WatchSettings> broadcastEntities;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        UserError.Log.e(TAG, "starting service");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e(TAG, "killing service");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (WatchBroadcastEntry.isEnabled()) {
            if (intent != null) {
                final String function = intent.getStringExtra("function");
                if (function != null) {
                    UserError.Log.d(TAG, "onStartCommand with function:" + function);

                    String message_type = intent.getStringExtra("message_type");
                    String message = intent.getStringExtra("message");
                    String title = intent.getStringExtra("title");

                    message = message != null ? message : "";
                    message_type = message_type != null ? message_type : "";
                    title = title != null ? title : "";
                    handleCommand(function, message_type, message, title);
                } else {
                    // no specific function
                }
            }
            return START_STICKY;
        } else {
            UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void handleCommand(String functionName, String message_type, String message, String title) {

    }
}
