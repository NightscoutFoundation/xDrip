package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.eveningoutpost.dexdrip.models.JoH;

public class Agreement extends BaseAppCompatActivity {

    final static String prefmarker = "warning_agreed_to";
    boolean IUnderstand;
    CheckBox agreeCheckBox;
    Button saveButton;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);
        JoH.fixActionBar(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        IUnderstand = prefs.getBoolean(prefmarker, false);
        agreeCheckBox = (CheckBox) findViewById(R.id.agreeCheckBox2);
        agreeCheckBox.setChecked(IUnderstand);
        saveButton = (Button) findViewById(R.id.saveButton2);
        addListenerOnButton();
        if (xdrip.isRunningTest()) {
            findViewById(R.id.scrollView6).setScrollBarFadeDuration(100); // avoid espresso lameness
        }
    }

    public void addListenerOnButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                prefs.edit().putBoolean(prefmarker, agreeCheckBox.isChecked()).apply();
                IUnderstand = prefs.getBoolean(prefmarker, false);
                if (IUnderstand) {
                    Intent intent = new Intent(getApplicationContext(), Home.class);
                    startActivity(intent);
                    finish();
                }
            }

        });
    }
}
