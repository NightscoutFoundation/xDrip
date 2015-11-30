package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Constants;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.WixelReader;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.IdempotentMigrations;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.DatabaseUtil;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;


public class Home extends ActivityWithMenu {
    static String TAG = Home.class.getName();
    public static String menu_name = "xDrip";
    private boolean updateStuff;
    private boolean updatingPreviewViewport = false;
    private boolean updatingChartViewport = false;
    private BgGraphBuilder bgGraphBuilder;
    private SharedPreferences prefs;
    private Viewport tempViewport = new Viewport();
    private Viewport holdViewport = new Viewport();
    private boolean isBTShare;
    private BroadcastReceiver _broadcastReceiver;
    private BroadcastReceiver newDataReceiver;
    private LineChartView            chart;
    private PreviewLineChartView     previewChart;
    private TextView                 dexbridgeBattery;
    private TextView                 currentBgValueText;
    private TextView                 notificationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(getApplicationContext());
        collectionServiceStarter.start(getApplicationContext());
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_source, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        checkEula();
        new IdempotentMigrations(getApplicationContext()).performAll();
        setContentView(R.layout.activity_home);

        this.dexbridgeBattery = (TextView) findViewById(R.id.textBridgeBattery);
        this.currentBgValueText = (TextView) findViewById(R.id.currentBgValueRealTime);
        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            this.currentBgValueText.setTextSize(100);
        }
        this.notificationText = (TextView) findViewById(R.id.notices);
        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            this.notificationText.setTextSize(40);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            Log.d(TAG, "Maybe ignoring battery optimization");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName) &&
                    !prefs.getBoolean("requested_ignore_battery_optimizations", false)) {
                Log.d(TAG, "Requesting ignore battery optimization");

                prefs.edit().putBoolean("requested_ignore_battery_optimizations", true).apply();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void checkEula() {
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if (!IUnderstand) {
            Intent intent = new Intent(getApplicationContext(), LicenseAgreementActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkEula();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateCurrentBgInfo();
                }
            }
        };
        newDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                holdViewport.set(0, 0, 0, 0);
                setupCharts();
                updateCurrentBgInfo();
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(newDataReceiver, new IntentFilter(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA));
        holdViewport.set(0, 0, 0, 0);
        setupCharts();
        updateCurrentBgInfo();
    }

    public void setupCharts() {
        bgGraphBuilder = new BgGraphBuilder(this);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);

        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chart.getLayoutParams();
            params.topMargin = 130;
            chart.setLayoutParams(params);
        }

        chart.setZoomType(ZoomType.HORIZONTAL);

        //Transmitter Battery Level
        final Sensor sensor = Sensor.currentSensor();
        if (sensor != null && sensor.latest_battery_level != 0 && sensor.latest_battery_level <= Constants.TRANSMITTER_BATTERY_LOW && ! prefs.getBoolean("disable_battery_warning", false)) {
            Drawable background = new Drawable() {

                @Override
                public void draw(Canvas canvas) {

                    DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
                    int px = (int) (30 * (metrics.densityDpi / 160f));
                    Paint paint = new Paint();
                    paint.setTextSize(px);
                    paint.setAntiAlias(true);
                    paint.setColor(Color.parseColor("#FFFFAA"));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setAlpha(100);
                    canvas.drawText("transmitter battery", 10, chart.getHeight() / 3 - (int) (1.2 * px), paint);
                    if(sensor.latest_battery_level <= Constants.TRANSMITTER_BATTERY_EMPTY){
                        paint.setTextSize((int)(px*1.5));
                        canvas.drawText("VERY LOW", 10, chart.getHeight() / 3, paint);
                    } else {
                        canvas.drawText("low", 10, chart.getHeight() / 3, paint);
                    }
                }

                @Override
                public void setAlpha(int alpha) {}
                @Override
                public void setColorFilter(ColorFilter cf) {}
                @Override
                public int getOpacity() {return 0;}
            };
            chart.setBackground(background);
        }
        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraphBuilder.lineData());
        chart.setOnValueTouchListener(bgGraphBuilder.getOnValueSelectTooltipListener());
        previewChart.setLineChartData(bgGraphBuilder.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());
        setViewport();
    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart));
        } else {
            previewChart.setCurrentViewport(holdViewport);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null ) {
            try {
                unregisterReceiver(_broadcastReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "_broadcast_receiver not registered", e);
            }
        }
        if (newDataReceiver != null) {
            try {
                unregisterReceiver(newDataReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "newDataReceiver not registered", e);
            }
        }
    }

    public void updateCurrentBgInfo() {
        final TextView notificationText = (TextView) findViewById(R.id.notices);
        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            notificationText.setTextSize(40);
        }
        notificationText.setText("");
        notificationText.setTextColor(Color.RED);
        boolean isBTWixel = CollectionServiceStarter.isBTWixel(getApplicationContext());
        boolean isDexbridgeWixel = CollectionServiceStarter.isDexbridgeWixel(getApplicationContext());
        isBTShare = CollectionServiceStarter.isBTShare(getApplicationContext());
        boolean isWifiWixel = CollectionServiceStarter.isWifiWixel(getApplicationContext());
        if (isBTShare) {
            updateCurrentBgInfoForBtShare(notificationText);
        }
        if (isBTWixel || isDexbridgeWixel) {
            updateCurrentBgInfoForBtBasedWixel(notificationText);
        }
        if (isWifiWixel) {
            updateCurrentBgInfoForWifiWixel(notificationText);
        }
        if (prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n ALL ALERTS CURRENTLY DISABLED");
        } else if (prefs.getLong("low_alerts_disabled_until", 0) > new Date().getTime()
			&&
			prefs.getLong("high_alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n LOW AND HIGH ALERTS CURRENTLY DISABLED");
        } else if (prefs.getLong("low_alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n LOW ALERTS CURRENTLY DISABLED");
        } else if (prefs.getLong("high_alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n HIGH ALERTS CURRENTLY DISABLED");
        } 
        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    private void updateCurrentBgInfoForWifiWixel(TextView notificationText) {
        if (!WixelReader.IsConfigured(getApplicationContext())) {
            notificationText.setText("First configure your wifi wixel reader ip addresses");
            return;
        }

        updateCurrentBgInfoCommon(notificationText);

    }

    private void updateCurrentBgInfoForBtBasedWixel(TextView notificationText) {
        if ((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            notificationText.setText("Unfortunately your android version does not support Bluetooth Low Energy");
            return;
        }

        if (ActiveBluetoothDevice.first() == null) {
            notificationText.setText("First pair with your BT device!");
            return;
        }
        updateCurrentBgInfoCommon(notificationText);
    }

    private void updateCurrentBgInfoCommon(TextView notificationText) {
        final boolean isSensorActive = Sensor.isActive();
        if(!isSensorActive){
            notificationText.setText("Now start your sensor");
            return;
        }

        final long now = System.currentTimeMillis();
        if (Sensor.currentSensor().started_at + 60000 * 60 * 2 >= now) {
            double waitTime = (Sensor.currentSensor().started_at + 60000 * 60 * 2 - now) / 60000.0;
            notificationText.setText("Please wait while sensor warms up! (" + String.format("%.2f", waitTime) + " minutes)");
            return;
        }

        if (BgReading.latest(2).size() > 1) {
            List<Calibration> calibrations = Calibration.latest(2);
            if (calibrations.size() > 1) {
                if (calibrations.get(0).possible_bad != null && calibrations.get(0).possible_bad == true && calibrations.get(1).possible_bad != null && calibrations.get(1).possible_bad != true) {
                    notificationText.setText("Possible bad calibration slope, please have a glass of water, wash hands, then recalibrate in a few!");
                }
                displayCurrentInfo();
            } else {
                notificationText.setText("Please enter two calibrations to get started!");
            }
        } else {
            if (BgReading.latestUnCalculated(2).size() < 2) {
                notificationText.setText("Please wait, need 2 readings from transmitter first.");
            } else {
                List<Calibration> calibrations = Calibration.latest(2);
                if (calibrations.size() < 2) {
                    notificationText.setText("Please enter two calibrations to get started!");
                }
            }
        }
    }

    private void updateCurrentBgInfoForBtShare(TextView notificationText) {
        if ((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            notificationText.setText("Unfortunately your android version does not support Bluetooth Low Energy");
            return;
        }

        String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
        if (receiverSn.compareTo("SM00000000") == 0 || receiverSn.length() == 0) {
            notificationText.setText("Please set your Dex Receiver Serial Number in App Settings");
            return;
        }

        if (receiverSn.length() < 10) {
            notificationText.setText("Double Check Dex Receiver Serial Number, should be 10 characters, don't forget the letters");
            return;
        }

        if (ActiveBluetoothDevice.first() == null) {
            notificationText.setText("Now pair with your Dexcom Share");
            return;
        }

        if (!Sensor.isActive()) {
            notificationText.setText("Now choose start your sensor in your settings");
            return;
        }

        displayCurrentInfo();
    }

    public void displayCurrentInfo() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        boolean isDexbridge = CollectionServiceStarter.isDexbridgeWixel(getApplicationContext());
        int bridgeBattery = prefs.getInt("bridge_battery", 0);

        if (isDexbridge) {
            if (bridgeBattery == 0) {
                dexbridgeBattery.setText("Waiting for packet");
            } else {
                dexbridgeBattery.setText("Bridge Battery: " + bridgeBattery + "%");
            }
            if (bridgeBattery < 50) dexbridgeBattery.setTextColor(Color.YELLOW);
            if (bridgeBattery < 25) dexbridgeBattery.setTextColor(Color.RED);
            else dexbridgeBattery.setTextColor(Color.GREEN);
            dexbridgeBattery.setVisibility(View.VISIBLE);
        } else {
            dexbridgeBattery.setVisibility(View.INVISIBLE);
        }

        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            dexbridgeBattery.setPaintFlags(dexbridgeBattery.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        BgReading lastBgReading = BgReading.lastNoSenssor();
        boolean predictive = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("predictive_bg", false);
        if (isBTShare) {
            predictive = false;
        }
        if (lastBgReading != null) {
            displayCurrentInfoFromReading(lastBgReading, predictive);
        }
        setupCharts();
    }

    private void displayCurrentInfoFromReading(BgReading lastBgReading, boolean predictive) {
        double estimate = 0;
        if ((new Date().getTime()) - (60000 * 11) - lastBgReading.timestamp > 0) {
            notificationText.setText("Signal Missed");
            if (!predictive) {
                estimate = lastBgReading.calculated_value;
            } else {
                estimate = BgReading.estimated_bg(lastBgReading.timestamp + (6000 * 7));
            }
            currentBgValueText.setText(bgGraphBuilder.unitized_string(estimate));
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            dexbridgeBattery.setPaintFlags(dexbridgeBattery.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            if (notificationText.getText().length()==0){
                notificationText.setTextColor(Color.WHITE);
            }
            if (!predictive) {
                estimate = lastBgReading.calculated_value;
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                String slope_arrow = lastBgReading.slopeArrow();
                if (lastBgReading.hide_slope) {
                    slope_arrow = "";
                }
                currentBgValueText.setText(stringEstimate + " " + slope_arrow);
            } else {
                estimate = BgReading.activePrediction();
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                currentBgValueText.setText(stringEstimate + " " + BgReading.activeSlopeArrow());
            }
        }
        int minutes = (int)(System.currentTimeMillis() - lastBgReading.timestamp) / (60 * 1000);
        notificationText.append("\n" + minutes + ((minutes==1)?" Minute ago":" Minutes ago"));
        List<BgReading> bgReadingList = BgReading.latest(2);
        if(bgReadingList != null && bgReadingList.size() == 2) {
            // same logic as in xDripWidget (refactor that to BGReadings to avoid redundancy / later inconsistencies)?
            if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
                notificationText.append("  ");
            } else {
                notificationText.append("\n");
            }
            notificationText.append(
                    bgGraphBuilder.unitizedDeltaString(true, true));
        }
        if(bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
            currentBgValueText.setTextColor(Color.parseColor("#C30909"));
        } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
            currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
        } else {
            currentBgValueText.setTextColor(Color.WHITE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_database) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    int permissionCheck = ContextCompat.checkSelfPermission(Home.this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(Home.this,
                                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                0);
                        return null;
                    } else {
                        return DatabaseUtil.saveSql(getBaseContext());
                    }

                }

                @Override
                protected void onPostExecute(String filename) {
                    super.onPostExecute(filename);
                    if (filename != null) {
                        SnackbarManager.show(
                                Snackbar.with(Home.this)
                                        .type(SnackbarType.MULTI_LINE)
                                        .duration(4000)
                                        .text("Exported to " + filename) // text to display
                                        .actionLabel("Share") // action button label
                                        .actionListener(new SnackbarUriListener(Uri.fromFile(new File(filename)))),
                                Home.this);
                    } else {
                        Toast.makeText(Home.this, "Could not export Database :(", Toast.LENGTH_LONG).show();
                    }
                }
            }.execute();

            return true;
        }

        if (item.getItemId() == R.id.action_import_db) {
            startActivity(new Intent(this, ImportDatabaseActivity.class));
            return true;
        }



        if (item.getItemId() == R.id.action_export_csv_sidiary) {
          new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    return DatabaseUtil.saveCSV(getBaseContext());
                }

                @Override
                protected void onPostExecute(String filename) {
                    super.onPostExecute(filename);
                    if (filename != null) {
                        SnackbarManager.show(
                                Snackbar.with(Home.this)
                                        .type(SnackbarType.MULTI_LINE)
                                        .duration(4000)
                                        .text("Exported to " + filename) // text to display
                                        .actionLabel("Share") // action button label
                                        .actionListener(new SnackbarUriListener(Uri.fromFile(new File(filename)))),
                                Home.this);
                    } else {
                        Toast.makeText(Home.this, "Could not export CSV :(", Toast.LENGTH_LONG).show();
                    }
                }
            }.execute();

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport);
                updatingChartViewport = false;
            }
        }
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }

    class SnackbarUriListener implements ActionClickListener {
        Uri uri;

        SnackbarUriListener(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void onActionClicked(Snackbar snackbar) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("application/octet-stream");
            startActivity(Intent.createChooser(shareIntent, "Share database..."));
        }
    }
}
