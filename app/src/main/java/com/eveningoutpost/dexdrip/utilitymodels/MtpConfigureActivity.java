package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.Build;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.databinding.ActivityMtpConfigureBinding;
import com.eveningoutpost.dexdrip.utils.usb.UsbTools;

// jamorham

public class MtpConfigureActivity extends AppCompatActivity {

    final NanoStatus nanoStatus = new NanoStatus("mtp-configure", 300);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < 24) {
            JoH.static_toast_long("Needs Android 7 or above!");
            finish();
        }

        final ActivityMtpConfigureBinding binding = ActivityMtpConfigureBinding.inflate(getLayoutInflater());
        binding.setNano(nanoStatus);
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);

        nanoStatus.setDoveTail(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MtpConfigure.handleConnect(UsbTools.getUsbDevice(0x0bb4, 0x0c02, "oFi"));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        nanoStatus.setRunning(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nanoStatus.setRunning(false);
    }

}
