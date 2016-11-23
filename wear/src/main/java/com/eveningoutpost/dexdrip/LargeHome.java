package com.eveningoutpost.dexdrip;

import android.graphics.Color;
import android.view.LayoutInflater;

import com.ustwo.clockwise.WatchMode;

public class LargeHome extends BaseWatchFace {

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_home_large, null);
        performViewSetup();
    }


    @Override
    protected void setColorDark(){
        //Log.d("setColorDark", "WatchMode=" + getCurrentWatchMode());
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
    }



    @Override
    protected void setColorBright() {
        if (getCurrentWatchMode() == WatchMode.INTERACTIVE) {
            mRelativeLayout.setBackgroundColor(Color.WHITE);
            mLinearLayout.setBackgroundColor(Color.BLACK);
            if (sgvLevel == 1) {
                mSgv.setTextColor(Color.YELLOW);
                mDirection.setTextColor(Color.YELLOW);
                mDelta.setTextColor(Color.YELLOW);
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
        } else {
            //RED is not supported in Ambient mode on WatchMode=LOW_BIT sa Sony SmartWatch 3
            //Therefore, use a cold color to indicate a low value
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
                mSgv.setTextColor(lowColorWatchMode);
                mDirection.setTextColor(lowColorWatchMode);
                mDelta.setTextColor(lowColorWatchMode);
            }

            mUploaderBattery.setTextColor(Color.BLACK);
            mTimestamp.setTextColor(Color.BLACK);
            mRaw.setTextColor(Color.BLACK);
            mTime.setTextColor(Color.WHITE);
        }
    }
}
