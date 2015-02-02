package com.eveningoutpost.dexdrip.Tables;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;

import java.util.ArrayList;


public class CalibrationDataTable extends ListActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Calibration Data Table";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private ArrayList<String> results = new ArrayList<String>();
    private View mRootView;

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
        Cursor cursor = Cache.openDatabase().rawQuery("Select * from Calibration order by timestamp desc", null);

//        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
//                R.layout.raw_data_list_item,
//                cursor,
//                new String[] { "bg", "estimate_raw_at_time_of_calibration", "slope", "intercept" },
//                new int[] { R.id.raw_data_id, R.id.raw_data_value , R.id.raw_data_slope, R.id.raw_data_timestamp });

        CalibrationDataCursorAdapter adapter = new CalibrationDataCursorAdapter(this, cursor, 0);

        this.setListAdapter(adapter);
//        ListView listView = (ListView) findViewById(R.id.list);
//        listView.setAdapter(adapter);
    }


    public static class CalibrationDataCursorAdapterViewHolder {
        TextView raw_data_id;
        TextView raw_data_value;
        TextView raw_data_slope;
        TextView raw_data_timestamp;

        public CalibrationDataCursorAdapterViewHolder(View root) {
            raw_data_id = (TextView) root.findViewById(R.id.raw_data_id);
            raw_data_value = (TextView) root.findViewById(R.id.raw_data_value);
            raw_data_slope = (TextView) root.findViewById(R.id.raw_data_slope);
            raw_data_timestamp = (TextView) root.findViewById(R.id.raw_data_timestamp);
        }
    }

    public static class CalibrationDataCursorAdapter extends CursorAdapter {
        Calibration bgReading = new Calibration();

        public CalibrationDataCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final CalibrationDataCursorAdapterViewHolder holder = new CalibrationDataCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            bgReading.loadFromCursor(cursor);

            final CalibrationDataCursorAdapterViewHolder tag = (CalibrationDataCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(Double.toString(bgReading.bg));
            tag.raw_data_value.setText(Double.toString(bgReading.estimate_raw_at_time_of_calibration));
            tag.raw_data_slope.setText(Double.toString(bgReading.slope));
            tag.raw_data_timestamp.setText(Double.toString(bgReading.intercept));
        }
    }
}
