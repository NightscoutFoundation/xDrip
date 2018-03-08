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

                mBuilder.setSmallIcon(Icon.createWithBitmap(getBitmap(text)));

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

    public static Bitmap getBitmap(final String text) {
        {
            if ((text == null) || (text.length() > 4)) return null;
            try {
                final int width = (int) xdrip.getAppContext().getResources().getDimension(android.R.dimen.notification_large_icon_width);
                final int height = (int) xdrip.getAppContext().getResources().getDimension(android.R.dimen.notification_large_icon_height);

                final DisplayMetrics dm = new DisplayMetrics();
                final Resources r = xdrip.getAppContext().getResources();

                if ((width > 600) || height > 600 || height < 16 || width < 16) return null;

                final Paint paint = new Paint();

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setAntiAlias(true);
                //paint.setTypeface(Typeface.MONOSPACE);
                paint.setTypeface(Typeface.SANS_SERIF); // TODO BEST?
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(17);
                final Rect bounds = new Rect();


                paint.getTextBounds(text, 0, text.length(), bounds);
                float textsize = (16 * width) / bounds.width();
                paint.setTextSize(textsize);
                paint.getTextBounds(text, 0, text.length(), bounds);

                // cannot be Config.ALPHA_8 as it doesn't work on Samsung
                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                final Canvas c = new Canvas(bitmap);

                c.drawText(text, 0, (height / 2) + (bounds.height() / 2), paint);
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
