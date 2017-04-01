package com.eveningoutpost.dexdrip.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;

/**
 * Helper for checking if location services are enabled on the device.
 */
public class LocationHelper {

    static final String TAG = "dexdrip LocationHelper";
    /**
     * Determine if GPS is currently enabled.
     *
     * On Android 6 (Marshmallow), location needs to be enabled for Bluetooth discovery to work.
     *
     * @param context The current app context.
     * @return true if location is enabled, false otherwise.
     */
    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Prompt the user to enable GPS location if it isn't already on.
     *
     * @param parent The currently visible activity.
     */
    public static void requestLocation(final Activity parent) {
        if (LocationHelper.isLocationEnabled(parent)) {
            return;
        }

        // Shamelessly borrowed from http://stackoverflow.com/a/10311877/868533

        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setTitle(R.string.location_not_found_title);
        builder.setMessage(R.string.location_not_found_message);
        builder.setPositiveButton(R.string.location_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                parent.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    /**
     * Prompt the user to enable GPS location on devices that need it for Bluetooth discovery.
     *
     * Android 6 (Marshmallow) needs GPS enabled for Bluetooth discovery to work.
     *
     * @param activity The currently visible activity.
     */
    public static void requestLocationForBluetooth(final Activity activity) {
        // Location needs to be enabled for Bluetooth discovery on Marshmallow.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(activity,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                JoH.show_ok_dialog(activity, activity.getString(R.string.please_allow_permission), activity.getString(R.string.bluetooth_scan_location_permission), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        } catch (Exception e) {
                            JoH.static_toast_long(activity.getString(R.string.location_permission_exception, e));
                        }
                    }
                });
            }

            LocationHelper.requestLocation(activity);
        }
    }

/*    private static void toast(final Activity activity,final String msg) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            });
            android.util.Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }*/

    public static Boolean locationPermission(ActivityWithMenu act) {
        return ActivityCompat.checkSelfPermission(act, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
