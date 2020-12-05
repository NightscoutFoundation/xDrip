package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.sharemodels.models.ExistingFollower;
import com.eveningoutpost.dexdrip.sharemodels.ShareRest;
import com.squareup.okhttp.ResponseBody;

import java.util.List;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Emma Black on 8/11/15.
 */
import static com.eveningoutpost.dexdrip.xdrip.gs;
public class FollowerListAdapter extends BaseAdapter {
    private List<ExistingFollower> list;
    private Context context;
    private ShareRest shareRest;
    public FollowerListAdapter(Context context, ShareRest shareRest, List<ExistingFollower> list) {
        this.context = context;
        this.list = list;
        this.shareRest = shareRest;
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
        Button deleteButton = (Button) view.findViewById(R.id.deleteFollower);

        final ExistingFollower follower = list.get(position);

        followerName.setText(follower.ContactName);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Callback<ResponseBody> deleteFollowerListener = new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                        if (response.isSuccess()) {
                            Toast.makeText(context, gs(R.string.follower_deleted_succesfully), Toast.LENGTH_LONG).show();
                            list.remove(position);
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(context, gs(R.string.failed_to_delete_follower), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(context, gs(R.string.failed_to_delete_follower), Toast.LENGTH_LONG).show();
                    }
                };
                shareRest.deleteContact(follower.ContactId, deleteFollowerListener);
            }
        });
        return view;
    }
}
