package com.eveningoutpost.dexdrip;


import android.content.Context;
import android.preference.PreferenceActivity;

// jamorham

public abstract class BasePreferenceActivity extends PreferenceActivity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
