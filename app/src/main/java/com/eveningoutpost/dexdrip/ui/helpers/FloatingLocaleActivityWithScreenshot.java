package com.eveningoutpost.dexdrip.ui.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.util.Locale;

/**
 * JamOrHam
 */
public abstract class FloatingLocaleActivityWithScreenshot extends ActivityWithMenu {

    public static final String FORCE_ACTIVITY_LANGUAGE = "FORCE_ACTIVITY_LANGUAGE";
    public static final String SCREENSHOT_AND_EXIT = "SCREENSHOT_AND_EXIT";

    private static final int MY_PERMISSIONS_REQUEST_STORAGE_SCREENSHOT = 39032;

    public static volatile String localeString = null;
    private Locale locale;


    private Context specialContext;

    private Context oldContext;

    @Override
    protected void attachBaseContext(Context newBase) {
        if (localeString != null) {
            locale = new Locale(localeString);
            specialContext = LocaleHelper.setLocale(newBase, locale);
            super.attachBaseContext(specialContext);
        } else {
            super.attachBaseContext(newBase);
        }
    }

    @Override
    protected void onResume() {
        if (localeString != null) {
            oldContext = xdrip.getAppContext();
            xdrip.setContextAlways(specialContext);
            if (checkPermissions()) {
                doScreenShot();
            }
        }
        super.onResume();
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE_SCREENSHOT);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE_SCREENSHOT) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                doScreenShot();
            } else {
                JoH.static_toast_long(this, "Cannot download without storage permission");
            }
        }
    }

    private void doScreenShot() {
        Inevitable.task("screenshot-screen", 1500, () -> JoH.runOnUiThread(() -> {
            View rootView = getWindow().getDecorView().getRootView();
            // TODO probably should centralize this code
            String file_name = "xDrip-Status-Screenshot-" + JoH.dateTimeText(JoH.tsl()).replace(" ", "-").replace(":", "-").replace(".", "-") + ".png";
            final String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Screenshots";
            JoH.bitmapToFile(JoH.screenShot(rootView, "xDrip+ Status @ " + JoH.dateText(JoH.tsl())), dirPath, file_name);
            JoH.shareImage(getApplicationContext(), new File(dirPath + "/" + file_name));
            finish();
        }));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (localeString != null) {
            xdrip.setContextAlways(oldContext);
        }
    }

    @Override
    protected void onDestroy() {
        localeString = null;
        super.onDestroy();
    }

}
