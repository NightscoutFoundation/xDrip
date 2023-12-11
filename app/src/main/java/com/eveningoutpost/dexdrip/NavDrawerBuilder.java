package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.tables.BgReadingTable;
import com.eveningoutpost.dexdrip.tables.CalibrationDataTable;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Experience;
import com.eveningoutpost.dexdrip.stats.StatsActivity;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Preferences;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;

/**
 * Created by Emma Black on 11/5/14.
 */
public class NavDrawerBuilder {
    private List<Calibration> last_two_calibrations = Calibration.latestValid(2);
    private final List<BgReading> last_two_bgReadings = BgReading.latestUnCalculated(2);
    private final List<BgReading> bGreadings_in_last_30_mins = BgReading.last30Minutes();
    private boolean is_active_sensor = Sensor.isActive();
    public final List<Intent> nav_drawer_intents = new ArrayList<>();
    public final List<String> nav_drawer_options = new ArrayList<>();

    private static boolean use_note_search = false;

    public NavDrawerBuilder(final Context context) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if (!IUnderstand) {
            this.nav_drawer_options.add(context.getString(R.string.settings));
            this.nav_drawer_intents.add(new Intent(context, Preferences.class));
            return;
        }

        this.nav_drawer_options.add(context.getString(R.string.home_screen));
        this.nav_drawer_intents.add(new Intent(context, Home.class));

        if ((is_active_sensor) && (last_two_calibrations != null) && (last_two_calibrations.size() > 0)) {
            this.nav_drawer_options.add(context.getString(R.string.calibration_graph));
            this.nav_drawer_intents.add(new Intent(context, CalibrationGraph.class));
        }

        if (prefs.getBoolean("show_data_tables", false)) {
            this.nav_drawer_options.add(context.getString(R.string.bg_data_table));
            this.nav_drawer_intents.add(new Intent(context, BgReadingTable.class));
            this.nav_drawer_options.add(context.getString(R.string.calibration_data_table));
            this.nav_drawer_intents.add(new Intent(context, CalibrationDataTable.class));
        }

        if ((prefs.getString("dex_collection_method", "").equals("Follower"))) {
            this.nav_drawer_options.add(context.getString(R.string.add_calibration));
            this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
        } else {

            if (is_active_sensor) {
                if (!CollectionServiceStarter.isBTShare(context)) {
                    if (last_two_bgReadings.size() > 1 || Ob1G5CollectionService.isG5WantingCalibration()) {
                        if ((last_two_calibrations.size() > 1) && !Ob1G5CollectionService.isG5WantingInitialCalibration()) { //After two successful initial calibrations
                            // TODO tighten this time limit
                            if (bGreadings_in_last_30_mins.size() >= 2) {
                                long time_now = JoH.tsl();
                                if ((time_now - last_two_calibrations.get(0).timestamp < (1000 * 60 * 60))
                                        && !Ob1G5CollectionService.isG5WantingCalibration()) { //Put steps in place to discourage over calibration
                                    this.nav_drawer_options.add(context.getString(R.string.override_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, CalibrationOverride.class));
                                } else { //G5, old G6, or Firefly in no-code mode, after initial calibration and long enough after previous calibration
                                    this.nav_drawer_options.add(context.getString(R.string.add_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
                                }
                            } else {  //G5, old G6 or Firefly in no-code mode, not long after a calibration
                                this.nav_drawer_options.add(context.getString(R.string.cannot_calibrate_right_now));
                                this.nav_drawer_intents.add(new Intent(context, Home.class));
                            }
                        } else { //If there haven't been two initial calibrations
                            if (BgReading.isDataSuitableForDoubleCalibration() || Ob1G5CollectionService.isG5WantingInitialCalibration()) {
                                if ((FirmwareCapability.isTransmitterRawIncapable(getTransmitterID()) && last_two_bgReadings.size() > 1) || FirmwareCapability.isDeviceG7(getTransmitterID()) ) { //A Firefly G6 after third reading or a G7
                                    this.nav_drawer_options.add(context.getString(R.string.add_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
                                } else { //G5 or non-Firefly G6 or Firefly G6 in no-code mode, after warm-up before initial calibration
                                    this.nav_drawer_options.add(context.getString(R.string.initial_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, DoubleCalibrationActivity.class));
                                }
                            }
                        }
                    }
                }
                this.nav_drawer_options.add(context.getString(R.string.stop_sensor));
                this.nav_drawer_intents.add(new Intent(context, StopSensor.class));
            } else {
                this.nav_drawer_options.add(context.getString(R.string.start_sensor));
                this.nav_drawer_intents.add(new Intent(context, StartNewSensor.class));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (DexCollectionType.hasBluetooth() && (DexCollectionType.getDexCollectionType() != DexCollectionType.DexcomG5)) {

                this.nav_drawer_options.add(context.getString(R.string.bluetooth_scan));
                this.nav_drawer_intents.add(new Intent(context, BluetoothScan.class));
            }
        }

        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
        this.nav_drawer_options.add(context.getString(R.string.system_status));
        this.nav_drawer_intents.add(new Intent(context, MegaStatus.class));
        //}

        boolean bg_alerts = prefs.getBoolean("bg_alerts_from_main_menu", false);
        if (bg_alerts) {
            this.nav_drawer_options.add(context.getString(R.string.level_alerts));
            this.nav_drawer_intents.add(new Intent(context, AlertList.class));
        }

        if (Experience.gotData()) {
            this.nav_drawer_options.add(context.getString(R.string.snooze_alert));
            this.nav_drawer_intents.add(new Intent(context, SnoozeActivity.class));
        }

        if (use_note_search || (Treatments.last() != null)) {
            this.nav_drawer_options.add(context.getString(R.string.note_search));
            this.nav_drawer_intents.add(new Intent(context, NoteSearch.class));
            use_note_search = true; // cache
        }

        if (Experience.gotData()) {
            this.nav_drawer_options.add(context.getString(R.string.statistics));
            this.nav_drawer_intents.add(new Intent(context, StatsActivity.class));

            this.nav_drawer_options.add(context.getString(R.string.history));
            this.nav_drawer_intents.add(new Intent(context, BGHistory.class));
        }

        this.nav_drawer_options.add(context.getString(R.string.settings));
        this.nav_drawer_intents.add(new Intent(context, Preferences.class));
    }
}
