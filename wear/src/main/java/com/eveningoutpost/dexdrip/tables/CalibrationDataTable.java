package com.eveningoutpost.dexdrip.tables;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CalibrationDataTable extends ListActivity {//implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String menu_name = "Calibration Data Table";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.raw_data_list);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getData();
    }

    private void getData() {
        final long startTime = new Date().getTime() - (60000 * 60 * 24 * 3);//3 days
        final List<Calibration> latest = Calibration.latestForGraph(60, startTime);

        CalibrationDataCursorAdapter adapter = new CalibrationDataCursorAdapter(this, latest);
        this.setListAdapter(adapter);

        String msg = "";
        int size = 0;
        if (latest != null) size = latest.size();
        if (size == 0) {
            msg = getResources().getString(R.string.notify_table_size, "Calibration", size);
            JoH.static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
        }
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

        CalibrationDataCursorAdapter(Context context, List<Calibration> calibrations) {
            this.context = context;
            if(calibrations == null)
                calibrations = new ArrayList<>();

            this.calibrations = calibrations;
        }

        View newView(Context context, ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final CalibrationDataCursorAdapterViewHolder holder = new CalibrationDataCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        void bindView(View view, final Context context, final Calibration calibration) {
            final CalibrationDataCursorAdapterViewHolder tag = (CalibrationDataCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(JoH.qs(calibration.bg, 4) + "    "+ BgGraphBuilder.unitized_string_static(calibration.bg));
            tag.raw_data_value.setText("raw: " + JoH.qs(calibration.estimate_raw_at_time_of_calibration, 4));
            tag.raw_data_slope.setText("slope: " + JoH.qs(calibration.slope, 4) + " intercept: " + JoH.qs(calibration.intercept, 4));
            tag.raw_data_timestamp.setText(JoH.dateTimeText(calibration.timestamp) + "  (" + JoH.dateTimeText(calibration.raw_timestamp) + ")");

            if (calibration.isNote()) {
                // green note
                view.setBackgroundColor(Color.parseColor("#004400"));
            } else if (!calibration.isValid()) {
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
                                    calibration.invalidate();
                                    notifyDataSetChanged();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Disable this calibration?\nFlagged calibrations will no longer have an effect.").setPositiveButton(gs(R.string.yes), dialogClickListener)
                            .setNegativeButton(gs(R.string.no), dialogClickListener).show();
                    return true;
                }
            });*/


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
