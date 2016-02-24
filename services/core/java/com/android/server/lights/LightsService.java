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

package com.android.server.lights;

import com.android.server.SystemService;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IHardwareService;
import android.os.Message;
import android.os.Trace;
import android.util.Slog;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/* Vanzo:yujianpeng on: Mon, 22 Jun 2015 10:39:26 +0800
 *  add status light menu
 */
import android.os.SystemProperties;
// End of Vanzo:yujianpeng
/* Vanzo:luanshijun on: Thu, 23 Apr 2015 14:49:31 +0800
 * add breathlight support
 */
import com.android.server.lights.BreathLightsDev;
// End of Vanzo:luanshijun

public class LightsService extends SystemService {
    static final String TAG = "LightsService";
    static final boolean DEBUG = false;

    final LightImpl mLights[] = new LightImpl[LightsManager.LIGHT_ID_COUNT];

/* Vanzo:luanshijun on: Sat, 25 Apr 2015 15:54:51 +0800
 * add breathlight support
 */
    public final class LightImpl extends Light {
// End of Vanzo:luanshijun

        private LightImpl(int id) {
            mId = id;
        }

        @Override
        public void setBrightness(int brightness) {
            setBrightness(brightness, BRIGHTNESS_MODE_USER);
        }

        @Override
        public void setBrightness(int brightness, int brightnessMode) {
            synchronized (this) {
                int color = brightness & 0x000000ff;
                color = 0xff000000 | (color << 16) | (color << 8) | color;
                setLightLocked(color, LIGHT_FLASH_NONE, 0, 0, brightnessMode);
            }
        }

        @Override
        public void setColor(int color) {
            synchronized (this) {
                setLightLocked(color, LIGHT_FLASH_NONE, 0, 0, 0);
            }
        }

        @Override
        public void setFlashing(int color, int mode, int onMS, int offMS) {
            synchronized (this) {
                setLightLocked(color, mode, onMS, offMS, BRIGHTNESS_MODE_USER);
            }
        }

        @Override
        public void pulse() {
            pulse(0x00ffffff, 7);
        }

        @Override
        public void pulse(int color, int onMS) {
            synchronized (this) {
                if (mColor == 0 && !mFlashing) {
                    setLightLocked(color, LIGHT_FLASH_HARDWARE, onMS, 1000, BRIGHTNESS_MODE_USER);
                    mColor = 0;
                    mH.sendMessageDelayed(Message.obtain(mH, 1, this), onMS);
                }
            }
        }

        @Override
        public void turnOff() {
            synchronized (this) {
                setLightLocked(0, LIGHT_FLASH_NONE, 0, 0, 0);
            }
        }

        private void stopFlashing() {
            synchronized (this) {
                setLightLocked(mColor, LIGHT_FLASH_NONE, 0, 0, BRIGHTNESS_MODE_USER);
            }
        }

        private void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
/* Vanzo:yujianpeng on: Mon, 22 Jun 2015 10:40:13 +0800
 * add status light meune
            if (color != mColor || mode != mMode || onMS != mOnMS || offMS != mOffMS) {
                if (DEBUG) Slog.v(TAG, "setLight #" + mId + ": color=#"
                        + Integer.toHexString(color));
                mColor = color;
                mMode = mode;
                mOnMS = onMS;
                mOffMS = offMS;
                Trace.traceBegin(Trace.TRACE_TAG_POWER, "setLight(" + mId + ", " + color + ")");
                try {
                    setLight_native(mNativePointer, mId, color, mode, onMS, offMS, brightnessMode);
                } finally {
                    Trace.traceEnd(Trace.TRACE_TAG_POWER);
                }
            }
 */
            if(com.android.featureoption.FeatureOption.VANZO_FEATURE_ADD_STATUS_LIGHT_MENU){
                if (SystemProperties.getBoolean("persist.sys.status_light", true) == false) {
                    if (mId == LightsManager.LIGHT_ID_BATTERY || mId == LightsManager.LIGHT_ID_NOTIFICATIONS || mId == LightsManager.LIGHT_ID_ATTENTION) {
                        setLight_native(mNativePointer, mId, 0, LIGHT_FLASH_NONE, 0, 0, 0);
                        return;
                    }
                }
                if (color != mColor || mode != mMode || onMS != mOnMS || offMS != mOffMS
                  || SystemProperties.getBoolean("persist.sys.status_light", true)) {
                    if (DEBUG) Slog.v(TAG, "setLight #" + mId + ": color=#"
                            + Integer.toHexString(color));
                    mColor = color;
                    mMode = mode;
                    mOnMS = onMS;
                    mOffMS = offMS;
                    Trace.traceBegin(Trace.TRACE_TAG_POWER, "setLight(" + mId + ", " + color + ")");
                    try {
                        setLight_native(mNativePointer, mId, color, mode, onMS, offMS, brightnessMode);
                    } finally {
                        Trace.traceEnd(Trace.TRACE_TAG_POWER);
                    }
                }
            } else if(color != mColor || mode != mMode || onMS != mOnMS || offMS != mOffMS) {
                    if (DEBUG) Slog.v(TAG, "setLight #" + mId + ": color=#"
                            + Integer.toHexString(color));
                    mColor = color;
                    mMode = mode;
                    mOnMS = onMS;
                    mOffMS = offMS;
                    Trace.traceBegin(Trace.TRACE_TAG_POWER, "setLight(" + mId + ", " + color + ")");
                    try {
                        setLight_native(mNativePointer, mId, color, mode, onMS, offMS, brightnessMode);
                    } finally {
                        Trace.traceEnd(Trace.TRACE_TAG_POWER);
                    }
            }
// End of Vanzo:yujianpeng
        }

