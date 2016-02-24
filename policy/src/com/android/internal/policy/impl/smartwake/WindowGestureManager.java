package com.android.internal.policy.impl.smartwake;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import com.android.featureoption.FeatureOption;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WindowGestureManager {

    private final static String TAG = "WindowGestureManager";

    private WindowGestureView mWindowGestureView;

    private Context mContext;
    static private String mGestureFileString;
    public WindowGestureManager(Context context) {
        mContext = context;
        Log.i(TAG, "init   -----WindowGestureManager----");

        mGestureFileString = searchGestureFile(context);
    }

    private String searchGestureFile(Context context) {
        String[] files = context.getResources().getStringArray(com.android.internal.R.array.smart_wake_file);
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i] + "gesture");
            if (file.exists())
                return files[i];
        }
        return null;
    }

    static public String getGestureFile() {
        return mGestureFileString;
    }

    public void show(int gesture) {
        mWindowGestureView.showView(gesture);
    }

    public void hide() {
        mWindowGestureView.hideView();
    }
    public void initGestureView(Context context) {

        mWindowGestureView = new WindowGestureView(context);
    }

    public void onSystemReady() {
        Log.i(TAG, "-----WindowGestureManager  onSystemReady----");
        initGestureView(mContext);

        if (FeatureOption.VANZO_TOUCHPANEL_GESTURES_SUPPORT) {
            if (SystemProperties.getInt("persist.sys.smartwake_switch", 0) == 1) {
                writeOffScreenGestureState(1);
            } else {
                writeOffScreenGestureState(0);
            }
        }
    }

    private void writeOffScreenGestureState(int value) {
        try {
            if (mGestureFileString == null) {
                return;
            }
            File file = new File(mGestureFileString + "gesture");
            if (file.exists()) {
                FileWriter mWriter = new FileWriter(file);
                mWriter.write(String.valueOf(value));
                mWriter.flush();
                mWriter.close();
                mWriter = null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't open gesture device", e);
        }
    }
}
