package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class SendFeedBack extends Activity {

    private final String send_url = "https://xdrip-plus-updates.appspot.com/joh-feedback";
    private final String TAG = "jamorham feedback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feed_back);
    }

    public void closeActivity(View myview) {
        finish();
    }

    public void sendFeedback(View myview) {

        final EditText contact = (EditText) findViewById(R.id.contactText);
        final EditText yourtext = (EditText) findViewById(R.id.yourText);
        final RatingBar myrating = (RatingBar) findViewById(R.id.ratingBar);
        final OkHttpClient client = new OkHttpClient();

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
                            Home.toaststatic("Error sending feedback: " + response.message().toString());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in execute: " + e.toString());

                    }
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "General exception: " + e.toString());
        }
    }

}
