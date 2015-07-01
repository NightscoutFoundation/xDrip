package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

/**
 * Created by adrian on 30/06/15.
 */
public class PercentileView extends View {

    private RangeData rangeData = null;
    private boolean ranteDataCalculating = false;

    public static final int OFFSET = 10;

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

            int side = Math.min((canvas.getWidth() - 10), (canvas.getHeight() - 10));
            RectF rect = new RectF((canvas.getWidth() - side) / 2, (canvas.getHeight() - side) / 2, (canvas.getWidth() - side) / 2 + side, (canvas.getHeight() - side) / 2 + side);
            Paint myPaint = new Paint();
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setAntiAlias(true);
            myPaint.setColor(Color.BLUE);

            int[] q25 = new int[24];
            int[] q75 = new int[24];
            for (int i = 0; i<q25.length; i++){
                q25[i] = (int) (100 + Math.random()*100);
                q75[i] = (int) (200 + Math.random()*200);
            }

            drawPolygon(canvas, myPaint, q25, q75);


//            myPaint.setColor(Color.WHITE);
//            myPaint.setStrokeWidth(2);
//            myPaint.setAntiAlias(true);
//            myPaint.setStyle(Paint.Style.STROKE);
//            canvas.drawText("DUMMY!", 30, canvas.getHeight() / 2, myPaint);

        }


    }

    private void drawPolygon(Canvas canvas, Paint myPaint, int[] q25, int[] q75) {
        Path myPath = new Path();
        myPath.reset();
        myPath.moveTo(0, q25[0] * canvas.getHeight() / 400);

        int height = canvas.getHeight() - OFFSET;

        double xStep = canvas.getWidth()*1d/q25.length;
        for (int i = 1; i<q25.length; i++){
            myPath.lineTo((int)(i * xStep), (q25[i] * height) / 400);
        }
        for (int i = q75.length-1; i>=0; i--){
            myPath.lineTo((int)(i * xStep), (q75[i] * height) / 400);
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
