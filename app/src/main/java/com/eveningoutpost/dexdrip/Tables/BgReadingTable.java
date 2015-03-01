package com.eveningoutpost.dexdrip.Tables;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.widget.SimpleCursorAdapter;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;


public class BgReadingTable extends ListActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "BG Data Table";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.raw_data_list);
    }

    @Override
    protected void onResume(){
        super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);

        getData();
    }
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    private void getData() {
        Cursor cursor = Cache.openDatabase().rawQuery("Select * from BgReadings order by timestamp desc limit 50", null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.raw_data_list_item,
                cursor,
                new String[] { "calculated_value", "age_adjusted_raw_value", "raw_data", "time_since_sensor_started" },
                new int[] { R.id.raw_data_id, R.id.raw_data_value , R.id.raw_data_slope, R.id.raw_data_timestamp });

        this.setListAdapter(adapter);
    }

}
