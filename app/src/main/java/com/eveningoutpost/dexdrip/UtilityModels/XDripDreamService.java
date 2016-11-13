package com.eveningoutpost.dexdrip.UtilityModels;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;

/**
 * Created by jamorham on 31/10/2016.
 * <p>
 * Just a bit of fun: graph and glucose data should very slowly float around the daydream screen.
 * Could be used on a nightstand for continuous nighttime display.
 * <p>
 * Improvements might include preferences to adjust motion, enable/disable full screen mode
 * add remove visible components, change component size etc.
 */

public class XDripDreamService extends DreamService {

    private static final String TAG = "xDripDreamService";
    private TextView widgetbg;
    private TextView widgetArrow;
    private TextView widgetDelta;
    private TextView widgetStatusLine;
    private TextView widgetReadingAge;
    private ImageView graphimage;

    private int graph_width;
    private int graph_height;

    private Bouncer mBouncer;
    private Handler mainHandler;
    private boolean updated;
    private boolean keep_running;

    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (!keep_running) {
                Log.d(TAG, "Keep running false: exiting");
                return;
            }
            Log.d(TAG, "Runnable executing");
            mainHandler.postDelayed(mRunnable, updateData());
        }
    };


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mainHandler = new Handler(getApplicationContext().getMainLooper());
        setInteractive(false);
        //  setFullscreen(true);
        setScreenBright(false);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        final DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screen_width = dm.widthPixels;
        int screen_height = dm.heightPixels;

        // TODO test on various screens

        double screen_scale = (dm.densityDpi / 160f);
        graph_width = (int) (230 * screen_scale);
        graph_height = (int) (128 * screen_scale);
        int widget_width = (int) (180 * screen_scale);
        int widget_height = (int) (100 * screen_scale);
        Log.d(TAG, "Width: " + graph_width + " Height: " + graph_height);

        // sanity check
        if (graph_height >= screen_height) graph_height = screen_height - 20;
        if (graph_width >= screen_width) graph_width = screen_width - 20;
        if (widget_height >= screen_height) widget_height = screen_height - 20;
        if (widget_width >= screen_width) widget_width = screen_width - 20;

        final FrameLayout.LayoutParams gl = new FrameLayout.LayoutParams(graph_width, graph_height);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(150, 150);

        mBouncer = new Bouncer(this);
        mBouncer.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mBouncer.setSpeed(0.1f);


        graphimage = new ImageView(this);
        updateGraph();
        //  image.setBackgroundColor(0xFF004000);
        mBouncer.addView(graphimage, gl);

        for (int i = 0; i < 1; i++) {
            final ImageView image = new ImageView(this);
            image.setImageResource(R.drawable.ic_launcher);
            image.setAlpha(0.3f);
            //  image.setBackgroundColor(0xFF004000);
            mBouncer.addView(image, lp);
        }

        // final View tv = new View(this);
        final LayoutInflater inflater = LayoutInflater.from(this);
        final View inflatedLayout = inflater.inflate(R.layout.x_drip_widget, null, false);

        inflatedLayout.setBackgroundColor(Color.TRANSPARENT);
        widgetbg = (TextView) inflatedLayout.findViewById(R.id.widgetBg);
        widgetArrow = (TextView) inflatedLayout.findViewById(R.id.widgetArrow);
        widgetDelta = (TextView) inflatedLayout.findViewById(R.id.widgetDelta);
        widgetStatusLine = (TextView) inflatedLayout.findViewById(R.id.widgetStatusLine);
        widgetReadingAge = (TextView) inflatedLayout.findViewById(R.id.readingAge);

        final float alpha = 0.6f;
        widgetbg.setAlpha(alpha);
        widgetArrow.setAlpha(alpha);
        widgetDelta.setAlpha(alpha);
        widgetReadingAge.setAlpha(alpha);

        keep_running = true;
        if (!updated) {
            mainHandler.post(mRunnable);
        }

        mBouncer.addView(inflatedLayout, new FrameLayout.LayoutParams(widget_width, widget_height));

        setContentView(mBouncer);

    }

    @Override
    public void onDreamingStopped() {
        keep_running = false;
        super.onDreamingStopped();

    }

    private long updateData() {
        updated = true;
        long delay = 60000;
        final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        if (dg != null) {
            // TODO Coloring
            widgetbg.setText(dg.spannableString(dg.unitized,true));
            widgetArrow.setText(dg.isStale() ? "" : dg.spannableString(dg.delta_arrow,true));
            widgetDelta.setText(dg.spannableString(dg.unitized_delta));
            widgetReadingAge.setText(dg.minutesAgo(true));
            widgetStatusLine.setText("");
            // try to align our minute updates with 10 seconds after reading should arrive to show
            // most recent data with least polling and without using broadcast receiver
            final long timemod = ((300000 - dg.mssince) % 60000) + 10000;
            Log.d(TAG, "Time mod: " + timemod);
            if (timemod > 1000) delay = timemod;
            updateGraph();
        }
        return delay; // default clock
    }

    private void updateGraph() {
        long end = System.currentTimeMillis() + (60000 * 5);
        long start = end - (60000 * 60 * 3) - (60000 * 10);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(this, start, end);
        final Bitmap dreamBitmap = new BgSparklineBuilder(this)
                .setBgGraphBuilder(bgGraphBuilder)
                .setWidthPx(graph_width)
                .setHeightPx(graph_height)
                .showHighLine()
                .showLowLine()
                .setStart(System.currentTimeMillis() - 60000 * 60 * 3)
                .showAxes(true)
                .setBackgroundColor(getCol(ColorCache.X.color_notification_chart_background))
                .setShowFiltered(DexCollectionType.hasFiltered() && Home.getPreferencesBooleanDefaultFalse("show_filtered_curve"))
                .build();
        graphimage.setImageBitmap(dreamBitmap);
    }

    // from http://code.google.com/p/android-daydream-samples
    public class Bouncer extends FrameLayout implements TimeAnimator.TimeListener {
        private float mMaxSpeed;
        private final TimeAnimator mAnimator;
        private int mWidth, mHeight;

        public Bouncer(Context context) {
            this(context, null);
        }

        public Bouncer(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public Bouncer(Context context, AttributeSet attrs, int flags) {
            super(context, attrs, flags);
            mAnimator = new TimeAnimator();
            mAnimator.setTimeListener(this);
        }

        /**
         * Start the bouncing as soon as we’re on screen.
         */
        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            mAnimator.start();
        }

        /**
         * Stop animations when the view hierarchy is torn down.
         */
        @Override
        public void onDetachedFromWindow() {
            mAnimator.cancel();
            super.onDetachedFromWindow();
        }

        /**
         * Whenever a view is added, place it randomly.
         */
        @Override
        public void addView(View v, ViewGroup.LayoutParams lp) {
            super.addView(v, lp);
            setupView(v);
        }

        /**
         * Reposition all children when the container size changes.
         */
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mWidth = w;
            mHeight = h;
            for (int i = 0; i < getChildCount(); i++) {
                setupView(getChildAt(i));
            }
        }

        /**
         * Bouncing view setup: random placement, random velocity.
         */
        private void setupView(View v) {
            final PointF p = new PointF();
            final float a = (float) (Math.random() * 360);
            p.x = mMaxSpeed * (float) (Math.cos(a));
            p.y = mMaxSpeed * (float) (Math.sin(a));
            v.setTag(p);
            v.setX((float) (Math.random() * (mWidth - v.getWidth())));
            v.setY((float) (Math.random() * (mHeight - v.getHeight())));
        }

        /**
         * Every TimeAnimator frame, nudge each bouncing view along.
         */
        public void onTimeUpdate(TimeAnimator animation, long elapsed, long dt_ms) {
            final float dt = dt_ms / 1000f; // seconds
            for (int i = 0; i < getChildCount(); i++) {
                final View view = getChildAt(i);
                final PointF v = (PointF) view.getTag();

                // step view for velocity * time
                view.setX(view.getX() + v.x * dt);
                view.setY(view.getY() + v.y * dt);

                // handle reflections
                final float l = view.getX();
                final float t = view.getY();
                final float r = l + view.getWidth();
                final float b = t + view.getHeight();
                boolean flipX = false, flipY = false;
                if (r > mWidth) {
                    view.setX(view.getX() - 2 * (r - mWidth));
                    flipX = true;
                } else if (l < 0) {
                    view.setX(-l);
                    flipX = true;
                }
                if (b > mHeight) {
                    view.setY(view.getY() - 2 * (b - mHeight));
                    flipY = true;
                } else if (t < 0) {
                    view.setY(-t);
                    flipY = true;
                }
                if (flipX) v.x *= -1;
                if (flipY) v.y *= -1;
            }
        }

        public void setSpeed(float s) {
            mMaxSpeed = s;
        }
    }
}
