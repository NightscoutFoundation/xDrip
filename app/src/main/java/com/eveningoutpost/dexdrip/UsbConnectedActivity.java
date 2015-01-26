package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.SyncingService;

public class UsbConnectedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SyncingService.isG4Connected(getApplicationContext())){
            Intent checkInIntent = new Intent(getApplicationContext(), CalibrationCheckInActivity.class);
            startActivity(checkInIntent);
            finish();
        } else {

        }
        setContentView(R.layout.activity_usb_connected);
    }
}
