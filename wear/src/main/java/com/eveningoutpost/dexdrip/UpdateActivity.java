package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.utils.AdbInstaller;
import com.eveningoutpost.dexdrip.utils.VersionFixer;

// jamorham

public class UpdateActivity extends Activity {

    private static boolean canInstallHere = DemiGod.isPresent();

    @Override
    protected void onResume() {
        super.onResume();
    }

    // TODO i18n

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(() -> {
            if (!canInstallHere) {
                JoH.static_toast_long("Checking ADB enabled");
                AdbInstaller.ping(() -> {
                    canInstallHere = true;
                    JoH.static_toast_long("ADB looks good");
                });
                JoH.threadSleep(10000);
            }
            if (canInstallHere) {
                if (JoH.pratelimit("forced update request", 60)) {

                    JoH.static_toast_long("Asking for update file");
                    VersionFixer.downloadApk();
                } else {
                    JoH.static_toast_long("Please wait at least 1 minute between requests");
                }
            } else {
                JoH.static_toast_long("ADB doesn't seem to be working");
            }
        }).start();
        finish();
    }

}
