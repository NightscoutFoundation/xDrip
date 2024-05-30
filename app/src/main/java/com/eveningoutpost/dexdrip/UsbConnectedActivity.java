package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.SyncingService;

public class UsbConnectedActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SyncingService.isG4Connected(getApplicationContext())){
            Intent checkInIntent = new Intent(getApplicationContext(), CalibrationCheckInActivity.class);
            startActivity(checkInIntent);
            finish();
        } else {
            //TODO: Put check for usb wixel in here as an elseif
            Intent homeIntent = new Intent(getApplicationContext(), Home.class);
            startActivity(homeIntent);
            finish();
        }
        setContentView(R.layout.activity_usb_connected);
    }
}
