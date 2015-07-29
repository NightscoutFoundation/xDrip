package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Tables.BgReadingTable;
import com.eveningoutpost.dexdrip.Tables.CalibrationDataTable;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.stats.StatsActivity;
import com.eveningoutpost.dexdrip.utils.Preferences;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/5/14.
 */
public class NavDrawerBuilder {
    public List<Calibration> last_two_calibrations = Calibration.latest(2);
    public List<BgReading> last_two_bgReadings = BgReading.latestUnCalculated(2);
    public List<BgReading> bGreadings_in_last_30_mins = BgReading.last30Minutes();
    public boolean is_active_sensor = Sensor.isActive();
    public double time_now = new Date().getTime();
    public List<Intent> nav_drawer_intents = new ArrayList<>();
    public List<String> nav_drawer_options = new ArrayList<>();
    public Context context;

    public NavDrawerBuilder(Context aContext) {
        context = aContext;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if(IUnderstand == false) {
            this.nav_drawer_options.add("Settings");
            this.nav_drawer_intents.add(new Intent(context, Preferences.class));
            return;
        }

        this.nav_drawer_options.add(Home.menu_name);
        this.nav_drawer_intents.add(new Intent(context, Home.class));
        if(is_active_sensor) {
            this.nav_drawer_options.add("Calibration Graph");
            this.nav_drawer_intents.add(new Intent(context, CalibrationGraph.class));
        }
        this.nav_drawer_options.add("BG Data Table");
        this.nav_drawer_intents.add(new Intent(context, BgReadingTable.class));
        this.nav_drawer_options.add("Calibration Data Table");
        this.nav_drawer_intents.add(new Intent(context, CalibrationDataTable.class));
        if(is_active_sensor) {
            if(!CollectionServiceStarter.isBTShare(context)) {
                if (last_two_bgReadings.size() > 1) {
                    if (last_two_calibrations.size() > 1) {
                        if (bGreadings_in_last_30_mins.size() >= 2) {
                            if (time_now - last_two_calibrations.get(0).timestamp < (1000 * 60 * 60)) { //Put steps in place to discourage over calibration
                                this.nav_drawer_options.add(CalibrationOverride.menu_name);
                                this.nav_drawer_intents.add(new Intent(context, CalibrationOverride.class));
                            } else {
                                this.nav_drawer_options.add(AddCalibration.menu_name);
                                this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
                            }
                        } else {
                            this.nav_drawer_options.add("Cannot Calibrate right now");
                            this.nav_drawer_intents.add(new Intent(context, Home.class));
                        }
                    } else {
                        this.nav_drawer_options.add(DoubleCalibrationActivity.menu_name);
                        this.nav_drawer_intents.add(new Intent(context, DoubleCalibrationActivity.class));
                    }
                }
            }
            this.nav_drawer_options.add(StopSensor.menu_name);
            this.nav_drawer_intents.add(new Intent(context, StopSensor.class));
        } else {
            this.nav_drawer_options.add(StartNewSensor.menu_name);
            this.nav_drawer_intents.add(new Intent(context, StartNewSensor.class));
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(CollectionServiceStarter.isBTWixel(context) || CollectionServiceStarter.isDexbridgeWixel(context)|| CollectionServiceStarter.isBTShare(context)) {
                this.nav_drawer_options.add(BluetoothScan.menu_name);
                this.nav_drawer_intents.add(new Intent(context, BluetoothScan.class));
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.nav_drawer_options.add(SystemStatus.menu_name);
            this.nav_drawer_intents.add(new Intent(context, SystemStatus.class));
        }

        boolean bg_alerts = prefs.getBoolean("bg_alerts_from_main_menu", false);
        if (bg_alerts) {
            this.nav_drawer_options.add(AlertList.menu_name);
            this.nav_drawer_intents.add(new Intent(context, AlertList.class));
        }
        this.nav_drawer_options.add(SnoozeActivity.menu_name);
        this.nav_drawer_intents.add(new Intent(context, SnoozeActivity.class));

        this.nav_drawer_options.add(StatsActivity.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, StatsActivity.class));

        this.nav_drawer_options.add("Settings");
        this.nav_drawer_intents.add(new Intent(context, Preferences.class));
    }
}
