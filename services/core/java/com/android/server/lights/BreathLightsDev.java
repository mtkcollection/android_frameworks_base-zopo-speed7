/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Vanzo:helianyi on: Wed, 26 Nov 2014 13:41:59 +0800
 * TODO: helianyi add breath light support
 */

package com.android.server.lights;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IHardwareService;
import android.os.ServiceManager;
import android.os.Message;
import android.util.Slog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.CallLog;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputFilter;
import android.view.KeyEvent;
import android.os.PowerManager;
import android.os.BatteryManager;
import android.database.ContentObserver;
import android.database.DatabaseUtils;
import android.os.SystemProperties;
import com.android.server.power.ShutdownThread;
import android.os.SystemClock;
import android.view.IWindowManager;
import android.os.RemoteException;
import com.android.server.lights.LightsService;

public class BreathLightsDev  {

    private static final boolean DEBUG = true;
    private static final String TAG = "BreathLightsDev";
    private static String OWER;
    private static final String DevName = "aw2013";
    private static final String BreathKeyIntent = "android.intent.action.BREATH_LIGHT";
    private static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";
    private static LightsService.LightImpl myBreathlight;
    private static PowerManager mPm;
    private static IWindowManager mWindowManagerService;
    private static boolean isInited = false;


    private static final int LED_OFF = 0;
    private static final int LED_RED = 1;
    private static final int LED_GREEN = 2;
    private static final int LED_BLUE = 3;
    private static final int LED_CONST_RED = 4;
    private static final int LED_CONST_GREEN = 5;
    private static final int LED_CONST_BLUE = 6;
    private static final int LED_CONST_RG = 7;
    private static final int LED_CONST_RB = 8;
    private static final int LED_CONST_GB = 9;
    private static final int LED_CONST_RGB = 10;
    private static final int LED_BREATH_RG = 11;
    private static final int LED_BREATH_RB = 12;
    private static final int LED_BREATH_GB = 13;
    private static final int LED_BREATH_RGB = 14;
    private static final int LED_AUTO_BREATH = 15;   // screen on with lock, chargering
    private static final int LED_AUTO_BREATH_ON_SHUTDOWN = 16;  //  power off chargering, dont handle this in here, this state be handled in mediatek/external/charger
    private static final int LED_BRIGHT_STATUS = 17;  //screen unlock
    private static final int LED_FREE_BUTTON = 18;  //key release
    private static final int LED_MAX = LED_FREE_BUTTON + 1;

    private static final int screenOff = 0;
    private static final int screenOn = 1;
    private static final int screenUnlock = 2;

    private static int mLed = LED_MAX;
    private static  String mAction;
    private static boolean chargering = false;
    private static boolean isPlugged = false;
    private static int screenState = screenOn;
    private static boolean mInCall = false;
    private static String mPhoneState =TelephonyManager.EXTRA_STATE_IDLE;
    private static int mBatteryStatus  = BatteryManager.BATTERY_STATUS_DISCHARGING;
    private static int mPlugged = BatteryManager.BATTERY_PLUGGED_AC;
    private static int level;
    private static boolean mMissLastCall = false;
    private static boolean mUnreadLastMsg = false;
    private static boolean mBatteryNotice = false;
    private static  Context myContext;
    private static int mNewUnreadMsgCount = 0;
    private static int mLastUnreadMsgCount = 0;
    private static int mNewMissCallCount = 0;
    private static int mLastMissCallCount = 0;
    private static boolean isBootup = false;
    private static final Object actionLock = new Object();
    private static boolean isChargeSwitch = false;
    private static boolean isBatteryLowSwitch = false;
    private static boolean isMissEventSwitch = false;
    private static boolean isRunning = false;


    public BreathLightsDev(Context mContext, String name, LightsService.LightImpl mLight){
        if (isInited == false) {
            OWER = name;
            myContext = mContext;
            myBreathlight = mLight;
            listenForBroadcasts();
            //registerNewSmsObserver();
            isInited = true;
            myBreathlight.setFlashing(LED_BRIGHT_STATUS, -1, -1, -1);
        }
    }

