package com.eveningoutpost.dexdrip;

// jamorham

import android.content.Context;
import android.preference.PreferenceActivity;

public abstract class BasePreferenceActivity extends PreferenceActivity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
