package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.Color;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.WatchSettings;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.X;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;

public class WatchBroadcastService extends Service {
    protected static final int NUM_VALUES = (60 / 5) * 24;

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
        Bundle bundle = new Bundle();
        bundle.putString("action", functionName);
        switch (functionName) {
            case "refresh":
                break;
            case "message":
                break;
            case "alarm":
                break;
            case "update_bg":
                bundle = prepareBundle(1, JoH.tsl(), bundle);
                break;
            case "update_bg_force":
                bundle = prepareBundle(1, JoH.tsl(), bundle);
                break;
        }
        Intent intent = new Intent(Intents.ACTION_DATABASE);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }

    public Bundle prepareBundle(long start, long end, Bundle bundle) {
        if (start > end) {
            long temp = end;
            end = start;
            start = temp;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());

        bundle.putInt("fuzzer", Pref.getBoolean("lower_fuzzer", false) ? 500 * 15 * 5 : 1000 * 30 * 5); // 37.5 seconds : 2.5 minutes
        bundle.putLong("start", start);
        bundle.putLong("end", end);
        bundle.putDouble("highMark", JoH.tolerantParseDouble(prefs.getString("highValue", "170"), 170));
        bundle.putDouble("lowMark", JoH.tolerantParseDouble(prefs.getString("lowValue", "70"), 70));
        bundle.putBoolean("doMgdl", (prefs.getString("units", "mgdl").equals("mgdl")));

        ArrayList<Color> colors = new ArrayList<>();
        for (X color : X.values()) {
            colors.add(new Color(color.name(), getCol(color)));
        }
        bundle.putParcelableArrayList("colors", colors);

        List<BgReading> bgReadings = BgReading.latestForGraph(NUM_VALUES, start, end);
        List<Treatments> treatments = Treatments.latestForGraph(NUM_VALUES, start, end + (120 * 60 * 1000));

        return bundle;
    }
}
