package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

import static com.eveningoutpost.dexdrip.ListenerService.SendData;

// xDrip+ spoken treatments jamorham

public class Simulation extends Activity {

    private static final String WEARABLE_VOICE_PAYLOAD = "/xdrip_plus_voice_payload";
    private static final String WEARABLE_APPROVE_TREATMENT = "/xdrip_plus_approve_treatment";
    private static final String WEARABLE_CANCEL_TREATMENT = "/xdrip_plus_cancel_treatment";
    private static final String INTENT_TEXT_CONTENT = "android.intent.extra.TEXT";
    private static final String TAG = "jamorham simulation";
    private static final int REQ_CODE_SPEECH_INPUT = 94;
    private static int stackcounter = 0;
    private TextView mTextView;
    private TextView mCarbsText;
    private TextView mInsulinText;
    private TextView mTimeText;
    private TextView mBloodText;
    private ImageButton btnSpeak;
    private ImageButton btnApprove;
    private ImageButton btnCancel;
    private ImageButton btnCarbohydrates;
    private ImageButton btnBloodGlucose;
    private ImageButton btnInsulinDose;
    private ImageButton btnTime;
    private boolean recognitionRunning = false;
    private boolean inflated = false;

    public static void static_toast(final Context context, final String msg, final int length) {
        try {
            Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, length).show();
                }
            });
            Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast: " + msg + " e: " + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        final Intent intent = this.getIntent();
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mBloodText = (TextView) stub.findViewById(R.id.textBloodGlucose);
                mCarbsText = (TextView) stub.findViewById(R.id.textCarbohydrate);
                mInsulinText = (TextView) stub.findViewById(R.id.textInsulinUnits);
                mTimeText = (TextView) findViewById(R.id.textTimeButton);
                btnBloodGlucose = (ImageButton) findViewById(R.id.bloodTestButton);
                btnCarbohydrates = (ImageButton) findViewById(R.id.buttonCarbs);
                btnInsulinDose = (ImageButton) findViewById(R.id.buttonInsulin);
                btnCancel = (ImageButton) findViewById(R.id.cancelTreatment);
                btnApprove = (ImageButton) findViewById(R.id.approveTreatment);
                btnTime = (ImageButton) findViewById(R.id.timeButton);

                mTextView.setText("");
                mBloodText.setVisibility(View.INVISIBLE);
                mCarbsText.setVisibility(View.INVISIBLE);
                mInsulinText.setVisibility(View.INVISIBLE);
                mTimeText.setVisibility(View.INVISIBLE);

                btnBloodGlucose.setVisibility(View.INVISIBLE);
                btnCarbohydrates.setVisibility(View.INVISIBLE);
                btnInsulinDose.setVisibility(View.INVISIBLE);
                btnCancel.setVisibility(View.INVISIBLE);
                btnApprove.setVisibility(View.INVISIBLE);
                btnTime.setVisibility(View.INVISIBLE);
                inflated = true;

                final boolean debug = false;

                if (intent != null) {

                    // debug section

                    final Bundle bundle = intent.getExtras();

                    if ((bundle != null) && (debug)) {
                        for (String key : bundle.keySet()) {
                            Object value = bundle.get(key);
                            if (value != null) {
                                Log.d(TAG, String.format("%s %s (%s)", key,
                                        value.toString(), value.getClass().getName()));
                            }
                        }
                    }
                    processIncomingIntent(intent);
                }
            }
        });

        Log.i(TAG, "triggered");
        final Bundle bundle = intent.getExtras();
        if (bundle == null) promptSpeechInput();


    }

    @Override
    public void onNewIntent(Intent intent) {
        if (inflated) {
            processIncomingIntent(intent);
        } else {
            stackcounter = 0;
            processIncomingIntentWhenReady(intent);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        finish();
    }

    private void processIncomingIntentWhenReady(final Intent intent) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (inflated) {
                    processIncomingIntent(intent);
                } else {
                    stackcounter++;
                    Log.d(TAG, "Waiting for inflation count:" + stackcounter);
                    if (stackcounter < 30) processIncomingIntentWhenReady(intent);
                }
            }
        }, 100);
    }

    private void processIncomingIntent(Intent intent) {
        Log.d(TAG, "Processing incoming Intent");
        final Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        boolean hascontent = false;
        Log.d(TAG, "Processing incoming bundle");


        if (intent != null) {
            final String words = bundle.getString(INTENT_TEXT_CONTENT);
            if (words != null) {
                // note to self bundle
                if (words.length() > 1) {
                    SendData(this, WEARABLE_VOICE_PAYLOAD, words.getBytes(StandardCharsets.UTF_8));
                    if (mTextView != null) mTextView.setText(words);
                }
            } else {
                // datamap bundle
                try {
                    final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
                    DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra(WEARABLE_TREATMENT_PAYLOAD));
                    double insulin = dataMap.getDouble("insulin");
                    if (insulin > 0) {
                        mInsulinText.setText(Double.toString(insulin) + " units");
                        mInsulinText.setVisibility(View.VISIBLE);
                        btnInsulinDose.setVisibility(View.VISIBLE);
                        hascontent = true;
                    } else {
                        mInsulinText.setVisibility(View.INVISIBLE);
                        btnInsulinDose.setVisibility(View.INVISIBLE);
                    }

                    double carbs = dataMap.getDouble("carbs");
                    if (carbs > 0) {
                        mCarbsText.setText(Integer.toString((int) carbs) + " carbs");
                        mCarbsText.setVisibility(View.VISIBLE);
                        btnCarbohydrates.setVisibility(View.VISIBLE);
                        hascontent = true;
                    } else {
                        mCarbsText.setVisibility(View.INVISIBLE);
                        btnCarbohydrates.setVisibility(View.INVISIBLE);
                    }

                    double bloodtest = dataMap.getDouble("bloodtest");
                    if (bloodtest > 0) {
                        mBloodText.setText(Double.toString(bloodtest) + " " + (dataMap.getBoolean("ismgdl") ? "mgdl" : "mmol"));
                        mBloodText.setVisibility(View.VISIBLE);
                        btnBloodGlucose.setVisibility(View.VISIBLE);
                        hascontent = true;
                    } else {
                        mBloodText.setVisibility(View.INVISIBLE);
                        btnBloodGlucose.setVisibility(View.INVISIBLE);
                    }
                    String timestring = dataMap.getString("timestring");
                    double timeoffset = dataMap.getDouble("timeoffset");
                    if ((timeoffset > 0) && (timestring.length() > 0)) {
                        mTimeText.setText(timestring);
                        mTimeText.setVisibility(View.VISIBLE);
                        btnTime.setVisibility(View.VISIBLE);
                        hascontent = true;
                    } else {
                        mTimeText.setVisibility(View.INVISIBLE);
                        btnTime.setVisibility(View.INVISIBLE);
                    }

                    if ((insulin > 0) || (carbs > 0) || (bloodtest > 0)) {
                        btnApprove.setVisibility(View.VISIBLE);
                        btnCancel.setVisibility(View.VISIBLE);
                    } else {
                        btnApprove.setVisibility(View.INVISIBLE);
                        btnCancel.setVisibility(View.INVISIBLE);
                    }

                    if (!hascontent) {
                        finish();
                    } else {
                        KeypadInputActivity.resetValues();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Got exception processing treatment intent: " + e);
                }

                try {
                    final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
                    DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra(WEARABLE_TOAST_NOTIFICATON));
                    final String msg = dataMap.getString("msg");
                    static_toast(this, msg, dataMap.getInt("length"));
                    if ((!hascontent) || (msg.contains("Treatment cancelled")) || (msg.contains("Treatment processed"))) finish();
                } catch (Exception e) {
                    Log.e(TAG, "Got exception processing toast intent: " + e);
                }

            }
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String results = result.get(0);
                    if ((results != null) && (results.length() > 1)) {
                        if (mTextView != null) {
                            mTextView.setText(results);
                            mTextView.setVisibility(View.VISIBLE);
                        }
                        SendData(this, WEARABLE_VOICE_PAYLOAD, results.getBytes(StandardCharsets.UTF_8));
                    }
                    //   last_speech_time = JoH.ts();
                    //  naturalLanguageRecognition(result.get(0));
                }
                recognitionRunning = false;
                break;
            }

        }
    }

    public void WatchMic(View myview) {
        promptSpeechInput();
    }

    public void Approve(View myview) {
        SendData(this, WEARABLE_APPROVE_TREATMENT, null);
        finish();
    }

    public void Cancel(View myview) {
        SendData(this, WEARABLE_CANCEL_TREATMENT, null);
        finish();
    }

    private void promptSpeechInput() {


        if (recognitionRunning) return;
        recognitionRunning = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // debug voice
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say Treatment eg: x.x units / xx carbs");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Speech recognition is not supported",
                    Toast.LENGTH_LONG).show();
        }

    }
}