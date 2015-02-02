package com.eveningoutpost.dexdrip.Tables;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;

import java.util.Date;


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
        Cursor cursor = Cache.openDatabase().rawQuery("Select * from BgReadings order by _ID desc limit 50", null);

//        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
//                R.layout.raw_data_list_item,
//                cursor,
//                new String[] { "calculated_value", "age_adjusted_raw_value", "raw_data", "time_since_sensor_started" },
//                new int[] { R.id.raw_data_id, R.id.raw_data_value , R.id.raw_data_slope, R.id.raw_data_timestamp });

        CursorAdapter adapter = new BgReadingCursorAdapter(this, cursor, 0);

        this.setListAdapter(adapter);
    }


    public static class BgReadingCursorAdapterViewHolder {
        TextView raw_data_id;
        TextView raw_data_value;
        TextView raw_data_slope;
        TextView raw_data_timestamp;

        public BgReadingCursorAdapterViewHolder(View root) {
            raw_data_id = (TextView) root.findViewById(R.id.raw_data_id);
            raw_data_value = (TextView) root.findViewById(R.id.raw_data_value);
            raw_data_slope = (TextView) root.findViewById(R.id.raw_data_slope);
            raw_data_timestamp = (TextView) root.findViewById(R.id.raw_data_timestamp);
        }
    }

    public static class BgReadingCursorAdapter extends CursorAdapter {
        BgReading bgReading = new BgReading();

        public BgReadingCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final BgReadingCursorAdapterViewHolder holder = new BgReadingCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            bgReading.loadFromCursor(cursor);

            final BgReadingCursorAdapterViewHolder tag = (BgReadingCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(Double.toString(bgReading.calculated_value));
            tag.raw_data_value.setText(Double.toString(bgReading.age_adjusted_raw_value));
            tag.raw_data_slope.setText(Double.toString(bgReading.raw_data));
            tag.raw_data_timestamp.setText(new Date(bgReading.timestamp).toString());
        }
    }
}
