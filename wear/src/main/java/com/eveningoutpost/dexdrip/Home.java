package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.ustwo.clockwise.WatchMode;

import lecho.lib.hellocharts.util.ChartUtils;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

public class Home extends BaseWatchFace {
    //KS the following were copied from app/home
    private static Context context;//KS
    private static final String TAG = "wearHome";//KS
    private static String nexttoast;//KS
    private static boolean is_follower = false;
    private static boolean is_follower_set = false;
    private static SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        //KS copied from app/Home
        Home.context = getApplicationContext();
        set_is_follower(); // not sure if we actually need this and associated logic? (jamorham)
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_home, null);
        performViewSetup();
    }

    protected void setColorDark() {
        Log.d("setColorDark", "WatchMode=" + getCurrentWatchMode());
        mTime.setTextColor(Color.WHITE);
        mRelativeLayout.setBackgroundColor(Color.BLACK);
        mLinearLayout.setBackgroundColor(Color.WHITE);
        if (sgvLevel == 1) {
            mSgv.setTextColor(Color.YELLOW);
            mDirection.setTextColor(Color.YELLOW);
            mDelta.setTextColor(Color.YELLOW);
        } else if (sgvLevel == 0) {
            mSgv.setTextColor(Color.WHITE);
            mDirection.setTextColor(Color.WHITE);
            mDelta.setTextColor(Color.WHITE);
        } else if (sgvLevel == -1) {
            mSgv.setTextColor(lowColorWatchMode);
            mDirection.setTextColor(lowColorWatchMode);
            mDelta.setTextColor(lowColorWatchMode);
        }
        if (ageLevel == 1) {
            mTimestamp.setTextColor(Color.BLACK);
        } else {
            mTimestamp.setTextColor(Color.RED);
        }

        if (batteryLevel == 1) {
            mUploaderBattery.setTextColor(Color.BLACK);
        } else {
            mUploaderBattery.setTextColor(Color.RED);
        }
        mRaw.setTextColor(Color.BLACK);
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
            mLinearLayout.setBackgroundColor(Color.BLACK);
            if (sgvLevel == 1) {
                mSgv.setTextColor(ChartUtils.COLOR_ORANGE);
                mDirection.setTextColor(ChartUtils.COLOR_ORANGE);
                mDelta.setTextColor(ChartUtils.COLOR_ORANGE);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.BLACK);
                mDirection.setTextColor(Color.BLACK);
                mDelta.setTextColor(Color.BLACK);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(Color.RED);
                mDirection.setTextColor(Color.RED);
                mDelta.setTextColor(Color.RED);
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
            mRaw.setTextColor(Color.WHITE);
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
            mRelativeLayout.setBackgroundColor(Color.BLACK);
            mLinearLayout.setBackgroundColor(Color.WHITE);
            if (sgvLevel == 1) {
                mSgv.setTextColor(Color.YELLOW);
                mDirection.setTextColor(Color.YELLOW);
                mDelta.setTextColor(Color.YELLOW);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.WHITE);
                mDirection.setTextColor(Color.WHITE);
                mDelta.setTextColor(Color.WHITE);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(lowColorWatchMode);
                mDirection.setTextColor(lowColorWatchMode);
                mDelta.setTextColor(lowColorWatchMode);
            }
            mRaw.setTextColor(Color.BLACK);
            mUploaderBattery.setTextColor(Color.BLACK);
            mTimestamp.setTextColor(Color.BLACK);

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

    public static Context getAppContext() {
        return Home.context;
    }//KS from app / xdrip.java

    public static void setAppContext(Context context) {
        Home.context = context;
    }//KS

    //KS Toast Messages from app / Home
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
        is_follower = PreferenceManager.getDefaultSharedPreferences(Home.getAppContext()).getString("dex_collection_method", "").equals("Follower");
        is_follower_set = true;
    }

    public static boolean get_follower() {
        if (!is_follower_set) set_is_follower();
        return Home.is_follower;
    }

    public static long getPreferencesLong(final String pref, final long def) {
        if ((prefs == null) && (Home.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(Home.getAppContext());
        }
        if (prefs != null) {
            return prefs.getLong(pref, def);
        }
        return def;
    }

    public static boolean getPreferencesBooleanDefaultFalse(final String pref) {
        if ((prefs == null) && (Home.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(Home.getAppContext());
        }
        if ((prefs != null) && (prefs.getBoolean(pref, false))) {
            return true;
        }
        return false;
    }

    public static String getPreferencesStringWithDefault(final String pref, final String def) {
        if ((prefs == null) && (Home.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(Home.getAppContext());
        }
        if (prefs != null) {
            return prefs.getString(pref, def);
        }
        return "";
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
        if ((prefs == null) && (Home.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(Home.getAppContext());
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

