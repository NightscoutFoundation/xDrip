package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.NanoStatus;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import lombok.val;

import static com.eveningoutpost.dexdrip.xdrip.gs;

public class StopSensor extends ActivityWithMenu {
   public Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Sensor.isActive()) {
            Intent intent = new Intent(this, StartNewSensor.class);
            startActivity(intent);
            finish();
        } else {
            JoH.fixActionBar(this);
            setContentView(R.layout.activity_stop_sensor);
            button = (Button)findViewById(R.id.stop_sensor);
            addListenerOnButton();
        }
    }

    @Override
    public String getMenuName() {
        return getString(R.string.stop_sensor);
    }

    public void addListenerOnButton() {
        button = (Button)findViewById(R.id.stop_sensor);
        val activity = this;
        button.setOnClickListener(v -> GenericConfirmDialog.show(activity, gs(R.string.are_you_sure), gs(R.string.sensor_stop_confirm), () -> {
            stop();
            JoH.startActivity(Home.class);
            finish();
        }));
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

        builder.setNegativeButton(gs(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        builder.setPositiveButton(gs(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Calibration.invalidateAllForSensor();
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();


    }
}
