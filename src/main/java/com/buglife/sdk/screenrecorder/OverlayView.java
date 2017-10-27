package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.buglife.sdk.R;
import com.buglife.sdk.ViewUtils;

@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
final class OverlayView extends FrameLayout {
    private static final int ANIMATION_DURATION = 300;

    private ImageButton mStopButton;
    private final @NonNull OverlayViewClickListener mListener;
    private final WindowManager mWindowManager;
    private final DisplayMetrics mDisplayMetrics;
    private float mInitialTouchX = 0;
    private float mInitialX = 0;
    private float mInitialTouchY = 0;
    private float mInitialY = 0;

    public OverlayView(@NonNull Context context, @NonNull OverlayViewClickListener listener) {
        super(context);
        mListener = listener;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = getResources().getDisplayMetrics();
        inflate(context, R.layout.overlay_view, this);
        setUpView();
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouchX = event.getRawX();
                mInitialTouchY = event.getRawY();

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
                mInitialX = params.x;
                mInitialY = params.y;
                return false;
            case MotionEvent.ACTION_MOVE:
                return true;
            default: return super.onInterceptTouchEvent(event);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                float currentTouchX = event.getRawX();
                float currentTouchY = event.getRawY();
                float deltaTouchX = currentTouchX - mInitialTouchX;
                float deltaTouchY = currentTouchY - mInitialTouchY;

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
                params.x = (int) (mInitialX + deltaTouchX);
                params.y = (int) (mInitialY + deltaTouchY);

                int screenLowerBound = 0;
                int screenUpperBound = mDisplayMetrics.widthPixels;
                if (params.x <= screenLowerBound) {
                    params.x = screenLowerBound - (getWidth() / 2);
                    mStopButton.setEnabled(false);
                } else if (params.x >= screenUpperBound - getWidth()) {
                    params.x = screenUpperBound - (getWidth() / 2);
                    mStopButton.setEnabled(false);
                } else {
                    mStopButton.setEnabled(true);
                }

                mWindowManager.updateViewLayout(this, params);
                return true;
        }
        return super.onTouchEvent(event);
    }

    public ImageButton getStopButton() {
        return (ImageButton) findViewById(R.id.stop_button);
    }

    private void setUpView() {
        int padding = (int) ViewUtils.dpToPx(8, getResources());
        setPadding(padding, padding, padding, padding);

        mStopButton = (ImageButton) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onStopButtonClicked();
            }
        });
    }

    interface OverlayViewClickListener {
        void onResize();
        void onStopButtonClicked();
    }
}
