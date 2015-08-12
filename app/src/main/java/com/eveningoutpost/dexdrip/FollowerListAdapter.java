package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.ShareModels.FollowerManager;
import com.eveningoutpost.dexdrip.ShareModels.Models.ExistingFollower;

import java.util.List;

import rx.functions.Action1;

/**
 * Created by stephenblack on 8/11/15.
 */
public class FollowerListAdapter extends BaseAdapter {
    private List<ExistingFollower> list;
    private Context context;
    private FollowerManager followerManager;

    public FollowerListAdapter(Context context, FollowerManager followerManager, List<ExistingFollower> list) {
        this.context = context;
        this.list = list;
        this.followerManager = followerManager;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public ExistingFollower getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_follower, null);
        }
        TextView followerName = (TextView) view.findViewById(R.id.follwerName);
        final LinearLayout row = (LinearLayout) view.findViewById(R.id.followerListRow);
        Button deleteButton = (Button) view.findViewById(R.id.deleteFollower);

        final ExistingFollower follower = list.get(position);

        followerName.setText(follower.ContactName);
        final View finalView = view;
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Action1<Boolean> deleteFollowerListener = new Action1<Boolean>() {
                    @Override
                    public void call(Boolean deleted) {
                        if (deleted) {
                            Toast.makeText(context, "Follower deleted succesfully", Toast.LENGTH_LONG).show();
                            finalView.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(context, "Failed to delete follower", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                followerManager.deleteFollower(deleteFollowerListener, follower.ContactId);
            }
        });
        return view;
    }
}
