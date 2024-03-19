package com.eveningoutpost.dexdrip;

// jamorham

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;


public abstract class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
