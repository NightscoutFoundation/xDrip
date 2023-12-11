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
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;

/**
 * Helper for checking if location services are enabled on the device.
 */
public class LocationHelper {

    static final String TAG = "xDrip LocationHelper";
    private static final boolean newType = false;
    /**
     * Determine if Network provider is currently enabled.
     *
     * On Android 6 (Marshmallow), location needs to be enabled for Bluetooth discovery to work.
     *
     * @param context The current app context.
     * @return true if location is enabled, false otherwise.
     */
    public static boolean isLocationEnabled(final Context context) {
        try {
            final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 28) {
                return locationManager == null || locationManager.isLocationEnabled();
            } else {
                return locationManager == null || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Prompt the user to enable location if it isn't already on.
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
        try {
            builder.create().show();
        } catch (RuntimeException e) {
            Looper.prepare();
            builder.create().show();
        }
    }


    // TODO this is just temporary until sdk tools are updated
    private static final String ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION";
    /**
     * Prompt the user to enable GPS location on devices that need it for Bluetooth discovery.
     *
     * Android 6 (Marshmallow) needs GPS enabled for Bluetooth discovery to work.
     *
     * @param activity The currently visible activity.
     * @return true if we have needed permissions, false if we needed to ask for more
     */
    public static boolean requestLocationForBluetooth(final Activity activity) {
        // Location needs to be enabled for Bluetooth discovery on Marshmallow.

        if (newType && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED)
            || (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED)) {

                JoH.show_ok_dialog(activity, activity.getString(R.string.please_allow_permission), "Need bluetooth permissions", new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.BLUETOOTH_SCAN
                                    , Manifest.permission.BLUETOOTH_CONNECT},
                                    0);
                            // below is not ideal as we should really trap the activity result but it can come from different activities and there is no parent...
                            Inevitable.task("location-perm-restart", 6000, CollectionServiceStarter::restartCollectionServiceBackground);
                        } catch (Exception e) {
                            JoH.static_toast_long("Got Exception with Bluetooth Permission: " + e);
                        }
                    }
                });
                return false;

            }
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                JoH.show_ok_dialog(activity, activity.getString(R.string.please_allow_permission), activity.getString(R.string.without_location_scan_doesnt_work), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    0);
                            // below is not ideal as we should really trap the activity result but it can come from different activities and there is no parent...
                            Inevitable.task("location-perm-restart", 6000, CollectionServiceStarter::restartCollectionServiceBackground);
                        } catch (Exception e) {
                            JoH.static_toast_long("Got Exception with Location Permission: " + e);
                        }
                    }
                });
                return false;
            } else {
                // Android 10 check additional permissions
                if (Build.VERSION.SDK_INT >= 29) {
                    if (ContextCompat.checkSelfPermission(activity,
                            ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        JoH.show_ok_dialog(activity, activity.getString(R.string.please_allow_permission), activity.getString(R.string.android_10_need_background_location), new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ActivityCompat.requestPermissions(activity,
                                            new String[]{ACCESS_BACKGROUND_LOCATION},
                                            0);
                                } catch (Exception e) {
                                    JoH.static_toast_long("Got Exception with Android 10 Location Permission: " + e);
                                }
                            }
                        });
                        return false;
                    }
                }
            }

            LocationHelper.requestLocation(activity);
        }
        return true;
    }

    public static void requestLocationForEmergencyMessage(final Activity activity) {
        // Location needs to be enabled for Bluetooth discovery on Marshmallow.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(activity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                JoH.show_ok_dialog(activity, activity.getString(R.string.please_allow_permission), activity.getString(R.string.without_location_permission_emergency_cannot_get_location), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    0);
                        } catch (Exception e) {
                            JoH.static_toast_long("Got Exception with Location Permission: " + e);
                        }
                    }
                });
            }

            LocationHelper.requestLocation(activity);
        }
    }

    // TODO probably can use application context here
    public static boolean isLocationPermissionOk(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= 29) {
                if (ContextCompat.checkSelfPermission(context,
                        ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Boolean locationPermission(final Activity activity) {
        if (newType && Build.VERSION.SDK_INT >=  Build.VERSION_CODES.S) {
            return ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
                    && (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED));
        } else if (Build.VERSION.SDK_INT >= 29) {
                // check background location as well on android 10+
                return ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        && (ActivityCompat.checkSelfPermission(activity, ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED));
            } else {
                return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }

    }

}
