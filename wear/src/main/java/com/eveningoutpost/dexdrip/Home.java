package com.eveningoutpost.dexdrip;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;

import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.ustwo.clockwise.common.WatchMode;

public class Home extends BaseWatchFace {
    //KS the following were copied from app/home
    private static Context context;//KS
    private static final String TAG = "jamorham: " + Home.class.getSimpleName();
    public final static String SHOW_NOTIFICATION = "SHOW_NOTIFICATION";
    public final static String HOME_FULL_WAKEUP = "HOME_FULL_WAKEUP";
    public final static String ACTIVITY_SHOWCASE_INFO = "ACTIVITY_SHOWCASE_INFO";
    public final static String ENABLE_STREAMING_DIALOG = "ENABLE_STREAMING_DIALOG";
    public static final int SHOWCASE_MOTION_DETECTION = 11;
    private static String nexttoast;//KS
    private static boolean is_follower = false;
    private static boolean is_follower_set = false;
    private long chartTapTime = 0l;
    private long fontsizeTapTime = 0l;

    @Override
    public void onCreate() {
        super.onCreate();

        //KS copied from app/Home
        Home.context = getApplicationContext();
        xdrip.checkAppContext(getApplicationContext());
        set_is_follower(); // not sure if we actually need this and associated logic? (jamorham)

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_home, null);
        performViewSetup();
        //checkBatteryOptimization();
    }

    @Override
    protected void onTapCommand(int tapType, int x, int y, long eventTime) {

        if (tapType == TAP_TYPE_TAP&&
                x >=chart.getLeft() &&
                x <= chart.getRight()&&
                y >= chart.getTop() &&
                y <= chart.getBottom()){
            if (eventTime - chartTapTime < 800){
                changeChartTimeframe();
            }
            chartTapTime = eventTime;
        }
        if (tapType == TAP_TYPE_TAP && linearLayout(mDirectionDelta, x, y)) {
            if (eventTime - fontsizeTapTime < 800) {
                setSmallFontsize(true);
            }
            fontsizeTapTime = eventTime;
        }
        if (sharedPrefs.getBoolean("show_toasts", true)) {
            if (tapType == TAP_TYPE_TOUCH && linearLayout(mLinearLayout, x, y)) {
                JoH.static_toast_short(mStatusLine);
            }
            if (tapType == TAP_TYPE_TOUCH && linearLayout(mStepsLinearLayout, x, y)) {
                if (sharedPrefs.getBoolean("showSteps", false) && mStepsCount > 0) {
                    JoH.static_toast_long(mStepsToast);
                }
            }
            if (tapType == TAP_TYPE_TOUCH && linearLayout(mDirectionDelta, x, y)) {
                if (sharedPrefs.getBoolean("extra_status_line", false) && mExtraStatusLine != null && !mExtraStatusLine.isEmpty()) {
                    JoH.static_toast_long(mExtraStatusLine);
                }
            }
        }
        if (tapType == TAP_TYPE_TOUCH && linearLayout(mMenuLinearLayout, x, y)) {
            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(intent);
        }
    }

    private boolean linearLayout(LinearLayout layout,int x, int y) {
        if (x >=layout.getLeft() && x <= layout.getRight()&&
            y >= layout.getTop() && y <= layout.getBottom()) {
            return true;
        }
        return false;
    }

    private void changeChartTimeframe() {
        int timeframe = Integer.parseInt(sharedPrefs.getString("chart_timeframe", "3"));
        Log.e(TAG, "changeChartTimeframe timeframe: " + timeframe);
        timeframe = (timeframe%5) + 1;
        sharedPrefs.edit().putString("chart_timeframe", "" + timeframe).commit();
    }

    @Override
    protected WatchFaceStyle getWatchFaceStyle(){
        return new WatchFaceStyle.Builder(this)
                .setAcceptsTapEvents(true)
                .setHotwordIndicatorGravity(Gravity.START | -20)
                .setStatusBarGravity(Gravity.END | -20)
                .build();
    }

    @Override
    protected void setColorDark() {
        final boolean matchingDividerBar = Pref.getBooleanDefaultFalse("use_black_divider");
        try {
          //mLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_statusView));
            mTime.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mDate.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mRelativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_background));
            if (sgvLevel == 1) {
                mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_highColor));
                mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_highColor));
                mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_highColor));
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
                mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
                mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_lowColor));
                mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_lowColor));
                mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_lowColor));
            }

            if (ageLevel == 1) {
                mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTimestamp1_home));
            } else {
                mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_TimestampOld));
            }

            if (batteryLevel == 1) {
                mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBattery));
            } else {
                mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBatteryEmpty));
            }
            if (mXBatteryLevel == 1) {
                mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBattery));
            } else {
                mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBatteryEmpty));
            }

            mStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mStatus_home));

            mLinearLayout.setBackgroundColor(matchingDividerBar ? Color.BLACK : Color.WHITE);

            mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));
            mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));
            mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));
            mStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));


            if (chart != null) {
                highColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_highColor);
                lowColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_lowColor);
                midColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                pointSize = 2;
                setupCharts();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception in setColorDark in Home: " + e);
        }
    }

    @Override
    protected void setColorLowRes() {
        try {
            mTime.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mDate.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mRelativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_background));
            mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            mStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
            if (chart != null) {
                highColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                lowColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                midColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                pointSize = 2;
                setupCharts();
            }

            mLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_background));


        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception in setColorLowRes: " + e);
        }

    }

    @Override
    protected void setColorBright() {
        try {

            final boolean matchingDividerBar = !Pref.getBooleanDefaultFalse("use_black_divider");
            if (getCurrentWatchMode() == WatchMode.INTERACTIVE) {
                mLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_stripe_background));
                mRelativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_background));
                if (sgvLevel == 1) {
                    mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_highColor));
                    mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_highColor));
                    mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_highColor));
                } else if (sgvLevel == 0) {
                    mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_midColor));
                    mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_midColor));
                    mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_midColor));
                } else if (sgvLevel == -1) {
                    mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_lowColor));
                    mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_lowColor));
                    mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.light_lowColor));
                }

                if (ageLevel == 1) {
                    mTimestamp.setTextColor(Color.WHITE);
                } else {
                    mTimestamp.setTextColor(Color.RED);
                }

                if (batteryLevel == 1) {
                    mUploaderBattery.setTextColor(Color.WHITE);
                } else {
                    mUploaderBattery.setTextColor(Color.RED);
                }
                if (mXBatteryLevel == 1) {
                    mUploaderXBattery.setTextColor(Color.WHITE);
                } else {
                    mUploaderXBattery.setTextColor(Color.RED);
                }

                mStatus.setTextColor(Color.WHITE);

                mTime.setTextColor(Color.BLACK);
                mDate.setTextColor(Color.BLACK);

                mLinearLayout.setBackgroundColor(Color.BLACK);


                mLinearLayout.setBackgroundColor(matchingDividerBar ? Color.BLACK : Color.WHITE);

                mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));
                mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));
                mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));
                mStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), matchingDividerBar ? R.color.dark_mTime : R.color.dark_mStatus_home));

                if (chart != null) {
                    highColor = ContextCompat.getColor(getApplicationContext(), R.color.light_highColor);
                    lowColor = ContextCompat.getColor(getApplicationContext(), R.color.light_lowColor);
                    midColor = ContextCompat.getColor(getApplicationContext(), R.color.light_midColor);
                    pointSize = 2;
                    setupCharts();
                }
            } else {
                setColorDark();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception in setColorBright in home " + e);
        }
    }

    //KS from app / Home
    public static Context getAppContext() {
        return Home.context;
    }//KS from app / xdrip.java

    public static void setAppContext(Context context) {//KS
        Home.context = context;
    }

    public static void startHomeWithExtra(Context context, String extra, String text) {
        startHomeWithExtra(context, extra, text, "");
    }

    public static void startHomeWithExtra(Context context, String extra, String text, String even_more) {
        /*Intent intent = new Intent(context, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(extra, text);
        intent.putExtra(extra + "2", even_more);
        context.startActivity(intent);*/

        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra(extra, text);
        Log.d(TAG, "startHomeWithExtra extra=" + extra + " text=" + text);
        LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(messageIntent);
    }

    public static void staticBlockUI(Context context, boolean state) {
        // stub placeholder
    }

    public static void toaststatic(final String msg) {
        nexttoast = msg;
        //KS staticRefreshBGCharts();
        toastStaticFromUI(msg);//KS
    }

    public static void toaststaticnext(final String msg) {
        nexttoast = msg;
        Log.e(TAG, "Toast next: " + msg);
    }

    public void toast(final String msg) {
        try {
            Context context = getApplicationContext();
            Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            toast.show();
            Log.d(TAG, "toast: " + msg);
        } catch (Exception e) {
            Log.d(TAG, "Couldn't display toast: " + msg + " / " + e.toString());
        }
    }

    public static void toastStaticFromUI(final String msg) {
        try {
            Toast.makeText(Home.context, msg, Toast.LENGTH_LONG).show();//mActivity
            Log.d(TAG, "toast: " + msg);
        } catch (Exception e) {
            toaststaticnext(msg);
            Log.d(TAG, "Couldn't display toast (rescheduling): " + msg + " / " + e.toString());
        }
    }

    private static void set_is_follower() {
        is_follower = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).getString("dex_collection_method", "").equals("Follower");
        is_follower_set = true;
    }

    public static boolean get_follower() {
        if (!is_follower_set) set_is_follower();
        return Home.is_follower;
    }

    public static boolean get_engineering_mode() {
        return Pref.getBooleanDefaultFalse("engineering_mode");
    }

    public static boolean get_forced_wear() {
        return Pref.getBooleanDefaultFalse("enable_wearG5") &&
                Pref.getBooleanDefaultFalse("force_wearG5");
    }



    public static double convertToMgDlIfMmol(double value) {
        if (!Pref.getString("units", "mgdl").equals("mgdl")) {
            return value * com.eveningoutpost.dexdrip.utilitymodels.Constants.MMOLL_TO_MGDL;
        } else {
            return value; // no conversion needed
        }
    }


    public static long stale_data_millis()
    {
        if (DexCollectionType.getDexCollectionType() == DexCollectionType.LibreAlarm) return (60000 * 13);
        if (DexCollectionType.getDexCollectionType() == DexCollectionType.DexcomG5 &&
            Pref.getBooleanDefaultFalse("engineering_mode")) return (60000 * 5);
        return (60000 * 11);
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String packageName = getPackageName();
            //Log.d(TAG, "Maybe ignoring battery optimization");
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                // &&
                //            !prefs.getBoolean("requested_ignore_battery_optimizations_new", false)) {
                Log.d(TAG, "Requesting ignore battery optimization");

                // if (PersistentStore.incrementLong("asked_battery_optimization") < 40) {
                // JoH.show_ok_dialog(this, gs(R.string.please_allow_permission), gs(R.string.xdrip_needs_whitelisting_for_proper_performance), new Runnable() {

                //     @Override
                //    public void run() {
                try {
                    final Intent intent = new Intent();

                    // ignoring battery optimizations required for constant connection
                    // to peripheral device - eg CGM transmitter.
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);

                } catch (ActivityNotFoundException e) {
                    final String msg = "Device does not appear to support battery optimization whitelisting!";
                    JoH.static_toast_short(msg);
                    UserError.Log.wtf(TAG, msg);
                }
                //      }
                //     });
            } else {
                JoH.static_toast_long("This app needs battery optimization whitelisting or it will not work well. Please reset app preferences");
            }
        }
    }
    // just for code compatibility
    public static boolean get_master() {
        return false;
    }

    public static void staticRefreshBGCharts() {
    }
}



