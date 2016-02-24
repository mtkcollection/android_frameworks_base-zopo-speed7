/* Vanzo:libing on: Fri, 01 Nov 2013 14:23:38 +0800
 * add hall switch
 */
// End of Vanzo:libing
package com.android.server;

import android.content.Intent;
import android.content.Context;
import android.os.PowerManager;
import android.os.UEventObserver;
import android.util.Slog;
import java.io.FileReader;
import android.os.SystemClock;
import java.io.FileNotFoundException;
import android.view.IWindowManager;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.app.StatusBarManager;
import android.os.SystemProperties;
import com.android.featureoption.FeatureOption;
//import com.mediatek.common.featureoption.FeatureOption;


class HallSwitchObserver extends UEventObserver {
    private static final String TAG = HallSwitchObserver.class.getSimpleName();

    private static final String HALL_SWITCH_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/hall";
    private static final String HALL_STATE_PATH = "/sys/class/switch/hall/state";

    private int mHallState = -1;

    private final Context mContext;
    IWindowManager mIWindowManager;
    PowerManager mPmg;
    StatusBarManager mStatusBarManager;


 private Handler mHandler = new Handler() {

     @Override
         public void handleMessage(Message msg){
             switch (msg.what) {
                 case 1 :
                     Log.d(TAG, "goToSleep");
                     mPmg.goToSleep(SystemClock.uptimeMillis());
                     break;
             }
         }

 };
    public HallSwitchObserver(Context context) {
        mContext = context;

        mPmg = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mStatusBarManager = (StatusBarManager)
                                mContext.getSystemService(Context.STATUS_BAR_SERVICE);
        startObserving(HALL_SWITCH_UEVENT_MATCH);

        init();  // set initial status
    }

    @Override
    public void onUEvent(UEventObserver.UEvent event) {
        Slog.v(TAG, "Hall Switch UEVENT: " + event.toString());

        try {
            update(Integer.parseInt(event.get("SWITCH_STATE")));
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Could not parse switch state from event " + event);
        }
    }

    private synchronized final void init() {
        char[] buffer = new char[8];

        int newState = mHallState;
        try {
            FileReader file = new FileReader(HALL_STATE_PATH);
            int len = file.read(buffer, 0, 8);
            file.close();
            newState = Integer.valueOf((new String(buffer, 0, len)).trim());
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "This kernel does not have hall switch support");
        } catch (Exception e) {
            Slog.e(TAG, "" , e);
        }

        update(newState);
    }

    private synchronized final void update(int newState) {
        // reject all suspect transitions: only accept state changes from:
        // - a: 0 heaset to 1 mute
        // - b: 1 mute to 0 mute
/* Vanzo:libing on: Tue, 03 Sep 2013 15:46:17 +0800
 * implement #48035
 */
        if (!isNormalBoot()) return;
// End of Vanzo:libing
        if (mHallState == newState) return;

        mHallState = newState;

        // VANZO_SMART_COVER_SUPPORT is ture use smart cover feature; else use hall to screen on/off
        if (FeatureOption.VANZO_FEATURE_CIRCLE_WINDOW_SUPPORT) {
            if (newState == 0) {
                if (mStatusBarManager != null) {
                    mStatusBarManager.collapsePanels();
                }
                Intent i = new Intent("com.vanzotec.hallswitch.close");
                i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                mContext.sendBroadcast(i);

                Message msg = mHandler.obtainMessage(1);
                Log.d(TAG,"send delayed message");
                //mHandler.sendMessageDelayed(msg, 7000);
            } else {
                Log.i(TAG,"remove msg");
                mHandler.removeMessages(1);
                mPmg.wakeUp(SystemClock.uptimeMillis());

                Intent i = new Intent("com.vanzotec.hallswitch.open");
                i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                mContext.sendBroadcast(i);

                /*
                Log.i(TAG,"send keyevent 218");
                try {
                    Runtime.getRuntime().exec("/system/bin/input keyevent 218");
                } catch(java.io.IOException e) {
                    Log.w(TAG, "sendevent failed " + e);
                }
                */

            }
        } else {
            PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
            if (newState == 0) {
                pm.goToSleep(SystemClock.uptimeMillis());
            } else {
                pm.wakeUp(SystemClock.uptimeMillis());
            }
        }
    }

    private IWindowManager getWindowManager() {
        if (mIWindowManager == null) {
            IBinder b = ServiceManager.getService(Context.WINDOW_SERVICE);
            mIWindowManager = IWindowManager.Stub.asInterface(b);
        }
        return mIWindowManager;
    }
/* Vanzo:libing on: Tue, 03 Sep 2013 15:43:03 +0800
 * implement #48035
 */
    boolean isNormalBoot() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        Log.d("HallSwitchObserver", "isNormalBoot() bootReason = " + bootReason);
        boolean ret = (bootReason != null && bootReason.equals("0")) ? true : false;
        Log.d("HallSwitchObserver", "isNormalBoot() ret = " + ret);
        return ret;
    }
// End of Vanzo:libing

}
