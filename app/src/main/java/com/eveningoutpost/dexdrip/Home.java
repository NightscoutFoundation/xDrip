package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Services.WixelReader;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.eveningoutpost.dexdrip.utils.DatabaseUtil;
import com.eveningoutpost.dexdrip.utils.ShareNotification;


import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.ViewportChangeListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;


public class Home extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "xDrip";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    SharedPreferences prefs;
    Viewport tempViewport = new Viewport();
    Viewport holdViewport = new Viewport();
    public float left;
    public float right;
    public float top;
    public float bottom;
    public boolean updateStuff;
    public boolean updatingPreviewViewport = false;
    public boolean updatingChartViewport = false;
    boolean isBTWixel;
    boolean isBTShare;
    boolean isWifiWixel;

    public BgGraphBuilder bgGraphBuilder;
    BroadcastReceiver _broadcastReceiver;
    BroadcastReceiver newDataReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(getApplicationContext());
        collectionServiceStarter.start(getApplicationContext());
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_source, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        checkEula();
        setContentView(R.layout.activity_home);
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
    protected void onResume(){
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
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
        holdViewport.set(0, 0, 0, 0);
        setupCharts();
        updateCurrentBgInfo();
    }

    public void setupCharts() {
        bgGraphBuilder = new BgGraphBuilder(this);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);

        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraphBuilder.lineData());
        previewChart.setLineChartData(bgGraphBuilder.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());
        setViewport();
    }

    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport, false);
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
                chart.setCurrentViewport(newViewport, false);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff == true) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right  >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart), false);
        } else {
            previewChart.setCurrentViewport(holdViewport, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null) {
            unregisterReceiver(_broadcastReceiver);
        }
        if(newDataReceiver != null) {
            unregisterReceiver(newDataReceiver);
        }
    }

    public void updateCurrentBgInfo() {
        final TextView currentBgValueText = (TextView) findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        notificationText.setText("");
        isBTWixel = CollectionServiceStarter.isBTWixel(getApplicationContext());
        isBTShare = CollectionServiceStarter.isBTShare(getApplicationContext());
        isWifiWixel = CollectionServiceStarter.isWifiWixel(getApplicationContext());
        if(isBTShare) {
            if((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)) {
                notificationText.setText("Unfortunately your android version does not support Bluetooth Low Energy");
            } else {
                String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
                if (receiverSn.compareTo("SM00000000") == 0 || receiverSn.length() == 0) {
                    notificationText.setText("Please set your Dex Receiver Serial Number in App Settings");
                } else {
                    if (receiverSn.length() < 10) {
                        notificationText.setText("Double Check Dex Receiver Serial Number, should be 10 characters, don't forget the letters");
                    } else {
                        if (ActiveBluetoothDevice.first() == null) {
                            notificationText.setText("Now pair with your Dexcom Share");
                        } else {
                            if (!Sensor.isActive()) {
                                notificationText.setText("Now choose start your sensor in your settings");
                            } else {
                                displayCurrentInfo();
                            }
                        }
                    }
                }
            }
        }
        if(isBTWixel) {
            if ((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)) {
                notificationText.setText("Unfortunately your android version does not support Bluetooth Low Energy");
            } else {
                if (ActiveBluetoothDevice.first() == null) {
                    notificationText.setText("First pair with your BT device!");
                } else {
                    if (Sensor.isActive() && (Sensor.currentSensor().started_at + (60000 * 60 * 2)) < new Date().getTime()) {
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
                            if(BgReading.latestUnCalculated(2).size() < 2) {
                                notificationText.setText("Please wait, need 2 readings from transmitter first.");
                            } else {
                                List<Calibration> calibrations = Calibration.latest(2);
                                if (calibrations.size() < 2) {
                                    notificationText.setText("Please enter two calibrations to get started!");
                                }
                            }
                        }
                    } else if (Sensor.isActive() && ((Sensor.currentSensor().started_at + (60000 * 60 * 2))) >= new Date().getTime()) {
                        double waitTime = ((Sensor.currentSensor().started_at + (60000 * 60 * 2)) - (new Date().getTime())) / (60000);
                        notificationText.setText("Please wait while sensor warms up! (" + String.format("%.2f", waitTime) + " minutes)");
                    } else {
                        notificationText.setText("Now start your sensor");
                    }
                }
            }
        }
        if(isWifiWixel) {
            if (!WixelReader.IsConfigured(getApplicationContext())) {
                notificationText.setText("First configure your wifi wixel reader ip addresses");
            } else {
                if (Sensor.isActive() && (Sensor.currentSensor().started_at + (60000 * 60 * 2)) < new Date().getTime()) {
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
                        notificationText.setText("Please wait, need 2 readings from transmitter first.");
                    }
                } else if (Sensor.isActive() && ((Sensor.currentSensor().started_at + (60000 * 60 * 2))) >= new Date().getTime()) {
                    double waitTime = ((Sensor.currentSensor().started_at + (60000 * 60 * 2)) - (new Date().getTime())) / (60000);
                    notificationText.setText("Please wait while sensor warms up! (" + String.format("%.2f", waitTime) + " minutes)");
                } else {
                    notificationText.setText("Now start your sensor");
                }
            }
        }
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    public void displayCurrentInfo() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        BgReading lastBgreading = BgReading.lastNoSenssor();
        boolean predictive = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("predictive_bg", false);
        if(isBTShare) { predictive = false; }
        if (lastBgreading != null) {
            double estimate = 0;
            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.timestamp > 0) {
                notificationText.setText("Signal Missed");
                if(!predictive){
                    estimate=lastBgreading.calculated_value;
                } else {
                    estimate = BgReading.estimated_bg(lastBgreading.timestamp + (6000 * 7));
                }
                currentBgValueText.setText(bgGraphBuilder.unitized_string(estimate));
                currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                if(!predictive){
                    estimate=lastBgreading.calculated_value;
                    String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                    String slope_arrow = BgReading.slopeArrow((lastBgreading.calculated_value_slope * 60000));
                    if(lastBgreading.hide_slope) {
                        slope_arrow = "";
                    }
                    currentBgValueText.setText( stringEstimate + " " + slope_arrow);
                } else {
                    estimate = BgReading.activePrediction();
                    String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                    currentBgValueText.setText( stringEstimate + " " + BgReading.slopeArrow());
                }
            }
            if(bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
                currentBgValueText.setTextColor(Color.parseColor("#C30909"));
            } else if(bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
                currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
            } else {
                currentBgValueText.setTextColor(Color.WHITE);
            }
        }
    setupCharts();
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
                    return DatabaseUtil.saveSql(getBaseContext());
                }

                @Override
                protected void onPostExecute(String filename) {
                    super.onPostExecute(filename);

                    final Context ctx = getApplicationContext();

                    Toast.makeText(ctx, "Export stored at " + filename, Toast.LENGTH_SHORT).show();

                    final NotificationCompat.Builder n = new NotificationCompat.Builder(ctx);
                    n.setContentTitle("Export complete");
                    n.setContentText("Ready to be sent.");
                    n.setAutoCancel(true);
                    n.setSmallIcon(R.drawable.ic_action_communication_invert_colors_on);
                    ShareNotification.viewOrShare("application/octet-stream", Uri.fromFile(new File(filename)), n, ctx);

                    final NotificationManager manager = (NotificationManager) ctx.getSystemService(Service.NOTIFICATION_SERVICE);
                    manager.notify(Notifications.exportCompleteNotificationId, n.build());
                }
            }.execute();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
