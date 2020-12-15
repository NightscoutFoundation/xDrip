package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
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
import java.util.Map;

import static com.eveningoutpost.dexdrip.Home.startHomeWithExtra;
import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.UtilityModels.Unitized.mmolConvert;

// jamorham xdrip plus

public class BolusCalculatorActivity extends AppCompatActivity {

    private TextView correctionBolusTextView, mealBolusTextView, bgUnitText;
    private CheckBox mealBolusCheckbox, correctionBolusCheckBox;
    public Button carbsMinusButton, carbsPlusButton, bgMinusButton, bgPlusButton,
            totalBolusMinusButton, totalBolusPlusButton, confirmButton;

    private TableLayout proteinsAndFatsTable;
    private EditText carbsEditText, bloodGlucoseEditText, totalBolusEditText, notesEditText, proteinsEditText, fatsEditText;

    private static final String menu_name = "Bolus Calculator";
    private static final String TAG = "BolusCalculator";
    private BroadcastReceiver statusReceiver;

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

    int maxCarbs = 300;
    int maxBloodGlucose = 400;

    int mgdl_value = (int) last.getDg_mgdl();
    double mmol_value = roundDouble(mmolConvert(last.getDg_mgdl()), 1);
    DecimalFormat df = new DecimalFormat("#0.00");

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

        mealBolusCheckbox = (CheckBox) findViewById(R.id.mealBolusCheckbox);
        correctionBolusCheckBox = (CheckBox) findViewById(R.id.correctionBolusCheckbox);

        carbsEditText.addTextChangedListener(textWatcher);
        bloodGlucoseEditText.addTextChangedListener(textWatcher);
        totalBolusEditText.addTextChangedListener(textWatcher);

        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            bgUnitText.setText("mg/dL");
            plusMinusDifference = 1;
            bloodGlucoseEditText.setText(String.valueOf(mgdl_value));
        } else {
            bgUnitText.setText("mmol/l");
            plusMinusDifference = 0.1;
            bloodGlucoseEditText.setText(String.valueOf(mmol_value));
        }

        carbsPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carbsValue = getCarbsText();
                carbsEditText.setText(String.valueOf(carbsValue += 1));
            }
        });

        carbsMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carbsValue = getCarbsText();
                carbsEditText.setText(String.valueOf(carbsValue -= 1));
            }
        });

        bgPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgValue = getBloodGlucoseText();
                bloodGlucoseEditText.setText(String.valueOf(bgValue += plusMinusDifference));
            }
        });

        bgMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgValue = getBloodGlucoseText();
                bloodGlucoseEditText.setText(String.valueOf(bgValue -= plusMinusDifference));
            }
        });

        totalBolusPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalBolus = getTotalBolusText();
                totalBolusEditText.setText(String.valueOf(totalBolus += bolusIncrement()));
            }
        });

        totalBolusMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalBolus = getTotalBolusText();
                totalBolusEditText.setText(String.valueOf(totalBolus -= bolusIncrement()));
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAll();
            }
        });

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
        showDialogues();
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch (view.getId()) {
            case R.id.mealBolusCheckbox:
                if (!checked) {
                    totalBolus -= mealBolus();
                    mealBolusTextView.setText("0.00");
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
                    correctionBolusTextView.setText("0.00");
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
            if (carbsValue < 0) {
                carbsEditText.setText("0.00");
            }
            if (bgValue < 0) {
                bloodGlucoseEditText.setText("0.00");
            }

            mealBolus();
            correctionBolus();

            totalBolus = Double.parseDouble(totalBolusEditText.getText().toString());
            totalBolus();
        }
        @Override
        public void afterTextChanged(Editable s) {
            showDialogues();
        }
    };

    private void showDialogues() {
        //low occurs at
        if (BgGraphBuilder.low_occurs_at > 0 && getBloodGlucoseText() > lowMark()) {
            final double now = JoH.ts();
            final double predicted_low_in_mins = (BgGraphBuilder.low_occurs_at - now) / 60000;
            Toast.makeText(getApplicationContext(),
                    getString(R.string.low_predicted) + " " + getString(R.string.in) + ": " + (int) predicted_low_in_mins + getString(R.string.space_mins) +
                            "\n" + getString(R.string.dialog_caution),
                    Toast.LENGTH_SHORT).show();
        }

        //max bolus
        if (getTotalBolusText() > maxBolus()) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dialog_bolus_constraint),
                    Toast.LENGTH_SHORT).show();
            totalBolusEditText.setText(String.valueOf(maxBolus()));
        }

        //max carbs
        if (getCarbsText() > maxCarbs) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dialog_carbs_constraint),
                    Toast.LENGTH_SHORT).show();
            totalBolusEditText.setText(String.valueOf(maxCarbs));
        }

        //max blood glucose
        if (getBloodGlucoseText() > maxBloodGlucose) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dialog_high_glucose),
                    Toast.LENGTH_SHORT).show();
            bloodGlucoseEditText.setText(String.valueOf(maxBloodGlucose));
        }

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

    private double correctAbove(){
        return Double.parseDouble(Pref.getString("correct_above_value", ""));
    }

    double lowMark(){
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
        totalBolus = Double.parseDouble(df.format(getMealBolus() + getCorrectionBolus()));
        totalBolus = roundUnits(totalBolus);

        if (totalBolus < 0) {
            totalBolus = 0;
            totalBolusEditText.setText("0.00");
        }

        totalBolusEditText.setText(df.format(totalBolus));
        return roundUnits(totalBolus);
    }

    public double roundUnits(double num) {
        num = Double.parseDouble(df.format(num));
        return Math.ceil(num / bolusIncrement()) * bolusIncrement();
    }

    private static String getValue(String tab) { //if has value, return it, else just put a space
        if (values.containsKey(tab)) {
            return values.get(tab);
        } else {
            values.put(tab, "");
            return values.get(tab);
        }
    }

    private void submitAll() {
        //insulin, carbs, bloodtest, time
        //units, carbs,
        values.put("bloodtest", String.valueOf(getBloodGlucoseText()));
        values.put("carbs", String.valueOf(getCarbsText()));
        values.put("units", String.valueOf(getTotalBolusText()));

        PhoneKeypadInputActivity keypadActivity = new PhoneKeypadInputActivity();

        boolean nonzeroBloodValue = keypadActivity.isNonzeroValueInTab("bloodtest");
        boolean nonzeroCarbsValue = keypadActivity.isNonzeroValueInTab("carbs");

        String mystring = "";

        if (nonzeroBloodValue && getBloodGlucoseText() != mgdl_value)
            mystring += getValue("bloodtest") + " blood ";
        if (nonzeroCarbsValue) mystring += getValue("carbs") + " carbs ";

        double units = Double.parseDouble(getValue("units"));
        mystring += String.format("%.1f", units).replace(",", ".") + " units ";


        String treatment_text = getNotesEntry().trim();
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
    }
}