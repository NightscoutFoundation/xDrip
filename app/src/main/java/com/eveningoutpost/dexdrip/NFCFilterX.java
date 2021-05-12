package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Created by jamorham on 10/09/2016.
 * <p>
 * Triggered from homescreen
 */
public class NFCFilterX extends BaseActivity {

    private View decorView;
    private final static String TAG = "NFCFilterX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcscan);
        decorView = getWindow().getDecorView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        this.setIntent(intent);

        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return;
        }
        NFCReaderX.scanFromActivity(this, intent);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        NFCReaderX.windowFocusChange(this, hasFocus, decorView);
    }
}