    private void listenForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        // intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        //  intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_REBOOT);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        // intentFilter.addAction("android.intent.action.ACTION_BOOT_IPO");
        intentFilter.addAction("android.intent.action.ACTION_PRE_SHUTDOWN");
        intentFilter.addAction(BreathKeyIntent);
        myContext.registerReceiver(mBroadcastReciever, intentFilter);

    }

    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {
        @Override 
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action != null) {
                    saveActionPara(action,  intent);
                    if ((action.equals(Intent.ACTION_SCREEN_ON))||(action.equals(Intent.ACTION_USER_PRESENT)
                                ||(action.equals(Intent.ACTION_SCREEN_OFF)))||(action.equals(BreathKeyIntent))
                            ||(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))||(action.equals(Intent.ACTION_REBOOT))
                            ||(action.equals("android.intent.action.ACTION_PRE_SHUTDOWN"))) {
                        // do this in new thread, to avoid blocking the main thread with sleep and I/O		                 
                        doBreathLightInNewThread();
                    }

                }
            }
    };

    private void saveActionPara(String action, Intent intent) {
        synchronized(actionLock) {
            if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                mPhoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mBatteryStatus = intent.getIntExtra("status", 0);
                mPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                if (DEBUG) Slog.v(TAG, "mPlugged: " + mPlugged);
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                if(level <= 15){
                    mBatteryNotice = true;
                } else {
                    mBatteryNotice = false;
                }
                if (DEBUG) Slog.v(TAG, "level: " + level);
            } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                if (mPm == null) {
                    mPm = (PowerManager)myContext.getSystemService(Context.POWER_SERVICE);
                }
                if (mWindowManagerService == null) {
                    mWindowManagerService = (IWindowManager) ServiceManager.getService(Context.WINDOW_SERVICE);
                }
                isBootup = true;
            }
            mAction = action;
        }
    }


    private synchronized void doBreathLightInNewThread() {
        // handle Breath Light in the background to avoid blocking the main thread with I/O
        if(isRunning){
             return;
        }
        isRunning = true;
        new Thread() {
            @Override
                public void run() {
                    doBreathLight();
                    isRunning = false;
                }
        }.start();
    }

    private synchronized void doBreathLight() {

        String action;
        String PhoneState;
        int BatteryStatus;

        synchronized(actionLock) {
            action = mAction;
            PhoneState = mPhoneState;
            BatteryStatus = mBatteryStatus;
        }

        if ((action == null)||(PhoneState == null)) {
            return;
        }

        mInCall = PhoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) ||
            PhoneState.equals(TelephonyManager.EXTRA_STATE_RINGING);

        if (BatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
            chargering = true;
        } else {
            chargering = false;
        }
        if(mPlugged == BatteryManager.BATTERY_PLUGGED_AC || mPlugged == BatteryManager.BATTERY_PLUGGED_USB){
            isPlugged = true;
        } else {
            isPlugged = false;
        }
        if (DEBUG) Slog.v(TAG, "isPlugged: " + isPlugged);
        if(SystemProperties.getInt("persist.sys.bl_charge",1)==1){
            if (DEBUG) Slog.v(TAG, "charge: " + SystemProperties.getInt("persist.sys.bl_charge",0));
            isChargeSwitch = true; 
        } else {
            isChargeSwitch = false;
        }
        if(SystemProperties.getInt("persist.sys.bl_battery_low",1)==1){
            isBatteryLowSwitch = true;
        } else {
            isBatteryLowSwitch = false;
        }
        if(SystemProperties.getInt("persist.sys.bl_miss_evevt",1)==1){
            isMissEventSwitch = true;
        } else {
            isMissEventSwitch = false;
        }

        if (action.equals(Intent.ACTION_SCREEN_ON)) {
            if (screenState != screenUnlock) {
                screenState = screenOn;
            }

        } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)){


        } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
            screenState = screenUnlock;
            mNewMissCallCount = readMissCall(myContext);
            mLastMissCallCount  = mNewMissCallCount;
            mNewUnreadMsgCount = getUnreadMsg(myContext);
            mLastUnreadMsgCount  = mNewUnreadMsgCount;
            mMissLastCall = false;
            mUnreadLastMsg = false;

        } else if (action.equals(BreathKeyIntent)) {
            mLed = LED_MAX;  // make sure turn on the release key feature of breath light every time in unlock screen
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {


        } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            //myBreathlight.setBreathLightStateDiret(LED_OFF, -1);  // make light off first, wait 500ms
            //SystemClock.sleep(500);
            screenState = screenOff;
            mNewUnreadMsgCount = getUnreadMsg(myContext);
            if (mNewUnreadMsgCount > 0) {
                mUnreadLastMsg = true;
            } else {
                mUnreadLastMsg = false;
            }
            mNewMissCallCount= readMissCall(myContext);
            if (mNewMissCallCount > 0) {
                mMissLastCall = true;
            } else {
                mMissLastCall = false;
            }

        } else if (action.equals(Intent.ACTION_REBOOT)) {
            screenState = screenOff;
            SystemClock.sleep(1200);

        } else if (action.equals("android.intent.action.ACTION_PRE_SHUTDOWN")) {
            screenState = screenOff;
            final String shutdownAction = SystemProperties.get(
                    ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
            if (shutdownAction.charAt(0) == '1') {
                SystemClock.sleep(1200);
                action = Intent.ACTION_REBOOT;

            } else {
                //  SystemClock.sleep(400);
                action = "android.intent.action.ACTION_PRE_SHUTDOWN";

            }

        } else {

        }

        if ((action.equals(Intent.ACTION_SCREEN_ON))||(action.equals(Intent.ACTION_USER_PRESENT)
                    ||(action.equals(Intent.ACTION_SCREEN_OFF)))||(action.equals(BreathKeyIntent))
                ||(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))||(action.equals(Intent.ACTION_REBOOT))
                ||(action.equals("android.intent.action.ACTION_PRE_SHUTDOWN"))) {
            int led =  action2ledMap(action);
            if (mLed != led) {
                mLed = led;
                if (DEBUG) Slog.v(TAG, "setFlashing: " + mLed);
                myBreathlight.setFlashing(mLed, -1, -1, -1);
                if (mLed == LED_FREE_BUTTON) {
                    // make sure can do key feature every time
                    myBreathlight.setFlashing(LED_MAX, -1, -1, -1);
                }
            }
        }

    }

    private int action2ledMap(String action) {
        int led;
        String stemp;

        // rule 1 if screen state is in unlock state and lcd back light is on, dont breath just keep light on,  handle key breath feature
        // rule 2 if screen state is in lock state and  lcd back light is on, make the light breathing, dont handle key breath feature
        // rule 3 if screen state is in off state and phone is charging, or has unread last msg,  or miss last call, make the light breathing, dont handle key breath feature
        // rule 4 if screen state is in off state not in charging, not unread last msg, not miss last call, make the light off, dont handle key breath feature 
        // rule 5 if device reboot, make the light off anyway
        // rule 6 if in calling, dont breathing
        // rule 7 at device boot completed, check is there any miss calls and unread msgs, if it is exist, when screen off, make light breathing.

        if (action == null) {
            return LED_MAX;
        }

        if (isBootup) {
            if (mPm != null) {
                if (mPm.isScreenOn()) {
                    screenState = screenOn;
                } else {
                    screenState = screenOff;
                }
                if (DEBUG) Slog.v(TAG, "mPm.isScreenOn() screenState: " + screenState);
            }
            if (mWindowManagerService != null) {
                try {
                    if ((!mWindowManagerService.isKeyguardLocked())&&(screenState != screenOff)) {
                        screenState = screenUnlock;
                        if (DEBUG) Slog.v(TAG, "mWindowManagerService.isKeyguardLocked() screenState: " + screenState);
                    }
                } catch (RemoteException re) {
                    if (DEBUG) Slog.v(TAG, "mWindowManagerService RemoteException");
                    return LED_MAX;
                }

            }
        }

        if (DEBUG) Slog.v(TAG, "  action: " + action + "  screenState:" + screenState + "  chargering: " + chargering + "  mMissLastCall:" + mMissLastCall + "  mUnreadLastMsg:" + mUnreadLastMsg);
        if (DEBUG) Slog.v(TAG, "  mNewUnreadMsgCount: " + mNewUnreadMsgCount + "   mLastUnreadMsgCount :" + mLastUnreadMsgCount  + "  mNewMissCallCount" + mNewMissCallCount + "  mLastMissCallCount" + mLastMissCallCount);



        if (screenState == screenOn) {
            led = LED_AUTO_BREATH;
            if (mInCall) {
                led = LED_BRIGHT_STATUS;
            }
            if (action.equals(Intent.ACTION_REBOOT)) {
                led = LED_OFF;
            }

        } else if (screenState == screenUnlock) {
            if (action.equals(BreathKeyIntent)) {
                led = LED_FREE_BUTTON;
            } else {
                led = LED_BRIGHT_STATUS;
            }

            if (action.equals(Intent.ACTION_REBOOT)) {
                led = LED_OFF;
            }

        } else if (screenState == screenOff) {
            if (chargering && isChargeSwitch){
                 led = LED_AUTO_BREATH;
            } else if ((mMissLastCall || mUnreadLastMsg) && isMissEventSwitch) {
                 led = LED_AUTO_BREATH;
            } else if (mBatteryNotice && isBatteryLowSwitch){
                 led = LED_AUTO_BREATH;
            } else if(isPlugged && level==100){
                led = LED_BRIGHT_STATUS;
            } else{
                led = LED_OFF;
            }

            if (action.equals(Intent.ACTION_REBOOT)) {
                led = LED_OFF;
            }

        } else {
            led = LED_MAX;

        }

        return led;
    }

    private int getUnreadMsg(Context mContext) {
        return  (getNewSmsCount(mContext) + getNewMmsCount(mContext));
    }

    private int getNewSmsCount(Context mContext) {
        int result = 0;
        Cursor csr = mContext.getContentResolver().query(Uri.parse("content://sms"), null,
                "type = 1 and read = 0", null, null);
        if (csr != null) {
            result = csr.getCount();
            csr.close();
        }
        if (DEBUG) Slog.v(TAG, "getNewSmsCount result: " + result);
        return result;
    }

    private int getNewMmsCount(Context mContext) {
        int result = 0;
        Cursor csr = mContext.getContentResolver().query(Uri.parse("content://mms/inbox"),
                null, "read = 0", null, null);
        if (csr != null) {
            result = csr.getCount();
            csr.close();
        }
        if (DEBUG)  Slog.v(TAG, "getNewMmsCount result: " + result);
        return result;
    }

    private int readMissCall(Context mContext) {
        int result = 0;
        Cursor cursor = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] {
                Calls.TYPE
                }, " type=? and new=?", new String[] {
                Calls.MISSED_TYPE + "", "1"
                }, "date desc");

        if (cursor != null) {
            result = cursor.getCount();
            cursor.close();
        }
        if (DEBUG) Slog.v(TAG, "readMissCall result: " + result);
        return result;
    }  


}


// End of Vanzo:helianyi
