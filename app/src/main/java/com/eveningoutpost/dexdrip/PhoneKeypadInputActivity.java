package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.HashMap;
import java.util.Map;

import static com.eveningoutpost.dexdrip.Home.startHomeWithExtra;


/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class PhoneKeypadInputActivity extends BaseActivity {

    private TextView mDialTextView;
    private Button zeroButton, oneButton, twoButton, threeButton, fourButton, fiveButton,
            sixButton, sevenButton, eightButton, nineButton, starButton, backSpaceButton;
    private ImageButton callImageButton, backspaceImageButton, insulintabbutton, carbstabbutton,
            bloodtesttabbutton, timetabbutton, speakbutton;

    private static String currenttab = "insulin";
    private static final String LAST_TAB_STORE = "phone-keypad-treatment-last-tab";
    private static final String TAG = "KeypadInput";
    private static Map<String, String> values = new HashMap<String, String>();
    private String units;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keypad_activity_phone);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        final int refdpi = 320;
        Log.d(TAG, "Width height: " + width + " " + height + " DPI:" + dm.densityDpi);
        getWindow().setLayout((int) Math.min(((520 * dm.densityDpi) / refdpi), width), (int) Math.min((650 * dm.densityDpi) / refdpi, height));
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.5f;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mDialTextView = (TextView) findViewById(R.id.dialed_no_textview);
        zeroButton = (Button) findViewById(R.id.zero_button);
        oneButton = (Button) findViewById(R.id.one_button);
        twoButton = (Button) findViewById(R.id.two_button);
        threeButton = (Button) findViewById(R.id.three_button);
        fourButton = (Button) findViewById(R.id.four_button);
        fiveButton = (Button) findViewById(R.id.five_button);
        sixButton = (Button) findViewById(R.id.six_button);
        sevenButton = (Button) findViewById(R.id.seven_button);
        eightButton = (Button) findViewById(R.id.eight_button);
        nineButton = (Button) findViewById(R.id.nine_button);
        starButton = (Button) findViewById(R.id.star_button);
        backSpaceButton = (Button) findViewById(R.id.backspace_button);
        // callImageButton = (ImageButton) stub.findViewById(R.id.call_image_button);
        // backspaceImageButton = (ImageButton) stub.findViewById(R.id.backspace_image_button);

        insulintabbutton = (ImageButton) findViewById(R.id.insulintabbutton);
        bloodtesttabbutton = (ImageButton) findViewById(R.id.bloodtesttabbutton);
        timetabbutton = (ImageButton) findViewById(R.id.timetabbutton);
        carbstabbutton = (ImageButton) findViewById(R.id.carbstabbutton);
        speakbutton = (ImageButton) findViewById(R.id.btnKeypadSpeak);

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

        speakbutton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startHomeWithExtra(getApplicationContext(), Home.START_SPEECH_RECOGNITION, "ok");
                        finish();
                    }
                });
        speakbutton.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        startHomeWithExtra(getApplicationContext(), Home.START_TEXT_RECOGNITION, "ok");
                        finish();
                        return true;
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

        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            units = " mg/dl";
        } else {
            units = " mmol/l";
        }

        updateTab();
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


    private void submitAll() {

        String mystring = "";
        mystring += (getValue("time").length() > 0) ? getValue("time") + " time " : "";
        mystring += (getValue("bloodtest").length() > 0) ? getValue("bloodtest") + " blood " : "";
        mystring += (getValue("carbs").length() > 0) ? (!getValue("carbs").equals("0") ? getValue("carbs") + " carbs " : "") : "";
        mystring += (getValue("insulin").length() > 0) ? (!getValue("insulin").equals("0") ? getValue("insulin") + " units " : "") : "";

        if (mystring.length() > 1) {
            //SendData(this, WEARABLE_VOICE_PAYLOAD, mystring.getBytes(StandardCharsets.UTF_8));
            resetValues();
            //WatchUpdaterService.receivedText(this, mystring); // reuse watch handling function to send data to home
            startHomeWithExtra(this, WatchUpdaterService.WEARABLE_VOICE_PAYLOAD, mystring); // send data to home directly
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
                append = " " + getString(R.string.units);
                break;
            case "carbs":
                carbstabbutton.setBackgroundColor(onColor);
                append = " " + getString(R.string.carbs);
                break;
            case "bloodtest":
                bloodtesttabbutton.setBackgroundColor(onColor);
                append = units;
                break;
            case "time":
                timetabbutton.setBackgroundColor(onColor);
                append = " " + getString(R.string.when);
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


    @Override
    protected void onResume() {
        final String savedtab = PersistentStore.getString(LAST_TAB_STORE);
        if (savedtab.length() > 0) currenttab = savedtab;
        updateTab();
        super.onResume();
    }

    @Override
    protected void onPause() {
        PersistentStore.setString(LAST_TAB_STORE, currenttab);
        super.onPause();
    }
}
