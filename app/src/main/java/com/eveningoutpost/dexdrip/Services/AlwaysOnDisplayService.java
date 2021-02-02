package com.eveningoutpost.dexdrip.Services;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.utils.math.BlockFinder;
import com.eveningoutpost.dexdrip.xDripWidget;

import androidx.annotation.RequiresApi;
import lombok.val;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
import static com.eveningoutpost.dexdrip.Models.JoH.tsl;
import static com.eveningoutpost.dexdrip.ui.helpers.UiHelper.convertDpToPixel;

/**
 * jamorham
 *
 * Use the Accessibility feature to inject a widget in to the Always On Display lock screen
 *
 * Not all phones have this feature. Only ones with OLED style screen technology at a minimum.
 *
 */
public class AlwaysOnDisplayService extends AccessibilityService {

    public static final String UPDATE_AOD_DISPLAY = "com.eveingoutpost.dexdrip.action.UPDATE_AOD";
    private static final String LAYOUT = "android.widget.FrameLayout";
    private static final String TAG = "AlwaysOnDisplay";
    private static final boolean D = false;
    private volatile long lastScreenOn = 1;
    private FrameLayout frameLayout;
    private View aodView;

    private final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

    {
        try {
            layoutParams.width = convertDpToPixel(180);
            layoutParams.height = convertDpToPixel(100);
        } catch (Exception e) {
            //
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        }

        layoutParams.format = TRANSLUCENT;
        layoutParams.setTitle("xDrip Always On");
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        layoutParams.verticalMargin = 0f;

        if (Build.VERSION.SDK_INT >= 28) {
            layoutParams.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        layoutParams.flags = FLAG_FULLSCREEN | FLAG_NOT_TOUCHABLE | FLAG_LAYOUT_IN_SCREEN;
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        UserError.Log.d(TAG, "Accessibility event: " + event.toString());
    }

    @Override
    public void onInterrupt() {
        UserError.Log.d(TAG, "Interrupt event");
    }

    @Override
    public void onServiceConnected() {
        UserError.Log.d(TAG, "on Service connected");
        if (Build.VERSION.SDK_INT >= 22) {
            registerReceivers();
        } else {
            UserError.Log.e(TAG, "Not expected to work on older versions");
        }
        super.onServiceConnected();
    }

    @Override
    public void onDestroy() {
        UserError.Log.d(TAG, "on Destroy");
        removeLayout();
        unregisterReceivers();
        super.onDestroy();
    }

    void registerReceivers() {
        try {
            final IntentFilter intentFilter = new IntentFilter(UPDATE_AOD_DISPLAY);
            intentFilter.addAction(Intent.ACTION_TIME_TICK);
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            this.registerReceiver(this.screenReceiver, intentFilter);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Unable to register receivers: " + e);
        }
    }

    void unregisterReceivers() {
        try {
            this.unregisterReceiver(this.screenReceiver);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Unable to unregister receivers: " + e);
        }
    }

    void addLayout() {
        try {
            final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                UserError.Log.wtf(TAG, "Cannot get window manager to inject layout");
                return;
            }
            windowManager.addView(aodView, layoutParams);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Unable to add layout " + e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void injectLayout() {
        refreshView();
    }

    void removeLayout() {
        if (this.aodView != null) {
            removeLayout(this.aodView);
        }
    }

    void removeLayout(final View aodView) {
        try {
            final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                UserError.Log.wtf(TAG, "Cannot get window manager to remove layout");
                return;
            }
            if (aodView != null) {
                try {
                    windowManager.removeView(aodView);
                } catch (IllegalArgumentException e) {
                    UserError.Log.d(TAG, "Cannot remove layout as not attached");
                }
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Unable to remove layout: " + e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private boolean isScreenOn() {
        val displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            UserError.Log.wtf(TAG, "Cannot get display manager");
            return true;
        }
        for (val display : displayManager.getDisplays()) {
            UserError.Log.d(TAG, "Display state: " + display.getState());
            if (display.getState() != Display.STATE_OFF) {
                return true;
            }
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private synchronized void refreshView() {
        if (lastScreenOn != 0) {
            UserError.Log.d(TAG, "Screen got turned on - skipping");
            return;
        }
        final RemoteViews views = xDripWidget.displayCurrentInfo(this, convertDpToPixel(75), convertDpToPixel(50));
        val oldview = aodView;
        if (frameLayout == null) {
            frameLayout = new FrameLayout(this);
        }
        aodView = views.apply(this, frameLayout);
        aodView.setAlpha(0.8f);
        aodView.setBackgroundColor(Color.TRANSPARENT);
        if (D) aodView.setBackgroundColor(Color.RED);

        removeLayout(oldview);
        addLayout();
        rejigLayout();

    }

    // Examine the contents of the screen and find somewhere to randomly move the widget to
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void rejigLayout() {
        try {
            final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                UserError.Log.wtf(TAG, "Cannot get window manager to rejig layout");
                return;
            }
            if (aodView == null) {
                UserError.Log.d(TAG, "View not ready yet");
                return;
            }

            val bf = new BlockFinder();
            val list = getWindows();
            UserError.Log.d(TAG, "Windows list: " + list.size());
            val rect = new Rect();

            int screenMaxY = 0;

            for (val window : list) {
                window.getBoundsInScreen(rect);
                screenMaxY = Math.max(screenMaxY, rect.bottom);

                val root = window.getRoot();
                window.getBoundsInScreen(rect);
                UserError.Log.d(TAG, "Window bounds: " + rect.left + "," + rect.top + " " + rect.right + "," + rect.bottom + " MAX: " + screenMaxY);

                if (root != null) {
                    val children = root.getChildCount();
                    for (int i = 0; i < children; i++) {
                        val child = root.getChild(i);
                        child.getBoundsInScreen(rect);
                        if (child.getClassName().equals(LAYOUT)) {
                            val gchildren = child.getChildCount();
                            for (int j = 0; j < gchildren; j++) {
                                val gchild = child.getChild(j);
                                gchild.getBoundsInScreen(rect);
                                if (rect.top != 0 || (rect.bottom < 200)) {
                                    bf.addBlockWithMerge(rect.top, rect.bottom);
                                }
                            }
                        }
                    }

                } else {
                    UserError.Log.e(TAG, "Cannot get root view");
                }
            }
            UserError.Log.d(TAG, bf.toString());

            if (screenMaxY == 0) {
                final DisplayMetrics dm = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(dm);
                screenMaxY = dm.heightPixels;
                UserError.Log.d(TAG, "Couldn't determine max Y so using screen max of: " + screenMaxY);
            }

            layoutParams.y = bf.findRandomAvailablePositionWithFailSafe(layoutParams.height, screenMaxY);
            windowManager.updateViewLayout(aodView, layoutParams);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error with rejig display: " + e);
        }
    }

    final BroadcastReceiver screenReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(final Context context, final Intent intent) {
            try {
                final String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case Intent.ACTION_TIME_TICK:
                        UserError.Log.d(TAG, "Time tick");
                        refreshView();
                        break;

                    case Intent.ACTION_SCREEN_OFF:
                        lastScreenOn = 0;
                        Inevitable.task("aod-inject", 1500, () -> JoH.runOnUiThread(() -> injectLayout()));
                        break;

                    case Intent.ACTION_SCREEN_ON:
                        lastScreenOn = tsl();
                        removeLayout();
                        break;

                    default:
                        UserError.Log.e(TAG, "Unhandled action: " + action);
                        break;
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Exception in broadcast receiver: " + e);
            }
        }
    };

}
