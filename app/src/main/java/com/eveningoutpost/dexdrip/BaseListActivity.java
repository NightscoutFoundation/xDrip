package com.eveningoutpost.dexdrip;


import android.app.ListActivity;
import android.content.Context;

// jamorham

public abstract class BaseListActivity extends ListActivity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
