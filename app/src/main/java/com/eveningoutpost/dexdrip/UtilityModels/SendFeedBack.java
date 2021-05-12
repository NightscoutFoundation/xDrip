package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
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

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;

public class SendFeedBack extends BaseAppCompatActivity {

    private static final String TAG = "jamorham feedback";
    private static final String FEEDBACK_CONTACT_REFERENCE = "feedback-contact-reference";

    private String type_of_message = "Unknown";
    private String send_url;

    private String log_data = "";

    RatingBar myrating;
    TextView ratingtext;
    EditText contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feed_back);
        send_url = getString(R.string.wserviceurl) + "/joh-feedback";

        myrating = (RatingBar) findViewById(R.id.ratingBar);
        ratingtext = (TextView) findViewById(R.id.ratingtext);
        contact = (EditText) findViewById(R.id.contactText);
        contact.setText(PersistentStore.getString(FEEDBACK_CONTACT_REFERENCE));

        Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // TODO this probably should just use generic text method
                final String str = bundle.getString("request_translation");
                if (str != null) {
                    // don't extract string - english only
                    ((EditText) findViewById(R.id.yourText)).setText("Dear developers, please may I request that you add translation capability for: " + str + "\n\n");
                    type_of_message = "Language request";

                }
                final String str2 = bundle.getString("generic_text");
                if (str2 != null) {
                    log_data = str2;
                    ((EditText) findViewById(R.id.yourText)).setText(log_data.length() > 300 ? "\n\nPlease describe what you think these logs may show? Explain the problem if there is one.\n\nAttached " + log_data.length() + " characters of log data. (hidden)\n\n" : log_data);
                    type_of_message = "Log Push";
                    myrating.setVisibility(View.GONE);
                    ratingtext.setVisibility(View.GONE);
                }
            }
        }
        if (type_of_message.equals("Unknown")) {
            askType();
        }

    }

    public void closeActivity(View myview) {
        finish();
    }

    private void askType() {
        final CharSequence[] items = {"Bug Report", "Compliment", "Question", "Other"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type of feedback?");
        builder.setSingleChoiceItems(items, -1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        type_of_message = items[item].toString();
                        dialog.dismiss();
                    }
                });
        final AlertDialog typeDialog = builder.create();
        typeDialog.show();
    }

    private void askEmailAddress() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please supply email address or other contact reference");


        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);


        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                contact.setText(input.getText().toString());
                sendFeedback(null);
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } catch (NullPointerException e) {
            //
        }
        dialog.show();
    }

    public void sendFeedback(View myview) {

        final EditText contact = (EditText) findViewById(R.id.contactText);
        final EditText yourtext = (EditText) findViewById(R.id.yourText);
        final OkHttpClient client = new OkHttpClient();

        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);

        client.interceptors().add(new GzipRequestInterceptor());

        if (yourtext.length() == 0) {
            toast("No text entered - cannot send blank");
            return;
        }

        if (contact.length() == 0) {
            toast("Without some contact info we cannot reply");
            askEmailAddress();
            return;
        }

        if (type_of_message.equals("Unknown")) {
            askType();
            return;
        }

        PersistentStore.setString(FEEDBACK_CONTACT_REFERENCE, contact.getText().toString());
        toast("Sending..");

        try {
            final RequestBody formBody = new FormEncodingBuilder()
                    .add("contact", contact.getText().toString())
                    .add("body", JoH.getDeviceDetails() + "\n" + JoH.getVersionDetails() + "\n" + getBestCollectorHardwareName() + "\n===\n\n" + yourtext.getText().toString() + " \n\n===\nType: " + type_of_message + "\nLog data:\n\n" + log_data + "\n\n\nSent: " + JoH.dateTimeText(JoH.tsl()))
                    .add("rating", String.valueOf(myrating.getRating()))
                    .add("type", type_of_message)
                    .build();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final Request request = new Request.Builder()
                                .url(send_url)
                                .post(formBody)
                                .build();
                        Log.i(TAG, "Sending feedback request");
                        final Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            JoH.static_toast_long(response.body().string());
                            log_data = "";
                            //Home.toaststatic("Feedback sent successfully");
                            finish();
                        } else {
                            JoH.static_toast_short("Error sending feedback: " + response.message().toString());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in execute: " + e.toString());
                        JoH.static_toast_short("Error with network connection");
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
    @Override
    public Response intercept(Chain chain) throws IOException {
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

    /**
     * https://github.com/square/okhttp/issues/350
     */
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
            @Override
            public MediaType contentType() {
                return body.contentType();
            }

            @Override
            public long contentLength() {
                return -1; // We don't know the compressed length in advance!
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }
}

