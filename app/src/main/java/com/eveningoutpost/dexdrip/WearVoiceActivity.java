package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class WearVoiceActivity extends BaseActivity {

    final static boolean debug = true;
    final static String TAG = "jamorham wearvoice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_voice);

        Intent intent = this.getIntent();
        if (intent != null)
        {
            final Bundle bundle = intent.getExtras();
            //  BundleScrubber.scrub(bundle);
            final String action = intent.getAction();


            if ((bundle != null) && (debug)) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    if (value != null) {
                        Log.d(TAG, String.format("%s %s (%s)", key,
                                value.toString(), value.getClass().getName()));
                    }
                }
            }
        }

    }
}
