package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

/**
 * Created by adrian on 30/06/15.
 */
public class PercentileView extends View {

    private CalculatedData calculatedData = null;
    private boolean ranteDataCalculating = false;

    public static final int OFFSET = 30;
    public static final int NO_TIMESLOTS = 48;



    public PercentileView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("DrawStats", "onDraw");
        super.onDraw(canvas);

        CalculatedData rd = getMaybeCalculatedData();

        if (rd == null) {
            Log.d("DrawStats", "onDraw if");

            Paint myPaint = new Paint();
            myPaint.setColor(Color.WHITE);
            myPaint.setStrokeWidth(2);
            myPaint.setAntiAlias(true);
            myPaint.setStyle(Paint.Style.STROKE);
            canvas.drawText("Calculating", 30, canvas.getHeight() / 2, myPaint);
        } else {
            Log.d("DrawStats", "onDraw else");
            drawPolygon(canvas, rd.q10, rd.q90, Color.CYAN, Paint.Style.FILL_AND_STROKE);
            drawPolygon(canvas, rd.q25, rd.q75, Color.BLUE, Paint.Style.FILL_AND_STROKE);
            drawPolygon(canvas, rd.q50, rd.q50, Color.WHITE, Paint.Style.STROKE);

            drawHighLow(canvas);
            drawGrid(canvas);
            drawLegend(canvas);
        }


    }

    private void drawLegend(Canvas canvas) {
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setAntiAlias(false);
        myPaint.setColor(Color.CYAN);
        canvas.drawText("10%/90%", OFFSET + 10, 10, myPaint);
        myPaint.setColor(Color.BLUE);
        canvas.drawText("25%/75%", OFFSET + 80, 10, myPaint);
        myPaint.setColor(Color.WHITE);
        canvas.drawText("50% (median)", OFFSET + 150, 10, myPaint);
    }


    private void drawGrid(Canvas canvas) {
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setAntiAlias(false);
        myPaint.setColor(Color.LTGRAY);
        canvas.drawLine(OFFSET, 0, OFFSET, canvas.getHeight() - OFFSET, myPaint);
        canvas.drawLine(OFFSET, canvas.getHeight() - OFFSET, canvas.getWidth(), canvas.getHeight() - OFFSET, myPaint);

        for (int i = 0; i < 24; i++) {
            int x = (int) (OFFSET + ((canvas.getWidth() - OFFSET) / 24d) * i);
            if (i % 2 == 0) {
                canvas.drawLine(x, canvas.getHeight() - OFFSET - 2, x, canvas.getHeight() - OFFSET + 3, myPaint);
                if (i >= 10) x = x - 3;
                canvas.drawText(i + "", x - 4, canvas.getHeight() - OFFSET + 13, myPaint);
            } else {
                canvas.drawLine(x, canvas.getHeight() - OFFSET - 2, x, canvas.getHeight() - OFFSET + 6, myPaint);
                if (i >= 10) x = x - 3;
                canvas.drawText(i + "", x - 4, canvas.getHeight() - OFFSET + 20, myPaint);
            }
        }


        // add level markings
        myPaint.setPathEffect(new DashPathEffect(new float[]{2, 3}, 0));
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
            path.moveTo(OFFSET, getYfromBG(levels[i], canvas));
            path.lineTo(canvas.getWidth(), getYfromBG(levels[i], canvas));
            canvas.drawText(labels[i], 5, getYfromBG(levels[i], canvas) + 4, myPaint);
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
        myPaint.setStrokeWidth(3);
        canvas.drawLine(OFFSET, lowPosition, canvas.getWidth(), lowPosition, myPaint);
        myPaint.setColor(Color.YELLOW);
        canvas.drawLine(OFFSET, highPosition, canvas.getWidth(), highPosition, myPaint);
    }

    private void drawPolygon(Canvas canvas, double[] lowerValues, double[] higherValues, int color, Paint.Style style ) {

        Paint myPaint = new Paint();
        myPaint.setStyle(style);
        myPaint.setAntiAlias(true);
        myPaint.setColor(color);
        myPaint.setPathEffect(new CornerPathEffect(10));

        Path myPath = new Path();
        myPath.reset();

        double xStep = (canvas.getWidth() - OFFSET) * 1d / lowerValues.length;
        //lowerValues
        myPath.moveTo(OFFSET, getYfromBG(lowerValues[0], canvas));
        for (int i = 1; i < lowerValues.length; i++) {
            myPath.lineTo((int) (i * xStep + OFFSET), getYfromBG(lowerValues[i], canvas));
        }
        // 00:00 == 24:00
        myPath.lineTo((int) (lowerValues.length * xStep + OFFSET), getYfromBG(lowerValues[0], canvas));
        myPath.lineTo((int) (higherValues.length * xStep + OFFSET), getYfromBG(higherValues[0], canvas));
        //higher Values
        for (int i = higherValues.length - 1; i >= 0; i--) {
            myPath.lineTo((int) (i * xStep + OFFSET), getYfromBG(higherValues[i], canvas));
        }
        myPath.close();
        canvas.drawPath(myPath, myPaint);
    }

    private int getYfromBG(double bgValue, Canvas canvas) {
        return (int) (canvas.getHeight() - OFFSET - bgValue * (canvas.getHeight() - OFFSET) / 400);
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
                    List<BgReading> readings = DBSearchUtil.getReadings(getContext());
                    int day = 1000 * 60 * 60 * 24;

                    int timeslot = day / NO_TIMESLOTS;

                    Calendar date = new GregorianCalendar();
                    date.set(Calendar.HOUR_OF_DAY, 0);
                    date.set(Calendar.MINUTE, 0);
                    date.set(Calendar.SECOND, 0);
                    date.set(Calendar.MILLISECOND, 0);

                    final long offset = date.getTimeInMillis() % day;

                    double[] q10 = new double[NO_TIMESLOTS];
                    double[] q25 = new double[NO_TIMESLOTS];
                    double[] q50 = new double[NO_TIMESLOTS];
                    double[] q75 = new double[NO_TIMESLOTS];
                    double[] q90 = new double[NO_TIMESLOTS];



                    for (int i = 0; i < NO_TIMESLOTS; i++) {
                        int begin = i*timeslot;
                        int end = begin+timeslot;
                        List<Double> filtered = new Vector<Double>();

                        for (BgReading reading: readings){
                            long timeOfDay = (reading.timestamp-offset)%day;
                            if(timeOfDay >= begin && timeOfDay< end){
                                filtered.add(reading.calculated_value);
                            }
                        }
                        Collections.sort(filtered);
                        if(filtered.size()>0){
                            q10[i] = filtered.get((int)(filtered.size()*0.1));
                            q25[i] = filtered.get((int)(filtered.size()*0.25));
                            q50[i] = filtered.get((int)(filtered.size()*0.50));
                            q75[i] = filtered.get((int)(filtered.size()*0.75));
                            q90[i] = filtered.get((int)(filtered.size()*0.9));
                        }

                    }
                    CalculatedData cd = new CalculatedData();
                    cd.q10 = q10;
                    cd.q25 = q25;
                    cd.q50 = q50;
                    cd.q75 = q75;
                    cd.q90 = q90;
                    setCalculatedData(cd);
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

}
