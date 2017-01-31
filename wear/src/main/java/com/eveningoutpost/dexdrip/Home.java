package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.ustwo.clockwise.common.WatchMode;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

public class Home extends BaseWatchFace {
    //KS the following were copied from app/home
    private static Context context;//KS
    private static final String TAG = "jamorham: " + Home.class.getSimpleName();
    public final static String SHOW_NOTIFICATION = "SHOW_NOTIFICATION";
    public final static String HOME_FULL_WAKEUP = "HOME_FULL_WAKEUP";
    public final static String ACTIVITY_SHOWCASE_INFO = "ACTIVITY_SHOWCASE_INFO";
    public static final int SHOWCASE_MOTION_DETECTION = 11;
    private static String nexttoast;//KS
    private static boolean is_follower = false;
    private static boolean is_follower_set = false;
    private static SharedPreferences prefs;
    private long chartTapTime = 0l;
    private long fontsizeTapTime = 0l;

    @Override
    public void onCreate() {
        super.onCreate();

        //KS copied from app/Home
        Home.context = getApplicationContext();
        xdrip.checkAppContext(getApplicationContext());
        set_is_follower(); // not sure if we actually need this and associated logic? (jamorham)
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_home, null);
        performViewSetup();
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
        if (tapType == TAP_TYPE_TAP&&
                ((x >=mDirectionDelta.getLeft() &&
                x <= mDirectionDelta.getRight()&&
                y >= mDirectionDelta.getTop() &&
                y <= mDirectionDelta.getBottom()) )) {//||
                 //(x >=mLinearLayout.getLeft() &&
                  //x <= mLinearLayout.getRight()&&
                  //y >= mLinearLayout.getTop() &&
                  //y <= mLinearLayout.getBottom()) )) {
            if (eventTime - fontsizeTapTime < 800) {
                setSmallFontsize(true);
            }
            fontsizeTapTime = eventTime;
        }
        if (tapType == TAP_TYPE_TOUCH && statusArea(x, y)) {
            JoH.static_toast_short(mStatusLine);
        }
    }

    private boolean statusArea(int x, int y) {
        if (((x >=mDirectionDelta.getLeft() &&
                        //x <= mDirectionDelta.getRight()&&
                        //y >= mDirectionDelta.getTop() &&
                        //y <= mDirectionDelta.getBottom()) ||
                        (x >=mLinearLayout.getLeft() &&
                                x <= mLinearLayout.getRight()&&
                                y >= mLinearLayout.getTop() &&
                                y <= mLinearLayout.getBottom()) )) ) {
            return true;
        }
        return false;
    }

    private void changeChartTimeframe() {
        int timeframe = Integer.parseInt(sharedPrefs.getString("chart_timeframe", "3"));
        timeframe = (timeframe%5) + 1;
        sharedPrefs.edit().putString("chart_timeframe", "" + timeframe).commit();
    }

    @Override
    protected WatchFaceStyle getWatchFaceStyle(){
        return new WatchFaceStyle.Builder(this).setAcceptsTapEvents(true).build();
    }

    @Override
    protected void setColorDark() {
        try {
            mLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_statusView));
            mTime.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
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
            mRelativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_background));
            mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
            mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTimestamp1_home));
            mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBattery));
            mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBattery));
            mStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mStatus_home));
            if (chart != null) {
                highColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                lowColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                midColor = ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor);
                pointSize = 2;
                setupCharts();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception in setColorLowRes: " + e);
        }

    }

    @Override
    protected void setColorBright() {
        try {
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
        return Home.getPreferencesBooleanDefaultFalse("engineering_mode");
    }

    public static boolean getPreferencesBoolean(final String pref, boolean def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if ((prefs != null) && (prefs.getBoolean(pref, def))) return true;
        return false;
    }

    public static long getPreferencesLong(final String pref, final long def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getLong(pref, def);
        }
        return def;
    }

    public static boolean getPreferencesBooleanDefaultFalse(final String pref) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if ((prefs != null) && (prefs.getBoolean(pref, false))) {
            return true;
        }
        return false;
    }

    public static String getPreferencesStringDefaultBlank(final String pref) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getString(pref, "");
        }
        return "";
    }

    public static String getPreferencesStringWithDefault(final String pref, final String def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getString(pref, def);
        }
        return "";
    }

    public static int getPreferencesInt(final String pref, final int def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getInt(pref, def);
        }
        return def;
    }

    public static boolean setPreferencesBoolean(final String pref, final boolean lng) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putBoolean(pref, lng).apply();
            return true;
        }
        return false;
    }

    public static boolean setPreferencesInt(final String pref, final int num) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putInt(pref, num).apply();
            return true;
        }
        return false;
    }

    public static boolean setPreferencesString(final String pref, final String str) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putString(pref, str).apply();
            return true;
        }
        return false;
    }

    public static double convertToMgDlIfMmol(double value) {
        if (!getPreferencesStringWithDefault("units", "mgdl").equals("mgdl")) {
            return value * com.eveningoutpost.dexdrip.UtilityModels.Constants.MMOLL_TO_MGDL;
        } else {
            return value; // no conversion needed
        }
    }

    public static boolean setPreferencesLong(final String pref, final long lng) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putLong(pref, lng).apply();
            return true;
        }
        return false;
    }

    public static long stale_data_millis()
    {
        if (DexCollectionType.getDexCollectionType() == DexCollectionType.LibreAlarm) return (60000 * 13);
        return (60000 * 11);
    }
}

