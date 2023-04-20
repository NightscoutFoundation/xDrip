package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.tables.BgReadingTable;
import com.eveningoutpost.dexdrip.tables.BloodTestTable;
import com.eveningoutpost.dexdrip.tables.CalibrationDataTable;
import com.eveningoutpost.dexdrip.tables.TreatmentsTable;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.HashMap;
import java.util.Map;

import static com.eveningoutpost.dexdrip.ListenerService.WEARABLE_INITTREATMENTS_PATH;

/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class MenuActivity extends Activity {

    private final static String TAG = "jamorham " + MenuActivity.class.getSimpleName();
    //private TextView mDialTextView;
    private ImageButton addtreatmentbutton, xdripprefsbutton, restartcollectorbutton, refreshdbbutton,
            bloodtesttabbutton, treatmenttabbutton, calibrationtabbutton, bgtabbutton;
    private static String currenttab = "";
    private static Map<String, String> values = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        currenttab = "";
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                //mDialTextView = (TextView) stub.findViewById(R.id.dialed_no_textview);

                addtreatmentbutton = (ImageButton) stub.findViewById(R.id.addtreatmentbutton);
                restartcollectorbutton = (ImageButton) stub.findViewById(R.id.restartcollectorbutton);
                refreshdbbutton = (ImageButton) stub.findViewById(R.id.refreshdbbutton);
                xdripprefsbutton = (ImageButton) stub.findViewById(R.id.xdripprefsbutton);
                bloodtesttabbutton = (ImageButton) stub.findViewById(R.id.bloodtesttabbutton);
                treatmenttabbutton = (ImageButton) stub.findViewById(R.id.treatmenttabbutton);
                calibrationtabbutton = (ImageButton) stub.findViewById(R.id.calibrationtabbutton);
                bgtabbutton = (ImageButton) stub.findViewById(R.id.bgtabbutton);

                if (Home.get_forced_wear())
                    restartcollectorbutton.setVisibility(View.VISIBLE);
                else
                    restartcollectorbutton.setVisibility(View.GONE);

                /*mDialTextView.setText("");

                mDialTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submitAll();
                    }
                });*/


                addtreatmentbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "addtreatment";
                        updateTab();
                    }
                });
                refreshdbbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "refreshdb";
                        updateTab();
                    }
                });
                restartcollectorbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "restartcollector";
                        updateTab();
                    }
                });
                xdripprefsbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "xdripprefs";
                        updateTab();
                    }
                });
                bloodtesttabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "bloodtest";
                        updateTab();
                    }
                });
                treatmenttabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "treatments";
                        updateTab();
                    }
                });
                calibrationtabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "calibrations";
                        updateTab();
                    }
                });
                bgtabbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currenttab = "bgreadings";
                        updateTab();
                    }
                });

                updateTab();
            }
        });
    }

    private void updateTab() {

        String msg;
        final int offColor = Color.DKGRAY;
        final int onColor = Color.RED;

        addtreatmentbutton.setBackgroundColor(offColor);
        refreshdbbutton.setBackgroundColor(offColor);
        restartcollectorbutton.setBackgroundColor(offColor);
        xdripprefsbutton.setBackgroundColor(offColor);
        bloodtesttabbutton.setBackgroundColor(offColor);
        treatmenttabbutton.setBackgroundColor(offColor);
        calibrationtabbutton.setBackgroundColor(offColor);
        bgtabbutton.setBackgroundColor(offColor);

        switch (currenttab) {
            case "addtreatment":
                addtreatmentbutton.setBackgroundColor(onColor);
                startIntent(KeypadInputActivity.class);
                break;
            case "refreshdb":
                refreshdbbutton.setBackgroundColor(onColor);
                ListenerService.SendData(this, WEARABLE_INITTREATMENTS_PATH, null);
                msg = getResources().getString(R.string.notify_refreshdb);
                JoH.static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
                break;
            case "restartcollector":
                restartcollectorbutton.setBackgroundColor(onColor);
                CollectionServiceStarter.startBtService(getApplicationContext());
                msg = getResources().getString(R.string.notify_collector_started, DexCollectionType.getDexCollectionType());
                JoH.static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
                break;
            case "xdripprefs":
                xdripprefsbutton.setBackgroundColor(onColor);
                startIntent(NWPreferences.class);
                break;
            //View DB Tables:
            case "bloodtest":
                bloodtesttabbutton.setBackgroundColor(onColor);
                startIntent(BloodTestTable.class);// TODO get mgdl or mmol here
                break;
            case "treatments":
                treatmenttabbutton.setBackgroundColor(onColor);
                startIntent(TreatmentsTable.class);
                break;
            case "calibrations":
                calibrationtabbutton.setBackgroundColor(onColor);
                startIntent(CalibrationDataTable.class);
                break;
            case "bgreadings":
                bgtabbutton.setBackgroundColor(onColor);
                startIntent(BgReadingTable.class);
                break;
        }

    }

    private void startIntent(Class name) {
        Intent intent = new Intent(getApplicationContext(), name);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
