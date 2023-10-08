package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;


public class LicenseAgreementActivity extends BaseAppCompatActivity {
    boolean IUnderstand;
    CheckBox agreeCheckBox;
    Button saveButton;
    SharedPreferences prefs;
    private static final String TAG = "LicenseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        IUnderstand = prefs.getBoolean("I_understand", false);
        try {
            setContentView(R.layout.activity_license_agreement);

            JoH.fixActionBar(this);
            findViewById(R.id.googlelicenses).setAlpha(0.5f);
            agreeCheckBox = findViewById(R.id.agreeCheckBox);
            agreeCheckBox.setChecked(IUnderstand);
            saveButton = findViewById(R.id.saveButton);
            addListenerOnButton();

        } catch (UnsupportedOperationException e) {
            JoH.static_toast_long("Unable to display license agreement? blocked by user? Cannot continue");
            finish();
        }
      /*  try {
            final int gplaystatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
            if (gplaystatus != ConnectionResult.SUCCESS) {
                findViewById(R.id.googlelicenses).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception: " + e.toString());
        }*/
    }

    public void viewGoogleLicenses(View myview) {
        startActivity(new Intent(this, OssLicensesMenuActivity.class));
    }

    public void viewWarning(View myview) {
        startActivity(new Intent(getApplicationContext(), Agreement.class));
    }

    public void addListenerOnButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                prefs.edit().putBoolean("I_understand", agreeCheckBox.isChecked()).apply();

                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
    }
}
