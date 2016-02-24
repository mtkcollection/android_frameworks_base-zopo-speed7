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
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;
import com.android.systemui.settings.CurrentUserTracker;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback;

/** Quick settings tile: TimeOut **/
public class TimeOutTile extends QSTile<QSTile.BooleanState> {
    private CurrentUserTracker mUserTracker;
    public static final int MINIMUM_TIMEOUT = 15000;
    public static final int MEDIUM_TIMEOUT = 30000;
    public static final int MAXIMUM_TIMEOUT = 60000;

    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    public TimeOutTile(Host host, CurrentUserTracker currentUserTracker) {
        super(host);
        mUserTracker = currentUserTracker;
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }
    @Override
    public void setListening(boolean listening) {}

    public void toggleState() {
        toggleTimeout();
    }
    private void toggleTimeout() {
        try {
            ContentResolver cr = mContext.getContentResolver();
            int timeout = Settings.System.getIntForUser(cr, SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE, mUserTracker.getCurrentUserId());
            if (timeout <= MINIMUM_TIMEOUT) {
                timeout = MEDIUM_TIMEOUT;
            } else if (timeout <= MEDIUM_TIMEOUT) {
                timeout = MAXIMUM_TIMEOUT;
            } else {
                timeout = MINIMUM_TIMEOUT;
            }
            Settings.System.putIntForUser(cr, Settings.System.SCREEN_OFF_TIMEOUT, timeout, mUserTracker.getCurrentUserId());
            refreshState();
        } catch (Exception e) {
        }
    }

    private int getTimeout() {
        try {
            int timeout = Settings.System.getIntForUser(mContext.getContentResolver(), SCREEN_OFF_TIMEOUT,
                    FALLBACK_SCREEN_TIMEOUT_VALUE, mUserTracker.getCurrentUserId());
            if (timeout <= MINIMUM_TIMEOUT) {
                timeout = MINIMUM_TIMEOUT;
            } else if (timeout <= MEDIUM_TIMEOUT) {
                timeout = MEDIUM_TIMEOUT;
            } else {
                timeout = MAXIMUM_TIMEOUT;
            }
            return timeout;
        } catch (Exception e) {
            android.util.Log.i("yinjun", "e==="+e);
        }
        return MEDIUM_TIMEOUT;
    }
    @Override
    protected void handleClick() {
        toggleTimeout();
    }
    @Override
    protected void handleLongClick() {
        Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mHost.startSettingsActivity(intent);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final Resources res = mContext.getResources();
        int brightness = getTimeout();
        android.util.Log.i("yinjun", "handleUpdateState============"+brightness);
        switch (brightness) {
            case MINIMUM_TIMEOUT:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_light_low);
                break;
            case MEDIUM_TIMEOUT:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_light_middle);
                break;
            case MAXIMUM_TIMEOUT:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_light_fully);
                break;
            default:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_light_fully);
                break;
        }
        state.visible = true;
        state.label = res.getString(R.string.timeout);
    }
}
