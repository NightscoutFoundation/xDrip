package com.eveningoutpost.dexdrip;

import android.os.Bundle;
import android.os.PowerManager;

import android.view.View;
import android.widget.Button;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderTask;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Created by jamorham on 23/05/2017.
 * <p>
 * Manage and kick off a backfill request
 */

public class NightscoutBackfillActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private final static String TAG = "BackFillActivity";
    private Button dateButton;
    private Button doitButton;
    private Calendar calendar;

    private static long locked = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nightscout_backfill);
        JoH.fixActionBar(this);
        dateButton = (Button) findViewById(R.id.backfillDateButton);
        doitButton = (Button) findViewById(R.id.startbackfill);
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(JoH.tsl() - Constants.DAY_IN_MS);
        updateDateButton();
    }

    private void updateDateButton() {
        SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy h:mm a");
        dateButton.setText(format.format(calendar.getTime()));
    }

    @Override
    protected void onResume() {
        xdrip.checkForcedEnglish(this);
        setTitle("Nightscout Backfill");
        super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), "Nightscout Backfill", this);

        if (JoH.msSince(locked) < Constants.HOUR_IN_MS) {
            JoH.static_toast_long(gs(R.string.still_processing_previous_backfill_request));
            finish();
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    public void backfillCancel(View v) {
        finish();
    }

    public void backfillPick(View v) {
        final DatePickerFragment datePickerFragment = new DatePickerFragment();

        datePickerFragment.setAllowFuture(false);
        datePickerFragment.setInitiallySelectedDate(calendar.getTimeInMillis());
        datePickerFragment.setTitle("How far back?");
        datePickerFragment.setDateCallback(new ProfileAdapter.DatePickerCallbacks() {
            @Override
            public void onDateSet(int year, int month, int day) {
                calendar.set(year, month, day);
                updateDateButton();
            }
        });
        datePickerFragment.show(this.getFragmentManager(), "DatePicker");
    }

    public synchronized void backfillRun(View v) {
        locked = JoH.tsl();
        doitButton.setVisibility(View.INVISIBLE);
        JoH.static_toast_long(gs(R.string.please_wait));
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = JoH.getWakeLock("nightscout-backfill", 600000);
                try {
                    final List<BgReading> the_readings = BgReading.latestForGraphAsc(500000, calendar.getTimeInMillis(), JoH.tsl());
                    if ((the_readings != null) && (the_readings.size() > 0)) {
                        PersistentStore.setBoolean(UploaderTask.BACKFILLING_BOOSTER, true);
                        long bgcount = the_readings.size();
                        long trcount = 0;
                        for (BgReading bg : the_readings) {
                            UploaderQueue.newEntry("update", bg);
                        }

                        final List<Treatments> the_treatments = Treatments.latestForGraph(50000, calendar.getTimeInMillis(), JoH.tsl());
                        if ((the_treatments != null) && (the_treatments.size() > 0)) {
                            trcount = the_treatments.size();
                            for (Treatments tr : the_treatments) {
                                UploaderQueue.newEntry("update", tr);
                            }
                        }

                        // TODO Calibrations? Blood tests?

                        JoH.static_toast_long("Queued " + bgcount + " glucose readings and " + trcount + " treatments!");
                        SyncService.startSyncService(500);
                        locked = 0; // clear lock
                    } else {
                        JoH.static_toast_long(gs(R.string.didnt_find_any_glucose_readings_in_that_time_period));
                    }
                } finally {
                    JoH.releaseWakeLock(wl);
                }
            }
        }).start();

        finish();
    }

}
