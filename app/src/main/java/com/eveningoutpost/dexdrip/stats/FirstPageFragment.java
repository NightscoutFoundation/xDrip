package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.R;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by adrian on 30/06/15.
 */
public class FirstPageFragment extends Fragment {

    private View myView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DrawStats", "FirstPageFragment onCreateView");

        myView = inflater.inflate(
                R.layout.stats_general, container, false);

        myView.setTag(0);

        (new CalculationThread(myView, getActivity().getApplicationContext())).start();

        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        return myView;
    }

    private class CalculationThread extends Thread {

        private final View localView;
        private final Context context;

        public CalculationThread(View myView, Context context) {
            this.localView = myView;
            this.context = context;
        }

        @Override
        public void run() {
            super.run();
            Log.d("DrawStats", "FirstPageFragment CalculationThread started");

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

            if (context == null) {
                Log.d("DrawStats", "FirstPageFragment context == null, do not calculate if fragment is not attached");
                return;
            }

            //Ranges
            long aboveRange = DBSearchUtil.noReadingsAboveRange(context);
            long belowRange = DBSearchUtil.noReadingsBelowRange(context);
            long inRange = DBSearchUtil.noReadingsInRange(context);
            long total = aboveRange + belowRange + inRange;

            if (total == 0) {
                total = Long.MAX_VALUE;
            }

            TextView rangespercent = (TextView) localView.findViewById(R.id.textView_ranges_percent);
            TextView rangesabsolute = (TextView) localView.findViewById(R.id.textView_ranges_absolute);

            updateText(localView, rangespercent, inRange * 100 / total + "%/" + aboveRange * 100 / total + "%/" + belowRange * 100 / total + "%");
            updateText(localView, rangesabsolute, inRange + "/" + aboveRange + "/" + belowRange);

            List<BgReadingStats> bgList = DBSearchUtil.getReadings(true);
            if (bgList.size() > 0) {
                double median = bgList.get(bgList.size() / 2).calculated_value;
                TextView medianView = (TextView) localView.findViewById(R.id.textView_median);

                if (mgdl) {
                    updateText(localView, medianView, Math.round(median * 10) / 10d + " mg/dl");

                } else {
                    updateText(localView, medianView, Math.round(median * Dex_Constants.MG_DL_TO_MMOL_L * 100) / 100d + " mmol/l");

                }

                double mean = 0;
                double len = bgList.size();
                double stdev = 0;
                for (BgReadingStats bgr : bgList) {
                    mean += bgr.calculated_value / len;
                }

                TextView meanView = (TextView) localView.findViewById(R.id.textView_mean);
                //update mean
                if (mgdl) {
                    updateText(localView, meanView, (Math.round(mean * 10) / 10d) + " mg/dl");
                } else {
                    updateText(localView, meanView, (Math.round(mean * Dex_Constants.MG_DL_TO_MMOL_L * 100) / 100d) + " mmol/l");

                }
                //update A1c
                TextView a1cView = (TextView) localView.findViewById(R.id.textView_a1c);
                int a1c_ifcc = (int) Math.round(((mean + 46.7) / 28.7 - 2.15) * 10.929);
                double a1c_dcct = Math.round(10 * (mean + 46.7) / 28.7) / 10d;
                updateText(localView, a1cView, a1c_ifcc + " mmol/mol\n" + a1c_dcct + "%");


                for (BgReadingStats bgr : bgList) {
                    stdev += (bgr.calculated_value - mean) * (bgr.calculated_value - mean) / len;
                }
                stdev = Math.sqrt(stdev);
                TextView stdevView = (TextView) localView.findViewById(R.id.textView_stdev);
                if (mgdl) {
                    updateText(localView, stdevView, (Math.round(stdev * 10) / 10d) + " mg/dl");
                } else {
                    updateText(localView, stdevView, (Math.round(stdev * Dex_Constants.MG_DL_TO_MMOL_L * 100) / 100d) + " mmol/l");
                }

                TextView coefficientOfVariation = (TextView) localView.findViewById(R.id.textView_coefficient_of_variation);
                updateText(localView, coefficientOfVariation, Math.round(1000d*stdev/mean)/10d + "%");

                List<BgReadingStats> bgListByTime = DBSearchUtil.getReadings(false);
                double NormalReadingspct = inRange*100/total;
                double glucoseFirst = bgListByTime.get(0).calculated_value;
                double glucoseLast = glucoseFirst;
                double glucoseTotal =  glucoseLast;
                double GVITotal = 0;
                int usedRecords = 1;
                for (int i=1; i<bgListByTime.size();i++) {
                    BgReadingStats bgr = bgListByTime.get(i);
                    double delta = bgr.calculated_value - glucoseLast;
                    GVITotal += Math.sqrt(25 + Math.pow(delta, 2));
                    usedRecords += 1;
                    glucoseLast = bgr.calculated_value;
                    glucoseTotal +=  glucoseLast;
                }
                double GVIDelta = Math.abs(glucoseLast - glucoseFirst);//Math.floor(glucose_data[0].bgValue,glucose_data[glucose_data.length-1].bgValue);
                double GVIIdeal = Math.sqrt(Math.pow(usedRecords*5,2) + Math.pow(GVIDelta,2));
                double GVI = (GVITotal / GVIIdeal * 100) / 100;
                Log.d("DrawStats", "GVI=" + GVI + " GVIIdeal=" + GVIIdeal + " GVITotal=" + GVITotal + " GVIDelta=" + GVIDelta + " usedRecords=" + usedRecords);
                double glucoseMean = Math.floor(glucoseTotal / usedRecords);
                double tirMultiplier = NormalReadingspct / 100.0;
                double PGS = (GVI * glucoseMean * (1-tirMultiplier) * 100) / 100;
                Log.d("DrawStats", "NormalReadingspct=" + NormalReadingspct + " glucoseMean=" + glucoseMean + " tirMultiplier=" + tirMultiplier + " PGS=" + PGS);
                TextView gviView = (TextView) localView.findViewById(R.id.textView_gvi);
                DecimalFormat df = new DecimalFormat("#.00");
                updateText(localView, gviView,  df.format(GVI) + "  PGS:  " + df.format(PGS));

            }
        }

        private void updateText(final View localView, final TextView tv, final String s) {
            Log.d("DrawStats", "updateText: " + s);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    //Adrian: after screen rotation it might take some time to attach the view to the window
                    //Wait up to 3 seconds for this to happen.
                    int i = 0;
                    while (localView.getHandler() == null && i < 10) {
                        i++;
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                        }

                    }

                    if (localView.getHandler() == null) {
                        Log.d("DrawStats", "no Handler found - stopping to update view");
                        return;
                    }


                    boolean success = localView.post(new Runnable() {
                        @Override
                        public void run() {
                            tv.setText(s);
                            Log.d("DrawStats", "setText actually called: " + s);

                        }
                    });
                    Log.d("DrawStats", "updateText: " + s + " success: " + success);
                }
            });
            thread.start();

        }

    }

}
