package com.eveningoutpost.dexdrip.stats;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;

/**
 * Created by adrian on 30/06/15.
 */
public class ChartFragment extends Fragment {

    private ChartView chartView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        UserErrorLog.d("DrawStats", "ChartFragment onCreateView");
        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        UserErrorLog.d("DrawStats", "getView - ChartFragment");

        if (chartView == null) {
            chartView = new ChartView(getActivity().getApplicationContext());
            chartView.setTag(1);
        }
        return chartView;
    }
}
