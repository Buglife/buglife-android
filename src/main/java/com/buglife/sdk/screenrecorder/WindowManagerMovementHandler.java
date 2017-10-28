package com.buglife.sdk.screenrecorder;

import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

class WindowManagerMovementHandler {
    private final View mView;
    private final WindowManager mWindowManager;

    private float mInitialTouchX = 0;
    private float mInitialX = 0;
    private float mInitialTouchY = 0;
    private float mInitialY = 0;
    private boolean mMoved = false;
    @Nullable private MovementCallback mCallback;

    WindowManagerMovementHandler(View view, WindowManager windowManager) {
        mView = view;
        mWindowManager = windowManager;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mInitialTouchX = event.getRawX();
                mInitialTouchY = event.getRawY();

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
                mInitialX = params.x;
                mInitialY = params.y;
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                float currentTouchX = event.getRawX();
                float currentTouchY = event.getRawY();
                float deltaTouchX = currentTouchX - mInitialTouchX;
                float deltaTouchY = currentTouchY - mInitialTouchY;

                int x = (int) (mInitialX + deltaTouchX);
                int y = (int) (mInitialY + deltaTouchY);

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
                params.x = x;
                params.y = y;
                mWindowManager.updateViewLayout(mView, params);
                mMoved = true;

                if (mCallback != null) {
                    mCallback.onMove(mView, x, y);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (mMoved) {
                    mMoved = false;
                    mView.setPressed(false);
                    return true;
                }
            }
            default: return false;
        }
    }

    public void setMovementCallback(@Nullable MovementCallback callback) {
        mCallback = callback;
    }

    public interface MovementCallback {
        void onMove(View view, int x, int y);
    }
}
