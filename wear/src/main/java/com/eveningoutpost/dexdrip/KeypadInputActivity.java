package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.eveningoutpost.dexdrip.ListenerService.SendData;

/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class KeypadInputActivity extends Activity {

    private final static String TAG = "jamorham " + KeypadInputActivity.class.getSimpleName();
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
    private TextView mDialTextView;
    private Button zeroButton, oneButton, twoButton, threeButton, fourButton, fiveButton,
            sixButton, sevenButton, eightButton, nineButton, starButton, backSpaceButton;
    private ImageButton callImageButton, backspaceImageButton, insulintabbutton, carbstabbutton,
            bloodtesttabbutton, timetabbutton;
    //private GoogleApiClient mApiClient;
    private static String currenttab = "insulin";
    private static Map<String, String> values = new HashMap<String, String>();
    private static final String WEARABLE_VOICE_PAYLOAD = "/xdrip_plus_voice_payload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mDialTextView = (TextView) stub.findViewById(R.id.dialed_no_textview);
                zeroButton = (Button) stub.findViewById(R.id.zero_button);
                oneButton = (Button) stub.findViewById(R.id.one_button);
                twoButton = (Button) stub.findViewById(R.id.two_button);
                threeButton = (Button) stub.findViewById(R.id.three_button);
                fourButton = (Button) stub.findViewById(R.id.four_button);
                fiveButton = (Button) stub.findViewById(R.id.five_button);
                sixButton = (Button) stub.findViewById(R.id.six_button);
                sevenButton = (Button) stub.findViewById(R.id.seven_button);
                eightButton = (Button) stub.findViewById(R.id.eight_button);
                nineButton = (Button) stub.findViewById(R.id.nine_button);
                starButton = (Button) stub.findViewById(R.id.star_button);
                backSpaceButton = (Button) stub.findViewById(R.id.backspace_button);
                // callImageButton = (ImageButton) stub.findViewById(R.id.call_image_button);
                // backspaceImageButton = (ImageButton) stub.findViewById(R.id.backspace_image_button);

                insulintabbutton = (ImageButton) stub.findViewById(R.id.insulintabbutton);
                bloodtesttabbutton = (ImageButton) stub.findViewById(R.id.bloodtesttabbutton);
                timetabbutton = (ImageButton) stub.findViewById(R.id.timetabbutton);
                carbstabbutton = (ImageButton) stub.findViewById(R.id.carbstabbutton);


                mDialTextView.setText("");

                mDialTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submitAll();
                    }
                });

                zeroButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("0");
                    }
                });

                //zeroButton.setOnLongClickListener(new View.OnLongClickListener() {
                //    @Override
                //    public boolean onLongClick(View v) {
                //        mDialTextView.setText(mDialTextView.getText() + "+");
                //        return true;
                //    }
                //});

                oneButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("1");
                    }
                });

                twoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("2");
                    }
                });

                threeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("3");
                    }
                });

                fourButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("4");
                    }
                });

                fiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("5");
                    }
                });

                sixButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("6");
                    }
                });

                sevenButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("7");
                    }
                });

                eightButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("8");
                    }
                });

                nineButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appCurrent("9");
                    }
                });

                starButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!getValue(currenttab).contains(".")) appCurrent(".");
                    }
                });

                //hashButton.setOnClickListener(new View.OnClickListener() {
                //    @Override
                //    public void onClick(View v) {
                //        mDialTextView.setText(mDialTextView.getText() + "#");
                //    }
                //});

                backSpaceButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appBackSpace();
                    }
                });
                backSpaceButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        values.put(currenttab, "");
                        updateTab();
                        return true;
                    }
                });

                bloodtesttabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "bloodtest";
                        updateTab();
                    }
                });
                insulintabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "insulin";
                        updateTab();
                    }
                });
                carbstabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "carbs";
                        updateTab();
                    }
                });
                timetabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "time";
                        updateTab();
                    }
                });


                updateTab();

             /*   callImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mDialTextView.getText().toString().isEmpty()) {
                            sendMessage(mDialTextView.getText().toString(), null);
                            Toast.makeText(getApplicationContext(), "Calling " + mDialTextView.getText(), Toast.LENGTH_SHORT).show();
                            mDialTextView.setText(null);
                        }
                    }
                });*/
            }
        });
    }

    public static void resetValues() {
        values = new HashMap<String, String>();
    }

    private static String getValue(String tab) {
        if (values.containsKey(tab)) {
            return values.get(tab);
        } else {
            values.put(tab, "");
            return values.get(tab);
        }
    }

    private static String appendValue(String tab, String append) {
        values.put(tab, getValue(tab) + append);
        return values.get(tab);
    }

    private static String appendCurrent(String append) {
        String cval = getValue(currenttab);
        if (cval.length() < 6) {
            if ((cval.length() == 0) && (append.equals("."))) append = "0.";
            return appendValue(currenttab, append);
        } else {
            return cval;
        }
    }

    private void appCurrent(String append) {
        appendCurrent(append);
        updateTab();
    }

    private void appBackSpace() {
        String cval = getValue(currenttab);
        if (cval.length() > 0) {
            values.put(currenttab, cval.substring(0, cval.length() - 1));
        }
        updateTab();
    }

    private void handleWordPair() {
        boolean preserve = false;
        if ((thisnumber == -1) || (thisword == "")) return;

        Log.d(TAG, "GOT WORD PAIR: " + thisnumber + " = " + thisword);

        switch (thisword) {

            case "carbs":
                if ((carbsset == false) && (thisnumber > 0)) {
                    thiscarbsnumber = thisnumber;
                    carbsset = true;
                    Log.d(TAG, "Carbs eaten: " + Double.toString(thisnumber));
                } else {
                    Log.d(TAG, "Carbs already set");
                }
                break;

            case "blood":
                if ((glucoseset == false) && (thisnumber > 0)) {
                    thisglucosenumber = thisnumber;
                    Log.d(TAG, "Blood test: " + Double.toString(thisnumber));
                    glucoseset = true;
                } else {
                    Log.d(TAG, "Blood glucose already set");
                }
                break;

            case "time":
                Log.d(TAG, "processing time keyword");
                if ((timeset == false) && (thisnumber >= 0)) {

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

    private void createTreatment(String allWords) {
        allWords = allWords.trim();
        allWords = allWords.replaceAll(":", "."); // fix real times
        allWords = allWords.replaceAll("(\\d)([a-zA-Z])", "$1 $2"); // fix like 22mm
        allWords = allWords.replaceAll("([0-9].[0-9])([0-9][0-9])", "$1 $2"); // fix multi number order like blood 3.622 grams
        allWords = allWords.toLowerCase();
        // reset parameters for new speech
        glucoseset = false;
        insulinset = false;
        carbsset = false;
        timeset = false;
        thisnumber = -1;
        thisword = "";
        thistimetext = "";
        String[] wordsArray = allWords.split(" ");
        for (int i = 0; i < wordsArray.length; i++) {
            // per word in input stream
            try {
                double thisdouble = Double.parseDouble(wordsArray[i]);
                thisnumber = thisdouble; // if no exception
                handleWordPair();
            } catch (NumberFormatException nfe) {
                // detection of number or not
                //String result = classifyWord(wordsArray[i]);
                //if (result != null)
                    thisword = wordsArray[i];//result;
                handleWordPair();
            }
        }
        //Treatments.create(thiscarbsnumber, thisinsulinnumber, allWords, Treatments.getTimeStampWithOffset(thistimeoffset));
    }

    private void submitAll() {

        String mystring = "";
        //Long curtime = (long) (new Date().getTime());
        mystring += "1 watchkeypad ";//curtime.toString() + " watchkeypad ";
        mystring += (getValue("time").length() > 0) ? getValue("time") + " time " : "";
        mystring += (getValue("bloodtest").length() > 0) ? getValue("bloodtest") + " blood " : "";
        mystring += (getValue("carbs").length() > 0) ? (!getValue("carbs").equals("0") ? getValue("carbs") + " carbs " : "") : "";
        mystring += (getValue("insulin").length() > 0) ? (!getValue("insulin").equals("0") ? getValue("insulin") + " units " : "") : "";

        if (mystring.length() > 1) {
            //createTreatment(mystring);
            ListenerService.sendTreatment(mystring);
            /*ListenerService.sendTreatment(
                    thiscarbsnumber,
                    thisinsulinnumber,
                    thisglucosenumber,
                    thistimeoffset,
                    thistimetext, mystring);*/
            //SendData(this, WEARABLE_VOICE_PAYLOAD, mystring.getBytes(StandardCharsets.UTF_8));
            finish();
        }
    }


    private void updateTab() {

        final int offColor = Color.DKGRAY;
        final int onColor = Color.RED;

        insulintabbutton.setBackgroundColor(offColor);
        carbstabbutton.setBackgroundColor(offColor);
        timetabbutton.setBackgroundColor(offColor);
        bloodtesttabbutton.setBackgroundColor(offColor);


        String append = "";
        String value = "";
        switch (currenttab) {
            case "insulin":
                insulintabbutton.setBackgroundColor(onColor);
                append = " units";
                break;
            case "carbs":
                carbstabbutton.setBackgroundColor(onColor);
                append = " carbs";
                break;
            case "bloodtest":
                bloodtesttabbutton.setBackgroundColor(onColor);
                append = " BG";  // TODO get mgdl or mmol here
                break;
            case "time":
                timetabbutton.setBackgroundColor(onColor);
                append = " time";
                break;
        }
        value = getValue(currenttab);
        mDialTextView.setText(value + append);
        // show green tick
        if (value.length() > 0) {
            mDialTextView.getBackground().setAlpha(255);
        } else {
            mDialTextView.getBackground().setAlpha(0);
        }
    }

   /* private void sendMessage(final String message, final byte[] payload) {
        Log.i(KeypadInputActivity.class.getSimpleName(), message);
        Wearable.NodeApi.getConnectedNodes(mApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> nodes = getConnectedNodesResult.getNodes();
                for (Node node : nodes) {
                    Log.i(KeypadInputActivity.class.getSimpleName(), "WEAR sending " + message + " to " + node);
                    Wearable.MessageApi.sendMessage(mApiClient, node.getId(), message, payload).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(KeypadInputActivity.class.getSimpleName(), "WEAR Result " + sendMessageResult.getStatus());
                        }
                    });
                }
            }
        });
    }*/


    @Override
    protected void onResume() {
        super.onResume();
      /*  mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(KeypadInputActivity.class.getSimpleName(), "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
        /*Wearable.MessageApi.removeListener(mApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {

            }
        });
        mApiClient.disconnect();*/
    }
}
