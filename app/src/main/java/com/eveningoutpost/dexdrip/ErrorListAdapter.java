package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.UserError;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Emma Black on 8/4/15.
 * Used with old activity.
 */
public class ErrorListAdapter  extends BaseAdapter {
    private List<UserError> list;
    private Context context;

    public ErrorListAdapter(Context context, List<UserError> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public UserError getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return list.get(pos).getId();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_user_error, null);
        }
        LinearLayout row = (LinearLayout) view.findViewById(R.id.errorRow);
        TextView shortText = (TextView) view.findViewById(R.id.errorShort);
        TextView longText = (TextView) view.findViewById(R.id.errorLong);
        TextView timestamp = (TextView) view.findViewById(R.id.errorTimestamp);

        UserError error = list.get(position);

        row.setBackgroundColor(backgroundFor(error.severity));
        shortText.setText(error.shortError);
        longText.setText(error.message);
        timestamp.setText(dateformatter(error.timestamp));
        return view;
    }

    private int backgroundFor(int severity) {
        switch (severity) {
            case 1:
                return Color.rgb(255, 204, 102); // yellow
            case 2:
                return Color.rgb(255, 153, 102); // orange
        }
        return Color.rgb(255, 102, 102); // red
    }

    private String dateformatter(double timestamp) {
        Date date = new Date((long) timestamp);
        DateFormat format = DateFormat.getDateTimeInstance();
        return format.format(date);
    }
}
