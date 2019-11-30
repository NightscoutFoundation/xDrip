package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import android.util.SparseArray;
import android.view.View;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.text.DecimalFormat;

/**
 * Created by adrian on 30/06/15.
 */
public class TrendView extends View {

    private trendMap tMap = new trendMap();
    final DecimalFormat df = new DecimalFormat("#0.00");
    private CalculatedData calculatedData = null;
    private boolean ranteDataCalculating = false;

    public static final int OFFSET = 30;
    public static final int NO_TIMESLOTS = 48;
    private Paint outerPaint, outerPaintLabel, innerPaint, innerPaintLabel, medianPaint, medianPaintLabel, defaultTextPaint;
    private Resources resources;
    private int dpOffset;


    public TrendView(Context context) {
        super(context);
        resources = context.getResources();
        dpOffset = dp2px(OFFSET);

        float textSize = dp2px(14);
        outerPaint = new Paint();
        outerPaint.setColor(resources.getColor(R.color.percentile_outer));
        outerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        outerPaint.setPathEffect(new CornerPathEffect(dp2px(10)));
        outerPaint.setStrokeWidth(dp2px(1));


        outerPaintLabel = new Paint();
        outerPaintLabel.setColor(resources.getColor(R.color.percentile_outer));
        outerPaintLabel.setStyle(Paint.Style.FILL_AND_STROKE);
        outerPaintLabel.setPathEffect(new CornerPathEffect(dp2px(10)));
        outerPaintLabel.setTextSize(textSize);

        innerPaint = new Paint();
        innerPaint.setColor(resources.getColor(R.color.percentile_inner));
        innerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        innerPaint.setPathEffect(new CornerPathEffect(dp2px(10)));
        innerPaint.setStrokeWidth(dp2px(1));

        innerPaintLabel = new Paint();
        innerPaintLabel.setColor(resources.getColor(R.color.percentile_inner));
        innerPaintLabel.setStyle(Paint.Style.FILL_AND_STROKE);
        innerPaintLabel.setPathEffect(new CornerPathEffect(dp2px(10)));
        innerPaintLabel.setTextSize(textSize);


        medianPaint = new Paint();
        medianPaint.setColor(resources.getColor(R.color.percentile_median));
        medianPaint.setStyle(Paint.Style.STROKE);
        medianPaint.setPathEffect(new CornerPathEffect(dp2px(10)));
        medianPaint.setStrokeWidth(dp2px(1));


        medianPaintLabel = new Paint();
        medianPaintLabel.setColor(resources.getColor(R.color.percentile_median));
        medianPaintLabel.setStyle(Paint.Style.STROKE);
        medianPaintLabel.setPathEffect(new CornerPathEffect(dp2px(10)));
        medianPaintLabel.setTextSize(textSize);

        defaultTextPaint = new Paint();
        defaultTextPaint.setColor(Color.WHITE);
        defaultTextPaint.setAntiAlias(true);
        defaultTextPaint.setStyle(Paint.Style.STROKE);
        defaultTextPaint.setTextSize(dp2px(15));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("DrawStats", "PercentileView - onDraw");
        super.onDraw(canvas);

        generateTMap();

        trendMap rd = tMap;

        if (rd == null) {
            Log.d("DrawStats", "PercentileView - onDraw if");
            canvas.drawText("Calculating...", dp2px(30), canvas.getHeight() / 2, defaultTextPaint);
        }
        else {
            Log.d("DrawStats", "PercentileView - onDraw else");

            int day = 1000 * 60 * 60 * 24;
            int timeslot = day / NO_TIMESLOTS;

            /* for (int i = 0; i < NO_TIMESLOTS; i++) {
                int slot = i * timeslot;
                if (rd.get(slot) == null)
                    canvas.drawText("Failed...", dpOffset + dp2px(50), dp2px(14) * i, outerPaintLabel);
                else
                    canvas.drawText(Integer.toString(i) + ": High: " + df.format(rd.get(slot).getHighPercent()) + " Good: " + df.format(rd.get(slot).getGoodPercent()) + " Low: " + df.format(rd.get(slot).getLowPercent()), dp2px(14), dp2px(14) + (dp2px(14) * i), defaultTextPaint);
                //canvas.drawText("Testing...", dpOffset + dp2px(50), dp2px(14) * i, outerPaintLabel);
            }*/

            //canvas.drawText(rd.q10, dpOffset + dp2px(10), dp2px(14), outerPaintLabel);

            //drawHighLow(canvas);
            //drawGrid(canvas);
            //drawLegend(canvas);
            drawTrends(canvas);
        }


    }

