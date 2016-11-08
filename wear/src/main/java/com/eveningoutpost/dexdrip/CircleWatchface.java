package com.eveningoutpost.dexdrip;

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
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.WatchFace;
import com.ustwo.clockwise.WatchFaceTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;


public class CircleWatchface extends WatchFace implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final float PADDING = 20f;
    public final float CIRCLE_WIDTH = 10f;
    public final int BIG_HAND_WIDTH = 16;
    public final int SMALL_HAND_WIDTH = 8;
    public final int NEAR = 2; //how near do the hands have to be to activate overlapping mode
    public final boolean ALWAYS_HIGHLIGT_SMALL = false;

    //variables for time
    private float angleBig = 0f;
    private float angleSMALL = 0f;
    private int hour, minute;
    private int color;
    private Paint circlePaint = new Paint();
    private Paint removePaint = new Paint();
    private RectF rect, rectDelete;
    private boolean overlapping;

    private int animationAngle = 0;
    private boolean isAnimated = false;


    public Point displaySize = new Point();
    private MessageReceiver messageReceiver = new MessageReceiver();

    private int sgvLevel = 0;
    private String sgvString = "999";
    private String rawString = "x | x | x";


    private int batteryLevel = 0;
    private double datetime = 0;
    private String direction = "";
    private String delta = "";
    //public TreeSet<BgWatchData> bgDataList = new TreeSet<BgWatchData>();
    public ArrayList<BgWatchData> bgDataList = new ArrayList<>();

    private View layoutView;
    private int specW;
    private int specH;
    private View myLayout;

    protected SharedPreferences sharedPrefs;


    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CreateWakelock");
        wakeLock.acquire(30000);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        display.getSize(displaySize);

        specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                View.MeasureSpec.EXACTLY);
        specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                View.MeasureSpec.EXACTLY);

        sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        //register Message Receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(Intent.ACTION_SEND));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myLayout = inflater.inflate(R.layout.modern_layout, null);
        prepareLayout();
        prepareDrawTime();

        //ListenerService.requestData(this); //usually connection is not set up yet

        wakeLock.release();
    }


    @Override
    public void onDestroy() {
        if (messageReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        }
        if (sharedPrefs != null) {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        Log.d("CircleWatchface", "start onDraw");
        canvas.drawColor(getBackgroundColor());
        drawTime(canvas);
        drawOtherStuff(canvas);
        myLayout.draw(canvas);

    }

    private synchronized void prepareLayout() {

        Log.d("CircleWatchface", "start startPrepareLayout");

        // prepare fields

        TextView textView = null;

        textView = (TextView) myLayout.findViewById(R.id.sgvString);
        if (sharedPrefs.getBoolean("showBG", true)) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(getSgvString());
            textView.setTextColor(getTextColor());

        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            textView.setVisibility(View.INVISIBLE);
        }

        textView = (TextView) myLayout.findViewById(R.id.rawString);
        if (sharedPrefs.getBoolean("showRaw", false)||
                (sharedPrefs.getBoolean("showRawNoise", true) && getSgvString().equals("???"))
                ) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(getRawString());
            textView.setTextColor(getTextColor());

        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            textView.setVisibility(View.GONE);
        }

        textView = (TextView) myLayout.findViewById(R.id.agoString);
        if (sharedPrefs.getBoolean("showAgo", true)) {
            textView.setVisibility(View.VISIBLE);

            if (sharedPrefs.getBoolean("showBigNumbers", false)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            } else {
                ((TextView) myLayout.findViewById(R.id.agoString)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            }
            textView.setText(getMinutes());
            textView.setTextColor(getTextColor());
        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            textView.setVisibility(View.INVISIBLE);
        }

        textView = (TextView) myLayout.findViewById(R.id.deltaString);
        if (sharedPrefs.getBoolean("showDelta", true)) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(getDelta());
            textView.setTextColor(getTextColor());
            if (sharedPrefs.getBoolean("showBigNumbers", false)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
                if (delta.endsWith(" mg/dl")) {
                    textView.setText(getDelta().substring(0, delta.length() - 6));
                } else if (delta.endsWith(" mmol/l")) {
                    textView.setText(getDelta().substring(0, delta.length() - 7));
                }
            } else {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                textView.setText(getDelta());
            }
        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            textView.setVisibility(View.INVISIBLE);
        }
        //TODO: add more view elements?

        myLayout.measure(specW, specH);
        myLayout.layout(0, 0, myLayout.getMeasuredWidth(),
                myLayout.getMeasuredHeight());
    }

    public String getMinutes() {
        String minutes = "--\'";
        if (getDatetime() != 0) {
            minutes = ((int) Math.floor((System.currentTimeMillis() - getDatetime()) / 60000)) + "\'";
        }
        return minutes;
    }

    private void drawTime(Canvas canvas) {

        //draw circle
        circlePaint.setColor(color);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        canvas.drawArc(rect, 0, 360, false, circlePaint);
        //"remove" hands from circle
        removePaint.setStrokeWidth(CIRCLE_WIDTH * 3);

        canvas.drawArc(rectDelete, angleBig, (float) BIG_HAND_WIDTH, false, removePaint);
        canvas.drawArc(rectDelete, angleSMALL, (float) SMALL_HAND_WIDTH, false, removePaint);


        if (overlapping) {
            //add small hand with extra
            circlePaint.setStrokeWidth(CIRCLE_WIDTH * 2);
            circlePaint.setColor(color);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);

            //remove inner part of hands
            removePaint.setStrokeWidth(CIRCLE_WIDTH);
            canvas.drawArc(rect, angleBig, (float) BIG_HAND_WIDTH, false, removePaint);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, removePaint);
        }

    }

    private synchronized void prepareDrawTime() {
        Log.d("CircleWatchface", "start prepareDrawTime");

        hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % 12;
        minute = Calendar.getInstance().get(Calendar.MINUTE);
        angleBig = (((hour + minute / 60f) / 12f * 360) - 90 - BIG_HAND_WIDTH / 2f + 360) % 360;
        angleSMALL = ((minute / 60f * 360) - 90 - SMALL_HAND_WIDTH / 2f + 360) % 360;


        color = 0;
        switch (getSgvLevel()) {
            case -1:
                color = getLowColor();
                break;
            case 0:
                color = getInRangeColor();
                break;
            case 1:
                color = getHighColor();
                break;
        }


        if (isAnimated()) {
            //Animation matrix:
            int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE
                    , Color.CYAN};
            Shader shader = new LinearGradient(0, 0, 0, 20, rainbow,
                    null, Shader.TileMode.MIRROR);
            Matrix matrix = new Matrix();
            matrix.setRotate(animationAngle);
            shader.setLocalMatrix(matrix);
            circlePaint.setShader(shader);
        } else {
            circlePaint.setShader(null);
        }


        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(color);

        removePaint.setStyle(Paint.Style.STROKE);
        removePaint.setStrokeWidth(CIRCLE_WIDTH);
        removePaint.setAntiAlias(true);
        removePaint.setColor(getBackgroundColor());

        ;

        rect = new RectF(PADDING, PADDING, (float) (displaySize.x - PADDING), (float) (displaySize.y - PADDING));
        rectDelete = new RectF(PADDING - CIRCLE_WIDTH / 2, PADDING - CIRCLE_WIDTH / 2, (float) (displaySize.x - PADDING + CIRCLE_WIDTH / 2), (float) (displaySize.y - PADDING + CIRCLE_WIDTH / 2));
        overlapping = ALWAYS_HIGHLIGT_SMALL || areOverlapping(angleSMALL, angleSMALL + SMALL_HAND_WIDTH + NEAR, angleBig, angleBig + BIG_HAND_WIDTH + NEAR);
        Log.d("CircleWatchface", "end prepareDrawTime");

    }

    synchronized void animationStep() {
        animationAngle = (animationAngle + 1) % 360;
        prepareDrawTime();
        invalidate();
    }


    private boolean areOverlapping(float aBegin, float aEnd, float bBegin, float bEnd) {
        return
                aBegin <= bBegin && aEnd >= bBegin ||
                        aBegin <= bBegin && (bEnd > 360) && bEnd % 360 > aBegin ||
                        bBegin <= aBegin && bEnd >= aBegin ||
                        bBegin <= aBegin && aEnd > 360 && aEnd % 360 > bBegin;
    }

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        if (oldTime.hasMinuteChanged(newTime)) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimeChangedWakelock");
            wakeLock.acquire(30000);
            /*Preparing the layout just on every minute tick:
            *  - hopefully better battery life
            *  - drawback: might update the minutes since last reading up to one minute late*/
            prepareLayout();
            prepareDrawTime();
            invalidate();  //redraw the time
            wakeLock.release();

        }
    }


    // defining color for dark and bright
    public int getLowColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.argb(255, 255, 120, 120);
        } else {
            return Color.argb(255, 255, 80, 80);
        }
    }

    public int getInRangeColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.argb(255, 120, 255, 120);
        } else {
            return Color.argb(255, 0, 240, 0);

        }
    }

    public int getHighColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.argb(255, 255, 255, 120);
        } else {
            return Color.argb(255, 255, 200, 0);
        }

    }

    public int getBackgroundColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.BLACK;
        } else {
            return Color.WHITE;

        }
    }

    public int getTextColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.WHITE;
        } else {
            return Color.BLACK;

        }
    }

    public void drawOtherStuff(Canvas canvas) {
        Log.d("CircleWatchface", "start onDrawOtherStuff. bgDataList.size(): " + bgDataList.size());

        if (isAnimated()) return; // too many repaints when animated
        if (sharedPrefs.getBoolean("showRingHistory", false)) {
            //Perfect low and High indicators
            if (bgDataList.size() > 0) {
                addIndicator(canvas, 100, Color.LTGRAY);
                addIndicator(canvas, (float) bgDataList.iterator().next().low, getLowColor());
                addIndicator(canvas, (float) bgDataList.iterator().next().high, getHighColor());


                if (sharedPrefs.getBoolean("softRingHistory", true)) {
                    for (BgWatchData data : bgDataList) {
                        addReadingSoft(canvas, data);
                    }
                } else {
                    for (BgWatchData data : bgDataList) {
                        addReading(canvas, data);
                    }
                }
            }
        }
    }

    public int holdInMemory() {
        return 6;
    }

    //getters & setters

    private synchronized int getSgvLevel() {
        return sgvLevel;
    }

    private synchronized void setSgvLevel(int sgvLevel) {
        this.sgvLevel = sgvLevel;
    }

    private synchronized int getBatteryLevel() {
        return batteryLevel;
    }

    private synchronized void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }


    private synchronized double getDatetime() {
        return datetime;
    }

    private synchronized void setDatetime(double datetime) {
        this.datetime = datetime;
    }

    private synchronized String getDirection() {
        return direction;
    }

    private void setDirection(String direction) {
        this.direction = direction;
    }

    String getSgvString() {
        return sgvString;
    }

    void setSgvString(String sgvString) {
        this.sgvString = sgvString;
    }

    String getRawString() {
        return rawString;
    }

    void setRawString(String rawString) {
        this.rawString = rawString;
    }

    public String getDelta() {
        return delta;
    }

    private void setDelta(String delta) {
        this.delta = delta;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        prepareDrawTime();
        prepareLayout();
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
                    animationStep();
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                setIsAnimated(false);
                prepareDrawTime();
                invalidate();
                System.gc();
            }
        };

        animator.start();
    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyWakelockTag");
            wakeLock.acquire(30000);

            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            setSgvLevel((int) dataMap.getLong("sgvLevel"));
            Log.d("CircleWatchface", "sgv level : " + getSgvLevel());
            setSgvString(dataMap.getString("sgvString"));
            Log.d("CircleWatchface", "sgv string : " + getSgvString());
            setRawString(dataMap.getString("rawString"));
            setDelta(dataMap.getString("delta"));
            setDatetime(dataMap.getDouble("timestamp"));
            addToWatchSet(dataMap);


            //start animation?
            // dataMap.getDataMapArrayList("entries") == null -> not on "resend data".
            if (sharedPrefs.getBoolean("animation", false) && dataMap.getDataMapArrayList("entries") == null && (getSgvString().equals("100") || getSgvString().equals("5.5") || getSgvString().equals("5,5"))) {
                startAnimation();
            }

            prepareLayout();
            prepareDrawTime();
            invalidate();
            wakeLock.release();
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
        Collections.sort(bgDataList);
    }
    /*
    public synchronized void addToWatchSet(DataMap dataMap) {

        if(!sharedPrefs.getBoolean("showRingHistory", false)){
            bgDataList.clear();
            return;
        }

        Log.d("CircleWatchface", "start addToWatchSet");
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries == null) {
            double sgv = dataMap.getDouble("sgvDouble");
            double high = dataMap.getDouble("high");
            double low = dataMap.getDouble("low");
            double timestamp = dataMap.getDouble("timestamp");
            bgDataList.add(new BgWatchData(sgv, high, low, timestamp));
        } else if (!sharedPrefs.getBoolean("animation", false)) {
            // don't load history at once if animations are set (less resource consumption)
            Log.d("addToWatchSet", "entries.size(): " + entries.size());

            for (DataMap entry : entries) {
                double sgv = entry.getDouble("sgvDouble");
                double high = entry.getDouble("high");
                double low = entry.getDouble("low");
                double timestamp = entry.getDouble("timestamp");
                bgDataList.add(new BgWatchData(sgv, high, low, timestamp));
            }
        } else

            Log.d("addToWatchSet", "start removing bgDataList.size(): " + bgDataList.size());
        HashSet removeSet = new HashSet();
        double threshold = (new Date().getTime() - (1000 * 60 * 5 * holdInMemory()));
        for (BgWatchData data : bgDataList) {
            if (data.timestamp < threshold) {
                removeSet.add(data);

            }
        }
        bgDataList.removeAll(removeSet);
        Log.d("addToWatchSet", "after bgDataList.size(): " + bgDataList.size());
        removeSet = null;
        System.gc();
    }
    */

    public int darken(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = darkenColor(red, fraction);
        green = darkenColor(green, fraction);
        blue = darkenColor(blue, fraction);
        int alpha = Color.alpha(color);

        return Color.argb(alpha, red, green, blue);
    }

    private int darkenColor(int color, double fraction) {

        //if (sharedPrefs.getBoolean("dark", false)) {
        return (int) Math.max(color - (color * fraction), 0);
        //}
        // return (int)Math.min(color + (color * fraction), 255);
    }


    public void addArch(Canvas canvas, float offset, int color, float size) {
        Paint paint = new Paint();
        paint.setColor(color);
        RectF rectTemp = new RectF(PADDING + offset - CIRCLE_WIDTH / 2, PADDING + offset - CIRCLE_WIDTH / 2, (displaySize.x - PADDING - offset + CIRCLE_WIDTH / 2), (displaySize.y - PADDING - offset + CIRCLE_WIDTH / 2));
        canvas.drawArc(rectTemp, 270, size, true, paint);
    }

    public void addArch(Canvas canvas, float start, float offset, int color, float size) {
        Paint paint = new Paint();
        paint.setColor(color);
        RectF rectTemp = new RectF(PADDING + offset - CIRCLE_WIDTH / 2, PADDING + offset - CIRCLE_WIDTH / 2, (displaySize.x - PADDING - offset + CIRCLE_WIDTH / 2), (displaySize.y - PADDING - offset + CIRCLE_WIDTH / 2));
        canvas.drawArc(rectTemp, start + 270, size, true, paint);
    }

    public void addIndicator(Canvas canvas, float bg, int color) {
        float convertedBg;
        convertedBg = bgToAngle(bg);
        convertedBg += 270;
        Paint paint = new Paint();
        paint.setColor(color);
        float offset = 9;
        RectF rectTemp = new RectF(PADDING + offset - CIRCLE_WIDTH / 2, PADDING + offset - CIRCLE_WIDTH / 2, (displaySize.x - PADDING - offset + CIRCLE_WIDTH / 2), (displaySize.y - PADDING - offset + CIRCLE_WIDTH / 2));
        canvas.drawArc(rectTemp, convertedBg, 2, true, paint);
    }

    private float bgToAngle(float bg) {
        if (bg > 100) {
            return (((bg - 100f) / 300f) * 225f) + 135;
        } else {
            return ((bg / 100) * 135);
        }
    }


    public void addReadingSoft(Canvas canvas, BgWatchData entry) {

        Log.d("CircleWatchface", "addReadingSoft");
        double size;
        int color = Color.LTGRAY;
        if (sharedPrefs.getBoolean("dark", false)) {
            color = Color.DKGRAY;
        }

        float offsetMultiplier = (((displaySize.x / 2f) - PADDING) / 12f);
        float offset = (float) Math.max(1, Math.ceil((new Date().getTime() - entry.timestamp) / (1000 * 60 * 5)));
        size = bgToAngle((float) entry.sgv);
        addArch(canvas, offset * offsetMultiplier + 10, color, (float) size);
        addArch(canvas, (float) size, offset * offsetMultiplier + 10, getBackgroundColor(), (float) (360 - size));
        addArch(canvas, (offset + .8f) * offsetMultiplier + 10, getBackgroundColor(), 360);
    }

    public void addReading(Canvas canvas, BgWatchData entry) {
        Log.d("CircleWatchface", "addReading");

        double size;
        int color = Color.LTGRAY;
        int indicatorColor = Color.DKGRAY;
        if (sharedPrefs.getBoolean("dark", false)) {
            color = Color.DKGRAY;
            indicatorColor = Color.LTGRAY;
        }
        int barColor = Color.GRAY;
        if (entry.sgv >= entry.high) {
            indicatorColor = getHighColor();
            barColor = darken(getHighColor(), .5);
        } else if (entry.sgv <= entry.low) {
            indicatorColor = getLowColor();
            barColor = darken(getLowColor(), .5);
        }
        float offsetMultiplier = (((displaySize.x / 2f) - PADDING) / 12f);
        float offset = (float) Math.max(1, Math.ceil((new Date().getTime() - entry.timestamp) / (1000 * 60 * 5)));
        size = bgToAngle((float) entry.sgv);
        addArch(canvas, offset * offsetMultiplier + 11, barColor, (float) size - 2); // Dark Color Bar
        addArch(canvas, (float) size - 2, offset * offsetMultiplier + 11, indicatorColor, 2f); // Indicator at end of bar
        addArch(canvas, (float) size, offset * offsetMultiplier + 11, color, (float) (360f - size)); // Dark fill
        addArch(canvas, (offset + .8f) * offsetMultiplier + 11, getBackgroundColor(), 360);
    }
}