package com.eveningoutpost.dexdrip;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.BgReading;

public class FloatingWidgetService extends Service implements BloodSugarUpdateReceiver.BloodSugarUpdateListener  {
    private WindowManager mWindowManager;
    private View mFloatingWidget;
    private static TextView mTextViewBloodSugar;
    private WindowManager.LayoutParams params;


    private BloodSugarUpdateReceiver mBloodSugarUpdateReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建广播接收器并注册
        mBloodSugarUpdateReceiver = new BloodSugarUpdateReceiver(this);
        IntentFilter filter = new IntentFilter(BloodSugarUpdateReceiver.ACTION_UPDATE_BLOOD_SUGAR);
        registerReceiver(mBloodSugarUpdateReceiver, filter);
    }

    private String getCurrentBloodSugar() {
        return "--/--";
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFloatingWidget == null) {
            createFloatingWidget();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createFloatingWidget() {
        mFloatingWidget = LayoutInflater.from(this).inflate(R.layout.floating_widget_layout, null);
        mTextViewBloodSugar = mFloatingWidget.findViewById(R.id.textView_blood_sugar);

        // 设置悬浮窗参数
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingWidget, params);

        updateBloodSugarValue(getCurrentBloodSugar());

        // 设置浮窗可拖动
        mFloatingWidget.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingWidget, params);
                        return true;
                }
                return false;
            }
        });
    }


    public static void updateBloodSugarValue(String bloodSugarValue) {
        if (mTextViewBloodSugar != null) {
            mTextViewBloodSugar.setText(bloodSugarValue);
        }
    }

    public static void updateBloodSugarValue(BgReading bgReading) {
        if (mTextViewBloodSugar != null) {
            // 获取血糖测量时间并格式化
            String bgTime = " " + new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(bgReading.timestamp));

            // 修改显示格式，添加时间信息
            String tempBgValue = bgReading.displayValue(null) + bgReading.displaySlopeArrow() + bgTime;
            mTextViewBloodSugar.setText(tempBgValue);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingWidget != null) {
            mWindowManager.removeView(mFloatingWidget);
        }
    }

    @Override
    public void onBloodSugarUpdate(String bloodSugarValue) {
        updateBloodSugarValue(bloodSugarValue);
    }
}

