package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
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

    public static final int OFFSET = 30;

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
            drawGrid(canvas);

            Paint myPaint = new Paint();
            myPaint.setColor(Color.WHITE);
            myPaint.setStrokeWidth(2);
            myPaint.setAntiAlias(true);
            myPaint.setStyle(Paint.Style.STROKE);
            canvas.drawText("DUMMY! - random data", 60, canvas.getHeight() / 2, myPaint);

        }


    }

    private void drawGrid(Canvas canvas) {
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setAntiAlias(false);
        myPaint.setColor(Color.LTGRAY);
        canvas.drawLine(OFFSET, 0, OFFSET, canvas.getHeight() - OFFSET, myPaint);
        canvas.drawLine(OFFSET, canvas.getHeight() - OFFSET, canvas.getWidth(), canvas.getHeight() - OFFSET, myPaint);

        for (int i= 0; i <24; i++){
            int x = (int)(OFFSET + ((canvas.getWidth()-OFFSET)/24d)*i);
            if(i%2 ==0 ) {
                canvas.drawLine(x, canvas.getHeight() - OFFSET - 2, x, canvas.getHeight() - OFFSET + 3, myPaint);
                if(i>=10) x = x-3;
                canvas.drawText(i + "", x - 4, canvas.getHeight() - OFFSET + 13, myPaint);
            } else {
                canvas.drawLine(x, canvas.getHeight() - OFFSET - 2, x, canvas.getHeight() - OFFSET+6, myPaint);
                if(i>=10) x = x-3;
                canvas.drawText(i + "", x - 4, canvas.getHeight() - OFFSET + 20, myPaint);
            }
        }

        myPaint.setPathEffect(new DashPathEffect(new float[]{2, 3}, 0));
        Path path = new Path();
        double[] levels = new double[]{50,100,150,200,250,300,350};

        for (int i = 0; i < levels.length; i++) {
            path.moveTo(OFFSET, getYfromBG(levels[i], canvas));
            path.lineTo(canvas.getWidth(), getYfromBG(levels[i], canvas));
            canvas.drawText((int)levels[i] + "", 5, getYfromBG(levels[i], canvas)+5, myPaint);
        }

        canvas.drawPath(path, myPaint);

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

    private void drawPolygon(Canvas canvas, int[] lowerValues, int[] higherValues,  int color) {

        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.FILL);
        myPaint.setAntiAlias(true);
        myPaint.setColor(color);

        Path myPath = new Path();
        myPath.reset();

        double xStep = (canvas.getWidth()-OFFSET)*1d/lowerValues.length;
        //lowerValues
        myPath.moveTo(OFFSET, getYfromBG(lowerValues[0], canvas));
        for (int i = 1; i<lowerValues.length; i++){
            myPath.lineTo((int)(i * xStep + OFFSET), getYfromBG(lowerValues[i], canvas));
        }
        // 00:00 == 24:00
        myPath.lineTo((int) (lowerValues.length * xStep + OFFSET), getYfromBG(lowerValues[0], canvas));
        myPath.lineTo((int) (higherValues.length * xStep + OFFSET), getYfromBG(higherValues[0], canvas));
        //higher Values
        for (int i = higherValues.length-1; i>=0; i--){
            myPath.lineTo((int)(i * xStep + OFFSET), getYfromBG(higherValues[i], canvas));
        }
        myPath.close();
        canvas.drawPath(myPath, myPaint);
    }

    private int getYfromBG(double bgValue, Canvas canvas) {
        return (int)(canvas.getHeight()- OFFSET - bgValue * (canvas.getHeight()-OFFSET) / 400);
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
