package com.eveningoutpost.dexdrip;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.eveningoutpost.dexdrip.Models.JoH;

public class HelpActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        JoH.fixActionBar(this);
        WebView webview = (WebView)findViewById(R.id.helpWebView);


    }
}
