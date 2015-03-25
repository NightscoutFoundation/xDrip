package com.eveningoutpost.dexdrip.Tables;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;

import java.util.ArrayList;
import java.util.List;


public class CalibrationDataTable extends ListActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Calibration Data Table";
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
        final List<Calibration> latest = Calibration.latest(50);

        CalibrationDataCursorAdapter adapter = new CalibrationDataCursorAdapter(this, latest);

        this.setListAdapter(adapter);
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

    public static class CalibrationDataCursorAdapter extends BaseAdapter {
        private final Context           context;
        private final List<Calibration> calibrations;

        public CalibrationDataCursorAdapter(Context context, List<Calibration> calibrations) {
            this.context = context;
            if(calibrations == null)
                calibrations = new ArrayList<>();

            this.calibrations = calibrations;
        }

        public View newView(Context context, ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final CalibrationDataCursorAdapterViewHolder holder = new CalibrationDataCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        public void bindView(View view, Context context, Calibration calibration) {
            final CalibrationDataCursorAdapterViewHolder tag = (CalibrationDataCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(Double.toString(calibration.bg));
            tag.raw_data_value.setText(Double.toString(calibration.estimate_raw_at_time_of_calibration));
            tag.raw_data_slope.setText(Double.toString(calibration.slope));
            tag.raw_data_timestamp.setText(Double.toString(calibration.intercept));
        }

        @Override
        public int getCount() {
            return calibrations.size();
        }

        @Override
        public Calibration getItem(int position) {
            return calibrations.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = newView(context, parent);

            bindView(convertView, context, getItem(position));
            return convertView;
        }
    }
}