        private int mId;
        private int mColor;
        private int mMode;
        private int mOnMS;
        private int mOffMS;
        private boolean mFlashing;
    }

    /* This class implements an obsolete API that was removed after eclair and re-added during the
     * final moments of the froyo release to support flashlight apps that had been using the private
     * IHardwareService API. This is expected to go away in the next release.
     */
    private final IHardwareService.Stub mLegacyFlashlightHack = new IHardwareService.Stub() {

        private static final String FLASHLIGHT_FILE = "/sys/class/leds/spotlight/brightness";

        public boolean getFlashlightEnabled() {
            try {
                FileInputStream fis = new FileInputStream(FLASHLIGHT_FILE);
                int result = fis.read();
                fis.close();
                return (result != '0');
            } catch (Exception e) {
                return false;
            }
        }

        public void setFlashlightEnabled(boolean on) {
            final Context context = getContext();
            if (context.checkCallingOrSelfPermission(android.Manifest.permission.FLASHLIGHT)
                    != PackageManager.PERMISSION_GRANTED &&
                    context.checkCallingOrSelfPermission(android.Manifest.permission.HARDWARE_TEST)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires FLASHLIGHT or HARDWARE_TEST permission");
            }
            try {
                FileOutputStream fos = new FileOutputStream(FLASHLIGHT_FILE);
                byte[] bytes = new byte[2];
                bytes[0] = (byte)(on ? '1' : '0');
                bytes[1] = '\n';
                fos.write(bytes);
                fos.close();
            } catch (Exception e) {
                // fail silently
            }
        }
    };

    public LightsService(Context context) {
        super(context);

/* Vanzo:luanshijun on: Sat, 25 Apr 2015 15:53:21 +0800
 * add breathlight support
 */
        mContext = context;
// End of Vanzo:luanshijun
        mNativePointer = init_native();

        for (int i = 0; i < LightsManager.LIGHT_ID_COUNT; i++) {
            mLights[i] = new LightImpl(i);
        }

/* Vanzo:luanshijun on: Thu, 23 Apr 2015 14:51:38 +0800
 * add breathlight support
 */
        if (com.android.featureoption.FeatureOption.VANZO_FEATURE_BREATH_LIGHT_SUPPORT) {
            new BreathLightsDev( mContext, TAG, mLights[LightsManager.LIGHT_ID_BREATH]);
        }
// End of Vanzo:luanshijun
    }

    @Override
    public void onStart() {
        publishBinderService("hardware", mLegacyFlashlightHack);
        publishLocalService(LightsManager.class, mService);
    }

    private final LightsManager mService = new LightsManager() {
        @Override
        public com.android.server.lights.Light getLight(int id) {
            if (id < LIGHT_ID_COUNT) {
                return mLights[id];
            } else {
                return null;
            }
        }
    };

    @Override
    protected void finalize() throws Throwable {
        finalize_native(mNativePointer);
        super.finalize();
    }

    private Handler mH = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            LightImpl light = (LightImpl)msg.obj;
            light.stopFlashing();
        }
    };

    private static native long init_native();
    private static native void finalize_native(long ptr);

/* Vanzo:luanshijun on: Thu, 23 Apr 2015 20:34:12 +0800
 * add breathlight support
 */
    private final Context mContext;
// End of Vanzo:luanshijun
    static native void setLight_native(long ptr, int light, int color, int mode,
            int onMS, int offMS, int brightnessMode);

    private long mNativePointer;
}
