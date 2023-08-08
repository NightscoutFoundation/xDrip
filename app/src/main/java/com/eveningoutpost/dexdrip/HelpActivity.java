package com.eveningoutpost.dexdrip;

import android.os.Bundle;
import android.webkit.WebView;

import com.eveningoutpost.dexdrip.models.JoH;

public class HelpActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        JoH.fixActionBar(this);
        WebView webview = (WebView)findViewById(R.id.helpWebView);


    }
}
