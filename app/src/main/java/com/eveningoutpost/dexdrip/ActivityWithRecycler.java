package com.eveningoutpost.dexdrip;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by jamorham on 01/02/2017.
 */


public class ActivityWithRecycler extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerAdapater mAdapter;
    ItemTouchHelper mItemTouchHelper;
    int screen_width;
    int screen_height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screen_width = dm.widthPixels;
        screen_height = dm.heightPixels;
    }

    protected void postOnCreate() {

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder  {

        int position = -1;

        public MyViewHolder(View view) {
            super(view);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.RED);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(Color.TRANSPARENT);
           }
    }

    public class RecyclerAdapater extends RecyclerView.Adapter<MyViewHolder> implements ItemTouchHelperAdapter   {


        @Override
        public int getItemCount() {
            return 0; // stub
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null; // stub
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
        }

        @Override
        public void onItemDismiss(int position) {
            notifyItemRemoved(position);
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

    }

}
