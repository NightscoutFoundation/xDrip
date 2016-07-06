package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.util.concurrent.TimeUnit;

public class SendFeedBack extends AppCompatActivity {

    private  String send_url;
    private final String TAG = "jamorham feedback";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feed_back);
        send_url = getString(R.string.wserviceurl)+"/joh-feedback";

        Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final String str = bundle.getString("request_translation");
                if (str != null) {
                    // don't extract string - english only
                    ((EditText) findViewById(R.id.yourText)).setText("Dear developers, please may I request that you add translation capability for: "+str+"\n\n");

                }
            }
        }

    }

    public void closeActivity(View myview) {
        finish();
    }

    public void sendFeedback(View myview) {

        final EditText contact = (EditText) findViewById(R.id.contactText);
        final EditText yourtext = (EditText) findViewById(R.id.yourText);
        final RatingBar myrating = (RatingBar) findViewById(R.id.ratingBar);
        final OkHttpClient client = new OkHttpClient();

        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(20, TimeUnit.SECONDS);
        client.setWriteTimeout(20, TimeUnit.SECONDS);

        if (yourtext.length() == 0) {
            toast("No text entered - cannot send blank");
            return;
        }
        toast("Sending..");

        try {
            final RequestBody formBody = new FormEncodingBuilder()
                    .add("contact", contact.getText().toString())
                    .add("body", yourtext.getText().toString())
                    .add("rating", String.valueOf(myrating.getRating()))
                    .build();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final Request request = new Request.Builder()
                                .url(send_url)
                                .post(formBody)
                                .build();
                        Log.i(TAG, "Sending feedback request");
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            Home.toaststatic("Feedback sent successfully");
                            finish();
                        } else {
                            toast("Error sending feedback: " + response.message().toString());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in execute: " + e.toString());
                       toast("Error with network connection");
                    }
                }
            }).start();
        } catch (Exception e) {
            toast(e.getMessage());
            Log.e(TAG, "General exception: " + e.toString());
        }

    }

    private void toast(final String msg) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }

}
