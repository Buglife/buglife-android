package com.buglife.sdk.screenrecorder;

import android.graphics.Rect;
import android.support.animation.FlingAnimation;
import android.support.animation.FloatPropertyCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.buglife.sdk.MathUtils;
import com.buglife.sdk.ViewUtils;

class WindowManagerMovementHandler {
    private static final int UNIT_PX_PER_SEC = 1000;
    private final View mView;
    final WindowManager mWindowManager;
    private final VelocityTracker mVelocityTracker;
    private final FlingAnimation mFlingAnimationX;
    private final FlingAnimation mFlingAnimationY;

    private int mMinTouchSlop;
    private int mMinFlingVelocity;
    private float mInitialTouchX = 0;
    private float mInitialX = 0;
    private float mInitialTouchY = 0;
    private float mInitialY = 0;
    private Rect mScreenBounds = new Rect();
    private boolean mMoved = false;

    private FloatPropertyCompat LAYOUT_PARAMS_X = new FloatPropertyCompat<View>("layoutParamsX") {
        @Override public float getValue(View object) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            return lp.x;
        }

        @Override public void setValue(View object, float value) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            lp.x = (int) value;
            mWindowManager.updateViewLayout(object, lp);
        }
    };

    private FloatPropertyCompat LAYOUT_PARAMS_Y = new FloatPropertyCompat<View>("layoutParamsY") {
        @Override public float getValue(View object) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            return lp.y;
        }

        @Override public void setValue(View object, float value) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            lp.y = (int) value;
            mWindowManager.updateViewLayout(object, lp);
        }
    };

    WindowManagerMovementHandler(View view, WindowManager windowManager) {
        mView = view;
        mWindowManager = windowManager;
        mVelocityTracker = VelocityTracker.obtain();
        mFlingAnimationX = new FlingAnimation(mView, LAYOUT_PARAMS_X);
        mFlingAnimationY = new FlingAnimation(mView, LAYOUT_PARAMS_Y);

        ViewConfiguration viewConfig = ViewConfiguration.get(view.getContext());
        mMinTouchSlop = viewConfig.getScaledTouchSlop();
        mMinFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();

        DisplayMetrics dm = view.getResources().getDisplayMetrics();
        int top = 0;
        int left = 0;
        int right = dm.widthPixels;
        int bottom = dm.heightPixels - ViewUtils.navigationBarHeight(view.getResources()) - ViewUtils.statusBarHeight(view.getResources());
        mScreenBounds.set(left, top, right, bottom);
    }

    boolean onTouchEvent(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (mFlingAnimationX.isRunning()) {
                    mFlingAnimationX.cancel();
                }
                if (mFlingAnimationY.isRunning()) {
                    mFlingAnimationY.cancel();
                }
                mVelocityTracker.clear();
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

                if (Math.abs(deltaTouchX) >= mMinTouchSlop || Math.abs(deltaTouchY) >= mMinTouchSlop) {
                    mMoved = true;
                    int x = (int) (mInitialX + deltaTouchX);
                    int y = (int) (mInitialY + deltaTouchY);

                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
                    params.x = x;
                    params.y = y;

                    // Handle edge bounds
                    int screenLowerHorizontalBound = mScreenBounds.left - (mView.getWidth() / 2);
                    int screenUpperHorizontalBound = mScreenBounds.right - (mView.getWidth() / 2);
                    int screenLowerVerticalBound = mScreenBounds.top;
                    int screenUpperVerticalBound = mScreenBounds.bottom;
                    if (x <= screenLowerHorizontalBound || x >= screenUpperHorizontalBound) {
                        params.x = MathUtils.closest(screenLowerHorizontalBound, screenUpperHorizontalBound, x);
                        mView.setEnabled(false);
                    } else {
                        mView.setEnabled(true);
                    }

                    if (y <= screenLowerVerticalBound || y >= screenUpperVerticalBound) {
                        params.y = MathUtils.closest(screenLowerVerticalBound, screenUpperVerticalBound, y);
                    }

                    mWindowManager.updateViewLayout(mView, params);
                    return true;
                }

                return false;
            }
            case MotionEvent.ACTION_UP: {
                if (mMoved) {
                    mVelocityTracker.computeCurrentVelocity(UNIT_PX_PER_SEC);
                    float currentVelocityX = mVelocityTracker.getXVelocity();
                    float currentVelocityY = mVelocityTracker.getYVelocity();
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
                    if (Math.abs(currentVelocityX) >= mMinFlingVelocity || Math.abs(currentVelocityY) >= mMinFlingVelocity) {
                        mFlingAnimationX.setMinValue(mScreenBounds.left - (mView.getWidth() / 2));
                        mFlingAnimationX.setMaxValue(mScreenBounds.right - (mView.getWidth() / 2));
                        mFlingAnimationY.setMinValue(mScreenBounds.top);
                        mFlingAnimationY.setMaxValue(mScreenBounds.bottom);
                        mFlingAnimationX.setStartValue(params.x);
                        mFlingAnimationY.setStartValue(params.y);
                        mFlingAnimationX.setStartVelocity(currentVelocityX);
                        mFlingAnimationY.setStartVelocity(currentVelocityY);
                        mFlingAnimationX.start();
                        mFlingAnimationY.start();
                    }
                    mMoved = false;
                    mView.setPressed(false);
                    return true;
                }
            }
            default: return false;
        }
    }

    void recycle() {
        mVelocityTracker.recycle();
    }
}
