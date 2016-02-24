package com.android.internal.policy.impl.smartwake;

import android.view.WindowManager;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.graphics.PixelFormat;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.content.pm.ActivityInfo;


public class WindowGestureView {


    private Context mContext;
    private  WindowManager.LayoutParams params;
    private WindowManager wm;

    private static final String TAG = "WindowGestureView";

    private static final int SHOW = 0;
    private static final int HIDE = 1;
    private boolean mViewflag;
    private DrawGestureView mDrawGestureView;
    private int mGestureId = 0;


    private Handler mHandler = new Handler() {

        @Override
            public void handleMessage(Message msg){
                switch (msg.what) {

                    case SHOW:
                        break;

                    case HIDE:
                        wm.removeView(mDrawGestureView);
                        Intent intent = new Intent("android.intent.action.GESTURE_DONE");
                        mContext.sendBroadcast(intent);
                        mViewflag = false;
                        break;
                }
            }

    };
    public WindowGestureView(Context context) {
        mContext = context;
        initView();
    }

    public void initView() {
        registerBroadcast();
        wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);

        params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);

        mViewflag = false;
        mDrawGestureView = new DrawGestureView(mContext);
        mDrawGestureView.setOnGestureDrawDoneListener(new DrawGestureView.OnGestureDrawDoneListener(){
                @Override
                public void OnGestureDrawDone() {
                    mHandler.sendEmptyMessageDelayed(HIDE, 100);
                }
        });
    }

    public void showView(int gesture) {
        if (mViewflag == false) {
            Log.d(TAG,"show view");
            wm.addView(mDrawGestureView, params);
            mDrawGestureView.showView(gesture);
            mViewflag = true;
        }
    }

    public void hideView() {
        if (mViewflag == true) {
            Log.d(TAG,"hide view");
            mHandler.sendEmptyMessage(HIDE);
        }
    }

    public void registerBroadcast() {

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.GESTURE_O");
        intentFilter.addAction("android.intent.action.GESTURE_W");
        intentFilter.addAction("android.intent.action.GESTURE_C");
        intentFilter.addAction("android.intent.action.GESTURE_E");
        intentFilter.addAction("android.intent.action.GESTURE_M");
        mContext.registerReceiver(mGestureReceiver, intentFilter);
    }

    private final BroadcastReceiver mGestureReceiver = new BroadcastReceiver() {
        @Override
            public void onReceive(final Context context, final Intent intent) {
                if (intent.getAction().equals("android.intent.action.GESTURE_O")) {
                    showView(DrawGestureView.GESTURE_O);
                } else if (intent.getAction().equals("android.intent.action.GESTURE_W")) {
                    showView(DrawGestureView.GESTURE_W);
                } else if (intent.getAction().equals("android.intent.action.GESTURE_C")) {
                    showView(DrawGestureView.GESTURE_C);
                } else if (intent.getAction().equals("android.intent.action.GESTURE_E")) {
                    showView(DrawGestureView.GESTURE_E);
                } else if (intent.getAction().equals("android.intent.action.GESTURE_M")) {
                    showView(DrawGestureView.GESTURE_M);
                }

            }
    };

}
