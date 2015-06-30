package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

/**
 * Created by adrian on 30/06/15.
 */
public class ChartView extends View {
    public ChartView(Context context) {
        super(context);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("DrawStats", "onDraw");
        super.onDraw(canvas);
        Paint myPaint = new Paint();
        int r = (int)(Math.random()*255);
        int g = (int)(Math.random()*255);
        int b = (int)(Math.random()*255);
        myPaint.setColor(Color.rgb(r, g, b));
        myPaint.setStrokeWidth(10);
        myPaint.setAntiAlias(true);
        myPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(0, 0, canvas.getWidth(), canvas.getHeight(), myPaint);
    }
}
