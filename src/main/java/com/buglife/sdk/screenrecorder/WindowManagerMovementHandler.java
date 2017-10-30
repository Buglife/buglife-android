package com.buglife.sdk.screenrecorder;

import android.graphics.Rect;
import android.support.animation.FlingAnimation;
import android.support.animation.FloatPropertyCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.buglife.sdk.MathUtils;

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
    private Rect mMovementBounds = new Rect();
    private boolean mMoved = false;

    private FloatPropertyCompat LAYOUT_PARAMS_X = new FloatPropertyCompat<View>("layoutParamsX") {
        @Override public float getValue(View object) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            return lp.x;
        }

        @Override public void setValue(View object, float value) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            move(object, (int) value, lp.y);
        }
    };

    private FloatPropertyCompat LAYOUT_PARAMS_Y = new FloatPropertyCompat<View>("layoutParamsY") {
        @Override public float getValue(View object) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            return lp.y;
        }

        @Override public void setValue(View object, float value) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) object.getLayoutParams();
            move(object, lp.x, (int) value);
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
    }

    void setBounds(int left, int top, int right, int bottom) {
        mMovementBounds.set(left, top, right, bottom);
        mFlingAnimationX.setMinValue(mMovementBounds.left);
        mFlingAnimationX.setMaxValue(mMovementBounds.right);
        mFlingAnimationY.setMinValue(mMovementBounds.top);
        mFlingAnimationY.setMaxValue(mMovementBounds.bottom);
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

                    // Handle edge bounds
                    int movementLowerHorizontalBound = mMovementBounds.left;
                    int movementUpperHorizontalBound = mMovementBounds.right;
                    int movementLowerVerticalBound = mMovementBounds.top;
                    int movementUpperVerticalBound = mMovementBounds.bottom;
                    if (x <= movementLowerHorizontalBound || x >= movementUpperHorizontalBound) {
                        x = MathUtils.closest(movementLowerHorizontalBound, movementUpperHorizontalBound, x);
                    }

                    if (y <= movementLowerVerticalBound || y >= movementUpperVerticalBound) {
                        y = MathUtils.closest(movementLowerVerticalBound, movementUpperVerticalBound, y);
                    }

                    move(mView, x, y);
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

    private void move(View view, int x, int y) {
        onMove(x, y);
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
        params.x = x;
        params.y = y;
        mWindowManager.updateViewLayout(view, params);
    }

    private void onMove(int x, int y) {
        int movementLowerHorizontalBound = mMovementBounds.left;
        int movementUpperHorizontalBound = mMovementBounds.right;
        if (x <= movementLowerHorizontalBound || x >= movementUpperHorizontalBound) {
            mView.setEnabled(false);
        } else {
            mView.setEnabled(true);
        }
    }
}
