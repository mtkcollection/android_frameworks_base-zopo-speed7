package com.android.internal.policy.impl.smartwake;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.util.EncodingUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.util.DisplayMetrics;
import android.util.Log;

public class DrawGestureView extends View {

    private final int SHOW = 1;
    private final int DRAW = 2;
    private final int CLEAN = 3;

    public static final int GESTURE_O = 0;
    public static final int GESTURE_W = 1;
    public static final int GESTURE_C = 2;
    public static final int GESTURE_E = 3;
    public static final int GESTURE_M = 4;

    private Paint mPaint = null;
    private PaintFlagsDrawFilter mPaintFlags = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

    private Rect mDst = new Rect(0, 0, 300, 300);
    private Rect mSrc = new Rect(0, 0, 0, 0);
    private Rect mTempRect = new Rect();
    private Bitmap mBitmap = null;
    private int mRectIndex = -1;
    private Context mContext;

    private String listX;
    private String listY;

    public DrawGestureView(Context context) {
        super(context);

        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    public void showView(int gesture) {
        initBitmap(gesture);
        calcDstRect();

        mHandler.sendEmptyMessageDelayed(DRAW, 500);
    }

    private void calcDstRect() {
        listX = readPoint(0);
        listY = readPoint(1);
        if (listX != null && listY != null) {
            int minX = 1080;
            int minY = 1920;
            int maxX = 0;
            int maxY = 0;
            int tempPointX;
            int tempPointY;
            for (int i = 0; i < listX.split(" ").length - 2; i++){
                tempPointX = Integer.parseInt(listX.split(" ")[i]);
                tempPointY = Integer.parseInt(listY.split(" ")[i]);
                minX = minX > tempPointX ? tempPointX : minX;
                minY = minY > tempPointY ? tempPointY : minY;
                maxX = maxX < tempPointX ? tempPointX : maxX;
                maxY = maxY < tempPointY ? tempPointY : maxY;
            }
            mDst.left = minX;
            mDst.top = minY;
            mDst.right = maxX;
            mDst.bottom = maxY;
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels;
            int height = dm.heightPixels;
            int drawSize = (int)(height / 16) * 5;
            mDst.left = (width - drawSize) / 2;
            mDst.top = (height - drawSize) / 2;
            mDst.right = mDst.left + drawSize;
            mDst.bottom = mDst.top + drawSize;
        }
    }

    private void initBitmap (int gesture) {
        switch (gesture) {
            case GESTURE_O:
                mBitmap = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.smart_wake_o);
                break;
            case GESTURE_W:
                mBitmap = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.smart_wake_w);
                break;
            case GESTURE_E:
                mBitmap = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.smart_wake_e);
                break;
            case GESTURE_M:
                mBitmap = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.smart_wake_m);
                break;
            case GESTURE_C:
                mBitmap = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.smart_wake_c);
                break;
        }
    }

    private Rect getClipRect (int paramInt, Rect paramRect) {
        int i = paramInt % 4;
        int j = paramInt / 4;
        paramRect.left = (i * 300);
        paramRect.top = (j * 300);
        paramRect.right = (paramRect.left + 300);
        paramRect.bottom = (paramRect.top + 300);
        return paramRect;
    }

    private void recycle() {
        if ((mBitmap != null) && (!mBitmap.isRecycled())) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DRAW:
                        mRectIndex++;
                        if (mRectIndex < 20) {
                            invalidate();
                            sendEmptyMessageDelayed(DRAW, 50);
                        } else {
                            invalidate();
                            removeMessages(DRAW);
                            sendEmptyMessageDelayed(CLEAN, 100);
                            mListener.OnGestureDrawDone();
                            mRectIndex = -1;
                        }
                        break;

                    case SHOW:
                        break;

                    case CLEAN:
                        recycle();
                        invalidate();
                        break;
                }
            }
    };

    @Override
    public void onDraw(Canvas canvas) {
        canvas.setDrawFilter(mPaintFlags);
        canvas.drawColor(Color.BLACK);
        if (mBitmap != null) {
            mSrc = getClipRect(mRectIndex, mTempRect);
            canvas.drawBitmap(mBitmap, mSrc, mDst, mPaint);
        }
    }

    public interface OnGestureDrawDoneListener {
        public void OnGestureDrawDone();
    }

    OnGestureDrawDoneListener mListener;

    public void setOnGestureDrawDoneListener(OnGestureDrawDoneListener listener) {
        mListener = listener;
    }

    private String readPoint (int xy) {
        try {
            String fileString = WindowGestureManager.getGestureFile();
            if (fileString == null) {
                return null;
            }

            File points;
            if (xy == 0) {
                points = new File(fileString + "coordinate_x");
            } else {
                points = new File(fileString + "coordinate_y");
            }

            if (points.exists()) {
                FileInputStream fis = new FileInputStream(points);
                int length = fis.available();
                byte [] buffer = new byte[length];
                fis.read(buffer);

                String res = EncodingUtils.getString(buffer, "UTF-8");

                fis.close();
                return res;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't open gesture device", e);
        }
    }

}
