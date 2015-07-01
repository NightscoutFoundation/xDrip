package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.eveningoutpost.dexdrip.UtilityModels.Constants;

/**
 * Created by adrian on 30/06/15.
 */
public class PercentileView extends View {

    private RangeData rangeData = null;
    private boolean ranteDataCalculating = false;

    public static final int OFFSET = 20;

    public PercentileView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("DrawStats", "onDraw");
        super.onDraw(canvas);

        RangeData rd = getMaybeRangeData();

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
            int[] q25 = new int[24];
            int[] q75 = new int[24];
            for (int i = 0; i<q25.length; i++){
                q25[i] = (int) (100 + Math.random()*100);
                q75[i] = (int) (200 + Math.random()*200);
            }

            drawPolygon(canvas, q25, q75, Color.BLUE);



            drawHighLow(canvas);
            //draw(canvas);


//            myPaint.setColor(Color.WHITE);
//            myPaint.setStrokeWidth(2);
//            myPaint.setAntiAlias(true);
//            myPaint.setStyle(Paint.Style.STROKE);
//            canvas.drawText("DUMMY!", 30, canvas.getHeight() / 2, myPaint);

        }


    }

    private void drawHighLow(Canvas canvas) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));
        double high = Double.parseDouble(settings.getString("highValue", "170"));
        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl){
            high *= Constants.MMOLL_TO_MGDL;
            low *= Constants.MMOLL_TO_MGDL;

        }

        int highPosition = (int) (canvas.getHeight() - (high * (canvas.getHeight()-OFFSET)) / 400);
        int lowPosition = (int) (canvas.getHeight() - (low * (canvas.getHeight()-OFFSET)) / 400);
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setAntiAlias(false);
        myPaint.setColor(Color.RED);
        myPaint.setStrokeWidth(3);
        canvas.drawLine(OFFSET, lowPosition, canvas.getWidth(), lowPosition, myPaint);
        myPaint.setColor(Color.YELLOW);
        canvas.drawLine(OFFSET, highPosition, canvas.getWidth(), highPosition, myPaint);
    }

    private void drawPolygon(Canvas canvas, int[] lowerValues, int[] higherValues,  int color) {

        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.FILL);
        myPaint.setAntiAlias(true);
        myPaint.setColor(color);

        Path myPath = new Path();
        myPath.reset();

        int height = canvas.getHeight() - OFFSET;

        double xStep = (canvas.getWidth()-OFFSET)*1d/lowerValues.length;
        //lowerValuies
        myPath.moveTo(OFFSET,canvas.getHeight() - lowerValues[0] * canvas.getHeight() / 400);
        for (int i = 1; i<lowerValues.length; i++){
            myPath.lineTo((int)(i * xStep + OFFSET), canvas.getHeight() - (lowerValues[i] * height) / 400);
        }
        // 00:00 == 24:00
        myPath.lineTo((int) (lowerValues.length * xStep + OFFSET), canvas.getHeight() - (lowerValues[0] * height) / 400);
        myPath.lineTo((int) (higherValues.length * xStep + OFFSET), canvas.getHeight() - (higherValues[0] * height) / 400);
        //higher Values
        for (int i = higherValues.length-1; i>=0; i--){
            myPath.lineTo((int)(i * xStep + OFFSET), canvas.getHeight() - (higherValues[i] * height) / 400);
        }
        myPath.close();
        canvas.drawPath(myPath, myPaint);
    }


    public synchronized void setRangeData(RangeData rd) {
        rangeData = rd;
        postInvalidate();
    }


    //return either RangeData or start a calculation if not already started
    public synchronized RangeData getMaybeRangeData() {
        if (!ranteDataCalculating) {
            ranteDataCalculating = true;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    RangeData rd = new RangeData();
                    rd.aboveRange = DBSearchUtil.noReadingsAboveRange(getContext());
                    rd.belowRange = DBSearchUtil.noReadingsBelowRange(getContext());
                    rd.inRange = DBSearchUtil.noReadingsInRange(getContext());
                    setRangeData(rd);
                }
            };
            thread.start();
        }
        //will return null if not precalculated
        return rangeData;
    }

    protected class RangeData {
        public int inRange;
        public int aboveRange;
        public int belowRange;
    }

}
