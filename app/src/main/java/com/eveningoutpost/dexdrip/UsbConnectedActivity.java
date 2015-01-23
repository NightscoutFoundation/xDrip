package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.SyncingService;
import com.eveningoutpost.dexdrip.Models.CalibrationCheckInActivity;
import com.eveningoutpost.dexdrip.R;

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
