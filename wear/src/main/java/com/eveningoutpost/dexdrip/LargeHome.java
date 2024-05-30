package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.graphics.Color;

import android.support.wearable.watchface.WatchFaceStyle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.models.JoH;
import com.ustwo.clockwise.common.WatchMode;

public class LargeHome extends BaseWatchFace {
    private static final String TAG = "jamorham: " + Home.class.getSimpleName();
    private long fontsizeTapTime = 0l;

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_home_large, null);
        performViewSetup();
    }

    @Override
    protected void onTapCommand(int tapType, int x, int y, long eventTime) {

        if (tapType == TAP_TYPE_TAP &&
                ((x >= mDirectionDelta.getLeft() &&
                        x <= mDirectionDelta.getRight() &&
                        y >= mDirectionDelta.getTop() &&
                        y <= mDirectionDelta.getBottom()))) {//||
            if (eventTime - fontsizeTapTime < 800) {
                setSmallFontsize(true);
            }
            fontsizeTapTime = eventTime;
        }
        if (sharedPrefs.getBoolean("show_toasts", true)) {
            if (tapType == TAP_TYPE_TOUCH && x >= mDirectionDelta.getLeft() && linearLayout(mLinearLayout, x, y)) {
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

    private boolean linearLayout(LinearLayout layout, int x, int y) {
        if (x >=layout.getLeft() && x <= layout.getRight()&&
                y >= layout.getTop() && y <= layout.getBottom()) {
            return true;
        }
        return false;
    }

    @Override
    protected WatchFaceStyle getWatchFaceStyle(){
        //return new WatchFaceStyle.Builder(this).setAcceptsTapEvents(true).build();
        return new WatchFaceStyle.Builder(this)
                .setAcceptsTapEvents(true)
                .setHotwordIndicatorGravity(Gravity.CENTER | Gravity.TOP)
                .setStatusBarGravity(Gravity.END | -20)
                .build();
    }

    @Override
    protected void setColorDark(){
        mLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mLinearLayout));
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
    }

    @Override
    protected void setColorBright() {
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
            if (batteryLevel == 1) {
                mUploaderXBattery.setTextColor(Color.WHITE);
            } else {
                mUploaderXBattery.setTextColor(Color.RED);
            }
            mStatus.setTextColor(Color.WHITE);
            mTime.setTextColor(Color.BLACK);
            mDate.setTextColor(Color.BLACK);
        } else {
            mRelativeLayout.setBackgroundColor(Color.BLACK);
            mLinearLayout.setBackgroundColor(Color.LTGRAY);
            if (sgvLevel == 1) {
                mSgv.setTextColor(Color.YELLOW);
                mDirection.setTextColor(Color.YELLOW);
                mDelta.setTextColor(Color.YELLOW);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.WHITE);
                mDirection.setTextColor(Color.WHITE);
                mDelta.setTextColor(Color.WHITE);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(Color.RED);
                mDirection.setTextColor(Color.RED);
                mDelta.setTextColor(Color.RED);
            }

            mUploaderBattery.setTextColor(Color.BLACK);
            mTimestamp.setTextColor(Color.BLACK);
            mStatus.setTextColor(Color.BLACK);
            mTime.setTextColor(Color.WHITE);
            mDate.setTextColor(Color.WHITE);
        }
    }

    @Override
    protected void setColorLowRes() {
        mLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mLinearLayout));
        mTime.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
        mDate.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTime));
        mRelativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_background));
        mSgv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
        mDelta.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
        mDirection.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_midColor));
        mTimestamp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mTimestamp1_home));
        mUploaderBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBattery));
        mUploaderXBattery.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_uploaderBattery));
        mStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_mStatus_home));
    }
}
