package com.buglife.sdk.screenrecorder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.buglife.sdk.R;
import com.buglife.sdk.ViewUtils;

public class ScreenRecordButton extends AppCompatImageButton {
    private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRingBounds = new RectF();
    final AnimatorSet mInAnimator = new AnimatorSet();
    final AnimatorSet mOutAnimator = new AnimatorSet();
    private WindowManagerMovementHandler mMovementHandler;
    private WindowManager mWindowManager;
    private DisplayMetrics mDisplayMetrics;

    private ValueAnimator mRingAnimator;
    float mCurrentRingAngle = 360;

    public ScreenRecordButton(Context context) {
        this(context, null);
    }

    public ScreenRecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = getResources().getDisplayMetrics();

        int ringColor = Color.parseColor("#66FFFFFF");
        mRingPaint.setColor(ringColor);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(ViewUtils.dpToPx(3, getResources()));
        mRingAnimator = ValueAnimator.ofFloat(360, 0);
        mRingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentRingAngle = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        setScaleType(ScaleType.CENTER_INSIDE);
        setImageResource(R.drawable.ic_stop_white_24dp);
        setBackgroundResource(R.drawable.bg_circle);
        int backgroundColor = Color.parseColor("#F44336");
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(backgroundColor));
        ViewCompat.setElevation(this, ViewUtils.dpToPx(4, getResources()));

        ObjectAnimator inAnimationX = ObjectAnimator.ofFloat(this, View.SCALE_X, 0, 1);
        ObjectAnimator inAnimationY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0, 1);
        ObjectAnimator outAnimationX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1, 0);
        ObjectAnimator outAnimationY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1, 0);
        mInAnimator.playTogether(inAnimationX, inAnimationY);
        mOutAnimator.playTogether(outAnimationX, outAnimationY);

        mMovementHandler = new WindowManagerMovementHandler(this, mWindowManager);
    }

    public void setCountdownDuration(long duration) {
        mRingAnimator.setDuration(duration);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        boolean handled = mMovementHandler.onTouchEvent(event);
        return handled || super.onTouchEvent(event);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) ViewUtils.dpToPx(52, getResources());
        setMeasuredDimension(size, size);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Set the bounds of the ring to sit nicely inside of the button
        float inset = mRingPaint.getStrokeWidth() / 2;
        mRingBounds.set(
                inset,
                inset,
                getMeasuredWidth() - inset,
                getMeasuredHeight() - inset
        );
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mInAnimator.start();
        mRingAnimator.start();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMovementHandler.recycle();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(mRingBounds, -90, mCurrentRingAngle, false, mRingPaint);
    }

    public void hide(@Nullable final HideCallback callback) {
        setEnabled(false);
        mOutAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                mOutAnimator.removeListener(this);
                if (callback != null) {
                    callback.onViewHidden();
                }
            }
        });
        mRingAnimator.cancel();
        mOutAnimator.setStartDelay(400);
        mOutAnimator.start();
    }

    public interface HideCallback {
        void onViewHidden();
    }
}
