package com.eveningoutpost.dexdrip;


import android.app.Activity;
import android.content.Context;

// jamorham

public abstract class BaseActivity extends Activity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
