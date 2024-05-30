package com.eveningoutpost.dexdrip.stats;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by adrian on 30/06/15.
 */
public class PercentileFragment extends Fragment {

    private PercentileView percentileView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DrawStats", "PercentileFragment - onCreateView");
        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        Log.d("DrawStats", "PercentileFragment - getView");

        if (percentileView == null) {
            percentileView = new PercentileView(getActivity().getApplicationContext());
            percentileView.setTag(2);
        }
        return percentileView;
    }
}
