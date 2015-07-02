package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

/**
 * Created by adrian on 30/06/15.
 */
public class ChartView extends View {

    private RangeData rangeData = null;
    private boolean ranteDataCalculating = false;

    public ChartView(Context context) {
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

            if((rd.aboveRange + rd.belowRange + rd.inRange) == 0){
                Paint myPaint = new Paint();
                myPaint.setColor(Color.WHITE);
                myPaint.setStrokeWidth(2);
                myPaint.setAntiAlias(true);
                myPaint.setStyle(Paint.Style.STROKE);
                canvas.drawText("Not enough data!", 30, canvas.getHeight() / 2, myPaint);
                return;
            }

            int side = Math.min((canvas.getWidth() - 10), (canvas.getHeight() - 10));
            RectF rect = new RectF((canvas.getWidth() - side) / 2, (canvas.getHeight() - side) / 2, (canvas.getWidth() - side) / 2 + side, (canvas.getHeight() - side) / 2 + side);
            Paint myPaint = new Paint();
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setAntiAlias(true);


            float inDeg = rd.inRange * 360f / (rd.inRange + rd.belowRange + rd.aboveRange);
            float lowDeg = rd.belowRange * 360f / (rd.inRange + rd.belowRange + rd.aboveRange);
            float highDeg = rd.aboveRange * 360f / (rd.inRange + rd.belowRange + rd.aboveRange);

            Log.d("DrawStats", "in,low, high degree: " + inDeg + " " + lowDeg + " " + highDeg);

            myPaint.setColor(android.graphics.Color.RED);
            canvas.drawArc(rect, -90, lowDeg, true, myPaint);
            myPaint.setColor(Color.GREEN);
            canvas.drawArc(rect, -90 + lowDeg, inDeg, true, myPaint);
            myPaint.setColor(Color.YELLOW);
            canvas.drawArc(rect, -90 + lowDeg + inDeg, highDeg, true, myPaint);
        }


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
