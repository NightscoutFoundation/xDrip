package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

/**
 * Created by adrian on 30/06/15.
 */
public class ChartView extends View {

    private RangeData tdRangeData = null;
    private boolean tdDataCalculating = false;

    public ChartView(Context context) {
        super(context);
    }

    public void setRandom(double random) {
        this.random = random;
    }

    private double random = 0d;


    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("DrawStats", "onDraw");
        super.onDraw(canvas);


//        Thread thread = new Thread(){
//            @Override
//            public void run() {
//                super.run();
//                //TODO: just for testing
//                DBSearchUtil.readingsToday();
//                DBSearchUtil.readingsYesterday();
//                DBSearchUtil.lastXDays(90);
//                //setRandom(Math.random());
//                postInvalidate();
//            }
//        };
//        thread.start();

        RangeData rd = getMaybeTDRangeData();
        if (rd == null){
            Paint myPaint = new Paint();
            int r = (int)(Math.random()*255);
            int g = (int)(Math.random()*255);
            int b = (int)(Math.random()*255);
            myPaint.setColor(Color.rgb(r, g, b));
            myPaint.setStrokeWidth(3);
            myPaint.setAntiAlias(true);
            myPaint.setStyle(Paint.Style.STROKE);
            canvas.drawText("Calculating", 10, 10, myPaint);
            //canvas.drawLine(0, 0, canvas.getWidth(), canvas.getHeight(), myPaint);
        } else {

            int side = Math.min((canvas.getWidth()-10), (canvas.getHeight()-10));

            RectF rect = new RectF((canvas.getWidth()-side)/2, (canvas.getHeight()-side)/2, (canvas.getWidth()-side)/2+side, (canvas.getHeight()-side)/2+side);
            Paint myPaint = new Paint();
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setAntiAlias(true);

            float inDeg = rd.inRange*380f/(rd.inRange + rd.belowRange + rd.aboveRange);
            float lowDeg = rd.belowRange*380f/(rd.inRange + rd.belowRange + rd.aboveRange);
            float highDeg = rd.aboveRange*380f/(rd.inRange + rd.belowRange + rd.aboveRange);


            myPaint.setColor(android.graphics.Color.RED);
            canvas.drawArc(rect, -90, lowDeg, true, myPaint);
            myPaint.setColor(Color.GREEN);
            canvas.drawArc(rect, -90+lowDeg, inDeg, true, myPaint);
            myPaint.setColor(Color.YELLOW);
            canvas.drawArc(rect, -90+lowDeg+inDeg, highDeg, true, myPaint);

            /*Paint myPaint = new Paint();
            int r = (int)(Math.random()*255);
            int g = (int)(Math.random()*255);
            int b = (int)(Math.random()*255);
            myPaint.setColor(Color.rgb(r, g, b));
            myPaint.setStrokeWidth(3);
            myPaint.setAntiAlias(true);
            String text = "" + Math.round(rd.inRange*1000d/(rd.inRange + rd.belowRange + rd.aboveRange))/10d +
                    "%, " + + Math.round(rd.aboveRange*1000d/(rd.inRange + rd.belowRange + rd.aboveRange))/10d +
                    "%, " + + Math.round(rd.belowRange*1000d/(rd.inRange + rd.belowRange + rd.aboveRange))/10d + "%";
            canvas.drawText(text, 10, 10, myPaint);*/
        }



    }


    public  synchronized  void setTdRangeData(RangeData rd){
        tdRangeData = rd;
        postInvalidate();
    }

    public synchronized RangeData getMaybeTDRangeData(){
        if (!tdDataCalculating){
            tdDataCalculating = true;
            Thread thread = new Thread(){
                @Override
                public void run() {
                    super.run();
                    RangeData rd = new RangeData();
                    rd.aboveRange = DBSearchUtil.readingsAboveRangeAfterTimestamp(getContext());
                    rd.belowRange = DBSearchUtil.readingsBelowRangeAfterTimestamp( getContext());
                    rd.inRange = DBSearchUtil.readingsInRangeAfterTimestamp(getContext());
                    setTdRangeData(rd);
                }
            };
            thread.start();

        }


        return tdRangeData;
    }

    protected class RangeData  {
        public int inRange;
        public int aboveRange;
        public int belowRange;
    }

}
