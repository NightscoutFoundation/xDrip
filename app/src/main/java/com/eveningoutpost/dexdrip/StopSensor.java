package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.eveningoutpost.dexdrip.databinding.ActivityEventLogBinding;
import com.eveningoutpost.dexdrip.databinding.ActivityStopSensorBinding;
import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import lombok.val;

import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.shortTxId;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class StopSensor extends ActivityWithMenu {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Sensor.isActive()) {
            Intent intent = new Intent(this, StartNewSensor.class);
            startActivity(intent);
            finish();
        } else {
            JoH.fixActionBar(this);

            val binding = ActivityStopSensorBinding.inflate(getLayoutInflater());
            binding.setModel(new ViewModel());
            setContentView(binding.getRoot());
        }
    }

    @Override
    public String getMenuName() {
        return getString(R.string.stop_sensor);
    }

    public synchronized static void stop() {
        Sensor.stopSensor();
        Inevitable.task("stop-sensor",1000, Sensor::stopSensor);
        AlertPlayer.getPlayer().stopAlert(xdrip.getAppContext(), true, false);

        JoH.static_toast_long(gs(R.string.sensor_stopped));
        JoH.clearCache();
        LibreAlarmReceiver.clearSensorStats();
        PluggableCalibration.invalidateAllCaches();

        Treatments.sensorStop(null, "Stopped by xDrip");

        Ob1G5StateMachine.stopSensor();
        if (JoH.pratelimit("dex-stop-start", 15)) {
            //
        }

        CollectionServiceStarter.restartCollectionServiceBackground();
        Home.staticRefreshBGCharts();
        NanoStatus.keepFollowerUpdated(false);
    }

    public void resetAllCalibrations(View v) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(gs(R.string.are_you_sure));
        builder.setMessage(gs(R.string.do_you_want_to_delete_and_reset_the_calibrations_for_this_sensor));

        builder.setNegativeButton(gs(R.string.no), (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton(gs(R.string.yes), (dialog, which) -> {
            Calibration.invalidateAllForSensor();
            dialog.dismiss();
            finish();
        });
        builder.create().show();

    }

    public class ViewModel {
        // This is false only for Dexcom G6 Firefly and G7 as of December 2023.
        // These devices have two common characteristics:
        // 1- xDrip does not maintain a calibration graph for them.  Therefore, resetting calibrations has no impact.
        // 2- They cannot be restarted easily or they cannot be restarted at all.
        // TODO this could be moved to another utility class if the same logic is used elsewhere
        public boolean resettableCals() { // Used on the stop sensor menu.
            return DexCollectionType.getDexCollectionType() != DexCollectionType.DexcomG5
                    || !Pref.getBooleanDefaultFalse("using_g6")
                    || !FirmwareCapability.isTransmitterRawIncapable(getTransmitterID());
        }

        public void stopSensorClick() {
            String confirm = gs(R.string.are_you_sure);
            if (!resettableCals()) { // Dexcom G6 Firefly or G7
                confirm = gs(R.string.sensor_stop_confirm_norestart);
                if (shortTxId()) { // Dexcom G7
                    confirm = gs(R.string.sensor_stop_confirm_really_norestart);
                }
            }
            GenericConfirmDialog.show(StopSensor.this, gs(R.string.are_you_sure), confirm, () -> {
                stop();
                JoH.startActivity(Home.class);
                finish();
            });
        }
    }
}
