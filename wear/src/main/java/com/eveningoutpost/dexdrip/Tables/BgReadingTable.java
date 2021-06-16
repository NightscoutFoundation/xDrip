package com.eveningoutpost.dexdrip.Tables;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//import com.eveningoutpost.dexdrip.NavigationDrawerFragment;


public class BgReadingTable extends ListActivity {//implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private final static String TAG = "jamorham " + BgReadingTable.class.getSimpleName();
    //private String menu_name = "BG Data Table";
    //private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setTheme(R.style.OldAppTheme); // or null actionbar
        UserError.Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.raw_data_list);
    }

    @Override
    protected void onResume() {
        UserError.Log.d(TAG, "onResume");
        super.onResume();
        //mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        //mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);

        getData();
    }

    /*@Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }*/

    private void getData() {
        final long startTime = new Date().getTime() - (60000 * 60 * 24 * 3);//3 days
        final List<BgReading> latest = BgReading.latestForGraph(216, startTime);

        ListAdapter adapter = new BgReadingAdapter(this, latest);
        this.setListAdapter(adapter);

        String msg = "";
        int size = 0;
        if (latest != null) size = latest.size();
        if (size == 0) {
            msg = getResources().getString(R.string.notify_table_size, "BgReading", size);
            JoH.static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
        }
    }

    public static class BgReadingCursorAdapterViewHolder {
        TextView raw_data_id;
        TextView raw_data_value;
        TextView raw_data_slope;
        TextView raw_data_timestamp;

        public BgReadingCursorAdapterViewHolder(View root) {
            UserError.Log.d(TAG, "BgReadingCursorAdapterViewHolder");
            raw_data_id = (TextView) root.findViewById(R.id.raw_data_id);
            raw_data_value = (TextView) root.findViewById(R.id.raw_data_value);
            raw_data_slope = (TextView) root.findViewById(R.id.raw_data_slope);
            raw_data_timestamp = (TextView) root.findViewById(R.id.raw_data_timestamp);
        }
    }

    public static class BgReadingAdapter extends BaseAdapter {
        private final Context         context;
        private final List<BgReading> readings;

        public BgReadingAdapter(Context context, List<BgReading> readings) {
            UserError.Log.d(TAG, "BgReadingAdapter");
            this.context = context;
            if(readings == null)
                readings = new ArrayList<>();

            this.readings = readings;
            UserError.Log.d(TAG, "BgReadingAdapter readings.size()=" + readings.size());
        }

        public View newView(Context context, ViewGroup parent) {
            UserError.Log.d(TAG, "newView");
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final BgReadingCursorAdapterViewHolder holder = new BgReadingCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        public void bindView(View view, final Context context, final BgReading bgReading) {
            UserError.Log.d(TAG, "bindView");
            final BgReadingCursorAdapterViewHolder tag = (BgReadingCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(JoH.qs(bgReading.calculated_value, 4));
            tag.raw_data_value.setText(Double.toString(bgReading.age_adjusted_raw_value));
            tag.raw_data_slope.setText(Double.toString(bgReading.raw_data));
            tag.raw_data_timestamp.setText(new Date(bgReading.timestamp).toString());

            if (bgReading.ignoreForStats) {
                // red invalid/cancelled/overridden
                view.setBackgroundColor(Color.parseColor("#660000"));
            } else {
                // normal grey
                view.setBackgroundColor(Color.parseColor("#212121"));
            }

            /*view.setLongClickable(true);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    bgReading.ignoreForStats = true;
                                    bgReading.save();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    bgReading.ignoreForStats = false;
                                    bgReading.save();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Flag reading as \"bad\".\nFlagged readings have no impact on the statistics.").setPositiveButton(gs(R.string.yes), dialogClickListener)
                            .setNegativeButton(gs(R.string.no), dialogClickListener).show();
                    return true;
                }
            });*/

        }

        @Override
        public int getCount() {
            return readings.size();
        }

        @Override
        public BgReading getItem(int position) {
            return readings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            UserError.Log.d(TAG, "getView");
            if (convertView == null)
                convertView = newView(context, parent);

            bindView(convertView, context, getItem(position));
            return convertView;
        }
    }
}
