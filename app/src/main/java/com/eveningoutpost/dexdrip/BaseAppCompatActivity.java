package com.eveningoutpost.dexdrip;


import android.content.Context;
import android.support.v7.app.AppCompatActivity;

// jamorham

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
