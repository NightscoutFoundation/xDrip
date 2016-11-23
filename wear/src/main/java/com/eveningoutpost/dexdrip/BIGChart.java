package com.eveningoutpost.dexdrip;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.wearable.watchface.WatchFaceStyle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.watchface.WatchFaceService;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.WatchFace;
import com.ustwo.clockwise.WatchFaceTime;
import com.ustwo.clockwise.WatchMode;
import com.ustwo.clockwise.WatchShape;

import java.util.ArrayList;
import java.util.Date;

import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by stephenblack on 12/29/14.
 */
public class BIGChart extends WatchFace implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final static IntentFilter INTENT_FILTER;
    public static final long[] vibratePattern = {0,400,300,400,300,400};
    public TextView mTime, mSgv, mTimestamp, mDelta;
    public RelativeLayout mRelativeLayout;
    //public LinearLayout mLinearLayout;
    public long sgvLevel = 0;
    public int batteryLevel = 1;
    public int ageLevel = 1;
    public int highColor = Color.YELLOW;
    public int lowColor = Color.RED;
    public int midColor = Color.WHITE;
    public int lowColorWatchMode = Color.RED;
    public int pointSize = 2;
    public boolean singleLine = false;
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
    private int animationAngle = 0;
    private boolean isAnimated = false;

    private LocalBroadcastManager localBroadcastManager;
    private MessageReceiver messageReceiver;

    protected SharedPreferences sharedPrefs;
    private String rawString = "000 | 000 | 000";
    private String batteryString = "--";
    private String sgvString = "--";
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
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_bigchart, null);
        performViewSetup();
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
                mTimestamp = (TextView) stub.findViewById(R.id.timestamp);
                mDelta = (TextView) stub.findViewById(R.id.delta);
                mRelativeLayout = (RelativeLayout) stub.findViewById(R.id.main_layout);
                chart = (LineChartView) stub.findViewById(R.id.chart);
                layoutSet = true;
                showAgoRawBatt();
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
            }
        });
        ListenerService.requestData(this);
        wakeLock.acquire(50);
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
        if (datetime == 0) { return shortString?"--'":"-- Minute ago"; }
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + (shortString?"'":" Minute ago");
        }
        return minutesAgo + (shortString?"'":" Minutes ago");
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
            Log.d("onDraw", "draw");
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

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        if (newTime.hasHourChanged(oldTime) || newTime.hasMinuteChanged(oldTime)) {
            if (layoutSet) {
                wakeLock.acquire(50);
                final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(BIGChart.this);
                mTime.setText(timeFormat.format(System.currentTimeMillis()));
                showAgoRawBatt();

                if (ageLevel() <= 0) {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                missedReadingAlert();
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
            }
            else {
                missedReadingAlert();//KS TEST otherwise, it can be 10+ minutes before missedReadingAlert is called; hwr, aggressive restart does not always resolve ble connection
            }
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            if (layoutSet) {
                wakeLock.acquire(50);
                sgvLevel = dataMap.getLong("sgvLevel");
                batteryLevel = dataMap.getInt("batteryLevel");
                datetime = dataMap.getDouble("timestamp");
                rawString = dataMap.getString("rawString");
                sgvString = dataMap.getString("sgvString");
                batteryString = dataMap.getString("battery");
                mSgv.setText(dataMap.getString("sgvString"));

                if(ageLevel()<=0) {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(BIGChart.this);
                mTime.setText(timeFormat.format(System.currentTimeMillis()));

                showAgoRawBatt();

                String delta = dataMap.getString("delta");
                if (delta.endsWith(" mg/dl")) {
                    mDelta.setText(delta.substring(0, delta.length() - 6));
                } else if (delta.endsWith(" mmol/l")) {
                    mDelta.setText(delta.substring(0, delta.length() - 7));
                }

                if (chart != null) {
                    addToWatchSet(dataMap);
                    setupCharts();
                }
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                invalidate();
                setColor();

                //start animation?
                // dataMap.getDataMapArrayList("entries") == null -> not on "resend data".
                if (sharedPrefs.getBoolean("animation", false) && dataMap.getDataMapArrayList("entries") == null && (sgvString.equals("100") || sgvString.equals("5.5") || sgvString.equals("5,5"))) {
                    startAnimation();
                }


            } else {
                Log.d("ERROR: ", "DATA IS NOT YET SET");
            }
        }
    }

    private void showAgoRawBatt() {

        if( mTimestamp == null){
            return;
        }
        mTimestamp.setText(readingAge(true));
    }

    public void setColor() {
        if (getCurrentWatchMode() == WatchMode.INTERACTIVE) {
            lowColorWatchMode = Color.RED;
        }
        else {
            //RED is not supported in Ambient mode on WatchMode=LOW_BIT sa Sony SmartWatch 3
            //Therefore, use a cold color to indicate a low value
            int prefColor = Integer.parseInt(sharedPrefs.getString("ambient_lowcolor", "3"));
            int[] rainbow = {Color.CYAN, Color.GREEN, Color.RED, Color.WHITE};
            lowColorWatchMode = rainbow[prefColor-1];
        }
        if (sharedPrefs.getBoolean("dark", false)) {
            setColorDark();
        } else {
            setColorBright();
        }

    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
        setColor();
        if(layoutSet){
            showAgoRawBatt();
            mRelativeLayout.measure(specW, specH);
            mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                    mRelativeLayout.getMeasuredHeight());
        }
        invalidate();
    }

    protected void updateRainbow() {
        animationAngle = (animationAngle + 1) % 360;
        //Animation matrix:
        int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE
                , Color.CYAN};
        Shader shader = new LinearGradient(0, 0, 0, 20, rainbow,
                null, Shader.TileMode.MIRROR);
        Matrix matrix = new Matrix();
        matrix.setRotate(animationAngle);
        shader.setLocalMatrix(matrix);
        mSgv.getPaint().setShader(shader);
        invalidate();
    }

    private synchronized boolean isAnimated() {
        return isAnimated;
    }

    private synchronized void setIsAnimated(boolean isAnimated) {
        this.isAnimated = isAnimated;
    }

    void startAnimation() {
        Log.d("CircleWatchface", "start startAnimation");

        Thread animator = new Thread() {


            public void run() {
                //TODO:Wakelock?
                setIsAnimated(true);
                for (int i = 0; i <= 8 * 1000 / 40; i++) {
                    updateRainbow();
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mSgv.getPaint().setShader(null);
                setIsAnimated(false);
                invalidate();
                setColor();

                System.gc();
            }
        };

        animator.start();
    }

    private void resetRelativeLayout() {
        mRelativeLayout.measure(specW, specH);
        mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                mRelativeLayout.getMeasuredHeight());
        Log.d("resetRelativeLayout", "specW=" + specW + " specH=" + specH + " mRelativeLayout.getMeasuredWidth()=" + mRelativeLayout.getMeasuredWidth() + " mRelativeLayout.getMeasuredHeight()=" + mRelativeLayout.getMeasuredHeight());
    }

    private void displayCard() {
        int cardWidth = mCardRect.width();
        int cardHeight = mCardRect.height();
        Log.d("displayCard", "WatchFace.onCardPeek: getWidth()=" + getWidth() + " getHeight()=" + getHeight() + " cardWidth=" + cardWidth + " cardHeight=" + cardHeight);

        if (cardHeight > 0 && cardWidth > 0) {
            if (getCurrentWatchMode() != WatchMode.INTERACTIVE) {
                // get height of visible area (not including card)
                int visibleWidth = getWidth() - cardWidth;
                int visibleHeight = getHeight() - cardHeight;
                Log.d("onCardPeek", "WatchFace.onCardPeek: visibleWidth=" + visibleWidth + " visibleHeight=" + visibleHeight);
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
        mCardRect = peekCardRect;
        displayCard();
        int cardWidth = peekCardRect.width();
        int cardHeight = peekCardRect.height();
        Log.d("onCardPeek", "WatchFace.onCardPeek: getWidth()=" + getWidth() + " getHeight()=" + getHeight() + " cardWidth=" + cardWidth + " cardHeight=" + cardHeight);
    }

    @Override
    protected void onWatchModeChanged(WatchMode watchMode) {
        Log.d("onWatchModeChanged", "WatchFace.onWatchModeChanged: watchMode=" + watchMode.name());
        invalidate();
        setColor();
    }

    protected void setColorDark() {
        Log.d("setColorDark", "WatchMode=" + getCurrentWatchMode());

        mTime.setTextColor(Color.WHITE);
        mRelativeLayout.setBackgroundColor(Color.BLACK);
        if (sgvLevel == 1) {
            mSgv.setTextColor(Color.YELLOW);
            mDelta.setTextColor(Color.YELLOW);
        } else if (sgvLevel == 0) {
            mSgv.setTextColor(Color.WHITE);
            mDelta.setTextColor(Color.WHITE);
        } else if (sgvLevel == -1) {
            mSgv.setTextColor(lowColorWatchMode);
            mDelta.setTextColor(lowColorWatchMode);
        }


        if (ageLevel == 1) {
            mTimestamp.setTextColor(Color.WHITE);
        } else {
            mTimestamp.setTextColor(lowColorWatchMode);
        }

        if (batteryLevel == 1) {
        } else {
        }
        if (chart != null) {
            highColor = Color.YELLOW;
            lowColor = lowColorWatchMode;
            midColor = Color.WHITE;
            singleLine = false;
            pointSize = 2;
            setupCharts();
        }

    }


    protected void setColorBright() {

        Log.d("setColorBright", "WatchMode=" + getCurrentWatchMode());
        if (getCurrentWatchMode() == WatchMode.INTERACTIVE) {
            mRelativeLayout.setBackgroundColor(Color.WHITE);
            if (sgvLevel == 1) {
                mSgv.setTextColor(ChartUtils.COLOR_ORANGE);
                mDelta.setTextColor(ChartUtils.COLOR_ORANGE);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.BLACK);
                mDelta.setTextColor(Color.BLACK);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(Color.RED);
                mDelta.setTextColor(Color.RED);
            }

            if (ageLevel == 1) {
                mTimestamp.setTextColor(Color.BLACK);
            } else {
                mTimestamp.setTextColor(Color.RED);
            }


            mTime.setTextColor(Color.BLACK);
            if (chart != null) {
                highColor = ChartUtils.COLOR_ORANGE;
                midColor = Color.BLUE;
                lowColor = Color.RED;
                singleLine = false;
                pointSize = 2;
                setupCharts();
            }
        } else {
            //RED is not supported in Ambient mode on WatchMode=LOW_BIT sa Sony SmartWatch 3
            //Therefore, use a cold color to indicate a low value
            mRelativeLayout.setBackgroundColor(Color.BLACK);
            if (sgvLevel == 1) {
                mSgv.setTextColor(Color.YELLOW);
                mDelta.setTextColor(Color.YELLOW);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.WHITE);
                mDelta.setTextColor(Color.WHITE);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(lowColorWatchMode);
                mDelta.setTextColor(lowColorWatchMode);
            }
            mTimestamp.setTextColor(Color.WHITE);

            mTime.setTextColor(Color.WHITE);
            if (chart != null) {
                highColor = Color.YELLOW;
                midColor = Color.WHITE;
                lowColor = lowColorWatchMode;
                singleLine = true;
                pointSize = 2;
                setupCharts();
            }
        }

    }


    public void missedReadingAlert() {
        int minutes_since = (int) Math.floor(timeSince() / (1000 * 60));
        int maxDelay = 16;
        if (sharedPrefs.getBoolean("enable_wearG5", false)) {
            maxDelay = 4;
            Log.d("BIGChart", "missedReadingAlert Enter minutes_since " + minutes_since + " call requestData if >= 4 minutes mod 5");//KS
        }

        if (minutes_since >= maxDelay && ((minutes_since - maxDelay) % 5) == 0) {//KS TODO reduce time for debugging; add notifications
            /*NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle("Missed BG Readings")
                    .setVibrate(vibratePattern);
            NotificationManager mNotifyMgr = (hNotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            mNotifyMgr.notify(missed_readings_alert_id, notification.build());*/
            ListenerService.requestData(this); // attempt to recover missing data
        }
    }

    public void addDataMap(DataMap dataMap) {//KS
        double sgv = dataMap.getDouble("sgvDouble");
        double high = dataMap.getDouble("high");
        double low = dataMap.getDouble("low");
        double timestamp = dataMap.getDouble("timestamp");

        Log.d("addToWatchSet", "entry=" + dataMap);

        final int size = bgDataList.size();
        BgWatchData bgdata = new BgWatchData(sgv, high, low, timestamp);
        if (size > 0) {
            if (bgDataList.contains(bgdata)) {
                int i = bgDataList.indexOf(bgdata);
                BgWatchData bgd = bgDataList.get(bgDataList.indexOf(bgdata));
                Log.d("addToWatchSet", "replace indexOf=" + i + " bgDataList.sgv=" + bgd.sgv + " bgDataList.timestamp" + bgd.timestamp);
                bgDataList.set(i, bgdata);
            } else {
                Log.d("addToWatchSet", "add " + " entry.sgv=" + bgdata.sgv + " entry.timestamp" + bgdata.timestamp);
                bgDataList.add(bgdata);
            }
        }
        else {
            bgDataList.add(bgdata);
        }
    }

    public void addToWatchSet(DataMap dataMap) {

        Log.d("addToWatchSet", "bgDataList.size()=" + bgDataList.size());

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            Log.d("addToWatchSet", "entries.size()=" + entries.size());
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
            if (singleLine) {
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
