package com.eveningoutpost.dexdrip;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.eveningoutpost.dexdrip.Home.startHomeWithExtra;
import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.UtilityModels.Unitized.mmolConvert;

/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class BolusCalculatorActivity extends AppCompatActivity {

    private TextView correctionBolusTextView, mealBolusTextView, bgUnitText;
    private CheckBox mealBolusCheckbox, correctionBolusCheckBox;

    private Button confirmButton;
    private ImageButton noteSpeakButton;
    private EditText carbsEditText, bloodGlucoseEditText, totalBolusEditText, notesEditText;

    private static final String menu_name = "Bolus Calculator";
    private static final String TAG = "BolusCalculator";

    private boolean recognitionRunning = false;
    private static final int REQ_CODE_SPEECH_INPUT = 1994;
    private static final int REQ_CODE_SPEECH_NOTE_INPUT = 1995;

    private static Map<String, String> values = new HashMap<String, String>();

    // TODO sync profile from nightscout
    // TODO IOB adjustments
    // TODO calibrate sensor from calculator
    // TODO extended boluses?
    // TODO time custom set
    // TODO more settings for bolus calc (correct above, pen/pump increments, etc.)
    // TODO dialog warnings for low/high blood sugar

    final BgReading last = BgReading.last();
    public long time = new Date().getTime();
    double totalBolus = 0;

    int mgdl_value = (int) last.getDg_mgdl();
    double mmol_value = roundDouble(mmolConvert(last.getDg_mgdl()), 1);
    DecimalFormat df = new DecimalFormat("#0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolus_calc);
        JoH.fixActionBar(this);

        correctionBolusTextView = (TextView) findViewById(R.id.correctionBolusTextView);
        mealBolusTextView = (TextView) findViewById(R.id.mealBolusTextView);
        bgUnitText = (TextView) findViewById(R.id.bg_units);

        carbsEditText = (EditText) findViewById(R.id.editTextCarbs);
        bloodGlucoseEditText = (EditText) findViewById(R.id.editTextBloodGlucose);
        totalBolusEditText = (EditText) findViewById(R.id.editTextTotalBolus);
        notesEditText = (EditText) findViewById(R.id.editTextNotes);

        confirmButton = (Button) findViewById(R.id.confirm_button);
        noteSpeakButton = (ImageButton) findViewById(R.id.note_speak);

        mealBolusCheckbox = (CheckBox) findViewById(R.id.mealBolusCheckbox);
        correctionBolusCheckBox = (CheckBox) findViewById(R.id.correctionBolusCheckbox);

        carbsEditText.addTextChangedListener(textWatcher);
        bloodGlucoseEditText.addTextChangedListener(textWatcher);

        final Bundle bundle = getIntent().getExtras();
        processIncomingBundle(bundle);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAll();
            }
        });

        noteSpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechNoteInput(v);
            }
        });

        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            bgUnitText.setText(R.string.mgdl_units);
            bloodGlucoseEditText.setText(String.valueOf(mgdl_value));
        } else {
            bgUnitText.setText(R.string.mmol_units);
            bloodGlucoseEditText.setText(String.valueOf(mmol_value));
        }
    }

    double lowMark(){
        return JoH.tolerantParseDouble(Pref.getString("lowValue", "70"), 70d);
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch (view.getId()) {
            case R.id.mealBolusCheckbox:
                if (!checked) {
                    totalBolus -= mealBolus();
                    mealBolusTextView.setText("0");
                    totalBolus();
                }
                if (checked) {
                    mealBolusTextView.setText(String.valueOf(mealBolus()));
                    totalBolus();
                }
                break;
            case R.id.correctionBolusCheckbox:
                if (!checked) {
                    totalBolus -= correctionBolus();
                    correctionBolusTextView.setText("0");
                    totalBolus();
                }
                if (checked) {
                    correctionBolusTextView.setText(String.valueOf(correctionBolus()));
                    totalBolus();
                }
                break;
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mealBolus();
            correctionBolus();
            totalBolus = Double.parseDouble(totalBolusEditText.getText().toString());
            totalBolus();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(getBloodGlucoseText() < lowMark()){
                Toast.makeText(getApplicationContext(),
                        getString(R.string.dialog_low_bg),
                        Toast.LENGTH_SHORT).show();
            }
            if(BgGraphBuilder.low_occurs_at > 0 && getBloodGlucoseText() > lowMark()){
                final double now = JoH.ts();
                final double predicted_low_in_mins = (BgGraphBuilder.low_occurs_at - now) / 60000;
                Toast.makeText(getApplicationContext(),
                        getString(R.string.low_predicted) + " " + getString(R.string.in) + ": " + (int) predicted_low_in_mins + getString(R.string.space_mins) +
                                "\n" + getString(R.string.dialog_caution),
                        Toast.LENGTH_SHORT).show();
            }

        }
    };

    //getter methods
    private int getCarbsText() {
        try {
            return Integer.parseInt(String.valueOf(carbsEditText.getText()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getMealBolus() {
        return Double.parseDouble(String.valueOf(mealBolusTextView.getText()));
    }

    private double getCorrectionBolus() {
        return Double.parseDouble(String.valueOf(correctionBolusTextView.getText()));
    }

    private double getBloodGlucoseText() {
        try {
            return Double.parseDouble(String.valueOf(bloodGlucoseEditText.getText()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getTotalBolusText() {
        try {
            return Double.parseDouble(String.valueOf(totalBolusEditText.getText()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getNotesEntry() {
        return notesEditText.getText().toString();
    }

    public void promptSpeechNoteInput(View abc) {

        if (recognitionRunning) return;
        recognitionRunning = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // debug voice
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speak_your_note_text));

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_NOTE_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_recognition_is_not_supported),
                    Toast.LENGTH_LONG).show();
        }
        recognitionRunning = false;
    }

    private void processIncomingBundle(Bundle bundle) {
        Log.d(TAG, "Processing incoming bundle");
        if (bundle != null) {
            String receivedText = bundle.getString(WatchUpdaterService.WEARABLE_VOICE_PAYLOAD);
            if (receivedText != null) {
                notesEditText.setText(receivedText);
                Home home = new Home();
                home.naturalLanguageRecognition(receivedText);
            }
        }
    }

    private double mealBolus() {
        double carbRatio = Profile.getCarbRatio(time);
        double mealBolus = getCarbsText() / carbRatio;

        mealBolusTextView.setText(df.format(mealBolus));
        return Double.parseDouble(df.format(mealBolus));
    }

    private double correctionBolus() {
        double correctionBolus;
        if (getBloodGlucoseText() >= Profile.getTargetRangeInUnits(time)) {
            correctionBolus = ((getBloodGlucoseText() - Profile.getTargetRangeInUnits(time)) / Profile.getSensitivity(time));
        } else {
            correctionBolus = 0;
        }

        correctionBolusTextView.setText(df.format(correctionBolus));
        return Double.parseDouble(df.format(correctionBolus));
    }

    private double totalBolus() {
        totalBolus = Double.parseDouble(df.format(getMealBolus() + getCorrectionBolus()));

        if (totalBolus < 0) {
            totalBolus = 0;
            totalBolusEditText.setText("0");
        }

        totalBolusEditText.setText(df.format(totalBolus));
        return totalBolus;
    }

    private static String getValue(String tab) { //if has value, return it, else just put a space
        if (values.containsKey(tab)) {
            return values.get(tab);
        } else {
            values.put(tab, "");
            return values.get(tab);
        }
    }

    private boolean isNonzeroValueInTab(String tab) {
        try {
            return (0 != Double.parseDouble(getValue(tab)));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void resetValues() {
        values = new HashMap<String, String>();
    }

    private void submitAll() {
        //insulin, carbs, bloodtest, time
        //units, carbs,
        values.put("bloodtest", String.valueOf(getBloodGlucoseText()));
        values.put("carbs", String.valueOf(getCarbsText()));
        values.put("units", String.valueOf(getTotalBolusText()));

        if (getTotalBolusText() > 0) {
            confirmButton.setVisibility(View.VISIBLE);
        }

        boolean nonzeroBloodValue = isNonzeroValueInTab("bloodtest");
        boolean nonzeroCarbsValue = isNonzeroValueInTab("carbs");

        String mystring = "";

//        DateTime dt = new DateTime();
//        String timeValue = dt.getHourOfDay() +  "." + dt.getMinuteOfHour();
//        if (timeValue.length() > 0) mystring += timeValue + " time ";

        if (nonzeroBloodValue && getBloodGlucoseText() != mgdl_value)
            mystring += getValue("bloodtest") + " blood ";
        if (nonzeroCarbsValue) mystring += getValue("carbs") + " carbs ";

        double units = Double.parseDouble(getValue("units"));
        mystring += String.format("%.1f", units).replace(",", ".") + " units ";


        String treatment_text = getNotesEntry().trim();
        Log.d(TAG, "Got treatment note: " + treatment_text);
        Treatments.create_note(treatment_text, time, -1); // timestamp?
        Home.staticRefreshBGCharts();

        resetValues();
        startHomeWithExtra(this, WatchUpdaterService.WEARABLE_VOICE_PAYLOAD, mystring); // send data to home directly
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}