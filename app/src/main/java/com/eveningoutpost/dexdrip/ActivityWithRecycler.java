package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by jamorham on 01/02/2017.
 */


public class ActivityWithRecycler extends BaseAppCompatActivity {

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

        RecyclerView.LayoutManager mLayoutManager = new CustomLinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {

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

    public class RecyclerAdapater extends RecyclerView.Adapter<MyViewHolder> implements ItemTouchHelperAdapter {


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

    private class CustomLinearLayoutManager extends LinearLayoutManager {

        private static final float MILLISECONDS_PER_INCH = 200f;

        private CustomLinearLayoutManager(Context context) {
            super(context);
        }

        private CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        private CustomLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {

            final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {

                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    return CustomLinearLayoutManager.this.computeScrollVectorForPosition(targetPosition);
                }

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
                }
            };

            linearSmoothScroller.setTargetPosition(position);
            try {
                startSmoothScroll(linearSmoothScroller);
            } catch (IllegalArgumentException e) {
                // couldn't scroll for some reason, just ignore
            }
        }
    }

}