    /*
    private void drawLegend(Canvas canvas) {
        //canvas.drawText("10%/90%", dpOffset + dp2px(10), dp2px(14), outerPaintLabel);
        canvas.drawText("25%/75%", dpOffset + dp2px(80), dp2px(14), innerPaintLabel);
        canvas.drawText("50% (median)", dpOffset + dp2px(150), dp2px(14), medianPaintLabel);
    }


    private void drawGrid(Canvas canvas) {
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setAntiAlias(false);
        if (Pref.getBooleanDefaultFalse(StatsActivity.SHOW_STATISTICS_PRINT_COLOR)) {
            myPaint.setColor(Color.BLACK);
        } else {
            myPaint.setColor(Color.LTGRAY);
        }
        myPaint.setStrokeWidth(dp2px(1));

        Paint myPaintText = new Paint();
        myPaintText.setStyle(Paint.Style.STROKE);
        myPaintText.setAntiAlias(false);
        if (Pref.getBooleanDefaultFalse(StatsActivity.SHOW_STATISTICS_PRINT_COLOR)) {
            myPaintText.setColor(Color.BLACK);
        } else {
            myPaintText.setColor(Color.LTGRAY);
        }
        myPaintText.setTextSize(dp2px(10));

        canvas.drawLine(dpOffset, 0, dpOffset, canvas.getHeight() - dpOffset, myPaint);
        canvas.drawLine(dpOffset, canvas.getHeight() - dpOffset, canvas.getWidth(), canvas.getHeight() - dpOffset, myPaint);

        for (int i = 0; i < 24; i++) {
            int x = (int) (dpOffset + ((canvas.getWidth() - dpOffset) / 24d) * i);
            if (i % 2 == 0) {
                canvas.drawLine(x, canvas.getHeight() - dpOffset - dp2px(2), x, canvas.getHeight() - dpOffset + dp2px(3), myPaint);
                if (i >= 10) x = x - dp2px(3);
                canvas.drawText(i + "", x - dp2px(4), canvas.getHeight() - dpOffset + dp2px(13), myPaintText);
            } else {
                canvas.drawLine(x, canvas.getHeight() - dpOffset - dp2px(2), x, canvas.getHeight() - dpOffset + dp2px(6), myPaint);
                if (i >= 10) x = x - dp2px(3);
                canvas.drawText(i + "", x - dp2px(4), canvas.getHeight() - dpOffset + dp2px(20), myPaintText);
            }
        }


        // add level markings
        myPaint.setPathEffect(new DashPathEffect(new float[]{dp2px(2), dp2px(3)}, 0));
        Path path = new Path();
        double[] levels;
        String[] labels;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));
        if (mgdl) {
            levels = new double[]{50, 100, 150, 200, 250, 300, 350};
            labels = new String[]{"50", "100", "150", "200", "250", "300", "350"};
        } else {
            levels = new double[]{2.8, 5.5, 8.3, 11, 14, 17, 20};
            labels = new String[]{"2.8", "5.5", "8.3", "11", "14", "17", "20"};
            for (int i = 0; i < levels.length; i++) {
                levels[i] *= Constants.MMOLL_TO_MGDL;
            }
        }
        for (int i = 0; i < levels.length; i++) {
            path.moveTo(dpOffset, getYfromBG(levels[i], canvas));
            path.lineTo(canvas.getWidth(), getYfromBG(levels[i], canvas));
            canvas.drawText(labels[i], dp2px(5), getYfromBG(levels[i], canvas) + dp2px(4), myPaintText);
        }

        canvas.drawPath(path, myPaint);

    }

    private void drawHighLow(Canvas canvas) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));
        double high = Double.parseDouble(settings.getString("highValue", "170"));
        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
            low *= Constants.MMOLL_TO_MGDL;

        }

        int highPosition = getYfromBG(high, canvas);
        int lowPosition = getYfromBG(low, canvas);
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setAntiAlias(false);
        myPaint.setColor(Color.RED);
        myPaint.setStrokeWidth(dp2px(3));
        canvas.drawLine(dpOffset, lowPosition, canvas.getWidth(), lowPosition, myPaint);
        myPaint.setColor(Color.YELLOW);
        canvas.drawLine(dpOffset, highPosition, canvas.getWidth(), highPosition, myPaint);
    }

    private void drawPolygon(Canvas canvas, double[] lowerValues, double[] higherValues, Paint paint) {

        Path myPath = new Path();
        myPath.reset();

        double xStep = (canvas.getWidth() - dpOffset) * 1d / lowerValues.length;
        //lowerValues
        myPath.moveTo(dpOffset, getYfromBG(lowerValues[0], canvas));
        for (int i = 1; i < lowerValues.length; i++) {
            myPath.lineTo((int) (i * xStep + dpOffset), getYfromBG(lowerValues[i], canvas));
        }
        // 00:00 == 24:00
        myPath.lineTo((int) (lowerValues.length * xStep + dpOffset), getYfromBG(lowerValues[0], canvas));
        myPath.lineTo((int) (higherValues.length * xStep + dpOffset), getYfromBG(higherValues[0], canvas));
        //higher Values
        for (int i = higherValues.length - 1; i >= 0; i--) {
            myPath.lineTo((int) (i * xStep + dpOffset), getYfromBG(higherValues[i], canvas));
        }
        myPath.close();
        canvas.drawPath(myPath, paint);
    }

    private int getYfromBG(double bgValue, Canvas canvas) {
        return (int) (canvas.getHeight() - dpOffset - bgValue * (canvas.getHeight() - dpOffset) / 400);
    }
    */

