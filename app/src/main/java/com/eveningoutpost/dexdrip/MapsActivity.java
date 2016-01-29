package com.eveningoutpost.dexdrip;

// jamorham

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String defaultLocation = "-31.988644,115.515637"; // default bogus position
    private static final String TAG = "jamorham map";
    public static String lastGeoLocation = defaultLocation;
    private static List<Double> longs = new ArrayList<Double>();
    private static List<Double> lats = new ArrayList<Double>();
    private static boolean active = false;
    private static GoogleMap mMap;

    // receive updates from elsewhere
    public static void newMapLocation(String location, long when) {
        try {
            if (location != null) {
                lastGeoLocation = location;
                String[] splits = lastGeoLocation.split(",");
                if (splits.length == 2) {
                    Double thislat = Double.parseDouble(splits[0]);
                    Double thislong = Double.parseDouble(splits[1]);
                    if ((thislat != 0) && (thislong != 0)) {
                        if (longs.size() > 0) {
                            if ((longs.get(longs.size() - 1).equals(thislong))
                                    && (lats.get(lats.size() - 1).equals(thislat))) {
                                return; // dupe
                            }
                        }
                        longs.add(thislong);
                        lats.add(thislat);
                        if (longs.size() > 20) {
                            longs.remove(0);
                            lats.remove(0);
                        }
                    }
                }
                if (active) redrawmap();
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception in newmaplocation: " + e.toString());
        }
    }

    private static void redrawmap() {
        if (mMap == null) return;
        mMap.clear();

        String[] splits = lastGeoLocation.split(",");
        // sanity check goes here

        LatLng mylocation = new LatLng(Double.parseDouble(splits[0]), Double.parseDouble(splits[1]));
        CircleOptions circleOptions = new CircleOptions()
                .center(mylocation)
                .strokeWidth(2)
                .strokeColor(Color.GRAY)
                .radius(1500);

        String title = "";

        if (lastGeoLocation.equals(defaultLocation)) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylocation, 16));
            title = "No location data yet";
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylocation, 13));
        }

        if (lats.size() > 0) {
            PolylineOptions mylines = new PolylineOptions();
            for (int c = 0; c < lats.size(); c++) {
                mylines.add(new LatLng(lats.get(c), longs.get(c)));
            }
            mylines.width(1);
            mylines.color(Color.parseColor("#2ba367"));
            mMap.addPolyline(mylines);
        }

        mMap.addCircle(circleOptions);
        mMap.addMarker(new MarkerOptions()
                .position(mylocation)
                .title(title)
                .alpha(0.9f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.jamorham_parakeet_marker)));

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        active = true;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        active = true;
        redrawmap();
    }

    @Override
    public void onPause() {
        active = false;
        super.onPause();
    }
}
