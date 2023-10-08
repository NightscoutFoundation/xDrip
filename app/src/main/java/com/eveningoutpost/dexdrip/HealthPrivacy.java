package com.eveningoutpost.dexdrip;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.eveningoutpost.dexdrip.models.JoH;

// jamorham

public class HealthPrivacy extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_privacy);
        JoH.fixActionBar(this);
    }

    public void healthPrivacyClose(View view) {
        finish();
    }
}