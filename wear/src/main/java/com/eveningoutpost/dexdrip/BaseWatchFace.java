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

import android.support.wearable.view.WatchViewStub;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.services.HeartRateService;
import com.eveningoutpost.dexdrip.utilitymodels.BgSendQueue;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.common.WatchFaceTime;
import com.ustwo.clockwise.common.WatchMode;
import com.ustwo.clockwise.common.WatchShape;
import com.ustwo.clockwise.wearable.WatchFace;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by Emma Black on 12/29/14.
 */
public  abstract class BaseWatchFace extends WatchFace implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = BaseWatchFace.class.getSimpleName();
    public final static IntentFilter INTENT_FILTER;
    public static final long[] vibratePattern = {0,400,300,400,300,400};
    public TextView mDate, mTime, mSgv, mDirection, mTimestamp, mUploaderBattery, mUploaderXBattery, mDelta, mRaw, mStatus;
    public Button stepsButton;
    public Button heartButton;
    public Button menuButton;
    public RelativeLayout mRelativeLayout;
    public LinearLayout mLinearLayout;
    public LinearLayout mDirectionDelta;
    public LinearLayout mStepsLinearLayout;
    public LinearLayout mMenuLinearLayout;
    public LinearLayout mSecondaryLayout;
    public String mExtraStatusLine = "";
    public String mStepsToast = "";
    public int mStepsCount = 0;
    public int mHeartBPM = 0;
    public long mTimeStepsRcvd = 0;
    public long mTimeHeartRcvd = 0;
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
    public ArrayList<BgWatchData> treatsDataList = new ArrayList<>();
    public ArrayList<BgWatchData> calDataList = new ArrayList<>();
    public ArrayList<BgWatchData> btDataList = new ArrayList<>();
    private final static boolean d = false; // debug flag, could be read from preferences
    public PowerManager.WakeLock wakeLock;
    // related to manual layout
    public View layoutView;
    public String mStatusLine = "";
    private final Point displaySize = new Point();
    private int specW, specH;

    private LocalBroadcastManager localBroadcastManager;
    private MessageReceiver messageReceiver;

    protected SharedPreferences sharedPrefs;
    private static Locale oldLocale = null;
    private static String oldDate = "";
    private static SimpleDateFormat dateFormat = null;
    private String rawString = "000 | 000 | 000";
    private String batteryString = "--";
    private String sgvString = "--";
    private String externalStatusString = "no status";
    private String avgDelta = "";
    private String delta = "";
    private Rect mCardRect = new Rect(0,0,0,0);
    //private static BaseWatchFace mActivity;//TODO

    @Override
    public void onCreate() {
        super.onCreate();
        //mActivity = this;//TODO
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
                mDate = (TextView) stub.findViewById(R.id.watch_date);
                mSgv = (TextView) stub.findViewById(R.id.sgv);
                mDirection = (TextView) stub.findViewById(R.id.direction);
                mTimestamp = (TextView) stub.findViewById(R.id.timestamp);
                mStatus = (TextView) stub.findViewById(R.id.externaltstatus);
                mRaw = (TextView) stub.findViewById(R.id.raw);
                mUploaderBattery = (TextView) stub.findViewById(R.id.uploader_battery);
                mUploaderXBattery = (TextView) stub.findViewById(R.id.uploader_xbattery);
                mDelta = (TextView) stub.findViewById(R.id.delta);
                stepsButton=(Button)stub.findViewById(R.id.walkButton);
                try {
                    heartButton=(Button)stub.findViewById(R.id.heartButton);
                } catch (Exception e) {
                    //
                }
                mStepsLinearLayout = (LinearLayout) stub.findViewById(R.id.steps_layout);
                menuButton=(Button)stub.findViewById(R.id.menuButton);
                mMenuLinearLayout = (LinearLayout) stub.findViewById(R.id.menu_layout);
                mRelativeLayout = (RelativeLayout) stub.findViewById(R.id.main_layout);
                mLinearLayout = (LinearLayout) stub.findViewById(R.id.secondary_layout);
                mDirectionDelta = (LinearLayout) stub.findViewById(R.id.directiondelta_layout);
                setSmallFontsize(false);
                chart = (LineChartView) stub.findViewById(R.id.chart);
                layoutSet = true;
                Context context = xdrip.getAppContext();
                if (Home.get_forced_wear()) {
                    if (d) Log.d(TAG, "performViewSetup FORCE WEAR init BGs for graph");
                    BgSendQueue.resendData(context);
                }
                if ((chart != null) && sharedPrefs.getBoolean("show_wear_treatments", false)) {
                    if (d) Log.d(TAG, "performViewSetup init Treatments for graph");
                    ListenerService.showTreatments(context, "all");
                }
                showAgoRawBattStatus();
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                showSteps();
                showHeartRate();
            }
        });
        Log.d(TAG, "performViewSetup requestData");
        ListenerService.requestData(this);
        wakeLock.acquire(50);
    }

    public void setSmallFontsize(boolean toggle) {
        int fontvalue = Integer.parseInt(sharedPrefs.getString("toggle_fontsize", "1"));
        fontvalue = toggle ? (fontvalue%smallFontsizeArray.length) + 1 : fontvalue;
        int fontsize =  Integer.parseInt(smallFontsizeArray[fontvalue-1]);
        if (d) Log.d(TAG, "setSmallFontsize fontsize=" + fontsize + " fontvalue=" + fontvalue);
        mDelta.setTextSize(fontsize);
        stepsButton.setTextSize(fontsize);
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
            if (d) Log.d(TAG, "onDraw");
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

    private String getWatchDate() {
        final Date now = new Date();
        final String currentWatchDate = mDate.getText().toString();
        final String newDate = new SimpleDateFormat("yyyyMMdd").format(now);
        final Locale locale = BaseWatchFace.this.getResources().getConfiguration().locale;
        if (!oldDate.equals(newDate) || currentWatchDate.equals("ddd mm/dd") || (oldLocale != locale)) {
            final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", locale);
            if (d)
                Log.d(TAG, "getWatchDate oldDate: " + oldDate + " now: " + now + " currentWatchDate: " + currentWatchDate);
            if (dateFormat == null || oldLocale != locale)
                dateFormat = getShortDateInstanceWithoutYear(locale);
            String shortDate = dateFormat.format(now);
            if (d)
                Log.d(TAG, "getWatchDate shortDate " + locale.getDisplayName() + ": " + shortDate + " pattern: " + dateFormat.toPattern());

            String day = dayFormat.format(now);
            if (d)
                Log.d(TAG, "getWatchDate day: " + day + " dayFormat: " + dayFormat.toPattern());
            oldDate = newDate;
            oldLocale = locale;
            return day + "\n" + shortDate;
        }
        else
            return currentWatchDate;
    }

    private SimpleDateFormat getShortDateInstanceWithoutYear(Locale locale) {
        SimpleDateFormat sdf = (SimpleDateFormat) java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale);
        if (d) Log.d(TAG, "getShortDateInstanceWithoutYear pattern " + locale.getDisplayName() + ": " + sdf.toPattern());
        sdf.applyPattern(sdf.toPattern().replaceAll("'[^']*", ""));//remove single quotes eg: Bulgarian: d.MM.yy 'г'
        if (d) Log.d(TAG, "getShortDateInstanceWithoutYear pattern: " + sdf.toPattern());
        sdf.applyPattern(sdf.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
        if (d) Log.d(TAG, "getShortDateInstanceWithoutYear pattern: " + sdf.toPattern());
        if (sdf instanceof SimpleDateFormat)
            return sdf;
        else
            return new SimpleDateFormat("mm/yy", locale);
    }

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        if (newTime.hasHourChanged(oldTime) || newTime.hasMinuteChanged(oldTime)) {
            if (layoutSet) {
                wakeLock.acquire(50);
                //final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(BaseWatchFace.this);
                final SimpleDateFormat timeFormat = new SimpleDateFormat(sharedPrefs.getBoolean("use24HourFormat", false) ? "HH:mm" : "h:mm a");
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
            Bundle bundle = intent.getExtras();
            String extra_status_line = bundle.getString("extra_status_line");
            if (layoutSet && bundle != null && extra_status_line != null && !extra_status_line.isEmpty()) {
                if (d) Log.d(TAG, "MessageReceiver extra_status_line=" + extra_status_line);
                mExtraStatusLine = extra_status_line;
            }
            bundle = intent.getBundleExtra("locale");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                String localeStr = dataMap.getString("locale", "");
                if (d) Log.d(TAG, "MessageReceiver locale=" + localeStr);
                String locale[] = localeStr.split("_");
                final Locale newLocale = locale == null ? new Locale(localeStr) : locale.length > 1 ? new Locale(locale[0], locale[1]) : new Locale(locale[0]);//eg"en", "en_AU"
                final Locale curLocale = Locale.getDefault();
                if (newLocale != null && !curLocale.equals(newLocale)) {
                    Locale.setDefault(newLocale);
                }
            }
            bundle = intent.getBundleExtra("msg");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                String msg = dataMap.getString("msg", "");
                int length = dataMap.getInt("length", 0);
                JoH.static_toast(xdrip.getAppContext(), msg, length);
            }
            bundle = intent.getBundleExtra("steps");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                if (mTimeStepsRcvd <= dataMap.getLong("steps_timestamp", 0)) {
                    mStepsCount = dataMap.getInt("steps", 0);
                    mTimeStepsRcvd = dataMap.getLong("steps_timestamp", 0);
                    showSteps();
                }

                if (mTimeHeartRcvd < dataMap.getLong("heart_rate_timestamp", 0)) {
                    mHeartBPM = dataMap.getInt("heart_rate", 0);
                    mTimeHeartRcvd = dataMap.getLong("heart_rate_timestamp", 0);
                    showHeartRate();
                }
            }
            bundle = intent.getBundleExtra(Home.HOME_FULL_WAKEUP);
            if (layoutSet && bundle != null) {
                String msg = bundle.getString(Home.HOME_FULL_WAKEUP);
                if (msg != null && !msg.isEmpty()) {
                    if (msg.equals("1")) {
                        mRelativeLayout.setKeepScreenOn(true);
                    }
                    else {
                        mRelativeLayout.setKeepScreenOn(false);
                    }
                }
            }
            if (sharedPrefs.getBoolean("show_wear_treatments", false)) {
                Bundle treatsbundle = intent.getBundleExtra("treats");
                if (layoutSet && treatsbundle != null) {
                    DataMap treatsdataMap = DataMap.fromBundle(treatsbundle);
                    if (d) Log.d(TAG, "MessageReceiver treatsDataList.size=" + (treatsDataList != null ? treatsDataList.size() : "0"));
                    if (treatsdataMap != null) addToWatchSetTreats(treatsdataMap, treatsDataList);
                    if (d) Log.d(TAG, "MessageReceiver treatsDataList.size=" + treatsDataList.size());
                }
                treatsbundle = intent.getBundleExtra("cals");
                if (layoutSet && treatsbundle != null) {
                    DataMap calDataMap = DataMap.fromBundle(treatsbundle);
                    if (d) Log.d(TAG, "MessageReceiver calDataList.size=" + (calDataList != null ? calDataList.size() : "0"));
                    if (calDataMap != null) addToWatchSetTreats(calDataMap, calDataList);
                    if (d) Log.d(TAG, "MessageReceiver calDataList.size=" + calDataList.size());
                }
                treatsbundle = intent.getBundleExtra("bts");
                if (layoutSet && treatsbundle != null) {
                    DataMap btDataMap = DataMap.fromBundle(treatsbundle);
                    if (d) Log.d(TAG, "MessageReceiver btDataList.size=" + (btDataList != null ? btDataList.size() : "0"));
                    if (btDataMap != null) addToWatchSetTreats(btDataMap, btDataList);
                    if (d) Log.d(TAG, "MessageReceiver btDataList.size=" + btDataList.size());
                }
            }
            else {
                clearTreatmentLists();
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
                if (d) Log.d(TAG, "onReceive batteryString=" + batteryString + " batteryLevel=" + batteryLevel + " xBattery=" + xBattery + " sgvString=" + sgvString);
                mSgv.setText(dataMap.getString("sgvString"));
                if(ageLevel()<=0) {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                //final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(BaseWatchFace.this);
                final SimpleDateFormat timeFormat = new SimpleDateFormat(sharedPrefs.getBoolean("use24HourFormat", false) ? "HH:mm" : "h:mm a");
                mTime.setText(timeFormat.format(System.currentTimeMillis()));

                mDirection.setText(dataMap.getString("slopeArrow"));
                avgDelta = dataMap.getString("avgDelta", "");
                delta = dataMap.getString("delta");

                extra_status_line = bundle.getString("extra_status_line");
                if (d) Log.d(TAG, "MessageReceiver DATA extra_status_line=" + extra_status_line);
                if (extra_status_line != null)
                    mExtraStatusLine = extra_status_line;

                showAgoRawBattStatus();


                if (chart != null) {
                    addToWatchSet(dataMap);
                    //setupCharts();
                }
                /*mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                invalidate();
                setColor();*/
            } else {
                Log.d(TAG, "ERROR: DATA IS NOT YET SET");
            }
            //status
            bundle = intent.getBundleExtra("status");
            if (layoutSet && bundle != null) {
                dataMap = DataMap.fromBundle(bundle);
                wakeLock.acquire(50);
                externalStatusString = dataMap.getString("externalStatusString");
                if (d) Log.d(TAG, "MessageReceiver externalStatusString=" + externalStatusString);

                showAgoRawBattStatus();

                /*mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                invalidate();
                setColor();*/
            }
            if (layoutSet) {
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                if (sharedPrefs.getBoolean("refresh_on_change", false))
                    invalidate();//to conserve battery, use onTimeChanged() default of one minute instead
                setColor();
            }
        }
    }

    private void clearTreatmentLists() {
        if (!sharedPrefs.getBoolean("show_wear_treatments", false)) {
            if (d)
                Log.d(TAG, "clearTreatmentLists show_wear_treatments = false; clear treatment lists");
            treatsDataList.clear();
            calDataList.clear();
            btDataList.clear();
        }
    }

    private void showSteps() {
        if (sharedPrefs.getBoolean("showSteps", false)) {
            stepsButton.setVisibility(View.VISIBLE);
            stepsButton.setText(String.format("%d", mStepsCount));
            if (mStepsCount > 0) {
                DecimalFormat df = new DecimalFormat("#.##");
                Double km = (((double) mStepsCount) / 2000.0d) * 1.6d;
                Double mi = (((double) mStepsCount) / 2000.0d) * 1.0d;
                if (d) Log.d(TAG, "showSteps Sensor mStepsCount=" + mStepsCount + " km=" + km + " mi=" + mi + " rcvd=" + JoH.dateTimeText(mTimeStepsRcvd));
                mStepsToast = getResources().getString(R.string.label_show_steps, mStepsCount) +
                        (km > 0.0 ? "\n" + getResources().getString(R.string.label_show_steps_km, df.format(km)) : "0") +
                        (mi > 0.0 ? "\n" + getResources().getString(R.string.label_show_steps_mi, df.format(mi)) : "0") +
                        "\n" + getResources().getString(R.string.label_show_steps_rcvdtime, JoH.dateTimeText(mTimeStepsRcvd));
            }
            else {
                mStepsToast = getResources().getString(R.string.label_show_steps, mStepsCount) +
                        ("\n" + getResources().getString(R.string.label_show_steps_km, "0")) +
                        ("\n" + getResources().getString(R.string.label_show_steps_mi, "0")) +
                        "\n" + getResources().getString(R.string.label_show_steps_rcvdtime, JoH.dateTimeText(mTimeStepsRcvd));
            }
        }
        else {
            stepsButton.setVisibility(View.GONE);
            mStepsToast = "";
            if (d) Log.d(TAG, "showSteps GONE mStepsCount = " + getResources().getString(R.string.label_show_steps, mStepsCount));
        }
    }

    private void showHeartRate() {
        if ((mHeartBPM > 0) && (JoH.msSince(mTimeHeartRcvd) <= HeartRateService.READING_PERIOD * 2)
                && (sharedPrefs.getBoolean("showHeartRate", false) && (heartButton != null))) {
            heartButton.setVisibility(View.VISIBLE);
            heartButton.setText(String.format("%d", mHeartBPM));
        } else {
            if (heartButton != null) heartButton.setVisibility(View.GONE);
        }
    }

    private void showAgoRawBattStatus() {

        String uploaderXBatteryText = "";
        String uploaderBatteryText = "";
        String timestampText = "";

        if (sharedPrefs.getBoolean("showDate", true)) {
            mDate.setVisibility(View.VISIBLE);
            mDate.setText(getWatchDate());
        } else {
            mDate.setVisibility(View.GONE);
        }

        mDelta.setText(delta);

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
        mShowXBattery = sharedPrefs.getBoolean("showBridgeBattery", false) && xBattery > 0 && DexCollectionType.hasBattery();
        if (d) Log.d(TAG, "showAgoRawBattStatus mShowXBattery=" + mShowXBattery + " xBattery=" + xBattery);
        if (mShowXBattery) {//see app/home.java displayCurrentInfo()
            //mUploaderXBattery.setText((showStatus ? "B: " : "Bridge: ") + xbatteryString + ((mXBattery < 200) ? "%" : "mV"));
            String xbatteryString = "" + xBattery;
            if (xBattery < 200) {
                uploaderXBatteryText = getResources().getString(R.string.label_show_bridge_battery_percent, xbatteryString);
                if (showStatus)
                    mUploaderXBattery.setText(getResources().getString(R.string.label_show_bridge_battery_percent_abbrv, xbatteryString));
                else
                    mUploaderXBattery.setText(uploaderXBatteryText);
            }
            else {
                uploaderXBatteryText = getResources().getString(R.string.label_show_bridge_battery_volt, xbatteryString);
                if (showStatus)
                    mUploaderXBattery.setText(getResources().getString(R.string.label_show_bridge_battery_volt_abbrv, xbatteryString));
                else
                    mUploaderXBattery.setText(uploaderXBatteryText);
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
        timestampText = readingAge(false);

        boolean enable_wearG5 = sharedPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = sharedPrefs.getBoolean("force_wearG5", false);
        boolean isCollectorRunning = (enable_wearG5 && force_wearG5);
        if (!isCollectorRunning && enable_wearG5) {
            Class<?> serviceClass = DexCollectionType.getCollectorServiceClass();
            if (serviceClass != null) {
                //Log.d(TAG, "DexCollectionType.getCollectorServiceClass(): " + serviceClass.getName());
                isCollectorRunning = isServiceRunning(serviceClass);
            }
        }
        if (isCollectorRunning) {//Wear Collector
            int wearBattery = getWearBatteryLevel(getApplication());
            String wearBatteryString = "" + wearBattery;
            Log.d(TAG, DexCollectionType.getDexCollectionType() + " is running. batteryString=" + batteryString + " wearBatteryString=" + wearBatteryString);
            batteryLevel = (wearBattery >= 30) ? 1 : 0;
            uploaderBatteryText = getResources().getString(R.string.label_show_uploader_wear, wearBatteryString);
            if (showStatus || mShowXBattery)
                mUploaderBattery.setText(getResources().getString(R.string.label_show_uploader_wear_abbrv, wearBatteryString));//"W: " + batteryString + "%"
            else
                mUploaderBattery.setText(uploaderBatteryText);//"Wear: " + batteryString + "%"
        }
        else {//Phone Collector
            Log.d(TAG, "Collector running on phone batteryString=" + batteryString);
            uploaderBatteryText = getResources().getString(R.string.label_show_uploader, batteryString);
            if (showStatus || mShowXBattery)
                mUploaderBattery.setText(getResources().getString(R.string.label_show_uploader_abbrv, batteryString));//"U: " + batteryString + "%"
            else
                mUploaderBattery.setText(uploaderBatteryText);//"Uploader: " + batteryString + "%"
        }

        if (showStatus) {
            mStatus.setVisibility(View.VISIBLE);
            mStatus.setText(getResources().getString(R.string.label_show_external_status_abbrv) + externalStatusString);//"S: "
        } else {
            mStatus.setVisibility(View.GONE);
        }
        mStatusLine = delta + "\n" + timestampText + "\n" + uploaderBatteryText +
                (mShowXBattery ? "\n" + uploaderXBatteryText : "") +
                (showStatus ? "\n" + mStatus.getText().toString() : "");
    }

    // Custom method to determine whether a service is running
    private boolean isServiceRunning(Class<?> serviceClass){//Class<?> serviceClass
        if (serviceClass != null) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // Loop through the running services
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                //if (d) Log.d(TAG, "isServiceRunning: getClassName=" + service.service.getClassName() + " getShortClassName=" + service.service.getShortClassName());
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


    @Override
    protected void onWatchModeChanged(WatchMode watchMode) {
        Log.d(TAG,"OnWatchModeChanged: "+watchMode);
        if(lowResMode ^ isLowRes(watchMode)){ //if there was a change in lowResMode
            lowResMode = isLowRes(watchMode);
            setColor();
        } else if (! sharedPrefs.getBoolean("dark", true)){
            //in bright mode: different colours if active:
            setColor();
        }
    }

    private boolean isLowRes(WatchMode watchMode) {
        return (watchMode == WatchMode.LOW_BIT)
                || (watchMode == WatchMode.LOW_BIT_BURN_IN)
                || (watchMode == WatchMode.BURN_IN && Pref.getBoolean("use_low_bit_mode", true));
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
        setColor();
        if(layoutSet){
            final SimpleDateFormat timeFormat = new SimpleDateFormat(sharedPrefs.getBoolean("use24HourFormat", false) ? "HH:mm" : "h:mm a");
            mTime.setText(timeFormat.format(System.currentTimeMillis()));
            clearTreatmentLists();
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
            clearTreatmentLists();
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
        if (d) Log.d(TAG, "resetRelativeLayout specW=" + specW + " specH=" + specH + " mRelativeLayout.getMeasuredWidth()=" + mRelativeLayout.getMeasuredWidth() + " mRelativeLayout.getMeasuredHeight()=" + mRelativeLayout.getMeasuredHeight());
    }

    private void displayCard() {
        int cardWidth = mCardRect.width();
        int cardHeight = mCardRect.height();
        if (d) Log.d(TAG, "displayCard WatchFace.onCardPeek: getWidth()=" + getWidth() + " getHeight()=" + getHeight() + " cardWidth=" + cardWidth + " cardHeight=" + cardHeight);

        if (cardHeight > 0 && cardWidth > 0) {
            if (getCurrentWatchMode() != WatchMode.INTERACTIVE) {
                // get height of visible area (not including card)
                int visibleWidth = getWidth() - cardWidth;
                int visibleHeight = getHeight() - cardHeight;
                if (d) Log.d(TAG, "onCardPeek WatchFace.onCardPeek: visibleWidth=" + visibleWidth + " visibleHeight=" + visibleHeight);
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
            if (d) Log.d(TAG, "onCardPeek WatchFace.onCardPeek: getWidth()=" + getWidth() + " getHeight()=" + getHeight() + " cardWidth=" + cardWidth + " cardHeight=" + cardHeight);
        }
    }

    protected abstract void setColorDark();
    protected abstract void setColorBright();
    protected abstract void setColorLowRes();

    public void missedReadingAlert() {
        int minutes_since   = (int) Math.floor(timeSince()/(1000*60));
        int maxDelay = 16;
        if (sharedPrefs.getBoolean("enable_wearG5", false)) {
            maxDelay = 5;
            Log.d(TAG, "missedReadingAlert Enter minutes_since " + minutes_since + " call requestData if >= 5 minutes mod 5");//KS
        }

        if (minutes_since >= maxDelay && ((minutes_since - maxDelay) % 5) == 0) {//KS TODO reduce time for debugging; add notifications
            /*NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Missed BG Readings")
                        .setVibrate(vibratePattern);
            NotificationManager mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            mNotifyMgr.notify(missed_readings_alert_id, notification.build());*/
            Log.d(TAG, "missedReadingAlert requestData");//KS
            ListenerService.requestData(this); // attempt to recover missing data
        }
    }

     public void addDataMapTreats(DataMap dataMap, ArrayList<BgWatchData> dataList) {//KS
        double sgv = dataMap.getDouble("sgvDouble");
        double high = dataMap.getDouble("high");//carbs
        double low = dataMap.getDouble("low");//insulin
        double timestamp = dataMap.getDouble("timestamp");

         if (d) Log.d(TAG, "addDataMapTreats entry=" + dataMap);

        final int size = (dataList != null ? dataList.size() : 0);
        BgWatchData bgdata = new BgWatchData(sgv, high, low, timestamp);
        if (d) Log.d(TAG, "addDataMapTreats bgdata.sgv=" + bgdata.sgv + " bgdata.carbs=" + bgdata.high  + " bgdata.insulin=" + bgdata.low + " bgdata.timestamp=" + bgdata.timestamp + " timestamp=" + JoH.dateTimeText((long)bgdata.timestamp));
        if (size > 0) {
            if (dataList.contains(bgdata)) {
                int i = dataList.indexOf(bgdata);
                if (d) {
                    BgWatchData data = dataList.get(dataList.indexOf(bgdata));
                    Log.d(TAG, "addDataMapTreats replace indexOf=" + i + " treatsDataList.carbs=" + data.high + " treatsDataList.insulin=" + data.low + " treatsDataList.timestamp=" + data.timestamp);
                }
                dataList.set(i, bgdata);
            } else {
                if (d) Log.d(TAG, "addDataMapTreats add " + " treatsDataList.carbs=" + bgdata.high  + " treatsDataList.insulin=" + bgdata.low + " entry.timestamp=" + bgdata.timestamp);
                dataList.add(bgdata);
            }
        }
        else {
            dataList.add(bgdata);
        }
         if (d) Log.d(TAG, "addDataMapTreats dataList.size()=" + dataList.size());
    }
    public void addToWatchSetTreats(DataMap dataMap, ArrayList<BgWatchData> dataList) {

        if (d) Log.d(TAG, "addToWatchSetTreats dataList.size()=" + (dataList != null ? dataList.size() : "0"));
        dataList.clear();//necessary since treatments, bloodtest and calibrations can be deleted/invalidated
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            if (d) Log.d(TAG, "addToWatchSetTreats entries.size()=" + entries.size() + " entries=" + entries);
            for (DataMap entry : entries) {
                addDataMapTreats(entry, dataList);
            }
        }

        if (d) Log.d(TAG, "addToWatchSetTreats dataList.size()=" + dataList.size());
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
                Log.d(TAG, "addToWatchSet Remove bgDataList.get(i).timestamp: " + JoH.dateTimeText((long)bgDataList.get(i).timestamp)+ " i: " + i);
                bgDataList.remove(i); //Get rid of anything more than 5 hours old
            }
        }
    }

    public void setupCharts() {
        Log.d(TAG, "setupCharts bgDataList.size()=" + bgDataList.size());
        if(bgDataList.size() > 0) { //Dont crash things just because we dont have values, people dont like crashy things
            int timeframe = Integer.parseInt(sharedPrefs.getString("chart_timeframe", "5"));
            boolean doMgdl = (sharedPrefs.getString("units", "mgdl").equals("mgdl"));
            if (lowResMode) {
                bgGraphBuilder = new BgGraphBuilder(getApplicationContext(), bgDataList, treatsDataList, calDataList, btDataList, pointSize, midColor, timeframe, doMgdl, sharedPrefs.getBoolean("use24HourFormat", false));
            } else {
                bgGraphBuilder = new BgGraphBuilder(getApplicationContext(), bgDataList, treatsDataList, calDataList, btDataList, pointSize, highColor, lowColor, midColor, timeframe, doMgdl, sharedPrefs.getBoolean("use24HourFormat", false));
            }

            chart.setLineChartData(bgGraphBuilder.lineData());
            //chart.setOnValueTouchListener(bgGraphBuilder.getOnValueSelectTooltipListener(mActivity));//TODO
            chart.setViewportCalculationEnabled(true);
            chart.setMaximumViewport(chart.getMaximumViewport());
        } else if (!Home.get_forced_wear()){
            Log.d(TAG, "setupCharts requestData");
            ListenerService.requestData(this);
        }
    }
}