/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.os.SystemClock;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.FlashlightController;
/* Vanzo:yucheng on: Wed, 15 Apr 2015 18:06:19 +0800
 *  Optiomiztion for camera, flashlight must been closed before opening camera
 */
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
// End of Vanzo: yucheng

/** Quick settings tile: Control flashlight **/
public class FlashlightTile extends QSTile<QSTile.BooleanState> implements
        FlashlightController.FlashlightListener {

    /** Grace period for which we consider the flashlight
     * still available because it was recently on. */
    /// M:Fix [ALPS01838137] Modify timeout to 3000 
    private static final long RECENTLY_ON_DURATION_MILLIS = 3000;
    //private static final long RECENTLY_ON_DURATION_MILLIS = 500;

    private final AnimationIcon mEnable
            = new AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation);
    private final AnimationIcon mDisable
            = new AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation);
    private final FlashlightController mFlashlightController;
    private long mWasLastOn;
/* Vanzo:yucheng on: Wed, 15 Apr 2015 18:10:32 +0800
 *  Optiomiztion for camera, flashlight must been closed before opening camera
 */
    private static final String ATCTION_CLOSE_FLASH_LIGHT = "com.android.systemui.qs.tiles.close.flashlight";
    private static final String TAG = "FLASHLIGHT";
    private static final boolean DBG = true;
    private BroadcastReceiver mFlashLightReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        if(DBG) android.util.Log.d(TAG, "intent:" + intent + ", flash stat:" + (mState.value?"Open":"Close"));

        if (intent.getAction().equals(ATCTION_CLOSE_FLASH_LIGHT) && (mState.value==true)) {
                 mFlashlightController.setFlashlight(false);
                 refreshState(UserBoolean.USER_FALSE);
            }
        }
    };
// End of Vanzo: yucheng

    public FlashlightTile(Host host) {
        super(host);
        mFlashlightController = host.getFlashlightController();
        mFlashlightController.addListener(this);
/* Vanzo:yucheng on: Wed, 15 Apr 2015 20:18:37 +0800
 *  Optiomiztion for camera, flashlight must been closed before opening camera
 */
        if(DBG) android.util.Log.d(TAG, "registerReceiver --> mFlashLightReceiver");
        mContext.registerReceiver(mFlashLightReceiver, new IntentFilter(ATCTION_CLOSE_FLASH_LIGHT));
// End of Vanzo: yucheng
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mFlashlightController.removeListener(this);
/* Vanzo:yucheng on: Wed, 15 Apr 2015 20:19:20 +0800
 *  Optiomiztion for camera, flashlight must been closed before opening camera
 */
        if(DBG) android.util.Log.d(TAG, "unregisterReceiver <-- mFlashLightReceiver");
        mContext.unregisterReceiver(mFlashLightReceiver);
// End of Vanzo: yucheng
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    protected void handleClick() {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
        boolean newState = !mState.value;
        mFlashlightController.setFlashlight(newState);
        refreshState(newState ? UserBoolean.USER_TRUE : UserBoolean.USER_FALSE);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (state.value) {
            mWasLastOn = SystemClock.uptimeMillis();
        }

        if (arg instanceof UserBoolean) {
            state.value = ((UserBoolean) arg).value;
        }

        if (!state.value && mWasLastOn != 0) {
            if (SystemClock.uptimeMillis() > mWasLastOn + RECENTLY_ON_DURATION_MILLIS) {
                mWasLastOn = 0;
            } else {
                mHandler.removeCallbacks(mRecentlyOnTimeout);
                mHandler.postAtTime(mRecentlyOnTimeout, mWasLastOn + RECENTLY_ON_DURATION_MILLIS);
            }
        }

        // Always show the tile when the flashlight is or was recently on. This is needed because
        // the camera is not available while it is being used for the flashlight.
        state.visible = mWasLastOn != 0 || mFlashlightController.isAvailable();
        state.label = mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        final AnimationIcon icon = state.value ? mEnable : mDisable;
        icon.setAllowAnimation(arg instanceof UserBoolean && ((UserBoolean) arg).userInitiated);
        state.icon = icon;
        int onOrOffId = state.value
                ? R.string.accessibility_quick_settings_flashlight_on
                : R.string.accessibility_quick_settings_flashlight_off;
        state.contentDescription = mContext.getString(onOrOffId);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
        }
    }

    @Override
    public void onFlashlightOff() {
        refreshState(UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightError() {
        refreshState(UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }

    private Runnable mRecentlyOnTimeout = new Runnable() {
        @Override
        public void run() {
            refreshState();
        }
    };
}
