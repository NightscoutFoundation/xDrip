package com.eveningoutpost.dexdrip.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.DisplayMetrics;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenDpi;

/**
 * Created by jamorham on 05/03/2018.
 */

public class NumberGraphic {

    private static final String TAG = NumberGraphic.class.getSimpleName();
    private static final long[] vibratePattern = {0, 300, 300, 300, 300, 300};

    public static void testNotification(String text) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                final Notification.Builder mBuilder = new Notification.Builder(xdrip.getAppContext());

                mBuilder.setSmallIcon(Icon.createWithBitmap(getSmallIconBitmap(text)));

                mBuilder.setContentTitle("Test Number Graphic");
                mBuilder.setContentText("Check the number is visible");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mBuilder.setTimeoutAfter(Constants.SECOND_IN_MS * 30);
                } else {
                    JoH.runOnUiThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            JoH.cancelNotification(Constants.NUMBER_TEXT_TEST_ID);
                        }
                    }, Constants.SECOND_IN_MS * 30);
                }
                mBuilder.setOngoing(false);
                mBuilder.setVibrate(vibratePattern);

                int mNotificationId = Constants.NUMBER_TEXT_TEST_ID;
                final NotificationManager mNotifyMgr = (NotificationManager) xdrip.getAppContext().getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
                JoH.runOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());
                    }
                }, 1000);
            } else {
                JoH.static_toast_long("Not supported below Android 6");
            }
        }
    }

    public static boolean numberIconEnabled() {
        return Pref.getBooleanDefaultFalse("use_number_icon") && (Pref.getBooleanDefaultFalse("number_icon_tested"));
    }

    public static boolean largeNumberIconEnabled() {
        return Pref.getBooleanDefaultFalse("use_number_icon_large") && (Pref.getBooleanDefaultFalse("number_icon_tested"));
    }

    public static boolean largeWithArrowEnabled() {
        return largeNumberIconEnabled() && Pref.getBooleanDefaultFalse("number_icon_large_arrow");
    }

    public static Bitmap getSmallIconBitmap(final String text) {
        return getBitmap(text, Color.WHITE, null);
    }

    public static Bitmap getLargeIconBitmap(final String text) {
        return getBitmap(text, Color.BLACK, null);
    }

    public static Bitmap getLargeWithArrowBitmap(final String text, final String arrow) {
        return getBitmap(text, Color.BLACK, arrow);
    }

    public static Bitmap getLockScreenBitmapWhite(final String text, final String arrow, final boolean strike_through) {
        final double x_ratio = JoH.tolerantParseDouble(Pref.getString("numberwall_x_param", ""), 50d) / 100d;
        final double y_ratio = JoH.tolerantParseDouble(Pref.getString("numberwall_y_param", ""), 50d) / 100d;
        final double spacer_ratio = JoH.tolerantParseDouble(Pref.getString("numberwall_s_param", ""), 10d) / 100d;
        UserError.Log.d(TAG, "x_y_s_ratio: " + x_ratio + " " + y_ratio + " " + spacer_ratio);
        return getBitmap(text, Color.WHITE, arrow, (int) (getScreenDpi() * x_ratio), (int) (getScreenDpi() * x_ratio * y_ratio), (int) (getScreenDpi() * x_ratio * spacer_ratio), strike_through);
    }

    public static Bitmap getBitmap(final String text, int fillColor, final String arrow) {
        try {
            final int width = (int) xdrip.getAppContext().getResources().getDimension(android.R.dimen.notification_large_icon_width);
            final int height = (int) xdrip.getAppContext().getResources().getDimension(android.R.dimen.notification_large_icon_height);
            return getBitmap(text, fillColor, arrow, width, height);
        } catch (Exception e) {
            if (JoH.ratelimit("icon-failure", 60)) {
                UserError.Log.e(TAG, "Cannot create number icon dimensions: " + e);
            }
            return null;
        }
    }

    public static Bitmap getBitmap(final String text, int fillColor, final String arrow, final int width, final int height) {
        return getBitmap(text, fillColor, arrow, width, height, 0, false);
    }

    public static Bitmap getBitmap(final String text, int fillColor, final String arrow, final int width, final int height, final int margin, final boolean strike_through) {
        {
            if ((text == null) || (text.length() > 4)) return null;
            try {
                final DisplayMetrics dm = new DisplayMetrics();
                final Resources r = xdrip.getAppContext().getResources();

                if ((width > 1900) || height > 1900 || height < 16 || width < 16) return null;

                final Paint paint = new Paint();

                paint.setStrikeThruText(strike_through);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(fillColor);
                paint.setAntiAlias(true);
                //paint.setTypeface(Typeface.MONOSPACE);
                paint.setTypeface(Typeface.SANS_SERIF); // TODO BEST?
                paint.setTextAlign(Paint.Align.LEFT);
                float paintTs = (arrow == null ? 17 : 17 - arrow.length());
                paint.setTextSize(paintTs);
                final Rect bounds = new Rect();

                String fullText = text + (arrow != null ? arrow : "");

                paint.getTextBounds(fullText, 0, fullText.length(), bounds);
                float textsize = ((paintTs - 1) * (width - margin)) / bounds.width();
                paint.setTextSize(textsize);
                paint.getTextBounds(fullText, 0, fullText.length(), bounds);

                // cannot be Config.ALPHA_8 as it doesn't work on Samsung
                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                final Canvas c = new Canvas(bitmap);

                c.drawText(fullText, 0, (height / 2) + (bounds.height() / 2), paint);
                return bitmap;
            } catch (Exception e) {
                if (JoH.ratelimit("icon-failure", 60)) {
                    UserError.Log.e(TAG, "Cannot create number icon: " + e);
                }
                return null;
            }
        }
    }

}