    private int dp2px(float dp) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * (metrics.densityDpi / 160f));
        return px;
    }

    /*public synchronized void setCalculatedData(CalculatedData rd) {
        calculatedData = rd;
        postInvalidate();
    }*/

    public synchronized void drawTrends(Canvas canvas) {
        List<trend> trendList = tMap.getTrendList();
        double start;
        double end;
        boolean half = false;
        trend t;
        for (int i = 0; i < trendList.size(); i++) {
            t = trendList.get(i);
            // make milliseconds hours
            //TODO format into time, i.e. 1:30, not 1.50
            start = (((t.getBegin() / 1000.0) / 60.0) / 60.0);
            end = (((t.getEnd() / 1000.0) / 60.0) / 60.0);
            if (t.isHigh())
                canvas.drawText(df.format(start) + " - " + df.format(end) + ": Trending High.", 0, dp2px(14) + (dp2px(14) * i), defaultTextPaint);
            else if (!t.isHigh())
                canvas.drawText(df.format(start) + " - " + df.format(end) + ": Trending Low.", 0, dp2px(14) + (dp2px(14) * i), defaultTextPaint);
        }
    }

    //return either RangeData or start a calculation if not already started
    public synchronized trendMap generateTMap() {
//        if (!ranteDataCalculating) {
//            ranteDataCalculating = true;
//            Thread thread = new Thread() {
//                @Override
 //               public void run() {
//                    super.run();
                    List<BgReadingStats> readings = DBSearchUtil.getReadings(false);

                    int day = 1000 * 60 * 60 * 24;
                    int timeslot = day / NO_TIMESLOTS;

                    Calendar date = new GregorianCalendar();
                    date.set(Calendar.HOUR_OF_DAY, 0);
                    date.set(Calendar.MINUTE, 0);
                    date.set(Calendar.SECOND, 0);
                    date.set(Calendar.MILLISECOND, 0);

                    final long offset = date.getTimeInMillis() % day;

                    for (int i = 0; i < NO_TIMESLOTS; i++) {
                        int begin = i * timeslot;
                        int end = begin + timeslot;
                        tMap.setSlot(begin);
                        // goes through entire list of readings
                        // adds value for each reading that is within timestamp start and end
                        for (BgReadingStats reading : readings) {
                            long timeOfDay = (reading.timestamp - offset) % day;
                            if (timeOfDay >= begin && timeOfDay < end) {
                                tMap.add(begin, reading);
                            }
                        }
                    }
                //}
            //};
            //thread.start();
        //}
        //will return null if not precalculated
        return tMap;
    }

    protected class CalculatedData {
        public double[] q10;
        public double[] q25;
        public double[] q50;
        public double[] q75;
        public double[] q90;
    }

    protected class trendMap {
        private SparseArray<trendFrag> m_trendMap;
        private int m_size = 0;
        /*private double m_highCount = 0.0;
        private double m_lowCount = 0.0;
        private double m_goodCount = 0.0;
        private double m_highAvg;
        private double m_lowAvg;
        private double m_goodAvg;
        private double m_highTotal = 0.0;
        private double m_lowTotal = 0.0;
        private double m_goodTotal = 0.0;*/
        //private boolean highTrend;

        public trendMap() {
            m_trendMap = new SparseArray<trendFrag>();
        }

        //TODO high/low variable
        public void add(int slot, BgReadingStats reading) {
            if (m_trendMap.get(slot) == null) {
                trendFrag bgList = new trendFrag();
                m_trendMap.append(slot, bgList);
            }
            m_trendMap.get(slot).add(reading);
            m_size++;
            /*if (reading.calculated_value >= 200) {
                m_highCount++;
                m_highTotal = m_highTotal + reading.calculated_value;
                m_highAvg = m_highTotal / m_highCount;
            }
            else if (reading.calculated_value <= 80) {
                m_lowCount++;
                m_lowTotal = m_lowTotal + reading.calculated_value;
                m_lowAvg = m_lowTotal / m_lowCount;
            }
            else {
                m_goodCount++;
                m_goodTotal = m_goodTotal + reading.calculated_value;
                m_goodAvg = m_goodTotal / m_goodCount;
            }*/
        }

        public void setSlot(int slot) {
            trendFrag bgList = new trendFrag();
            m_trendMap.append(slot, bgList);
        }

        public List<trend> getTrendList() {
            List<trend> tList = new ArrayList<trend>();
            int key;
            boolean cur = false;
            trend t = new trend(true, 0);
            //first loop to check for high trends
            //high and low loops are separate in case of overlap.
            for(int i = 0; i < m_trendMap.size(); i++) {
                key = m_trendMap.keyAt(i);
                trendFrag tFrag = m_trendMap.get(key);

                if (tFrag.isHigh()) {
                    if (!cur) {
                        cur = true;
                        t = new trend(true, key);
                    }
                    t.add(tFrag);
                    if (i == m_trendMap.size() - 1) {
                        t.setEnd(key);
                        tList.add(t);
                        cur = false;
                    }
                }
                else if (cur) {
                    t.setEnd(key);
                    tList.add(t);
                    cur = false;
                }
            }
            //loop again to check for low trends
            for(int i = 0; i < m_trendMap.size(); i++) {
                key = m_trendMap.keyAt(i);
                trendFrag tFrag = m_trendMap.get(key);

                if (tFrag.isLow()) {
                    if (!cur) {
                        cur = true;
                        t = new trend(false, key);
                    }
                    t.add(tFrag);
                    if (i == m_trendMap.size() - 1) {
                        t.setEnd(key);
                        tList.add(t);
                        cur = false;
                    }
                }
                else if (cur) {
                    t.setEnd(key);
                    tList.add(t);
                    cur = false;
                }
            }
            return tList;
        }

        public int size() { return m_size; }
        public trendFrag get(int slot) { return m_trendMap.get(slot); }
        //public int keyAt(int slot) { return m_trendMap.keyAt(slot); }
        /*public double getHigh() { return m_highAvg; }
        public double getLow() { return m_lowAvg; }
        public double getGood() { return m_goodAvg; }*/
    }

    protected class trendFrag {
        private List<BgReadingStats> fragment;
        private double m_highCount = 0.0;
        private double m_lowCount = 0.0;
        private double m_goodCount = 0.0;
        private double m_highAvg;
        private double m_lowAvg;
        private double m_goodAvg;
        private double m_highTotal = 0.0;
        private double m_lowTotal = 0.0;
        private double m_goodTotal = 0.0;
        private double m_highPercent = 0.0;
        private double m_lowPercent = 0.0;
        private double m_goodPercent = 0.0;
        private boolean highTrend = false;
        private boolean lowTrend = false;
        //private boolean highTrend;

        public trendFrag() {
            fragment = new ArrayList<BgReadingStats>();
            //highTrend = high;
        }

        /* might be useful later?
        public List<BgReadingStats> getFrag() {
            return fragment;
        } */

        /*public BgReadingStats get(int i) {
            return fragment.get(i);
        }*/

        //TODO make threshold variable
        public void add(BgReadingStats reading) {
            fragment.add(reading);

            if (reading.calculated_value >= 200) {
                m_highCount++;
                m_highTotal = m_highTotal + reading.calculated_value;
                m_highAvg = m_highTotal / m_highCount;
            }
            else if (reading.calculated_value <= 80) {
                m_lowCount++;
                m_lowTotal = m_lowTotal + reading.calculated_value;
                m_lowAvg = m_lowTotal / m_lowCount;
            }
            else {
                m_goodCount++;
                m_goodTotal = m_goodTotal + reading.calculated_value;
                m_goodAvg = m_goodTotal / m_goodCount;
            }
            percentage();
        }

        //TODO make tolerances variable
        private void percentage() {
            m_highPercent = (m_highCount / size()) * 100;
            if (m_highPercent >= 40.0)
                highTrend = true;
            else
                highTrend = false;

            m_lowPercent = (m_lowCount / size()) * 100;
            if (m_lowPercent >= 40.0)
                lowTrend = true;
            else
                lowTrend = false;

            m_goodPercent = (m_goodCount / size()) * 100;
        }

        public int size() { return fragment.size(); }
        public double getHigh() { return m_highAvg; }
        public double getLow() { return m_lowAvg; }
        public double getGood() { return m_goodAvg; }
        public double getHighPercent() { return m_highPercent; }
        public double getLowPercent() { return m_lowPercent; }
        public double getGoodPercent() { return m_goodPercent; }
        public boolean isHigh() { return highTrend; }
        public boolean isLow() { return lowTrend; }

        /* public double getPeak() {
            double peak = fragment.get(0).calculated_value;

            for (BgReadingStats reading : fragment) {
                if (highTrend) {
                    if (peak < reading.calculated_value) {
                        peak = reading.calculated_value;
                    }
                }
                else {
                    if (peak > reading.calculated_value) {
                        peak = reading.calculated_value;
                    }
                }
            }
            return peak;
        } */
    }

    protected class trend {
        private List<trendFrag> trendFragList;
        private int begin;
        private int end;
        //private double peakReading;
        private boolean high;

        public trend(boolean h, int slot) {
            trendFragList = new ArrayList<trendFrag>();
            begin = slot;
            trendFragList.add(tMap.get(slot));
            high = h;
        }

        public void add(trendFrag t) {
            trendFragList.add(t);
        }

        public void setEnd(int slot) {
            end = slot;
        }

        /* public void calculateTrend() {
            //calculate average first and last timestamp of trendFrags
            firstTimestamp = 0;
            lastTimestamp = 0;
            peakReading = 0;

            for (trendFrag frag : trendFragList) {
                firstTimestamp = firstTimestamp + frag.get(0).timestamp;
                lastTimestamp = lastTimestamp + frag.get(frag.size() - 1).timestamp;
                peakReading = peakReading + frag.getPeak();
            }
            firstTimestamp = firstTimestamp / trendFragList.size();
            lastTimestamp = lastTimestamp / trendFragList.size();
            peakReading = peakReading / trendFragList.size();
        } */

        public int getBegin() {
            return begin;
        }

        public long getEnd() {
            return end;
        }

        /* public double peak() {
            return peakReading;
        }*/

        public boolean isHigh() {
            return high;
        }
    }
}
