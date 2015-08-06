package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.UserError;

import java.util.List;

/**
 * Created by stephenblack on 8/4/15.
 */
public class ErrorListAdapter extends BaseAdapter implements ListAdapter {
    private List<UserError> list;
    private Context context;

    public ErrorListAdapter(List<UserError> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
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

        UserError error = list.get(position);

        row.setBackgroundColor(backgroundFor(error.severity));
        shortText.setText(error.shortError);
        longText.setText(error.message);
        return view;
    }

    private int backgroundFor(int severity) {
        switch (severity) {
            case 1:
                return Color.rgb(255, 204, 102);
            case 2:
                return Color.rgb(255, 153, 102);
        }
        return Color.rgb(255, 102, 102);
    }
}
