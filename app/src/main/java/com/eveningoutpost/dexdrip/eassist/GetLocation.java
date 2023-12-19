package com.eveningoutpost.dexdrip.eassist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import androidx.core.app.ActivityCompat;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

// jamorham

// Do our best to get the most accurate location and geocode we can for emergency message feature

// Using old (9.x) apis of play services due to legacy compatibility

public class GetLocation {

    private static final String TAG = GetLocation.class.getSimpleName();


    private static GoogleApiClient mApiClient;
    private static volatile Location lastLocation;
    private static volatile String lastAddress;
    private static long addressUpdated = 0;


    public static void prepareForLocation() {
        // turn on wifi? gps? bluetooth?
        JoH.setBluetoothEnabled(xdrip.getAppContext(), true);
    }


    public static String getBestLocation() {

        if (lastLocation == null) {
            return "Location unknown!";
        }

        if (lastAddress == null) {
            return "GPS: " + lastLocation.getLatitude() + "," + lastLocation.getLongitude() + accuracyAddendum();
        }

        return lastAddress + accuracyAddendum();
    }

    private static String accuracyAddendum() {
        return lastLocation.hasAccuracy() ? " (+/- " + JoH.qs(lastLocation.getAccuracy(), 0) + "m)" : "";
    }

    public static String getMapUrl() {
        if (lastLocation == null) return "";
        return "https://maps.google.com/?q=" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
    }


    @Getter
    private final static long GPS_ACTIVE_TIME = 60000;

    public synchronized static void getLocation() {

        final Context context = xdrip.getAppContext();


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            UserError.Log.wtf(TAG, "No permission to obtain location");
            return;
        }


        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)

                //.addConnectionCallbacks(context)
                //.addOnConnectionFailedListener(context)
                .build();

        mApiClient.blockingConnect(60, TimeUnit.SECONDS);


        final Runnable runnable = () -> {
            if (mApiClient.isConnected()) {
                @SuppressLint("MissingPermission") final Location location = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
                if (location != null) {
                    lastLocation = location;
                    UserError.Log.d(TAG, location.toString());
                    lastAddress = getStreetLocation(location.getLatitude(), location.getLongitude());
                    UserError.Log.d(TAG, "Address: " + lastAddress);
                    addressUpdated = JoH.tsl();

                    if (ActivityCompat.checkSelfPermission(xdrip.getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(xdrip.getAppContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        UserError.Log.wtf(TAG, "Could not determine location as permission has been removed!");
                        return;
                    }

                    final LocationCallback callback = getLocationCallback();

                    Inevitable.task("update gps location", 200, () -> {
                        UserError.Log.d(TAG, "Requesting live GPS updates");
                        LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient,
                                getLocationRequest(), callback, Looper.getMainLooper());
                    });
                    Inevitable.task("remove gps updates " + JoH.tsl(), GPS_ACTIVE_TIME,
                            () -> LocationServices.FusedLocationApi.removeLocationUpdates(mApiClient, callback));

                } else {
                    UserError.Log.e(TAG, "Location result was null");
                    // TODO retry ?
                }
            } else {
                UserError.Log.e(TAG, "Could not connect google api");
            }
        };


        if (!mApiClient.isConnected()) {
            mApiClient.connect();
            UserError.Log.d(TAG, "Delaying location request as api not connected");
            Inevitable.task("get location", 5000, runnable);
        } else {
            runnable.run();
        }

    }

    private static LocationRequest getLocationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setExpirationDuration(GPS_ACTIVE_TIME)
                .setMaxWaitTime(1000);
    }

    private static final float SKIP_DISTANCE = 100;

    private static LocationCallback getLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                final Location thisLocation = locationResult.getLastLocation();
                UserError.Log.d(TAG, "Got location update callback!! " + thisLocation);
                if ((lastLocation == null)
                        || thisLocation.getAccuracy() < lastLocation.getAccuracy()
                        || ((thisLocation.getAccuracy() < SKIP_DISTANCE) && (thisLocation.distanceTo(lastLocation) > SKIP_DISTANCE))) {

                    lastLocation = thisLocation;
                    UserError.Log.d(TAG, "Got location UPDATED element: " + lastLocation);
                    Inevitable.task("update-street-location", 6000, () -> lastAddress = getStreetLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                }
            }
        };
    }


    public static String getStreetLocation(double latitude, double longitude) {
        try {
            final Geocoder geocoder = new Geocoder(xdrip.getAppContext(), Locale.getDefault());
            final List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            UserError.Log.d(TAG, addresses.toString());
            final String address = addresses.get(0).getAddressLine(0);
            UserError.Log.d(TAG, "Street address: " + address);
            return address;

        } catch (IndexOutOfBoundsException | NullPointerException e) {
            UserError.Log.e(TAG, "Couldn't isolate street address");
        } catch (IOException e) {
            UserError.Log.e(TAG, "Location error (reboot sometimes helps fix geocoding): " + e);

        }
        return null;

    }
}
