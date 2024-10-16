package com.eveningoutpost.dexdrip.tables;

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

import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class UserErrorTable extends ListActivity {
    private final static String TAG = UserErrorTable.class.getSimpleName();
    private volatile boolean runRefresh = false;
    private volatile long highest_id = 0;
    private volatile int lastScrollPosition = 0;
    private volatile boolean loading = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        UserError.Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.raw_data_list);
    }

    @Override
    protected void onResume() {
        UserError.Log.d(TAG, "onResume");
        super.onResume();

        getData();
    }

    // start streaming data
    private synchronized void startRefresh() {
        runRefresh = true;
        new Thread(() -> {
            int c;
            int turbo = 0;

            while (runRefresh) {
                UserError.Log.d(TAG, "refreshing data " + highest_id);
                if (getData()) {
                    turbo = 60;
                }
                if (turbo > 0) {
                    // short sleep
                    JoH.threadSleep(100);
                    turbo--;
                } else {
                    // long sleep
                    c = 0;
                    while (c < 2 && runRefresh) {
                        JoH.threadSleep(500);
                        c++;
                    }
                }
            }
        }).start();
    }

    // stop streaming data
    private void stopRefresh() {
        runRefresh = false;
    }

    private boolean getData() {
        UserError.Log.d(TAG, "getData");
        final long startTime = new Date().getTime() - (60000 * 60 * 24 * 3);//3 days
        //final List<BloodTest> latest = BloodTest.latestForGraph(60, startTime);
        final List<UserError> new_entries = UserError.latestDesc(500, startTime);
        ListAdapter adapter = new thisAdapter(this, new_entries);
        this.setListAdapter(adapter);

        String msg = "";
        int size = 0;
        if ((new_entries != null) && (new_entries.size() > 0)) {
            size = new_entries.size();
            final long new_highest = new_entries.get(0).getId();
            UserError.Log.d(TAG, "New streamed data size: " + new_entries.size() + " Highest: " + new_highest);
            highest_id = new_highest;
            msg = getResources().getString(R.string.notify_table_size, "UserError", size);
            JoH.static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
            return true;
        }
        msg = getResources().getString(R.string.notify_table_size, "UserError", size);
        JoH.static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
        return false;
    }

    public static class thisCursorAdapterViewHolder {
        TextView raw_data_id;
        TextView raw_data_value;
        TextView raw_data_slope;
        TextView raw_data_timestamp;

        public thisCursorAdapterViewHolder(View root) {
            UserError.Log.d(TAG, "thisCursorAdapterViewHolder");
            raw_data_id = (TextView) root.findViewById(R.id.raw_data_id);
            raw_data_value = (TextView) root.findViewById(R.id.raw_data_value);
            raw_data_slope = (TextView) root.findViewById(R.id.raw_data_slope);
            raw_data_timestamp = (TextView) root.findViewById(R.id.raw_data_timestamp);
        }
    }

    public static class thisAdapter extends BaseAdapter {
        private final Context         context;
        private final List<UserError> data;

        public thisAdapter(Context context, List<UserError> data) {
            UserError.Log.d(TAG, "thisAdapter");
            this.context = context;
            if(data == null)
                data = new ArrayList<>();

            this.data = data;
            UserError.Log.d(TAG, "thisAdapter data.size()=" + data.size());
        }

        public View newView(Context context, ViewGroup parent) {
            UserError.Log.d(TAG, "newView");
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final thisCursorAdapterViewHolder holder = new thisCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        public void bindView(View view, final Context context, final UserError data) {
            UserError.Log.d(TAG, "bindView");
            final thisCursorAdapterViewHolder tag = (thisCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(Long.toString(Math.round(data.getId())));
            tag.raw_data_value.setText(data.message!=null ? data.message : "");
            tag.raw_data_slope.setText("State: " + Long.toString(data.severity));
            tag.raw_data_timestamp.setText(new Date((long) data.timestamp).toString());
            view.setBackgroundColor(Color.parseColor("#212121"));

            /*if (bgReading.ignoreForStats) {
                // red invalid/cancelled/overridden
                view.setBackgroundColor(Color.parseColor("#660000"));
            } else {
                // normal grey
                view.setBackgroundColor(Color.parseColor("#212121"));
            }

            view.setLongClickable(true);
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
            return data.size();
        }

        @Override
        public UserError getItem(int position) {
            return data.get(position);
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
