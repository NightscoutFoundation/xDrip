package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Iob;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.Home.startHomeWithExtra;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MINUTE_IN_MS;

// jamorham xdrip plus

public class BolusCalculatorActivity extends AppCompatActivity {

    private TextView correctionBolusTextView, mealBolusTextView, bgUnitText;
    private CheckBox mealBolusCheckbox, correctionBolusCheckBox;
    private Button carbsMinusButton, carbsPlusButton, bgMinusButton, bgPlusButton,
            totalBolusMinusButton, totalBolusPlusButton, confirmButton;
    private ImageButton settingsButton;

    private TableLayout proteinsAndFatsTable;
    private EditText carbsEditText, bloodGlucoseEditText, totalBolusEditText, notesEditText, proteinsEditText, fatsEditText;

    private static final String menu_name = "Bolus Calculator";
    private static final String TAG = "BolusCalculator";
    private BroadcastReceiver statusReceiver;
    private BroadcastReceiver statusReceiver2;

    private static Map<String, String> values = new HashMap<String, String>();

    // TODO sync profile from nightscout?
    // TODO IOB adjustments
    // TODO time custom set

    final BgReading last = BgReading.last();
    long time = new Date().getTime();

    double totalBolus = 0;
    int carbsValue = 0;
    double bgValue = 0;
    double plusMinusDifference;

    int maxCarbs;
    int maxBloodGlucose = 400;

    int mgdl_value = (int) last.getDg_mgdl();
    double mmol_value = last.calculated_value_mmol();
    DecimalFormat df = new DecimalFormat("#0.00");

    @Getter
    private static String calcIOB = "";
    private static String calcCOB = "";

