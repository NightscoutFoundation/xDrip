package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;

import com.github.amlcurran.showcaseview.ShowcaseDrawer;

class StandardShowcaseDrawer implements ShowcaseDrawer {

    protected final Paint eraserPaint;
    protected final Drawable showcaseDrawable;
    private final Paint basicPaint;
    private final float showcaseRadius;
    protected int backgroundColour;

    public StandardShowcaseDrawer(Resources resources, Resources.Theme theme) {
        PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
        eraserPaint = new Paint();
        eraserPaint.setColor(0xFFFFFF);
        eraserPaint.setAlpha(0);
        eraserPaint.setXfermode(xfermode);
        eraserPaint.setAntiAlias(true);
        basicPaint = new Paint();
        showcaseRadius = resources.getDimension(com.github.amlcurran.showcaseview.R.dimen.showcase_radius);
        showcaseDrawable = ResourcesCompat.getDrawable(resources, com.github.amlcurran.showcaseview.R.drawable.cling_bleached, theme);
    }

    @Override
    public void setShowcaseColour(int color) {
        showcaseDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    @Override
    public void drawShowcase(Bitmap buffer, float x, float y, float scaleMultiplier) {
        Canvas bufferCanvas = new Canvas(buffer);

        bufferCanvas.drawCircle(x, y, showcaseRadius, eraserPaint);
        int halfW = getShowcaseWidth() / 2;
        int halfH = getShowcaseHeight() / 2;
        int left = (int) (x - halfW);
        int top = (int) (y - halfH);
        showcaseDrawable.setBounds(left, top,
                left + getShowcaseWidth(),
                top + getShowcaseHeight());
        showcaseDrawable.draw(bufferCanvas);
    }

    @Override
    public int getShowcaseWidth() {
        return showcaseDrawable.getIntrinsicWidth();
    }

    @Override
    public int getShowcaseHeight() {
        return showcaseDrawable.getIntrinsicHeight();
    }

    @Override
    public float getBlockedRadius() {
        return showcaseRadius;
    }

    @Override
    public void setBackgroundColour(int backgroundColor) {
        this.backgroundColour = backgroundColor;
    }

    @Override
    public void erase(Bitmap bitmapBuffer) {
        bitmapBuffer.eraseColor(backgroundColour);
    }

    @Override
    public void drawToCanvas(Canvas canvas, Bitmap bitmapBuffer) {
        canvas.drawBitmap(bitmapBuffer, 0, 0, basicPaint);
    }

}
