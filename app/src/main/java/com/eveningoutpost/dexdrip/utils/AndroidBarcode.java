package com.eveningoutpost.dexdrip.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a helper class to facilitate asking for camera permission and returning
 * the scan result to the original instantiating activity
 */


public class AndroidBarcode extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String SCAN_INTENT = Intents.Scan.ACTION;
    private static Object returnTo;
    private Activity activity;
    final static int MY_PERMISSIONS_REQUEST_CAMERA = 1003;
    final static String TAG = "jamorham barcode";

    public AndroidBarcode(final Activity activity) {
        this.activity = activity;
    }

    public AndroidBarcode() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activity = (Activity) returnTo; // restore saved context
        returnTo = null;
        super.onCreate(savedInstanceState);
        requestPermission();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                actuallyStartScan();
            } else {
                JoH.static_toast_long(xdrip.gs(R.string.without_camera_permission_cannot_scan_barcode));
            }
        }
        try {
            finish();
        } catch (Exception e) {
            //
        }
    }

    private void actuallyStartScan() {
        new IntentIntegrator(activity)
                .setPrompt(xdrip.gs(R.string.scan_to_load_xdrip_settings))
                .setDesiredBarcodeFormats(list("QR_CODE", "CODE_128"))
                .initiateScan();
    }

    // TODO move to utils
    private static List<String> list(final String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                MY_PERMISSIONS_REQUEST_CAMERA);
    }

    public void scan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                try {
                    // are we in an actual activity?
                    requestPermission();
                } catch (NullPointerException e) {
                    returnTo = activity; // save return context
                    JoH.startActivity(AndroidBarcode.class);
                }
            } else {
                actuallyStartScan();
            }
        } else {
            actuallyStartScan();
        }
    }

}