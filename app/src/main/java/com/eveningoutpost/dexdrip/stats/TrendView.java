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
import android.view.View;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

/**
 * Created by adrian on 30/06/15.
 */
public class TrendView extends View {

    private CalculatedData calculatedData = null;
    private boolean ranteDataCalculating = false;

    public static final int OFFSET = 30;
    public static final int NO_TIMESLOTS = 48;
    private Paint outerPaint, outerPaintLabel, innerPaint, innerPaintLabel, medianPaint, medianPaintLabel;
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

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("DrawStats", "PercentileView - onDraw");
        super.onDraw(canvas);

        CalculatedData rd = getMaybeCalculatedData();

        if (rd == null) {
            Log.d("DrawStats", "PercentileView - onDraw if");

            Paint myPaint = new Paint();
            myPaint.setColor(Color.WHITE);
            myPaint.setAntiAlias(true);
            myPaint.setStyle(Paint.Style.STROKE);
            myPaint.setTextSize(dp2px(15));
            canvas.drawText("Calculating...", dp2px(30), canvas.getHeight() / 2, myPaint);
        } else {
            Log.d("DrawStats", "PercentileView - onDraw else");
            //drawPolygon(canvas, rd.q10, rd.q90, outerPaint);
            //drawPolygon(canvas, rd.q25, rd.q75, innerPaint);
            //drawPolygon(canvas, rd.q50, rd.q50, medianPaint);


            for (int count = 0; count < rd.q75.length; count++) {
                canvas.drawText(Double.toString(rd.q75[count]), dpOffset + dp2px(50), dp2px(14) * count, outerPaintLabel);
            }
            //canvas.drawText(rd.q10, dpOffset + dp2px(10), dp2px(14), outerPaintLabel);

            //drawHighLow(canvas);
            //drawGrid(canvas);
            //drawLegend(canvas);
        }


    }

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

    private int dp2px(float dp) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * (metrics.densityDpi / 160f));
        return px;
    }


    public synchronized void setCalculatedData(CalculatedData rd) {
        calculatedData = rd;
        postInvalidate();
    }


    //return either RangeData or start a calculation if not already started
    public synchronized CalculatedData getMaybeCalculatedData() {
        if (!ranteDataCalculating) {
            ranteDataCalculating = true;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    List<BgReadingStats> readings = DBSearchUtil.getReadings(false);
                    List<BgReadingStats> trendFrag = new ArrayList<BgReadingStats>();
                    List<trend> trendList = new ArrayList<trend>();

                    int day = 1000 * 60 * 60 * 24;

                    int timeslot = day / NO_TIMESLOTS;

                    Calendar date = new GregorianCalendar();
                    date.set(Calendar.HOUR_OF_DAY, 0);
                    date.set(Calendar.MINUTE, 0);
                    date.set(Calendar.SECOND, 0);
                    date.set(Calendar.MILLISECOND, 0);

                    final long offset = date.getTimeInMillis() % day;

/*                    double[] q10 = new double[NO_TIMESLOTS];
                    double[] q25 = new double[NO_TIMESLOTS];
                    double[] q50 = new double[NO_TIMESLOTS];
                    double[] q75 = new double[NO_TIMESLOTS];
                    double[] q90 = new double[NO_TIMESLOTS];
*/

                    //i think this is a thing that iterates and collects timestamps based on NO_TIMESLOTS?
                    //limits it to 48 per day currently?
                    // OH IT MEANS NUMBER OF TIME SLOTS??
                    for (int i = 0; i < NO_TIMESLOTS; i++) {
                        int begin = i * timeslot;
                        int end = begin + timeslot;
                        List<Double> filtered = new Vector<Double>();

                        for (BgReadingStats reading : readings) {
                            long timeOfDay = (reading.timestamp - offset) % day;
                            if (timeOfDay >= begin && timeOfDay < end) {
                                filtered.add(reading.calculated_value);
                            }
                        }
                        /*
                        Collections.sort(filtered);
                        if (filtered.size() > 0) {
                            q10[i] = filtered.get((int) (filtered.size()  * 0.1));
                            q25[i] = filtered.get((int) (filtered.size() * 0.25));
                            q50[i] = filtered.get((int) (filtered.size() * 0.50));
                            q75[i] = filtered.get((int) (filtered.size() * 0.75));
                            q90[i] = filtered.get((int) (filtered.size() * 0.9));
                        } */

                    } /*
                    CalculatedData cd = new CalculatedData();
                    cd.q10 = q10;
                    cd.q25 = q25;
                    cd.q50 = q50;
                    cd.q75 = q75;
                    cd.q90 = q90;
                    setCalculatedData(cd);

                     */
                }
            };
            thread.start();
        }
        //will return null if not precalculated
        return calculatedData;
    }

    protected class CalculatedData {
        public double[] q10;
        public double[] q25;
        public double[] q50;
        public double[] q75;
        public double[] q90;
    }


    protected class trendFrag {
        private List<BgReadingStats> fragment;
        private boolean highTrend;

        public trendFrag(boolean high) {
            fragment = new ArrayList<BgReadingStats>();
            highTrend = high;
        }

        /* this is some lazy shit, but might be useful later if i give up
        public List<BgReadingStats> getFrag() {
            return fragment;
        } */

        public BgReadingStats get(int i) {
            return fragment.get(i);
        }

        public int size() {
            return fragment.size();
        }

        public void addReading(BgReadingStats reading) {
            fragment.add(reading);
        }

        public double getPeak() {
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
        }
    }
    protected class trend {
        private List<trendFrag> trendFragList;
        private long firstTimestamp;
        private long lastTimestamp;
        private double peakReading;
        private boolean highTrend;

        private double pRead;

        public trend(boolean high) {
            trendFragList = new ArrayList<trendFrag>();
            highTrend = high;
        }

        public void add(trendFrag t) {
            trendFragList.add(t);
        }

        //TODO is this the best way to calculate averages?
        public void calculateTrend() {
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
        }

        public long first() {
            return firstTimestamp;
        }

        public long last() {
            return lastTimestamp;
        }

        public double peak() {
            return peakReading;
        }

        public boolean isHighTrend() {
            return highTrend;
        }
    }
}
