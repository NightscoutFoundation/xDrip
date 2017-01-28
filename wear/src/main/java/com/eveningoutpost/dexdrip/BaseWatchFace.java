package com.eveningoutpost.dexdrip;

//KS import android.app.NotificationManager;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
//KS import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.common.WatchMode;
import com.ustwo.clockwise.wearable.WatchFace;
import com.ustwo.clockwise.common.WatchFaceTime;
import com.ustwo.clockwise.common.WatchShape;

import java.util.ArrayList;
import java.util.Date;

import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by Emma Black on 12/29/14.
 */
public  abstract class BaseWatchFace extends WatchFace implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = BaseWatchFace.class.getSimpleName();
    public final static IntentFilter INTENT_FILTER;
    public static final long[] vibratePattern = {0,400,300,400,300,400};
    public TextView mTime, mSgv, mDirection, mTimestamp, mUploaderBattery, mUploaderXBattery, mDelta, mRaw, mStatus;
    public RelativeLayout mRelativeLayout;
    public LinearLayout mLinearLayout;
    public LinearLayout mDirectionDelta;
    public long sgvLevel = 0;
    public int batteryLevel = 1;
    public int mXBatteryLevel = 1;
    public int xBattery = -1;
    public boolean mShowXBattery = false;
    String[] smallFontsizeArray;// = getResources().getStringArray(R.array.toggle_fontsize);
    public int ageLevel = 1;
    public int highColor = Color.YELLOW;
    public int lowColor = Color.RED;
    public int midColor = Color.WHITE;
    public boolean lowResMode = false;
    public int pointSize = 2;
    public boolean layoutSet = false;
    public int missed_readings_alert_id = 818;
    public BgGraphBuilder bgGraphBuilder;
    public LineChartView chart;
    public double datetime;
    public ArrayList<BgWatchData> bgDataList = new ArrayList<>();
    public PowerManager.WakeLock wakeLock;
    // related to manual layout
    public View layoutView;
    private final Point displaySize = new Point();
    private int specW, specH;

    private LocalBroadcastManager localBroadcastManager;
    private MessageReceiver messageReceiver;

    protected SharedPreferences sharedPrefs;
    private String rawString = "000 | 000 | 000";
    private String batteryString = "--";
    private String sgvString = "--";
    private String externalStatusString = "no status";
    private String avgDelta = "";
    private String delta = "";
    private Rect mCardRect = new Rect(0,0,0,0);

    @Override
    public void onCreate() {
        super.onCreate();
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        display.getSize(displaySize);
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Clock");

        specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                View.MeasureSpec.EXACTLY);
        specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                View.MeasureSpec.EXACTLY);
        sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        smallFontsizeArray = getResources().getStringArray(R.array.toggle_fontsize);
        externalStatusString = getResources().getString(R.string.init_external_status);
    }

    @Override
    protected void onLayout(WatchShape shape, Rect screenBounds, WindowInsets screenInsets) {
        super.onLayout(shape, screenBounds, screenInsets);
        layoutView.onApplyWindowInsets(screenInsets);
    }

    public void performViewSetup() {
        final WatchViewStub stub = (WatchViewStub) layoutView.findViewById(R.id.watch_view_stub);
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);

        messageReceiver = new MessageReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(messageReceiver, messageFilter);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTime = (TextView) stub.findViewById(R.id.watch_time);
                mSgv = (TextView) stub.findViewById(R.id.sgv);
                mDirection = (TextView) stub.findViewById(R.id.direction);
                mTimestamp = (TextView) stub.findViewById(R.id.timestamp);
                mStatus = (TextView) stub.findViewById(R.id.externaltstatus);
                mRaw = (TextView) stub.findViewById(R.id.raw);
                mUploaderBattery = (TextView) stub.findViewById(R.id.uploader_battery);
                mUploaderXBattery = (TextView) stub.findViewById(R.id.uploader_xbattery);
                mDelta = (TextView) stub.findViewById(R.id.delta);
                mRelativeLayout = (RelativeLayout) stub.findViewById(R.id.main_layout);
                mLinearLayout = (LinearLayout) stub.findViewById(R.id.secondary_layout);
                mDirectionDelta = (LinearLayout) stub.findViewById(R.id.directiondelta_layout);
                setSmallFontsize(false);
                chart = (LineChartView) stub.findViewById(R.id.chart);
                layoutSet = true;
                showAgoRawBattStatus();
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
            }
        });
        ListenerService.requestData(this);
        wakeLock.acquire(50);
    }

    public void setSmallFontsize(boolean toggle) {
        int fontvalue = Integer.parseInt(sharedPrefs.getString("toggle_fontsize", "1"));
        fontvalue = toggle ? (fontvalue%smallFontsizeArray.length) + 1 : fontvalue;
        int fontsize =  Integer.parseInt(smallFontsizeArray[fontvalue-1]);
        Log.d(TAG, "setSmallFontsize fontsize=" + fontsize + " fontvalue=" + fontvalue);
        mDelta.setTextSize(fontsize);
        setStatusTextSize(fontsize);
        if (fontvalue == 1) {
            mStatus.setMaxLines(1);
            mStatus.setEllipsize(TextUtils.TruncateAt.END);
        } else {
            mStatus.setMaxLines(Integer.MAX_VALUE);
            mStatus.setEllipsize(null);
        }
        if (toggle) sharedPrefs.edit().putString("toggle_fontsize", "" + fontvalue).commit();
    }

    private void setStatusTextSize(int size) {
        mDelta.setTextSize(size);
        mTimestamp.setTextSize(size);
        mUploaderBattery.setTextSize(size);
        mUploaderXBattery.setTextSize(size);
        mStatus.setTextSize(size);
    }

    public int ageLevel() {
        if(timeSince() <= (1000 * 60 * 12)) {
            return 1;
        } else {
            return 0;
        }
    }

    public double timeSince() {
        return System.currentTimeMillis() - datetime;
    }

    public String readingAge(boolean shortString) {
        if (datetime == 0) {
            //return shortString?"--'":"-- Minute ago";
            if (shortString)
                return (getResources().getString(R.string.label_minuteago_zero_abbrv));
            else
                return (getResources().getString(R.string.label_minuteago_zero));
        }
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            //return minutesAgo + (shortString?"'":" Minute ago");
            if (shortString)
                return (getResources().getString(R.string.label_minuteago_abbrv, minutesAgo));
            else
                return (getResources().getString(R.string.label_minuteago, minutesAgo));
        }
        //return minutesAgo + (shortString?"'":" Minutes ago");
        if (shortString)
            return (getResources().getString(R.string.label_minuteago_abbrv, minutesAgo));
        else
            return (getResources().getString(R.string.label_minuteago_pl, minutesAgo));
    }

    @Override
    public void onDestroy() {
        if(localBroadcastManager != null && messageReceiver != null){
            localBroadcastManager.unregisterReceiver(messageReceiver);}
        if (sharedPrefs != null){
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(layoutSet) {
            this.mRelativeLayout.draw(canvas);
            Log.d(TAG, "onDraw");
            if (sharedPrefs.getBoolean("showOpaqueCard", true)) {
                int cardWidth = mCardRect.width();
                int cardHeight = mCardRect.height();
                if (cardHeight > 0 && cardWidth > 0 && getCurrentWatchMode() != WatchMode.INTERACTIVE) {
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    paint.setStrokeWidth(0);
                    canvas.drawRect(mCardRect, paint);
                }
            }
        }
    }

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        if (newTime.hasHourChanged(oldTime) || newTime.hasMinuteChanged(oldTime)) {
            if (layoutSet) {
                wakeLock.acquire(50);
                final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(BaseWatchFace.this);
                mTime.setText(timeFormat.format(System.currentTimeMillis()));
                showAgoRawBattStatus();

                if (ageLevel() <= 0) {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                missedReadingAlert();
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
            } else {
                missedReadingAlert();//KS TEST otherwise, it can be 10+ minutes before missedReadingAlert is called; hwr, aggressive restart does not always resolve ble connection
            }
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataMap dataMap;
            Bundle bundle = intent.getBundleExtra("msg");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                String msg = dataMap.getString("msg", "");
                JoH.static_toast_short(msg);
            }
            bundle = intent.getBundleExtra("data");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                wakeLock.acquire(50);
                sgvLevel = dataMap.getLong("sgvLevel");
                batteryLevel = dataMap.getInt("batteryLevel");
                datetime = dataMap.getDouble("timestamp");
                rawString = dataMap.getString("rawString");
                sgvString = dataMap.getString("sgvString");
                batteryString = dataMap.getString("battery");
                xBattery = dataMap.getInt("bridge_battery", -1);
                mXBatteryLevel = (xBattery >= 30) ? 1 : 0;
                Log.d(TAG, "onReceive batteryString=" + batteryString + " batteryLevel=" + batteryLevel + " xBattery=" + xBattery + " sgvString=" + sgvString);
                mSgv.setText(dataMap.getString("sgvString"));
                if(ageLevel()<=0) {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(BaseWatchFace.this);
                mTime.setText(timeFormat.format(System.currentTimeMillis()));

                mDirection.setText(dataMap.getString("slopeArrow"));
                avgDelta = dataMap.getString("avgDelta", "");
                delta = dataMap.getString("delta");


                showAgoRawBattStatus();


                if (chart != null) {
                    addToWatchSet(dataMap);
                    setupCharts();
                }
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                invalidate();
                setColor();
            } else {
                Log.d(TAG, "ERROR: DATA IS NOT YET SET");
            }
            //status
            bundle = intent.getBundleExtra("status");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                wakeLock.acquire(50);
                externalStatusString = dataMap.getString("externalStatusString");

                showAgoRawBattStatus();

                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                invalidate();
                setColor();
            }
        }
    }

    private void showAgoRawBattStatus() {

        boolean showAvgDelta = sharedPrefs.getBoolean("showAvgDelta", false);
        mDelta.setText(delta);
        if(showAvgDelta){
            mDelta.append("  " + avgDelta);
        }

        if( mTimestamp == null || mUploaderBattery == null|| mStatus == null){
            return;
        }

        boolean showStatus = sharedPrefs.getBoolean("showExternalStatus", true);

        /* TODO showRaw
        if (sharedPrefs.getBoolean("showRaw", false)||
                (sharedPrefs.getBoolean("showRawNoise", true) && sgvString.equals("???"))
                ) {
            mRaw.setVisibility(View.VISIBLE);
            mRaw.setText("R: " + rawString);
            mTimestamp.setText(readingAge(true));
            mUploaderBattery.setText("U: " + batteryString + "%");
        } else {
            mRaw.setVisibility(View.GONE);
            mTimestamp.setText(readingAge(false));
            mUploaderBattery.setText("Uploader: " + batteryString + "%");
        }
        */
        mRaw.setVisibility(View.GONE);

        //xBridge Battery:
        mShowXBattery = false;
        if (sharedPrefs.getBoolean("showBridgeBattery", false) && xBattery > 0 && DexCollectionType.hasBattery()) {
            mShowXBattery = true;
        }
        Log.d(TAG, "showAgoRawBattStatus mShowXBattery=" + mShowXBattery + " xBattery=" + xBattery);
        if (mShowXBattery) {//see app/home.java displayCurrentInfo()
            //mUploaderXBattery.setText((showStatus ? "B: " : "Bridge: ") + xbatteryString + ((mXBattery < 200) ? "%" : "mV"));
            String xbatteryString = "" + xBattery;
            if (xBattery < 200) {
                if (showStatus)
                    mUploaderXBattery.setText(getResources().getString(R.string.label_show_bridge_battery_percent_abbrv, xbatteryString));
                else
                    mUploaderXBattery.setText(getResources().getString(R.string.label_show_bridge_battery_percent, xbatteryString));
            }
            else {
                if (showStatus)
                    mUploaderXBattery.setText(getResources().getString(R.string.label_show_bridge_battery_volt_abbrv, xbatteryString));
                else
                    mUploaderXBattery.setText(getResources().getString(R.string.label_show_bridge_battery_volt, xbatteryString));
            }
            mUploaderXBattery.setVisibility(View.VISIBLE);
        }
        else
            mUploaderXBattery.setVisibility(View.GONE);

        if(showStatus || mShowXBattery){
            //use short forms
            mTimestamp.setText(readingAge(true));
        } else {
            mTimestamp.setText(readingAge(false));
        }

        boolean enable_wearG5 = sharedPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = sharedPrefs.getBoolean("force_wearG5", false);
        boolean isCollectorRunning = (enable_wearG5 && force_wearG5);
        if (!isCollectorRunning && enable_wearG5) {
            Class<?> serviceClass = DexCollectionType.getCollectorServiceClass();
            if (serviceClass != null) {
                Log.d(TAG, "DexCollectionType.getCollectorServiceClass(): " + serviceClass.getName());
                isCollectorRunning = isServiceRunning(serviceClass);
            }
        }
        if (isCollectorRunning) {//Wear Collector
            Log.d(TAG, DexCollectionType.getDexCollectionType() + " is running.");
            int wearBattery = getWearBatteryLevel(getApplication());
            String wearBatteryString = "" + wearBattery;
            batteryLevel = (wearBattery >= 30) ? 1 : 0;
            if (showStatus || mShowXBattery)
                mUploaderBattery.setText(getResources().getString(R.string.label_show_uploader_wear_abbrv, wearBatteryString));//"W: " + batteryString + "%"
            else
                mUploaderBattery.setText(getResources().getString(R.string.label_show_uploader_wear, wearBatteryString));//"Wear: " + batteryString + "%"
        }
        else {//Phone Collector
            Log.d(TAG, "Collector running on phone");
            if (showStatus || mShowXBattery)
                mUploaderBattery.setText(getResources().getString(R.string.label_show_uploader_abbrv, batteryString));//"U: " + batteryString + "%"
            else
                mUploaderBattery.setText(getResources().getString(R.string.label_show_uploader, batteryString));//"Uploader: " + batteryString + "%"
        }

        if (showStatus) {
            mStatus.setVisibility(View.VISIBLE);
            mStatus.setText(getResources().getString(R.string.label_show_external_status_abbrv) + externalStatusString);//"S: "
        } else {
            mStatus.setVisibility(View.GONE);
        }
    }

    // Custom method to determine whether a service is running
    private boolean isServiceRunning(Class<?> serviceClass){//Class<?> serviceClass
        if (serviceClass != null) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // Loop through the running services
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                //Log.d(TAG, "isServiceRunning: getClassName=" + service.service.getClassName() + " getShortClassName=" + service.service.getShortClassName());
                if (serviceClass.getName().equals(service.service.getClassName())) return true;
            }
        }
        return false;
    }

    public static int getWearBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);//from BgSendQueue
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level == -1 || scale == -1) {
            return 50;
        }
        else
            return (int) (((float) level / (float) scale) * 100.0f);
    }

    public void setColor() {
        if(lowResMode){
            setColorLowRes();
        } else if (sharedPrefs.getBoolean("dark", true)) {
            setColorDark();
        } else {
            setColorBright();
        }

    }

    protected void onWatchModeChanged(WatchMode watchMode) {

        if(lowResMode ^ isLowRes(watchMode)){ //if there was a change in lowResMode
            lowResMode = isLowRes(watchMode);
            setColor();
        } else if (! sharedPrefs.getBoolean("dark", true)){
            //in bright mode: different colours if active:
            setColor();
        }
    }

    private boolean isLowRes(WatchMode watchMode) {
        return (watchMode == WatchMode.LOW_BIT) || (watchMode == WatchMode.LOW_BIT_BURN_IN) || (watchMode == WatchMode.LOW_BIT_BURN_IN);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
        setColor();
        if(layoutSet){
            showAgoRawBattStatus();
            setSmallFontsize(false);
            mRelativeLayout.measure(specW, specH);
            mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                    mRelativeLayout.getMeasuredHeight());
        }
        invalidate();
    }

    public void invalidateWatchface() {
        setColor();
        if(layoutSet){
            showAgoRawBattStatus();
            mRelativeLayout.measure(specW, specH);
            mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                    mRelativeLayout.getMeasuredHeight());
        }
        invalidate();
    }

    private void resetRelativeLayout() {
        mRelativeLayout.measure(specW, specH);
        mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                mRelativeLayout.getMeasuredHeight());
        Log.d(TAG, "resetRelativeLayout specW=" + specW + " specH=" + specH + " mRelativeLayout.getMeasuredWidth()=" + mRelativeLayout.getMeasuredWidth() + " mRelativeLayout.getMeasuredHeight()=" + mRelativeLayout.getMeasuredHeight());
    }

    private void displayCard() {
        int cardWidth = mCardRect.width();
        int cardHeight = mCardRect.height();
        Log.d(TAG, "displayCard WatchFace.onCardPeek: getWidth()=" + getWidth() + " getHeight()=" + getHeight() + " cardWidth=" + cardWidth + " cardHeight=" + cardHeight);

        if (cardHeight > 0 && cardWidth > 0) {
            if (getCurrentWatchMode() != WatchMode.INTERACTIVE) {
                // get height of visible area (not including card)
                int visibleWidth = getWidth() - cardWidth;
                int visibleHeight = getHeight() - cardHeight;
                Log.d(TAG, "onCardPeek WatchFace.onCardPeek: visibleWidth=" + visibleWidth + " visibleHeight=" + visibleHeight);
                mRelativeLayout.layout(0, 0, visibleWidth, visibleHeight);
            }
            else
                resetRelativeLayout();
        }
        else
            resetRelativeLayout();
        invalidate();
    }

    @Override
    protected void onCardPeek(Rect peekCardRect) {
        if (sharedPrefs.getBoolean("showOpaqueCard", true)) {
            mCardRect = peekCardRect;
            displayCard();
            int cardWidth = peekCardRect.width();
            int cardHeight = peekCardRect.height();
            Log.d(TAG, "onCardPeek WatchFace.onCardPeek: getWidth()=" + getWidth() + " getHeight()=" + getHeight() + " cardWidth=" + cardWidth + " cardHeight=" + cardHeight);
        }
    }

    protected abstract void setColorDark();
    protected abstract void setColorBright();
    protected abstract void setColorLowRes();

    public void missedReadingAlert() {
        int minutes_since   = (int) Math.floor(timeSince()/(1000*60));
        int maxDelay = 16;
        if (sharedPrefs.getBoolean("enable_wearG5", false)) {
            maxDelay = 4;
            Log.d(TAG, "missedReadingAlert Enter minutes_since " + minutes_since + " call requestData if >= 4 minutes mod 5");//KS
        }

        if (minutes_since >= maxDelay && ((minutes_since - maxDelay) % 5) == 0) {//KS TODO reduce time for debugging; add notifications
            /*NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Missed BG Readings")
                        .setVibrate(vibratePattern);
            NotificationManager mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            mNotifyMgr.notify(missed_readings_alert_id, notification.build());*/
            ListenerService.requestData(this); // attempt to recover missing data
        }
    }

    public void addDataMap(DataMap dataMap) {//KS
        double sgv = dataMap.getDouble("sgvDouble");
        double high = dataMap.getDouble("high");
        double low = dataMap.getDouble("low");
        double timestamp = dataMap.getDouble("timestamp");

        //Log.d(TAG, "addToWatchSet entry=" + dataMap);

        final int size = bgDataList.size();
        BgWatchData bgdata = new BgWatchData(sgv, high, low, timestamp);
        if (size > 0) {
            if (bgDataList.contains(bgdata)) {
                int i = bgDataList.indexOf(bgdata);
                BgWatchData bgd = bgDataList.get(bgDataList.indexOf(bgdata));
                //Log.d(TAG, "addToWatchSet replace indexOf=" + i + " bgDataList.sgv=" + bgd.sgv + " bgDataList.timestamp" + bgd.timestamp);
                bgDataList.set(i, bgdata);
            } else {
                //Log.d(TAG, "addToWatchSet add " + " entry.sgv=" + bgdata.sgv + " entry.timestamp" + bgdata.timestamp);
                bgDataList.add(bgdata);
            }
        }
        else {
            bgDataList.add(bgdata);
        }
    }

    public void addToWatchSet(DataMap dataMap) {

        Log.d(TAG, "addToWatchSet bgDataList.size()=" + bgDataList.size());

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            Log.d(TAG, "addToWatchSet entries.size()=" + entries.size());
            for (DataMap entry : entries) {
                addDataMap(entry);
            }
        } else {
            addDataMap(dataMap);
        }

        for (int i = 0; i < bgDataList.size(); i++) {
            if (bgDataList.get(i).timestamp < (new Date().getTime() - (1000 * 60 * 60 * 5))) {
                bgDataList.remove(i); //Get rid of anything more than 5 hours old
                break;
            }
        }
    }

    public void setupCharts() {
        if(bgDataList.size() > 0) { //Dont crash things just because we dont have values, people dont like crashy things
            int timeframe = Integer.parseInt(sharedPrefs.getString("chart_timeframe", "5"));
            if (lowResMode) {
                bgGraphBuilder = new BgGraphBuilder(getApplicationContext(), bgDataList, pointSize, midColor, timeframe);
            } else {
                bgGraphBuilder = new BgGraphBuilder(getApplicationContext(), bgDataList, pointSize, highColor, lowColor, midColor, timeframe);
            }

            chart.setLineChartData(bgGraphBuilder.lineData());
            chart.setViewportCalculationEnabled(true);
            chart.setMaximumViewport(chart.getMaximumViewport());
        } else {
            ListenerService.requestData(this);
        }
    }
}