package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.text.DecimalFormat;
import java.util.ListIterator;

/**
 * Created by Sean Curtis on 12/4/19.
 */
public class TrendView extends View {

    private trendMap tMap = new trendMap();
    final DecimalFormat df = new DecimalFormat("#0.00");

    public static final int OFFSET = 30;
    public static final int NO_TIMESLOTS = 48;
    private final double TREND_TOL = 35.0;
    private final double HIGH_TOL = 230.0;
    private final double LOW_TOL = 80.0;
    //private Paint outerPaint, outerPaintLabel, innerPaint, innerPaintLabel, medianPaint, medianPaintLabel, defaultTextPaint;
    private Paint defaultTextPaint, smallTextPaint;
    private Resources resources;
    private int dpOffset;


    public TrendView(Context context) {
        super(context);
        resources = context.getResources();
        dpOffset = dp2px(OFFSET);

        float textSize = dp2px(14);
        /* outerPaint = new Paint();
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
        medianPaintLabel.setTextSize(textSize);*/

        defaultTextPaint = new Paint();
        defaultTextPaint.setColor(Color.WHITE);
        defaultTextPaint.setAntiAlias(true);
        defaultTextPaint.setStyle(Paint.Style.STROKE);
        defaultTextPaint.setTextSize(dp2px(24));

        smallTextPaint = new Paint();
        smallTextPaint.setColor(Color.WHITE);
        smallTextPaint.setAntiAlias(true);
        smallTextPaint.setStyle(Paint.Style.STROKE);
        smallTextPaint.setTextSize(dp2px(14));

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

            /*int day = 1000 * 60 * 60 * 24;
            int timeslot = day / NO_TIMESLOTS;

            for (int i = 0; i < NO_TIMESLOTS; i++) {
                int slot = i * timeslot;
                if (rd.get(slot) == null)
                    canvas.drawText("Failed...", dpOffset + dp2px(50), dp2px(14) * i, outerPaintLabel);
                else
                    canvas.drawText(Integer.toString(i) + ": High: " + df.format(rd.get(slot).getHighPercent()) + " Good: " + df.format(rd.get(slot).getGoodPercent()) + " Low: " + df.format(rd.get(slot).getLowPercent()), dp2px(14), dp2px(14) + (dp2px(14) * i), defaultTextPaint);
                //canvas.drawText("Testing...", dpOffset + dp2px(50), dp2px(14) * i, outerPaintLabel);
            }*/

            drawTrends(canvas);
        }


    }

    private int dp2px(float dp) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * (metrics.densityDpi / 160f));
        return px;
    }

    public synchronized void drawTrends(Canvas canvas) {
        List<trend> trendList = tMap.getTrendList();
        double start;
        double end;
        boolean half = false;
        trend t;
        trendFrag tf;
        int y = 0;
        int x = 0;
        for (int i = 0; i < trendList.size(); i++) {
            t = trendList.get(i);
            x = 0;

            if (t.isHigh()) {
                canvas.drawText(t.getBegin() + " - " + t.getEnd() + ": Trending High.", dp2px(14), dp2px(24) + (dp2px(24) * y), defaultTextPaint);
                y++;
                for (int a = 0; a < t.size(); a++) {
                    if (a == 3) { y++; x = 0; }
                    tf = t.get(a);
                    canvas.drawText(a + ": " + df.format(tf.getHighPercent()) + "% ", dp2px(124) * x + dp2px(14), dp2px(24) + (dp2px(24) * y), smallTextPaint);
                    x++;
                }
            }
            else if (!t.isHigh()) {
                canvas.drawText(t.getBegin() + " - " + t.getEnd() + ": Trending Low.", dp2px(14), dp2px(24) + (dp2px(24) * y), defaultTextPaint);
            }
            y+=2;
        }
    }

    public synchronized trendMap generateTMap() {
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
        return tMap;
    }

    protected class trendMap {
        private SparseArray<trendFrag> m_trendMap;
        private int m_size = 0;

        public trendMap() {
            m_trendMap = new SparseArray<trendFrag>();
        }

        public void add(int slot, BgReadingStats reading) {
            if (m_trendMap.get(slot) == null) {
                trendFrag bgList = new trendFrag();
                m_trendMap.append(slot, bgList);
            }
            m_trendMap.get(slot).add(reading);
            m_size++;
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
            List<Integer> sl = new ArrayList<Integer>();

            //first loop to check for high trends
            //high and low loops are separate in case of overlap.
            for(int i = 0; i < m_trendMap.size(); i++) {
                key = m_trendMap.keyAt(i);
                trendFrag tFrag = m_trendMap.get(key);

                if (tFrag.isHigh()) {
                    if (!cur) {
                        cur = true;
                        t = new trend(true, key);
                        sl = new ArrayList<Integer>();
                    }
                    //t.add(key, tFrag);
                    t.add(tFrag);
                    sl.add(key);
                    if (i == m_trendMap.size() - 1) {
                        t.setEnd(key);
                        //t.setEnd(m_trendMap.keyAt(i + 1));
                        t.setSlotList(sl);
                        tList.add(t);
                        cur = false;
                    }
                }
                else if (cur) {
                    //t.setEnd(key);
                    t.setEnd(m_trendMap.keyAt(i + 1));
                    t.setSlotList(sl);
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
                        sl = new ArrayList<Integer>();
                    }
                    //t.add(key, tFrag);
                    t.add(tFrag);
                    if (i == m_trendMap.size() - 1) {
                        t.setEnd(key);
                        //t.setEnd(m_trendMap.keyAt(i + 1));
                        t.setSlotList(sl);
                        tList.add(t);
                        cur = false;
                    }
                }
                else if (cur) {
                    //t.setEnd(key);
                    t.setEnd(m_trendMap.keyAt(i + 1));
                    t.setSlotList(sl);
                    tList.add(t);
                    cur = false;
                }
            }
            return tList;
        }

        public int size() { return m_size; }
        public trendFrag get(int slot) { return m_trendMap.get(slot); }
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

        public trendFrag() {
            fragment = new ArrayList<BgReadingStats>();
        }


        public void add(BgReadingStats reading) {
            fragment.add(reading);

            if (reading.calculated_value >= HIGH_TOL) {
                m_highCount++;
                m_highTotal = m_highTotal + reading.calculated_value;
                m_highAvg = m_highTotal / m_highCount;
            }
            else if (reading.calculated_value <= LOW_TOL) {
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

        private void percentage() {
            m_highPercent = (m_highCount / size()) * 100;
            if (m_highPercent >= TREND_TOL)
                highTrend = true;
            else
                highTrend = false;

            m_lowPercent = (m_lowCount / size()) * 100;
            if (m_lowPercent >= TREND_TOL)
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

    }

    protected class trend {
        private List<trendFrag> trendFragList;
        private List<Integer> slotList;
        private int begin;
        private int end;
        private boolean high;

        public trend(boolean h, int slot) {
            trendFragList = new ArrayList<trendFrag>();
            //slotList = new ArrayList<Integer>();
            begin = slot;
            //trendFragList.add(slot, tMap.get(slot));
            trendFragList.add(tMap.get(slot));
            high = h;
        }

        public void add(trendFrag t) {
            trendFragList.add(t);
            //slotList.add(slot);
        }

        public void setSlotList(List<Integer> sl) {
            slotList = sl;
        }

        public void setEnd(int slot) { end = slot; }

        public String getBegin() {
            //long millis = durationInMillis % 1000;
            //long second = (durationInMillis / 1000) % 60;
            int minute = (begin / (1000 * 60)) % 60;
            int hour = (begin / (1000 * 60 * 60)) % 24;

            String time = String.format("%02d:%02d", hour, minute);

            return time;
        }

        public String getEnd() {
            int minute = (end / (1000 * 60)) % 60;
            int hour = (end / (1000 * 60 * 60)) % 24;

            String time = String.format("%02d:%02d", hour, minute);

            return time;
        }

        public trendFrag get(int i) { return trendFragList.get(i); }

        /*public String getSlot(int i) {
            int minute = (slotList.get(i) / (1000 * 60)) % 60;
            int hour = (slotList.get(i) / (1000 * 60 * 60)) % 24;

            //String time = String.format("%02d:%02d", hour, minute);

            return time;
        }*/

        public int size() { return trendFragList.size(); }
        public boolean isHigh() { return high; }
    }
}
