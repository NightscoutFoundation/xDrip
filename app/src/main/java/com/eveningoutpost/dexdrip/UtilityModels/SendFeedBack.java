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
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

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
                // TODO this probably should just use generic text method
                final String str = bundle.getString("request_translation");
                if (str != null) {
                    // don't extract string - english only
                    ((EditText) findViewById(R.id.yourText)).setText(getString(R.string.translation_request, str));

                }
                final String str2 = bundle.getString("generic_text");
                if (str2 != null) {
                    ((EditText) findViewById(R.id.yourText)).setText(str2);

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
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);

        client.interceptors().add(new GzipRequestInterceptor());

        if (yourtext.length() == 0) {
            toast(getString(R.string.no_text_entered));
            return;
        }
        toast("Sending..");

        try {
            final RequestBody formBody = new FormEncodingBuilder()
                    .add("contact", contact.getText().toString())
                    .add("body", JoH.getDeviceDetails()+"\n"+JoH.getVersionDetails()+"\n\n"+yourtext.getText().toString())
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
                            JoH.static_toast_long(response.body().string());
                            //Home.toaststatic("Feedback sent successfully");
                            finish();
                        } else {
                            JoH.static_toast_short(getString(R.string.error_sending_feedback, response.message().toString()));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in execute: " + e.toString());
                       JoH.static_toast_short(getString(R.string.error_with_network_connection));
                    }
                }
            }).start();
        } catch (Exception e) {
            JoH.static_toast_short(e.getMessage());
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

class GzipRequestInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
            return chain.proceed(originalRequest);
        }

        Request compressedRequest = originalRequest.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(originalRequest.method(), forceContentLength(gzip(originalRequest.body())))
                .build();
        return chain.proceed(compressedRequest);
    }

    /** https://github.com/square/okhttp/issues/350 */
    private RequestBody forceContentLength(final RequestBody requestBody) throws IOException {
        final Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return requestBody.contentType();
            }

            @Override
            public long contentLength() {
                return buffer.size();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(buffer.snapshot());
            }
        };
    }

    private RequestBody gzip(final RequestBody body) {
        return new RequestBody() {
            @Override public MediaType contentType() {
                return body.contentType();
            }

            @Override public long contentLength() {
                return -1; // We don't know the compressed length in advance!
            }

            @Override public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }
}