    /*
    if bg > target && correction <= IOBcarb(iob covering carbs): correction -= IOBcorrection (iob for correction)

    if bg > target && correction > IOBcarb: correction -= IOB

    if bg < target: correction - IOBcorrection
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolus_calc);
        JoH.fixActionBar(this);

        correctionBolusTextView = (TextView) findViewById(R.id.correctionBolusTextView);
        mealBolusTextView = (TextView) findViewById(R.id.mealBolusTextView);
        bgUnitText = (TextView) findViewById(R.id.bg_units);
        proteinsAndFatsTable = (TableLayout) findViewById(R.id.proteinsAndFatsTable);

        carbsEditText = (EditText) findViewById(R.id.editTextCarbs);
        bloodGlucoseEditText = (EditText) findViewById(R.id.editTextBloodGlucose);
        totalBolusEditText = (EditText) findViewById(R.id.editTextTotalBolus);
        proteinsEditText = (EditText) findViewById(R.id.editTextProteins);
        fatsEditText = (EditText) findViewById(R.id.editTextFats);
        notesEditText = (EditText) findViewById(R.id.editTextNotes);

        carbsMinusButton = (Button) findViewById(R.id.minusButton);
        carbsPlusButton = (Button) findViewById(R.id.plusButton);
        bgMinusButton = (Button) findViewById(R.id.minusButton2);
        bgPlusButton = (Button) findViewById(R.id.plusButton2);
        totalBolusMinusButton = (Button) findViewById(R.id.minusButton3);
        totalBolusPlusButton = (Button) findViewById(R.id.plusButton3);
        confirmButton = (Button) findViewById(R.id.confirm_button);
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);

        mealBolusCheckbox = (CheckBox) findViewById(R.id.mealBolusCheckbox);
        correctionBolusCheckBox = (CheckBox) findViewById(R.id.correctionBolusCheckbox);

        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            bgUnitText.setText("mg/dL");
            plusMinusDifference = 1;
            maxBloodGlucose = 400;

            if(Sensor.isActive()){
                bloodGlucoseEditText.setText(String.valueOf(mgdl_value));
            } else {
                bloodGlucoseEditText.setText("");
            }
        } else {
            bgUnitText.setText("mmol/l");
            plusMinusDifference = 0.1;
            maxBloodGlucose = 27;

            if(Sensor.isActive()){
                bloodGlucoseEditText.setText(String.valueOf(mmol_value));
            } else {
                bloodGlucoseEditText.setText("");
            }
        }

        mealBolus();
        correctionBolus();
        totalBolus();

        carbsPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carbsValue = getCarbsText();
                carbsValue += 1;

                if(carbsValue < 0){
                    carbsValue = 0;
                    carbsEditText.setText("0");
                } else {
                    carbsEditText.setText(String.valueOf(carbsValue));
                }
            }
        });

        carbsMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carbsValue = getCarbsText();
                carbsValue -= 1;

                if(carbsValue < 0){
                    carbsValue = 0;
                    carbsEditText.setText("0");
                } else {
                    carbsEditText.setText(String.valueOf(carbsValue));
                }
            }
        });

        bgPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgValue = getBloodGlucoseText();
                bgValue += plusMinusDifference;

                if(bgValue < 0){
                    bgValue = 0;
                    bloodGlucoseEditText.setText("0");
                } else {
                    bloodGlucoseEditText.setText(String.valueOf(roundUnits(bgValue)));
                }
            }
        });

        bgMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgValue = getBloodGlucoseText();
                bgValue -= plusMinusDifference;

                if(bgValue < 0){
                    bgValue = 0;
                    bloodGlucoseEditText.setText("0");
                } else {
                    bloodGlucoseEditText.setText(String.valueOf(roundUnits(bgValue)));
                }
            }
        });

        totalBolusPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalBolus = getTotalBolusText();
                totalBolus += bolusIncrement();

                if(totalBolus < 0){
                    totalBolus = 0;
                    totalBolusEditText.setText("0.00");
                } else {
                    totalBolusEditText.setText(String.valueOf(roundUnits(totalBolus)));
                }
            }
        });

        totalBolusMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalBolus = getTotalBolusText();
                totalBolus -= bolusIncrement();

                if(totalBolus < 0){
                    totalBolus = 0;
                    totalBolusEditText.setText("0.00");
                } else {
                    totalBolusEditText.setText(String.valueOf(roundUnits(totalBolus)));
                }
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAll();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), Preferences.class);
                startActivityForResult(myIntent, 0);
            }
        });

//        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver,
//                new IntentFilter(Intents.BOLUSCALC_IOB_RECEIVER));
//
//        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver2,
//                new IntentFilter(Intents.BOLUSCALC_COB_RECEIVER));

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String iob = intent.getStringExtra("iobValue");
                if (iob != null) {
                    calcIOB = iob;
                    Toast.makeText(getApplicationContext(), ("iobValue: " + calcIOB), Toast.LENGTH_SHORT).show();
                }
            }
        };

        statusReceiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String cob = intent.getStringExtra("cob");
                if (cob != null) {
                    Toast.makeText(getApplicationContext(), ("cob: " + calcCOB), Toast.LENGTH_SHORT).show();
                    calcCOB = cob;
                }
            }
        };


        if (Pref.getBooleanDefaultFalse("plus_minus_buttons_in_calc")) {
            carbsPlusButton.setVisibility(View.VISIBLE);
            carbsMinusButton.setVisibility(View.VISIBLE);

            bgPlusButton.setVisibility(View.VISIBLE);
            bgMinusButton.setVisibility(View.VISIBLE);

            totalBolusPlusButton.setVisibility(View.VISIBLE);
            totalBolusMinusButton.setVisibility(View.VISIBLE);

        } else {
            carbsPlusButton.setVisibility(View.GONE);
            carbsMinusButton.setVisibility(View.GONE);

            bgPlusButton.setVisibility(View.GONE);
            bgMinusButton.setVisibility(View.GONE);

            totalBolusPlusButton.setVisibility(View.GONE);
            totalBolusMinusButton.setVisibility(View.GONE);
        }

        if (Pref.getBooleanDefaultFalse("fats_proteins_enabled")) {
            proteinsAndFatsTable.setVisibility(View.VISIBLE);
        } else {
            proteinsAndFatsTable.setVisibility(View.GONE);
        }
        carbsEditText.addTextChangedListener(textWatcherCarbs);
        bloodGlucoseEditText.addTextChangedListener(textWatcherBG);



        //low warning
        if (getBloodGlucoseText() < lowMark()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(BolusCalculatorActivity.this);
            builder.setCancelable(true);
            builder.setMessage(getString(R.string.dialog_low_glucose));

            builder.setPositiveButton(getString(R.string.proceed), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        //low occurs at
        if (BgGraphBuilder.low_occurs_at > 0 && getBloodGlucoseText() > lowMark()) {
            final double now = JoH.ts();
            final double predicted_low_in_mins = (BgGraphBuilder.low_occurs_at - now) / 60000;
            Toast.makeText(getApplicationContext(),
                    getString(R.string.low_predicted) + " " + getString(R.string.in) + ": " + (int) predicted_low_in_mins + getString(R.string.space_mins) +
                            "\n" + getString(R.string.dialog_caution),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendValuesToCalc(String key, String value, String intent){
        final Intent bolusCalc = new Intent(intent);
        bolusCalc.putExtra(key, value);
        //LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(bolusCalc);
        Log.d(TAG, "Bolus Calc Update: " + key + " / " + value);
    }


    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch (view.getId()) {
            case R.id.mealBolusCheckbox:
                if (!checked) {
                    totalBolus -= mealBolus();
                    mealBolusTextView.setText("0.00");
                }
                if (checked) {
                    mealBolusTextView.setText(String.valueOf(mealBolus()));
                }
                break;
            case R.id.correctionBolusCheckbox:
                if (!checked) {
                    totalBolus -= correctionBolus();
                    correctionBolusTextView.setText("0.00");
                }
                if (checked) {
                    correctionBolusTextView.setText(String.valueOf(correctionBolus()));

                }
                break;
        }
        totalBolus();
    }

    TextWatcher textWatcherCarbs = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mealBolus();
            totalBolus();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (carbsValue < 0) {
                carbsEditText.setText("0");
            }
            showDialogues();
        }
    };

    TextWatcher textWatcherBG = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            correctionBolus();
            totalBolus();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (bgValue < 0) {
                bloodGlucoseEditText.setText("0");
            }
            showDialogues();
        }
    };

    private void showDialogues() {
        //max bolus
        if (getTotalBolusText() > maxBolus()) {
            totalBolus = maxBolus();
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dialog_bolus_constraint),
                    Toast.LENGTH_SHORT).show();
            totalBolusEditText.setText(String.valueOf(maxBolus()));
        }

        //max carbs
        if (getCarbsText() > getMaxCarbs()) {
            carbsValue = getMaxCarbs();
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dialog_carbs_constraint),
                    Toast.LENGTH_SHORT).show();
            carbsEditText.setText(String.valueOf(getMaxCarbs()));
        }

        //max blood glucose
        if (getBloodGlucoseText() > maxBloodGlucose) {
            bgValue = maxBloodGlucose;
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dialog_high_glucose),
                    Toast.LENGTH_SHORT).show();
            bloodGlucoseEditText.setText(String.valueOf(maxBloodGlucose));
        }
    }

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

    private double bolusIncrement() {
        return Double.parseDouble(Pref.getString("bolus_increment", ""));
    }

    private int maxBolus() {
        return Integer.parseInt(Pref.getString("max_bolus_value", ""));
    }

    public double getIOB() {
        final List<Treatments> treatmentsList = Treatments.latestForGraph(50000, JoH.tsl() - Constants.WEEK_IN_MS, JoH.tsl());
        Iob response = new Iob();

        long time = JoH.tsl();
        final double stepms = 5 * MINUTE_IN_MS;
        Pair<Double, Double> result;
        Treatments treatment = Treatments.last();

        double mytime = ((long) (treatment.timestamp / stepms)) * stepms;

//        if (!treatmentsList.isEmpty()) {
//            result = Treatments.calculateIobActivityFromTreatmentAtTime(treatment, time, false);
//            result
//            response.iob = result.first;
//        }
        
        result = Treatments.calculateLegacyIobActivityFromTreatmentAtTime(treatment, time);


        return result.first;
    }

    int getMaxCarbs(){
        //what amount of carbs will get to the max bolus
        maxCarbs = (int) (maxBolus() * Profile.getCarbRatio(time));
        return maxCarbs;
    }

    private double correctAbove() {
        return Double.parseDouble(Pref.getString("correct_above_value", ""));
    }

    private double lowMark() {
        return JoH.tolerantParseDouble(Pref.getString("lowValue", "70"), 70d);
    }

    private double mealBolus() {
        double carbRatio = Profile.getCarbRatio(time);
        double mealBolus = getCarbsText() / carbRatio;

        mealBolusTextView.setText(df.format(mealBolus));
        return Double.parseDouble(df.format(mealBolus));
    }

    private double correctionBolus() {
        double target = Profile.getTargetRangeInUnits(time);
        double correctionBolus;

        if (getBloodGlucoseText() >= correctAbove()) {
            correctionBolus = ((getBloodGlucoseText() - target) / Profile.getSensitivity(time));
        } else {
            correctionBolus = 0;
        }

        correctionBolusTextView.setText(df.format(correctionBolus));
        return Double.parseDouble(df.format(correctionBolus));
    }

    private double totalBolus() {
        totalBolus = roundUnits(getMealBolus() + getCorrectionBolus());
        totalBolusEditText.setText(String.valueOf(totalBolus));

        return totalBolus;
    }

    public double roundUnits(double num) {
        num = Math.ceil(num / bolusIncrement()) * bolusIncrement();
        return Double.parseDouble(df.format(num));
    }

    private void submitAll() {
        String mystring = "";
        String treatment_text = getNotesEntry().trim();

        if (getBloodGlucoseText() != 0 && getBloodGlucoseText() != mgdl_value)
            mystring += getBloodGlucoseText() + " blood ";
        if (getCarbsText() != 0) mystring += getCarbsText() + " carbs ";

        mystring += String.valueOf(getTotalBolusText()).replace(",", ".") + " units ";

        Log.d(TAG, "Got treatment note: " + treatment_text);
        Treatments.create_note(treatment_text, time, -1); // timestamp?
        Home.staticRefreshBGCharts();

        values = new HashMap<String, String>();
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

//        try {
//            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(statusReceiver);
//        } catch (Exception e) {
//            Log.e(TAG, "Exception unregistering broadcast receiver: " + e);
//        }
//
//        try {
//            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(statusReceiver2);
//        } catch (Exception e) {
//            Log.e(TAG, "Exception unregistering broadcast receiver: " + e);
//        }
    }
}