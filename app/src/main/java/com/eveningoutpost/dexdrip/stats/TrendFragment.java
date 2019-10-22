package com.eveningoutpost.dexdrip.stats;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eveningoutpost.dexdrip.Models.UserError.Log;

/**
 * Created by adrian on 30/06/15.
 */
public class TrendFragment extends Fragment {

    private TrendView trendView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DrawStats", "TrendFragment onCreateView");
        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        Log.d("DrawStats", "getView - TrendFragment");

        if (trendView == null) {
            trendView = new TrendView(getActivity().getApplicationContext());
            trendView.setTag(1);
        }
        return trendView;
    }
}
