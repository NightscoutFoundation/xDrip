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

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.android.gms.wearable.DataMap;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private boolean watchkeypad = false;
    double carbs = 0;
    double insulin = 0;
    double bloodtest = 0;
    double timeoffset = 0;
    String thisnotes = "";
    double thisnumber = -1;
    double thisglucosenumber = 0;
    double thiscarbsnumber = 0;
    double thisinsulinnumber = 0;
    double thistimeoffset = 0;
    String thistimetext = "";
    String thisword = "";
    boolean carbsset = false;
    boolean insulinset = false;
    boolean glucoseset = false;
    boolean timeset = false;
    boolean watchkeypadset = false;

    public static void static_toast(final Context context, final String msg, final int length) {
        try {
            Activity activity = (Activity) context;
            activity.runOnUiThread(() -> Toast.makeText(context, msg, length).show());
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
        stub.setOnLayoutInflatedListener(stub1 -> {
            mTextView = (TextView) stub1.findViewById(R.id.text);
            mBloodText = (TextView) stub1.findViewById(R.id.textBloodGlucose);
            mCarbsText = (TextView) stub1.findViewById(R.id.textCarbohydrate);
            mInsulinText = (TextView) stub1.findViewById(R.id.textInsulinUnits);
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
        handler.postDelayed(() -> {
            if (inflated) {
                processIncomingIntent(intent);
            } else {
                stackcounter++;
                Log.d(TAG, "Waiting for inflation count:" + stackcounter);
                if (stackcounter < 30) processIncomingIntentWhenReady(intent);
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
                    Bundle treatmentBundle = intent.getBundleExtra(ListenerService.WEARABLE_TREATMENT_PAYLOAD);
                    if (treatmentBundle != null) {
// todo gruoner: extract the injectionJSON on wearable to display and approve the treatment
                        DataMap dataMap = DataMap.fromBundle(treatmentBundle);
                        if (dataMap != null) {
                            initValues();
                            watchkeypad = dataMap.getBoolean("watchkeypad", false);
                            thisnotes = dataMap.getString("notes", "");
                            if (watchkeypad)
                                createTreatment(thisnotes);
                            insulin = dataMap.getDouble("insulin", thisinsulinnumber);
                            if (insulin > 0) {
                                mInsulinText.setText(Double.toString(insulin) + " units");
                                mInsulinText.setVisibility(View.VISIBLE);
                                btnInsulinDose.setVisibility(View.VISIBLE);
                                hascontent = true;
                            } else {
                                mInsulinText.setVisibility(View.INVISIBLE);
                                btnInsulinDose.setVisibility(View.INVISIBLE);
                            }

                            carbs = dataMap.getDouble("carbs", thiscarbsnumber);
                            if (carbs > 0) {
                                mCarbsText.setText(Integer.toString((int) carbs) + " carbs");
                                mCarbsText.setVisibility(View.VISIBLE);
                                btnCarbohydrates.setVisibility(View.VISIBLE);
                                hascontent = true;
                            } else {
                                mCarbsText.setVisibility(View.INVISIBLE);
                                btnCarbohydrates.setVisibility(View.INVISIBLE);
                            }

                            bloodtest = dataMap.getDouble("bloodtest", thisglucosenumber);
                            if (bloodtest > 0) {
                                mBloodText.setText(Double.toString(bloodtest) + " " + (dataMap.getBoolean("ismgdl") ? "mgdl" : "mmol"));
                                mBloodText.setVisibility(View.VISIBLE);
                                btnBloodGlucose.setVisibility(View.VISIBLE);
                                hascontent = true;
                            } else {
                                mBloodText.setVisibility(View.INVISIBLE);
                                btnBloodGlucose.setVisibility(View.INVISIBLE);
                            }
                            String timestring = dataMap.getString("timestring", thistimetext);
                            timeoffset = dataMap.getDouble("timeoffset", thistimeoffset);
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
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Got exception processing treatment intent: " + e);
                }

                try {
                    Bundle toastBundle = intent.getBundleExtra(ListenerService.WEARABLE_TOAST_NOTIFICATON);
                    String msg = "";
                    if (toastBundle != null) {
                        DataMap dataMap = DataMap.fromBundle(toastBundle);
                        if (dataMap != null) {
                            msg = dataMap.getString("msg");
                            if (msg != null && !msg.isEmpty())
                                static_toast(this, msg, dataMap.getInt("length"));
                        }
                    }
                    if ((!hascontent) || (msg.contains("Treatment cancelled")) || (msg.contains("Treatment processed")))
                        finish();
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
                        //TODO add speech recognition using the initiallexicon.txt used in Home.initializeSearchWords for Home.classifyWord called by naturalLanguageRecognition()
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
        if (watchkeypad) {
            //Treatments.create(carbs, insulin, thisnotes, new Date().getTime());
            DataMap dataMap = new DataMap();
            dataMap.putDouble("timeoffset", timeoffset);
            dataMap.putDouble("carbs", carbs);
            dataMap.putDouble("insulin", insulin);
            dataMap.putDouble("bloodtest", bloodtest);
            dataMap.putString("notes", thisnotes);
            //dataMap.putLong("timestamp", System.currentTimeMillis());
            ListenerService.createTreatment(dataMap, this);
        }
        else
            SendData(this, WEARABLE_APPROVE_TREATMENT, null);
        finish();
    }

    public void Cancel(View myview) {
        if (watchkeypad) {
            JoH.static_toast(xdrip.getAppContext(), "Treatment cancelled", Toast.LENGTH_SHORT);
        }
        else {
            SendData(this, WEARABLE_CANCEL_TREATMENT, null);
        }
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

    private void handleWordPair() {
        if ((thisnumber == -1) || "".equals(thisword)) {
            return;
        }

        Log.d(TAG, "GOT WORD PAIR: " + thisnumber + " = " + thisword);

        switch (thisword) {
            case "watchkeypad":
                if ((!watchkeypadset) && (thisnumber > 0)) {
                    watchkeypad = true;
                    watchkeypadset = true;
                    Log.d(TAG, "Treatment entered on watchkeypad: " + Double.toString(thisnumber));
                } else {
                    Log.d(TAG, "watchkeypad already set");
                }
                break;

            case "rapid":
            case "units":
                if ((!insulinset) && (thisnumber > 0)) {
                    thisinsulinnumber = thisnumber;
                    Log.d(TAG, "Rapid dose: " + Double.toString(thisnumber));
                    insulinset = true;
                } else {
                    Log.d(TAG, "Rapid dose already set");
                }
                break;

            case "carbs":
                if ((!carbsset) && (thisnumber > 0)) {
                    thiscarbsnumber = thisnumber;
                    carbsset = true;
                    Log.d(TAG, "Carbs eaten: " + Double.toString(thisnumber));
                } else {
                    Log.d(TAG, "Carbs already set");
                }
                break;

            case "blood":
                if ((!glucoseset) && (thisnumber > 0)) {
                    thisglucosenumber = thisnumber;
                    Log.d(TAG, "Blood test: " + Double.toString(thisnumber));
                    glucoseset = true;
                } else {
                    Log.d(TAG, "Blood glucose already set");
                }
                break;

            case "time":
                Log.d(TAG, "processing time keyword");
                if ((!timeset) && (thisnumber >= 0)) {

                    final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                    final DecimalFormat df = (DecimalFormat) nf;
                    //DecimalFormat df = new DecimalFormat("#");
                    df.setMinimumIntegerDigits(2);
                    df.setMinimumFractionDigits(2);
                    df.setMaximumFractionDigits(2);
                    df.setMaximumIntegerDigits(2);

                    final Calendar c = Calendar.getInstance();

                    final SimpleDateFormat simpleDateFormat1 =
                            new SimpleDateFormat("dd/M/yyyy ", Locale.US);
                    final SimpleDateFormat simpleDateFormat2 =
                            new SimpleDateFormat("dd/M/yyyy HH.mm", Locale.US); // TODO double check 24 hour 12.00 etc
                    final String datenew = simpleDateFormat1.format(c.getTime()) + df.format(thisnumber);

                    Log.d(TAG, "Time Timing data datenew: " + datenew);

                    final Date datethen;
                    final Date datenow = new Date();

                    try {
                        datethen = simpleDateFormat2.parse(datenew);
                        double difference = datenow.getTime() - datethen.getTime();
                        // is it more than 1 hour in the future? If so it must be yesterday
                        if (difference < -(1000 * 60 * 60)) {
                            difference = difference + (86400 * 1000);
                        } else {
                            // - midnight feast pre-bolus nom nom
                            if (difference > (60 * 60 * 23 * 1000))
                                difference = difference - (86400 * 1000);
                        }

                        Log.d(TAG, "Time Timing data: " + df.format(thisnumber) + " = difference ms: " + JoH.qs(difference));
                        thistimetext = df.format(thisnumber);
                        timeset = true;
                        thistimeoffset = difference;
                    } catch (ParseException e) {
                        // toast to explain?
                        Log.d(TAG, "Got exception parsing date time");
                    }
                } else {
                    Log.d(TAG, "Time data already set");
                }
                break;
        } // end switch
    }

    private void initValues() {
        thiscarbsnumber = 0;
        thisinsulinnumber = 0;
        thistimeoffset = 0;
        thisglucosenumber = 0;
        watchkeypadset = false;
        carbsset = false;
        insulinset = false;
        glucoseset = false;
        timeset = false;
    }

    private void createTreatment(String allWords) {
        Log.d(TAG, "createTreatment allWords=" + allWords);
        allWords = allWords.trim();
        allWords = allWords.replaceAll(":", "."); // fix real times
        allWords = allWords.replaceAll("(\\d)([a-zA-Z])", "$1 $2"); // fix like 22mm
        allWords = allWords.replaceAll("([0-9].[0-9])([0-9][0-9])", "$1 $2"); // fix multi number order like blood 3.622 grams
        allWords = allWords.toLowerCase();
        Log.d(TAG, "createTreatment after regex allWords=" + allWords);
        // reset parameters for new speech
        glucoseset = false;
        insulinset = false;
        carbsset = false;
        timeset = false;
        watchkeypadset = false;
        thisnumber = -1;
        thisword = "";
        thistimetext = "";
        String[] wordsArray = allWords.split(" ");
        for (String word : wordsArray) {
            // per word in input stream
            try {
                thisnumber = Double.parseDouble(word); // if no exception
                handleWordPair();
            } catch (NumberFormatException nfe) {
                // detection of number or not
                Log.d(TAG, "createTreatment NumberFormatException wordsArray[i]=" + word);
                //String result = classifyWord(wordsArray[i]);
                //if (result != null)
                thisword = word;//result;
                handleWordPair();
            }
        }
    }
}